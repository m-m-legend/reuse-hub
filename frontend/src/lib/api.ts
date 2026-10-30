export class ApiError extends Error {
    constructor(
        message: string,
        public status: number,
        public code?: string,
        public requestId?: string,
    ) {
        super(message);
    }
}
let token: string | null = null;
export function setToken(value: string | null) {
    token = value;
}
export async function request<T>(
    path: string,
    options: RequestInit = {},
    authenticated = false,
): Promise<T> {
    const headers = new Headers(options.headers);
    headers.set("Accept", "application/json");
    if (options.body) headers.set("Content-Type", "application/json");
    if (authenticated && token) headers.set("Authorization", `Bearer ${token}`);
    let response: Response;
    try {
        response = await fetch(`/api/v1${path}`, {
            ...options,
            headers,
            signal: options.signal || AbortSignal.timeout(20000),
        });
    } catch (error) {
        if (options.signal?.aborted) throw error;
        throw new ApiError(
            "Não foi possível confirmar a resposta. Verifique sua conexão e tente novamente.",
            0,
        );
    }
    if (!response.ok) {
        const problem = await response.json().catch(() => ({}));
        if (response.status === 401 && authenticated)
            window.dispatchEvent(new Event("session-expired"));
        throw new ApiError(
            problem.detail ||
                (response.status >= 500
                    ? "O serviço está indisponível. Tente novamente em instantes."
                    : "Não foi possível concluir a operação."),
            response.status,
            problem.code,
            problem.requestId,
        );
    }
    if (response.status === 204) return undefined as T;
    return response.json();
}
export const post = <T>(path: string, body?: unknown, headers?: HeadersInit) =>
    request<T>(
        path,
        {
            method: "POST",
            headers,
            body: body === undefined ? undefined : JSON.stringify(body),
        },
        true,
    );
export function errorMessage(error: unknown) {
    if (error instanceof ApiError)
        return `${error.message}${error.requestId ? ` Referência: ${error.requestId}` : ""}`;
    return error instanceof Error
        ? error.message
        : "Ocorreu um erro inesperado.";
}
// A chave permanece disponível após falha de rede, fechamento do diálogo ou reload.
// Uma nova quantidade e outra conta representam intenções independentes.
const pending = new Map<string, string>();
function intentKey(user: string, offer: string, quantity: number) {
    return `reuse-hub:intent:${user}:${offer}:${quantity}`;
}
export function reservationKey(
    user: string,
    offer: string,
    quantity: number,
): string {
    const id = intentKey(user, offer, quantity);
    let stored: string | null = null;
    try {
        stored = sessionStorage.getItem(id);
    } catch {
        /* Navegadores podem bloquear armazenamento. */
    }
    const key = stored || pending.get(id) || crypto.randomUUID();
    pending.set(id, key);
    try {
        sessionStorage.setItem(id, key);
    } catch {
        /* Memória cobre a sessão corrente. */
    }
    return key;
}
export function completeReservation(
    user: string,
    offer: string,
    quantity: number,
) {
    const id = intentKey(user, offer, quantity);
    pending.delete(id);
    try {
        sessionStorage.removeItem(id);
    } catch {
        /* Nada a remover. */
    }
}
