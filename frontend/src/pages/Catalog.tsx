import { useEffect, useState, type FormEvent } from "react";
import {
    ArrowRight,
    ArrowUpRight,
    Search,
    MapPin,
    SlidersHorizontal,
    Monitor,
    Cpu,
    Wrench,
    BookOpen,
    Shapes,
    PackageOpen,
    ChevronLeft,
    ChevronRight,
    RefreshCw,
    Leaf,
} from "lucide-react";
import { request, errorMessage } from "../lib/api";
import {
    categories,
    conditions,
    type Category,
    type Offer,
    type Page,
} from "../lib/types";
import { EquipmentArt } from "../components/EquipmentArt";
const icons = {
    COMPUTERS: Monitor,
    ELECTRONICS: Cpu,
    TOOLS: Wrench,
    EDUCATION: BookOpen,
    OTHER: Shapes,
};
export function Catalog({
    onOffer,
    onCreate,
    revision,
}: {
    onOffer: (id: string) => void;
    onCreate: () => void;
    revision: number;
}) {
    const [category, setCategory] = useState("");
    const [search, setSearch] = useState("");
    const [city, setCity] = useState("");
    const [available, setAvailable] = useState(true);
    const [filters, setFilters] = useState({ search: "", city: "" });
    const [page, setPage] = useState(0);
    const [data, setData] = useState<Page<Offer>>();
    const [busy, setBusy] = useState(true);
    const [error, setError] = useState("");
    const [retry, setRetry] = useState(0);
    useEffect(() => {
        const c = new AbortController();
        setBusy(true);
        setError("");
        const q = new URLSearchParams({
            ...filters,
            category,
            availableOnly: String(available),
            page: String(page),
            size: "9",
        });
        if (!category) q.delete("category");
        request<Page<Offer>>(`/offers?${q}`, { signal: c.signal })
            .then(setData)
            .catch((e) => {
                if (!c.signal.aborted) setError(errorMessage(e));
            })
            .finally(() => {
                if (!c.signal.aborted) setBusy(false);
            });
        return () => c.abort();
    }, [category, filters, available, page, revision, retry]);
    function submit(e: FormEvent) {
        e.preventDefault();
        setPage(0);
        setFilters({ search: search.trim(), city: city.trim() });
    }
    function reset() {
        setCategory("");
        setSearch("");
        setCity("");
        setFilters({ search: "", city: "" });
        setAvailable(true);
        setPage(0);
    }
    return (
        <>
            <section className="hero">
                <div className="hero-copy">
                    <div className="hero-label">
                        <span />
                        MENOS DESCARTE. MAIS POSSIBILIDADES.
                    </div>
                    <h1>
                        O que sobra aqui
                        <br />
                        pode <em>começar algo</em>
                        <br />
                        novo por aí.
                    </h1>
                    <p>
                        Equipamentos parados, ideias em movimento.
                        <br className="desktop-break" /> Conectamos quem tem
                        para doar a quem pode transformar.
                    </p>
                    <a className="button primary" href="#catalogo">
                        Explorar doações
                        <ArrowUpRight size={19} />
                    </a>
                    <div className="hero-footnote">
                        <Leaf size={16} />
                        <span>Reutilizar é dar espaço ao próximo começo.</span>
                    </div>
                </div>
                <div className="hero-visual" aria-hidden="true">
                    <div className="orbit orbit-one" />
                    <div className="orbit orbit-two" />
                    <span className="floating-label">
                        <span className="dot" />
                        Pronto para um novo ciclo
                    </span>
                    <div className="hero-equipment">
                        <EquipmentArt category="COMPUTERS" hero />
                    </div>
                    <div className="little-art">
                        <EquipmentArt category="ELECTRONICS" />
                    </div>
                    <div className="cycle-seal">
                        <RefreshCw size={24} />
                        <span>
                            REUTILIZE
                            <br />
                            REIMAGINE
                        </span>
                    </div>
                    <div className="visual-bottom">
                        <span>
                            BOAS IDEIAS MERECEM
                            <br />
                            UMA SEGUNDA CHANCE.
                        </span>
                        <ArrowUpRight size={32} />
                    </div>
                </div>
            </section>
            <div className="principles">
                <span>
                    <span className="principle-number">01</span>Encontre uma
                    possibilidade
                </span>
                <span>
                    <span className="principle-number">02</span>Reserve o que
                    precisa
                </span>
                <span>
                    <span className="principle-number">03</span>Comece um novo
                    ciclo
                </span>
            </div>
            <section id="catalogo" className="catalog-section">
                <div className="section-heading">
                    <div>
                        <span className="eyebrow">
                            CONECTANDO NOVOS COMEÇOS
                        </span>
                        <h2>
                            Encontre sua próxima possibilidade<span>.</span>
                        </h2>
                    </div>
                    <span className="section-note">
                        Doações, não vendas.
                        <Leaf size={17} />
                    </span>
                </div>
                <div className="category-tabs" aria-label="Categorias">
                    <button
                        className={!category ? "active" : ""}
                        aria-pressed={!category}
                        onClick={() => {
                            setCategory("");
                            setPage(0);
                        }}
                    >
                        <Shapes size={17} />
                        Todas as categorias
                    </button>
                    {Object.entries(categories).map(([key, label]) => {
                        const Icon = icons[key as Category];
                        return (
                            <button
                                className={category === key ? "active" : ""}
                                aria-pressed={category === key}
                                key={key}
                                onClick={() => {
                                    setCategory(key);
                                    setPage(0);
                                }}
                            >
                                <Icon size={17} />
                                {label}
                            </button>
                        );
                    })}
                </div>
                <form className="search-bar" onSubmit={submit}>
                    <label className="search-field">
                        <Search size={19} />
                        <input
                            aria-label="Buscar equipamento"
                            placeholder="O que você está procurando?"
                            maxLength={120}
                            value={search}
                            onChange={(e) => setSearch(e.target.value)}
                        />
                    </label>
                    <label className="city-field">
                        <MapPin size={18} />
                        <input
                            aria-label="Filtrar por cidade"
                            title="Use o nome exato da cidade"
                            placeholder="Sua cidade"
                            maxLength={100}
                            value={city}
                            onChange={(e) => setCity(e.target.value)}
                        />
                    </label>
                    <button className="button primary" type="submit">
                        Buscar
                        <ArrowRight size={17} />
                    </button>
                </form>
                <div className="results-toolbar">
                    <p role="status">
                        {busy ? (
                            "Buscando possibilidades…"
                        ) : error ? (
                            "Não foi possível carregar o catálogo"
                        ) : (
                            <>
                                <strong>{data?.totalElements || 0}</strong>{" "}
                                {(data?.totalElements || 0) === 1
                                    ? "doação encontrada"
                                    : "doações encontradas"}
                            </>
                        )}
                    </p>
                    <label className="check-label">
                        <SlidersHorizontal size={15} />
                        <input
                            type="checkbox"
                            checked={available}
                            onChange={(e) => {
                                setAvailable(e.target.checked);
                                setPage(0);
                            }}
                        />
                        Só com unidades disponíveis
                    </label>
                </div>
                {busy ? (
                    <div className="offer-grid" aria-busy="true">
                        {[1, 2, 3].map((i) => (
                            <div key={i} className="skeleton-card">
                                <div />
                                <span />
                                <span />
                            </div>
                        ))}
                    </div>
                ) : error ? (
                    <div className="empty-state">
                        <PackageOpen size={38} />
                        <h3>Vamos tentar de novo?</h3>
                        <p>{error}</p>
                        <button
                            className="button secondary"
                            onClick={() => setRetry(retry + 1)}
                        >
                            <RefreshCw size={17} />
                            Recarregar catálogo
                        </button>
                    </div>
                ) : !data?.content.length ? (
                    <div className="empty-state">
                        <PackageOpen size={42} />
                        <h3>Um novo ciclo pode começar com você.</h3>
                        <p>
                            Nenhuma doação encontrada com estes filtros. Explore
                            outras categorias ou publique o primeiro
                            equipamento.
                        </p>
                        <div className="inline-actions">
                            <button
                                className="button secondary"
                                onClick={reset}
                            >
                                Limpar filtros
                            </button>
                            <button
                                className="button primary"
                                onClick={onCreate}
                            >
                                Quero doar
                                <ArrowUpRight size={17} />
                            </button>
                        </div>
                    </div>
                ) : (
                    <div className="offer-grid">
                        {data.content.map((offer) => (
                            <button
                                className="offer-card"
                                key={offer.id}
                                onClick={() => onOffer(offer.id)}
                                aria-label={`Ver doação: ${offer.title}`}
                            >
                                <div
                                    className={`card-art art-${offer.category}`}
                                >
                                    <span className="condition-badge">
                                        {conditions[offer.condition]}
                                    </span>
                                    <EquipmentArt category={offer.category} />
                                    <span className="art-caption">
                                        Ilustração
                                    </span>
                                </div>
                                <div className="card-body">
                                    <span className="card-category">
                                        {categories[offer.category]}
                                    </span>
                                    <h3>{offer.title}</h3>
                                    <p className="card-description">
                                        {offer.description}
                                    </p>
                                    <span className="card-city">
                                        <MapPin size={14} />
                                        {offer.city}
                                    </span>
                                    <div className="card-bottom">
                                        <span>
                                            <span
                                                className={
                                                    offer.availableQuantity
                                                        ? "dot"
                                                        : "dot gray"
                                                }
                                            />
                                            {offer.availableQuantity}{" "}
                                            {offer.availableQuantity === 1
                                                ? "unidade disponível"
                                                : "unidades disponíveis"}
                                        </span>
                                        <span className="card-arrow">
                                            <ArrowUpRight size={19} />
                                        </span>
                                    </div>
                                </div>
                            </button>
                        ))}
                    </div>
                )}
                {!busy && !error && data && data.totalPages > 1 && (
                    <nav className="pagination" aria-label="Páginas de doações">
                        <button
                            className="button secondary"
                            disabled={page === 0}
                            onClick={() => setPage(page - 1)}
                        >
                            <ChevronLeft size={16} />
                            Anterior
                        </button>
                        <span>
                            Página {page + 1} de {data.totalPages}
                        </span>
                        <button
                            className="button secondary"
                            disabled={
                                page + 1 >= data.totalPages || page >= 10000
                            }
                            onClick={() => setPage(page + 1)}
                        >
                            Próxima
                            <ChevronRight size={16} />
                        </button>
                    </nav>
                )}
            </section>
            <section className="donate-banner">
                <div className="banner-icon">
                    <PackageOpen size={42} strokeWidth={1.3} />
                </div>
                <div>
                    <span className="eyebrow">TEM ALGO PARADO POR AÍ?</span>
                    <h2>Seu equipamento ainda tem muito para fazer.</h2>
                    <p>
                        Uma doação pode ser o ponto de partida de uma grande
                        ideia.
                    </p>
                </div>
                <button className="button primary" onClick={onCreate}>
                    Publicar uma doação
                    <ArrowUpRight size={18} />
                </button>
            </section>
            <section id="como-funciona" className="how-section">
                <span className="eyebrow">SIMPLES, DO INÍCIO AO RECOMEÇO</span>
                <h2>Uma boa conexão muda o destino das coisas.</h2>
                <div className="how-grid">
                    {[
                        [
                            "01",
                            "Encontre ou compartilhe",
                            "Explore o catálogo ou publique um equipamento que merece continuar em uso.",
                        ],
                        [
                            "02",
                            "Reserve com tranquilidade",
                            "Entre na sua conta e escolha a quantidade. Confira o prazo na sua reserva.",
                        ],
                        [
                            "03",
                            "Combine e transforme",
                            "Organize a retirada fora da plataforma. O doador confirma a entrega por aqui.",
                        ],
                    ].map(([n, t, d]) => (
                        <article key={n}>
                            <span>{n}</span>
                            <h3>{t}</h3>
                            <p>{d}</p>
                        </article>
                    ))}
                </div>
            </section>
        </>
    );
}
