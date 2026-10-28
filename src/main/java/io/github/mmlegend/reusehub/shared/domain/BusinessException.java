package io.github.mmlegend.reusehub.shared.domain;

public class BusinessException extends RuntimeException {
    public enum Kind { NOT_FOUND, FORBIDDEN, CONFLICT, UNAUTHORIZED, INVALID }

    private final Kind kind;
    private final String code;

    public BusinessException(Kind kind, String code, String message) {
        super(message);
        this.kind = kind;
        this.code = code;
    }

    public Kind kind() { return kind; }
    public String code() { return code; }

    public static BusinessException notFound(String resource) {
        return new BusinessException(Kind.NOT_FOUND, "NOT_FOUND", resource + " não encontrado(a).");
    }

    public static BusinessException forbidden() {
        return new BusinessException(Kind.FORBIDDEN, "FORBIDDEN", "Você não tem permissão para esta operação.");
    }

    public static BusinessException conflict(String code, String message) {
        return new BusinessException(Kind.CONFLICT, code, message);
    }
}
