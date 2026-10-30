import { useEffect, useState, type FormEvent } from "react";
import { Archive, MapPin, Package, ArrowUpRight } from "lucide-react";
import { Modal } from "./Modal";
import { EquipmentArt } from "./EquipmentArt";
import {
    ApiError,
    errorMessage,
    request,
    post,
    reservationKey,
    completeReservation,
} from "../lib/api";
import {
    categories,
    conditions,
    type Offer,
    type Reservation,
} from "../lib/types";
import { useSession } from "../lib/session";
export function OfferModal({
    id,
    onClose,
    onLogin,
    onChanged,
    notify,
}: {
    id: string;
    onClose: () => void;
    onLogin: () => void;
    onChanged: () => void;
    notify: (s: string) => void;
}) {
    const { user } = useSession();
    const [offer, setOffer] = useState<Offer>();
    const [error, setError] = useState("");
    const [quantity, setQuantity] = useState(1);
    const [busy, setBusy] = useState(false);
    const [confirmArchive, setConfirmArchive] = useState(false);
    useEffect(() => {
        const c = new AbortController();
        request<Offer>(`/offers/${id}`, { signal: c.signal })
            .then(setOffer)
            .catch((e) => {
                if (!c.signal.aborted) setError(errorMessage(e));
            });
        return () => c.abort();
    }, [id]);
    async function reserve(e: FormEvent) {
        e.preventDefault();
        if (!user || !offer || busy) return;
        setBusy(true);
        setError("");
        const key = reservationKey(user.id, offer.id, quantity);
        try {
            const reservation = await post<Reservation>(
                "/reservations",
                { offerId: offer.id, quantity },
                { "Idempotency-Key": key },
            );
            completeReservation(user.id, offer.id, quantity);
            onChanged();
            onClose();
            notify(
                reservation.status === "RESERVED"
                    ? "Reserva confirmada! Acompanhe em Minhas reservas."
                    : "Pedido já registrado. Confira o estado em Minhas reservas.",
            );
        } catch (e) {
            if (
                e instanceof ApiError &&
                e.status >= 400 &&
                e.status < 500 &&
                e.status !== 408 &&
                e.status !== 409 &&
                e.status !== 429
            )
                completeReservation(user.id, offer.id, quantity);
            setError(errorMessage(e));
        } finally {
            setBusy(false);
        }
    }
    async function archive() {
        if (!offer || busy) return;
        setBusy(true);
        setError("");
        try {
            await post(`/offers/${offer.id}/archive`);
            onChanged();
            onClose();
            notify(
                "Oferta arquivada. As reservas existentes foram preservadas.",
            );
        } catch (e) {
            setError(errorMessage(e));
        } finally {
            setBusy(false);
        }
    }
    return (
        <Modal
            title="Detalhes da doação"
            onClose={() => {
                if (!busy) onClose();
            }}
            wide
        >
            {!offer && !error && (
                <p className="loading" role="status">
                    Carregando equipamento…
                </p>
            )}
            {offer && (
                <>
                    <div className={`detail-art art-${offer.category}`}>
                        <EquipmentArt category={offer.category} />
                        <span className="art-caption">
                            Ilustração da categoria
                        </span>
                    </div>
                    <div className="detail-meta">
                        <span className="eyebrow">
                            {categories[offer.category]}
                        </span>
                        <span className="badge">
                            {conditions[offer.condition]}
                        </span>
                    </div>
                    <h3 className="detail-title">{offer.title}</h3>
                    <div className="meta">
                        <span>
                            <MapPin size={16} />
                            {offer.city}
                        </span>
                        <span>
                            <Package size={16} />
                            {offer.availableQuantity} de {offer.totalQuantity}{" "}
                            disponíveis
                        </span>
                    </div>
                    <p className="description">{offer.description}</p>
                    {!offer.active ? (
                        <p className="notice">
                            Esta oferta foi arquivada e não aceita novas
                            reservas.
                        </p>
                    ) : user?.id === offer.ownerId ? (
                        <div className="owner-actions">
                            <p className="notice">
                                Esta doação é sua. Você pode acompanhar os
                                pedidos em Minhas reservas.
                            </p>
                            {confirmArchive ? (
                                <div className="confirm">
                                    <p>
                                        Arquivar retira a oferta do catálogo.
                                        Reservas existentes continuam válidas.
                                    </p>
                                    <button
                                        className="button danger"
                                        disabled={busy}
                                        onClick={archive}
                                    >
                                        Confirmar arquivamento
                                    </button>
                                    <button
                                        className="text-button"
                                        disabled={busy}
                                        onClick={() => setConfirmArchive(false)}
                                    >
                                        Voltar
                                    </button>
                                </div>
                            ) : (
                                <button
                                    className="button secondary"
                                    onClick={() => setConfirmArchive(true)}
                                >
                                    <Archive size={17} />
                                    Arquivar oferta
                                </button>
                            )}
                        </div>
                    ) : offer.availableQuantity === 0 ? (
                        <p className="notice">
                            Todas as unidades estão reservadas ou já foram
                            retiradas.
                        </p>
                    ) : !user ? (
                        <button
                            className="button primary full"
                            onClick={onLogin}
                        >
                            Entrar para reservar
                            <ArrowUpRight size={18} />
                        </button>
                    ) : (
                        <form onSubmit={reserve} className="reserve-form">
                            <label>
                                Quantidade
                                <input
                                    type="number"
                                    min={1}
                                    max={offer.availableQuantity}
                                    required
                                    value={quantity}
                                    onChange={(e) =>
                                        setQuantity(Number(e.target.value))
                                    }
                                />
                            </label>
                            <button className="button primary" disabled={busy}>
                                {busy ? "Reservando…" : "Reservar equipamento"}
                                <ArrowUpRight size={18} />
                            </button>
                        </form>
                    )}
                    <p className="fineprint">
                        Doação gratuita. Consulte o prazo em Minhas reservas
                        após confirmar. A retirada é combinada fora do ReUse
                        Hub.
                    </p>
                </>
            )}
            {error && (
                <p className="error" role="alert">
                    {error}
                </p>
            )}
        </Modal>
    );
}
