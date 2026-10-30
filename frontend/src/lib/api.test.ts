import { afterEach, describe, expect, it, vi } from "vitest";
import {
    ApiError,
    completeReservation,
    request,
    reservationKey,
    setToken,
} from "./api";
const values = new Map<string, string>();
vi.stubGlobal("sessionStorage", {
    getItem: (k: string) => values.get(k) || null,
    setItem: (k: string, v: string) => values.set(k, v),
    removeItem: (k: string) => values.delete(k),
});
const dispatchEvent = vi.fn();
vi.stubGlobal("window", { dispatchEvent });
afterEach(() => {
    vi.restoreAllMocks();
    setToken(null);
    dispatchEvent.mockClear();
});
describe("contrato HTTP", () => {
    it("envia JSON, Bearer e chave de idempotência somente na requisição protegida", async () => {
        const fetch = vi
            .fn()
            .mockImplementation(() =>
                Promise.resolve(new Response('{"id":"r1"}', { status: 201 })),
            );
        vi.stubGlobal("fetch", fetch);
        setToken("test-token");
        await request(
            "/reservations",
            {
                method: "POST",
                body: '{"quantity":1}',
                headers: { "Idempotency-Key": "same-key" },
            },
            true,
        );
        const headers = fetch.mock.calls[0][1].headers as Headers;
        expect(headers.get("Authorization")).toBe("Bearer test-token");
        expect(headers.get("Idempotency-Key")).toBe("same-key");
        expect(headers.get("Content-Type")).toBe("application/json");
        await request("/offers");
        expect(fetch.mock.calls[1][1].headers.get("Authorization")).toBeNull();
    });
    it("expira a sessão somente quando uma chamada autenticada retorna 401", async () => {
        vi.stubGlobal(
            "fetch",
            vi
                .fn()
                .mockImplementation(() =>
                    Promise.resolve(
                        new Response('{"detail":"Token expirado"}', {
                            status: 401,
                        }),
                    ),
                ),
        );
        await expect(request("/auth/me", {}, true)).rejects.toMatchObject({
            status: 401,
        });
        expect(dispatchEvent).toHaveBeenCalledOnce();
        dispatchEvent.mockClear();
        await expect(request("/auth/login")).rejects.toThrow();
        expect(dispatchEvent).not.toHaveBeenCalled();
    });
    it("preserva detalhes e referência do ProblemDetails sem repetir uma mutação", async () => {
        const fetch = vi
            .fn()
            .mockResolvedValue(
                new Response(
                    '{"detail":"Quantidade indisponível.","code":"INSUFFICIENT_STOCK","requestId":"abc"}',
                    { status: 409 },
                ),
            );
        vi.stubGlobal("fetch", fetch);
        await expect(
            request("/reservations", { method: "POST" }, true),
        ).rejects.toMatchObject({
            status: 409,
            code: "INSUFFICIENT_STOCK",
            requestId: "abc",
        });
        expect(fetch).toHaveBeenCalledOnce();
    });
    it("distingue falha de rede de rejeição confirmada pelo servidor", async () => {
        vi.stubGlobal(
            "fetch",
            vi.fn().mockRejectedValue(new TypeError("Failed to fetch")),
        );
        await expect(
            request("/reservations", { method: "POST" }, true),
        ).rejects.toBeInstanceOf(ApiError);
        await expect(request("/reservations")).rejects.toMatchObject({
            status: 0,
        });
    });
});
describe("intenção de reserva", () => {
    it("mantém a chave após falha e separa usuário, equipamento e quantidade", () => {
        const first = reservationKey("alice", "kit", 1);
        expect(reservationKey("alice", "kit", 1)).toBe(first);
        expect(reservationKey("bob", "kit", 1)).not.toBe(first);
        expect(reservationKey("alice", "kit", 2)).not.toBe(first);
        expect(reservationKey("alice", "notebook", 1)).not.toBe(first);
        completeReservation("alice", "kit", 1);
        expect(reservationKey("alice", "kit", 1)).not.toBe(first);
    });
});
