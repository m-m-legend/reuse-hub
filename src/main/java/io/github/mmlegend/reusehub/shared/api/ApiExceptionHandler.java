package io.github.mmlegend.reusehub.shared.api;

import io.github.mmlegend.reusehub.shared.domain.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ProblemDetail> business(BusinessException exception, HttpServletRequest request) {
        HttpStatus status = switch (exception.kind()) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case CONFLICT -> HttpStatus.CONFLICT;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case INVALID -> HttpStatus.BAD_REQUEST;
        };
        return response(status, exception.code(), exception.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        var result = response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Confira os campos informados.", request);
        List<FieldError> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldError(error.getField(), error.getDefaultMessage())).toList();
        result.getBody().setProperty("errors", errors);
        return result;
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingRequestHeaderException.class, ConstraintViolationException.class})
    ResponseEntity<ProblemDetail> invalid(Exception exception, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "JSON, parâmetro ou cabeçalho inválido.", request);
    }

    @ExceptionHandler({HttpRequestMethodNotSupportedException.class, HttpMediaTypeNotSupportedException.class,
            HttpMediaTypeNotAcceptableException.class, MissingServletRequestParameterException.class,
            HandlerMethodValidationException.class})
    ResponseEntity<ProblemDetail> framework(Exception exception, HttpServletRequest request) {
        ErrorResponse error = (ErrorResponse) exception;
        HttpStatus status = HttpStatus.valueOf(error.getStatusCode().value());
        String code = switch (status.value()) {
            case 405 -> "METHOD_NOT_ALLOWED";
            case 415 -> "UNSUPPORTED_MEDIA_TYPE";
            case 406 -> "NOT_ACCEPTABLE";
            default -> "INVALID_REQUEST";
        };
        var result = response(status, code, "Método, formato ou parâmetros incompatíveis com esta rota.", request);
        return ResponseEntity.status(status).headers(error.getHeaders()).body(result.getBody());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> integrity(HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "DATA_CONFLICT", "Os dados entram em conflito com um registro existente.", request);
    }

    @ExceptionHandler(ConcurrencyFailureException.class)
    ResponseEntity<ProblemDetail> concurrency(HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "CONCURRENT_OPERATION", "Operação concorrente. Tente novamente.", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ProblemDetail> notFound(HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "NOT_FOUND", "Rota não encontrada.", request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> unexpected(Exception exception, HttpServletRequest request) {
        // Não registrar corpo, senha, token ou mensagem de SQL, que podem conter dados pessoais.
        log.error("Unexpected error type={} requestId={}", exception.getClass().getSimpleName(),
                request.getAttribute("requestId"));
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Não foi possível concluir a operação.", request);
    }

    private ResponseEntity<ProblemDetail> response(HttpStatus status, String code, String message,
                                                  HttpServletRequest request) {
        var problem = ProblemDetail.forStatusAndDetail(status, message);
        problem.setType(URI.create("urn:reuse-hub:problem:" + code.toLowerCase(java.util.Locale.ROOT)));
        problem.setTitle(status.getReasonPhrase());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", code);
        problem.setProperty("requestId", request.getAttribute("requestId"));
        return ResponseEntity.status(status).body(problem);
    }

    record FieldError(String field, String message) {}
}
