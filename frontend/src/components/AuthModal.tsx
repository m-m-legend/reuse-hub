import { useState, type FormEvent } from "react";
import { ArrowUpRight, Leaf } from "lucide-react";
import { Modal } from "./Modal";
import { request, errorMessage } from "../lib/api";
import { useSession } from "../lib/session";
export function AuthModal({
    onClose,
    onSuccess,
}: {
    onClose: () => void;
    onSuccess: () => void;
}) {
    const { login } = useSession();
    const [register, setRegister] = useState(false);
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState("");
    async function submit(e: FormEvent<HTMLFormElement>) {
        e.preventDefault();
        if (busy) return;
        const data = new FormData(e.currentTarget);
        const email = String(data.get("email"));
        const password = String(data.get("password"));
        if (new TextEncoder().encode(password).length > 72) {
            setError(
                "A senha deve ter até 72 bytes. Acentos podem ocupar mais de um byte.",
            );
            return;
        }
        setBusy(true);
        setError("");
        let created = false;
        try {
            if (register) {
                await request("/auth/register", {
                    method: "POST",
                    body: JSON.stringify({
                        name: data.get("name"),
                        email,
                        password,
                    }),
                });
                created = true;
            }
            await login(email, password);
            onSuccess();
        } catch (err) {
            if (created) setRegister(false);
            setError(
                (created ? "Conta criada. Entre com seus dados. " : "") +
                    errorMessage(err),
            );
        } finally {
            setBusy(false);
        }
    }
    return (
        <Modal
            title={
                register ? "Faça parte deste ciclo" : "Bom ter você por aqui"
            }
            onClose={() => {
                if (!busy) onClose();
            }}
        >
            <div className="auth-intro">
                <span className="symbol">
                    <Leaf size={22} />
                </span>
                <p>
                    {register
                        ? "Crie sua conta para dar um novo destino a equipamentos e ideias."
                        : "Entre para publicar doações e acompanhar suas reservas."}
                </p>
            </div>
            <form onSubmit={submit} className="form-stack">
                {register && (
                    <label>
                        Seu nome
                        <input
                            name="name"
                            autoComplete="name"
                            required
                            maxLength={100}
                            placeholder="Como podemos chamar você?"
                        />
                    </label>
                )}
                <label>
                    E-mail
                    <input
                        name="email"
                        type="email"
                        autoComplete="email"
                        required
                        maxLength={254}
                        placeholder="voce@exemplo.com"
                    />
                </label>
                <label>
                    Senha
                    <input
                        name="password"
                        type="password"
                        autoComplete={
                            register ? "new-password" : "current-password"
                        }
                        required
                        minLength={register ? 12 : 1}
                        maxLength={72}
                        placeholder={
                            register ? "Pelo menos 12 caracteres" : "Sua senha"
                        }
                    />
                </label>
                {error && (
                    <p className="error" role="alert">
                        {error}
                    </p>
                )}
                <button className="button primary full" disabled={busy}>
                    {busy
                        ? "Aguarde…"
                        : register
                          ? "Criar minha conta"
                          : "Entrar"}
                    <ArrowUpRight size={18} />
                </button>
            </form>
            <p className="auth-switch">
                {register ? "Já tem uma conta?" : "Ainda não tem conta?"}{" "}
                <button
                    className="text-button"
                    disabled={busy}
                    onClick={() => {
                        setRegister(!register);
                        setError("");
                    }}
                >
                    {register ? "Entrar" : "Cadastre-se"}
                </button>
            </p>
            <p className="fineprint">
                Por segurança, sua sessão termina ao atualizar a página ou
                quando o acesso expira.
            </p>
        </Modal>
    );
}
