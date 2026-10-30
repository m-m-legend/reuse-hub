import { expect, it, vi } from "vitest";
import {
    completeReservation,
    post,
    request,
    reservationKey,
    setToken,
} from "./api";
import type {
    Account,
    Offer,
    Page,
    Reservation,
    ReservationEvent,
} from "./types";

// Execução opt-in contra uma API descartável. Cria contas e registros reais de teste.
// Não usa mocks de resposta: o fetch somente recebe a URL absoluta do servidor.
it.skipIf(!process.env.LIVE_API_URL)(
    "cliente web: cadastro, login, oferta, replay, permissões, cancelamento, retirada e arquivamento",
    async () => {
        const fetchHttp = globalThis.fetch;
        const values = new Map<string, string>();
        vi.stubGlobal("window", new EventTarget());
        vi.stubGlobal("sessionStorage", {
            getItem: (k: string) => values.get(k) || null,
            setItem: (k: string, v: string) => values.set(k, v),
            removeItem: (k: string) => values.delete(k),
        });
        vi.stubGlobal("fetch", (path: string, options?: RequestInit) =>
            fetchHttp(`${process.env.LIVE_API_URL}${path}`, options),
        );
        const suffix = crypto.randomUUID();
        const password = "Frontend-integration-2026!";
        const ownerEmail = `owner-${suffix}@example.test`;
        const requesterEmail = `requester-${suffix}@example.test`;
        async function login(email: string) {
            const token = await request<{ accessToken: string }>(
                "/auth/login",
                { method: "POST", body: JSON.stringify({ email, password }) },
            );
            setToken(token.accessToken);
            return request<Account>("/auth/me", {}, true);
        }
        try {
            for (const email of [ownerEmail, requesterEmail])
                await request("/auth/register", {
                    method: "POST",
                    body: JSON.stringify({
                        name: "Teste do frontend",
                        email,
                        password,
                    }),
                });
            const owner = await login(ownerEmail);
            const offer = await post<Offer>("/offers", {
                title: `Frontend ${suffix}`,
                description: "Dado descartável de integração do cliente web.",
                category: "ELECTRONICS",
                condition: "GOOD",
                city: "Rio de Janeiro",
                quantity: 5,
            });
            expect(offer.ownerId).toBe(owner.id);
            const catalog = await request<Page<Offer>>(
                `/offers?search=${encodeURIComponent(offer.title)}&city=Rio+de+Janeiro&category=ELECTRONICS`,
            );
            expect(catalog.content.some((o) => o.id === offer.id)).toBe(true);
            const requester = await login(requesterEmail);
            const key = reservationKey(requester.id, offer.id, 2);
            const reserve = () =>
                post<Reservation>(
                    "/reservations",
                    { offerId: offer.id, quantity: 2 },
                    { "Idempotency-Key": key },
                );
            const first = await reserve();
            const replay = await reserve();
            expect(replay.id).toBe(first.id);
            expect(
                (await request<Offer>(`/offers/${offer.id}`)).availableQuantity,
            ).toBe(3);
            await expect(
                post(`/reservations/${first.id}/collect`),
            ).rejects.toMatchObject({ status: 403 });
            expect(
                (await post<Reservation>(`/reservations/${first.id}/cancel`))
                    .status,
            ).toBe("CANCELLED");
            await post(`/reservations/${first.id}/cancel`);
            expect(
                (await request<Offer>(`/offers/${offer.id}`)).availableQuantity,
            ).toBe(5);
            completeReservation(requester.id, offer.id, 2);
            const second = await post<Reservation>(
                "/reservations",
                { offerId: offer.id, quantity: 2 },
                {
                    "Idempotency-Key": reservationKey(
                        requester.id,
                        offer.id,
                        2,
                    ),
                },
            );
            expect(second.id).not.toBe(first.id);
            const mine = await request<Page<Reservation>>(
                "/reservations",
                {},
                true,
            );
            expect(mine.content.some((r) => r.id === second.id)).toBe(true);
            await login(ownerEmail);
            expect(
                (await post<Reservation>(`/reservations/${second.id}/collect`))
                    .status,
            ).toBe("COLLECTED");
            const events = await request<ReservationEvent[]>(
                `/reservations/${second.id}/events`,
                {},
                true,
            );
            expect(events.map((e) => e.status)).toEqual([
                "RESERVED",
                "COLLECTED",
            ]);
            expect(
                (await request<Offer>(`/offers/${offer.id}`)).availableQuantity,
            ).toBe(3);
            expect(
                (await post<Offer>(`/offers/${offer.id}/archive`)).active,
            ).toBe(false);
            expect(
                (
                    await request<Page<Offer>>(
                        `/offers?search=${encodeURIComponent(offer.title)}`,
                    )
                ).content,
            ).toHaveLength(0);
        } finally {
            setToken(null);
            vi.unstubAllGlobals();
        }
    },
    30000,
);
