import { useState, type FormEvent } from "react";
import { ArrowUpRight, Info } from "lucide-react";
import { Modal } from "./Modal";
import { errorMessage, post } from "../lib/api";
import { categories, conditions, type Offer } from "../lib/types";
export function CreateOfferModal({
    onClose,
    onSuccess,
}: {
    onClose: () => void;
    onSuccess: (offer: Offer) => void;
}) {
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState("");
    async function submit(e: FormEvent<HTMLFormElement>) {
        e.preventDefault();
        if (busy) return;
        const d = Object.fromEntries(new FormData(e.currentTarget));
        setBusy(true);
        setError("");
        try {
            onSuccess(
                await post<Offer>("/offers", {
                    ...d,
                    quantity: Number(d.quantity),
                }),
            );
        } catch (e) {
            setError(
                errorMessage(e) +
                    " Se a conexão caiu, confira o catálogo antes de publicar novamente.",
            );
        } finally {
            setBusy(false);
        }
    }
    return (
        <Modal
            title="Um novo destino começa aqui"
            onClose={() => {
                if (!busy) onClose();
            }}
            wide
        >
            <p className="muted">
                Conte o que você tem para doar. Uma descrição cuidadosa ajuda a
                encontrar quem precisa.
            </p>
            <form className="form-stack" onSubmit={submit}>
                <label>
                    Título da doação
                    <input
                        name="title"
                        required
                        maxLength={120}
                        placeholder="Ex.: Kit ESP32 com sensores"
                    />
                </label>
                <div className="form-grid">
                    <label>
                        Categoria
                        <select name="category">
                            {Object.entries(categories).map(([k, v]) => (
                                <option key={k} value={k}>
                                    {v}
                                </option>
                            ))}
                        </select>
                    </label>
                    <label>
                        Condição
                        <select name="condition" defaultValue="GOOD">
                            {Object.entries(conditions).map(([k, v]) => (
                                <option key={k} value={k}>
                                    {v}
                                </option>
                            ))}
                        </select>
                    </label>
                </div>
                <label>
                    Descrição
                    <textarea
                        name="description"
                        required
                        maxLength={2000}
                        rows={4}
                        placeholder="Descreva o equipamento, os acessórios e possíveis detalhes de uso."
                    />
                </label>
                <div className="form-grid">
                    <label>
                        Cidade
                        <input
                            name="city"
                            required
                            maxLength={100}
                            placeholder="Ex.: Rio de Janeiro"
                        />
                    </label>
                    <label>
                        Quantidade
                        <input
                            name="quantity"
                            type="number"
                            min={1}
                            max={10000}
                            defaultValue={1}
                            required
                        />
                    </label>
                </div>
                <p className="notice">
                    <Info size={18} />A retirada é combinada fora da plataforma.
                    Não inclua dados pessoais sensíveis na descrição pública.
                </p>
                {error && (
                    <p role="alert" className="error">
                        {error}
                    </p>
                )}
                <button className="button primary full" disabled={busy}>
                    {busy ? "Publicando…" : "Publicar doação"}
                    <ArrowUpRight size={18} />
                </button>
            </form>
        </Modal>
    );
}
