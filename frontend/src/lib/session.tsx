import {
    createContext,
    useContext,
    useEffect,
    useState,
    type ReactNode,
} from "react";
import { request, setToken } from "./api";
import type { Account } from "./types";
interface Session {
    user: Account | null;
    login: (email: string, password: string) => Promise<void>;
    logout: () => void;
}
const Context = createContext<Session>(null!);
// JWT apenas em memória. Atualizar a página encerra a sessão da interface.
export function SessionProvider({
    children,
    onExpired,
}: {
    children: ReactNode;
    onExpired: () => void;
}) {
    const [user, setUser] = useState<Account | null>(null);
    const [deadline, setDeadline] = useState(0);
    function logout() {
        setToken(null);
        setUser(null);
        setDeadline(0);
    }
    useEffect(() => {
        const expire = () => {
            logout();
            onExpired();
        };
        window.addEventListener("session-expired", expire);
        const timer = deadline
            ? setTimeout(expire, Math.max(0, deadline - Date.now()))
            : undefined;
        return () => {
            window.removeEventListener("session-expired", expire);
            clearTimeout(timer);
        };
    }, [deadline, onExpired]);
    async function login(email: string, password: string) {
        const data = await request<{ accessToken: string; expiresIn: number }>(
            "/auth/login",
            { method: "POST", body: JSON.stringify({ email, password }) },
        );
        setToken(data.accessToken);
        try {
            const account = await request<Account>("/auth/me", {}, true);
            setUser(account);
            setDeadline(Date.now() + data.expiresIn * 1000);
        } catch (error) {
            logout();
            throw error;
        }
    }
    return (
        <Context.Provider value={{ user, login, logout }}>
            {children}
        </Context.Provider>
    );
}
export const useSession = () => useContext(Context);
