import { useEffect, useState } from "react";
import {
    ArrowLeft,
    ArrowUpRight,
    Clock3,
    History,
    PackageCheck,
    PackageOpen,
    XCircle,
    RefreshCw,
} from "lucide-react";
import { request, post, errorMessage } from "../lib/api";
import { useSession } from "../lib/session";
import {
    dateTime,
    statuses,
    type Page,
    type Offer,
    type Reservation,
    type ReservationEvent,
} from "../lib/types";
import { Modal } from "../components/Modal";
export function Reservations({
    notify,
    onCatalog,
    onOffer,
    revision,
    onChanged,
}: {
    notify: (s: string) => void;
    onCatalog: () => void;
    onOffer: (id: string) => void;
    revision: number;
    onChanged: () => void;
}) {
    const { user } = useSession();
    const [status, setStatus] = useState("");
    const [page, setPage] = useState(0);
    const [data, setData] = useState<Page<Reservation>>();
    const [offers, setOffers] = useState<Record<string, Offer>>({});
    const [error, setError] = useState("");
    const [busy, setBusy] = useState(true);
    const [retry, setRetry] = useState(0);
    const [selected, setSelected] = useState<Reservation>();
    const [events, setEvents] = useState<ReservationEvent[]>();
    const [detailError, setDetailError] = useState("");
    const [action, setAction] = useState<"collect" | "cancel">();
    const [saving, setSaving] = useState(false);
    useEffect(() => {
        if (!user) return;
        const c = new AbortController();
        setBusy(true);
        setError("");
        const params = new URLSearchParams({ page: String(page), size: "10" });
        if (status) params.set("status", status);
        request<Page<Reservation>>(
            `/reservations?${params}`,
            { signal: c.signal },
            true,
        )
            .then(async (result) => {
                if (c.signal.aborted) return;
                setData(result);
                const ids = [...new Set(result.content.map((r) => r.offerId))];
                const details = await Promise.allSettled(
                    ids.map((id) =>
                        request<Offer>(`/offers/${id}`, { signal: c.signal }),
                    ),
                );
                if (!c.signal.aborted)
                    setOffers(
                        Object.fromEntries(
                            details.flatMap((d) =>
                                d.status === "fulfilled"
                                    ? [[d.value.id, d.value]]
                                    : [],
                            ),
                        ),
                    );
            })
            .catch((e) => {
                if (!c.signal.aborted) setError(errorMessage(e));
            })
            .finally(() => {
                if (!c.signal.aborted) setBusy(false);
            });
        return () => c.abort();
    }, [user, page, status, revision, retry]);
    useEffect(() => {
        if (!selected) return;
        const c = new AbortController();
        setEvents(undefined);
        setDetailError("");
        request<ReservationEvent[]>(
            `/reservations/${selected.id}/events`,
            { signal: c.signal },
            true,
        )
            .then(setEvents)
            .catch((e) => {
                if (!c.signal.aborted) setDetailError(errorMessage(e));
            });
        return () => c.abort();
    }, [selected]);
    async function transition() {
        if (!selected || !action || saving) return;
        setSaving(true);
        setDetailError("");
        try {
            const updated = await post<Reservation>(
                `/reservations/${selected.id}/${action}`,
            );
            setSelected(updated);
            setAction(undefined);
            onChanged();
            notify(
                action === "collect"
                    ? "Retirada confirmada. Mais um novo ciclo começou!"
                    : "Reserva cancelada. As unidades voltaram ao catálogo.",
            );
        } catch (e) {
            setDetailError(errorMessage(e));
        } finally {
            setSaving(false);
        }
    }
    if (!user) return null;
    return (
        <section className="reservations-section">
            <button className="text-button back-link" onClick={onCatalog}>
                <ArrowLeft size={17} />
                Voltar ao catálogo
            </button>
            <div className="section-heading">
                <div>
                    <span className="eyebrow">
                        SEUS EQUIPAMENTOS EM MOVIMENTO
                    </span>
                    <h1>
                        Minhas reservas<span>.</span>
                    </h1>
                    <p className="muted">
                        Acompanhe o que você reservou e os pedidos das suas
                        doações.
                    </p>
                </div>
                <button
                    className="button secondary"
                    onClick={() => setRetry(retry + 1)}
                >
                    <RefreshCw size={16} />
                    Atualizar
                </button>
            </div>
            <div className="category-tabs">
                <button
                    className={!status ? "active" : ""}
                    onClick={() => {
                        setStatus("");
                        setPage(0);
                    }}
                    aria-pressed={!status}
                >
                    Todas
                </button>
                {Object.entries(statuses).map(([k, v]) => (
                    <button
                        key={k}
                        className={status === k ? "active" : ""}
                        aria-pressed={status === k}
                        onClick={() => {
                            setStatus(k);
                            setPage(0);
                        }}
                    >
                        {v}s
                    </button>
                ))}
            </div>
            {busy ? (
                <p className="loading" role="status">
                    Carregando suas reservas…
                </p>
            ) : error ? (
                <p className="error" role="alert">
                    {error}
                </p>
            ) : !data?.content.length ? (
                <div className="empty-state">
                    <PackageOpen size={42} />
                    <h3>Os próximos ciclos aparecem aqui.</h3>
                    <p>
                        Quando você reservar um equipamento ou receber um
                        pedido, poderá acompanhar os detalhes nesta página.
                    </p>
                    <button className="button primary" onClick={onCatalog}>
                        Explorar doações
                        <ArrowUpRight size={17} />
                    </button>
                </div>
            ) : (
                <div className="reservation-list">
                    {data.content.map((r) => (
                        <button
                            className="reservation-row"
                            key={r.id}
                            onClick={() => {
                                setSelected(r);
                                setAction(undefined);
                            }}
                        >
                            <span className="reservation-icon">
                                <PackageCheck size={24} />
                            </span>
                            <span className="reservation-title">
                                <small>
                                    {r.requesterId === user.id
                                        ? "VOCÊ RESERVOU"
                                        : "PEDIDO NA SUA DOAÇÃO"}
                                </small>
                                <strong>
                                    {offers[r.offerId]?.title ||
                                        `Equipamento ${r.offerId.slice(0, 8)}`}
                                </strong>
                                <span>
                                    {r.quantity}{" "}
                                    {r.quantity === 1 ? "unidade" : "unidades"}{" "}
                                    · {dateTime(r.createdAt)}
                                </span>
                            </span>
                            <span className={`status status-${r.status}`}>
                                {statuses[r.status]}
                            </span>
                            <ArrowUpRight size={20} />
                        </button>
                    ))}
                </div>
            )}
            {!busy && data && !error && data.totalPages > 1 && (
                <nav className="pagination" aria-label="Páginas de reservas">
                    <button
                        className="button secondary"
                        disabled={!page}
                        onClick={() => setPage(page - 1)}
                    >
                        Anterior
                    </button>
                    <span>
                        {page + 1} de {data.totalPages}
                    </span>
                    <button
                        className="button secondary"
                        disabled={page + 1 >= data.totalPages || page >= 10000}
                        onClick={() => setPage(page + 1)}
                    >
                        Próxima
                    </button>
                </nav>
            )}
            {selected && (
                <Modal
                    title="Acompanhar reserva"
                    onClose={() => {
                        if (!saving) setSelected(undefined);
                    }}
                    wide
                >
                    <span className={`status status-${selected.status}`}>
                        {statuses[selected.status]}
                    </span>
                    <h3 className="detail-title">
                        {offers[selected.offerId]?.title ||
                            "Equipamento reservado"}
                    </h3>
                    <p className="muted">
                        {selected.quantity} unidade(s) ·{" "}
                        {selected.ownerId === user.id
                            ? "Você é o doador"
                            : "Você é o solicitante"}
                    </p>
                    <p className="notice">
                        <Clock3 size={18} />
                        Prazo para retirada: {dateTime(selected.expiresAt)}. A
                        retirada é combinada fora da plataforma.
                    </p>
                    <button
                        className="text-button"
                        onClick={() => onOffer(selected.offerId)}
                    >
                        Consultar oferta
                        <ArrowUpRight size={16} />
                    </button>
                    {selected.status === "RESERVED" && (
                        <div className="reservation-actions">
                            {selected.requesterId === user.id && (
                                <button
                                    className="button secondary"
                                    disabled={saving}
                                    onClick={() => setAction("cancel")}
                                >
                                    <XCircle size={17} />
                                    Cancelar reserva
                                </button>
                            )}
                            {selected.ownerId === user.id &&
                                (new Date(selected.expiresAt).getTime() >
                                Date.now() ? (
                                    <button
                                        className="button primary"
                                        disabled={saving}
                                        onClick={() => setAction("collect")}
                                    >
                                        <PackageCheck size={17} />
                                        Confirmar retirada
                                    </button>
                                ) : (
                                    <p className="notice">
                                        Prazo encerrado. Aguardando o
                                        processamento da expiração.
                                    </p>
                                ))}
                        </div>
                    )}
                    {action && (
                        <div className="confirm">
                            <p>
                                {action === "collect"
                                    ? "Confirma que os equipamentos foram entregues? Esta ação não pode ser desfeita."
                                    : "Deseja cancelar a reserva e devolver as unidades ao catálogo?"}
                            </p>
                            <div className="inline-actions">
                                <button
                                    className="button primary"
                                    onClick={transition}
                                    disabled={saving}
                                >
                                    {saving ? "Confirmando…" : "Sim, confirmar"}
                                </button>
                                <button
                                    className="button secondary"
                                    disabled={saving}
                                    onClick={() => setAction(undefined)}
                                >
                                    Voltar
                                </button>
                            </div>
                        </div>
                    )}
                    <h4 className="history-title">
                        <History size={18} />
                        Histórico da reserva
                    </h4>
                    {events ? (
                        <ol className="timeline">
                            {events.map((event) => (
                                <li key={event.id}>
                                    <span className="dot" />
                                    <div>
                                        <strong>
                                            {statuses[event.status]}
                                        </strong>
                                        <p>
                                            {dateTime(event.occurredAt)} ·{" "}
                                            {event.actorId === null
                                                ? "Sistema"
                                                : event.actorId === user.id
                                                  ? "Você"
                                                  : "Outro participante"}
                                        </p>
                                    </div>
                                </li>
                            ))}
                        </ol>
                    ) : (
                        !detailError && (
                            <p role="status">Carregando histórico…</p>
                        )
                    )}
                    {detailError && (
                        <p className="error" role="alert">
                            {detailError}
                        </p>
                    )}
                    <p className="fineprint">Identificador: {selected.id}</p>
                </Modal>
            )}
        </section>
    );
}
