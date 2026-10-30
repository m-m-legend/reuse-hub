import { useCallback, useEffect, useState } from "react";
import {
    ArrowUpRight,
    Plus,
    RefreshCw,
    LogOut,
    X,
    CheckCircle2,
    Menu,
    UserRound,
} from "lucide-react";
import { SessionProvider, useSession } from "./lib/session";
import { Catalog } from "./pages/Catalog";
import { Reservations } from "./pages/Reservations";
import { AuthModal } from "./components/AuthModal";
import { CreateOfferModal } from "./components/CreateOfferModal";
import { OfferModal } from "./components/OfferModal";
function Hub({
    toast,
    notify,
}: {
    toast: string;
    notify: (s: string) => void;
}) {
    const { user, logout } = useSession();
    const [view, setView] = useState<"catalog" | "reservations">("catalog");
    const [auth, setAuth] = useState(false);
    const [create, setCreate] = useState(false);
    const [offerId, setOfferId] = useState<string>();
    const [afterLogin, setAfterLogin] = useState<
        "create" | "reservations" | "offer" | null
    >(null);
    const [revision, setRevision] = useState(0);
    const [mobile, setMobile] = useState(false);
    useEffect(() => {
        if (!user) {
            setCreate(false);
            setView("catalog");
        }
    }, [user]);
    function catalog() {
        setView("catalog");
        setMobile(false);
    }
    function donate() {
        setMobile(false);
        if (user) setCreate(true);
        else {
            setAfterLogin("create");
            setAuth(true);
        }
    }
    function reservations() {
        setMobile(false);
        if (user) setView("reservations");
        else {
            setAfterLogin("reservations");
            setAuth(true);
        }
    }
    function signedIn() {
        setAuth(false);
        if (afterLogin === "create") setCreate(true);
        if (afterLogin === "reservations") setView("reservations");
        setAfterLogin(null);
    }
    return (
        <>
            <a className="skip-link" href="#main">
                Pular para o conteúdo
            </a>
            <header className="header">
                <div className="header-inner">
                    <button
                        className="brand"
                        aria-label="ReUse Hub — início"
                        onClick={catalog}
                    >
                        <span className="brand-mark">
                            <RefreshCw size={24} strokeWidth={2} />
                        </span>
                        <span>
                            ReUse<span className="brand-light"> Hub</span>
                            <small>BOAS COISAS CONTINUAM.</small>
                        </span>
                    </button>
                    <button
                        className="icon-button mobile-toggle"
                        aria-expanded={mobile}
                        aria-label="Abrir menu"
                        onClick={() => setMobile(!mobile)}
                    >
                        {mobile ? <X /> : <Menu />}
                    </button>
                    <nav
                        className={mobile ? "main-nav open" : "main-nav"}
                        aria-label="Principal"
                    >
                        <button
                            className={
                                view === "catalog"
                                    ? "nav-link active"
                                    : "nav-link"
                            }
                            onClick={catalog}
                        >
                            Explorar
                        </button>
                        <button
                            className={
                                view === "reservations"
                                    ? "nav-link active"
                                    : "nav-link"
                            }
                            onClick={reservations}
                        >
                            Minhas reservas
                        </button>
                        <a
                            className="nav-link"
                            href="#como-funciona"
                            onClick={catalog}
                        >
                            Como funciona
                        </a>
                    </nav>
                    <div className="header-actions">
                        {user ? (
                            <>
                                <span className="user-name" title={user.email}>
                                    <UserRound size={16} />
                                    {user.name.split(" ")[0]}
                                </span>
                                <button
                                    className="icon-button"
                                    aria-label="Sair da conta"
                                    onClick={() => {
                                        logout();
                                        setOfferId(undefined);
                                        notify("Você saiu da sua conta.");
                                    }}
                                >
                                    <LogOut size={18} />
                                </button>
                            </>
                        ) : (
                            <button
                                className="login-button"
                                onClick={() => {
                                    setAfterLogin(null);
                                    setAuth(true);
                                }}
                            >
                                Entrar
                            </button>
                        )}
                        <button
                            className="button primary small"
                            onClick={donate}
                        >
                            <Plus size={17} />
                            Quero doar
                        </button>
                    </div>
                </div>
            </header>
            <main id="main" className="container">
                {view === "catalog" ? (
                    <Catalog
                        onOffer={setOfferId}
                        onCreate={donate}
                        revision={revision}
                    />
                ) : (
                    <Reservations
                        notify={notify}
                        onCatalog={catalog}
                        onOffer={setOfferId}
                        revision={revision}
                        onChanged={() => setRevision((r) => r + 1)}
                    />
                )}
            </main>
            <footer className="footer">
                <div className="container footer-inner">
                    <div>
                        <span className="footer-brand">
                            <RefreshCw size={20} />
                            ReUse Hub
                        </span>
                        <p>
                            Equipamentos circulando.
                            <br />
                            Possibilidades crescendo.
                        </p>
                    </div>
                    <span className="footer-note">
                        Feito para conectar pessoas
                        <br />e prolongar boas histórias.
                    </span>
                    <a
                        href="https://github.com/m-m-legend/reuse-hub"
                        target="_blank"
                        rel="noreferrer"
                    >
                        Conheça o projeto
                        <ArrowUpRight size={16} />
                    </a>
                </div>
                <div className="container footer-bottom">
                    <span>Reutilize. Compartilhe. Transforme.</span>
                    <span>Um projeto de código aberto.</span>
                </div>
            </footer>
            {offerId && !auth && (
                <OfferModal
                    id={offerId}
                    onClose={() => setOfferId(undefined)}
                    onLogin={() => {
                        setAfterLogin("offer");
                        setAuth(true);
                    }}
                    onChanged={() => setRevision((r) => r + 1)}
                    notify={notify}
                />
            )}
            {auth && (
                <AuthModal
                    onClose={() => {
                        setAuth(false);
                        setAfterLogin(null);
                    }}
                    onSuccess={signedIn}
                />
            )}
            {create && user && (
                <CreateOfferModal
                    onClose={() => setCreate(false)}
                    onSuccess={(offer) => {
                        setCreate(false);
                        setView("catalog");
                        setOfferId(offer.id);
                        setRevision((r) => r + 1);
                        notify("Doação publicada. Um novo ciclo começa aqui!");
                    }}
                />
            )}
            {toast && (
                <div className="toast" role="status">
                    <CheckCircle2 size={20} />
                    <span>{toast}</span>
                    <button
                        className="icon-button"
                        aria-label="Fechar aviso"
                        onClick={() => notify("")}
                    >
                        <X size={17} />
                    </button>
                </div>
            )}
        </>
    );
}
export default function App() {
    const [toast, setToast] = useState("");
    const notify = useCallback((message: string) => setToast(message), []);
    const expired = useCallback(
        () => notify("Sua sessão expirou. Entre novamente para continuar."),
        [notify],
    );
    useEffect(() => {
        if (!toast) return;
        const timer = setTimeout(() => setToast(""), 7500);
        return () => clearTimeout(timer);
    }, [toast]);
    return (
        <SessionProvider onExpired={expired}>
            <Hub toast={toast} notify={notify} />
        </SessionProvider>
    );
}
