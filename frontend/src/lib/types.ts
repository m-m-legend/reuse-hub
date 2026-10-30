export type Category =
    "COMPUTERS" | "ELECTRONICS" | "TOOLS" | "EDUCATION" | "OTHER";
export type Condition = "NEW" | "GOOD" | "NEEDS_REPAIR";
export type Status = "RESERVED" | "COLLECTED" | "CANCELLED" | "EXPIRED";
export interface Account {
    id: string;
    name: string;
    email: string;
    createdAt: string;
}
export interface Offer {
    id: string;
    ownerId: string;
    title: string;
    description: string;
    category: Category;
    condition: Condition;
    city: string;
    totalQuantity: number;
    availableQuantity: number;
    active: boolean;
    createdAt: string;
}
export interface Reservation {
    id: string;
    offerId: string;
    requesterId: string;
    ownerId: string;
    quantity: number;
    status: Status;
    createdAt: string;
    expiresAt: string;
    updatedAt: string;
}
export interface ReservationEvent {
    id: string;
    sequence: number;
    status: Status;
    actorId: string | null;
    occurredAt: string;
}
export interface Page<T> {
    content: T[];
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
}
export const categories: Record<Category, string> = {
    COMPUTERS: "Informática",
    ELECTRONICS: "Eletrônica",
    TOOLS: "Ferramentas",
    EDUCATION: "Educação",
    OTHER: "Outros",
};
export const conditions: Record<Condition, string> = {
    NEW: "Novo",
    GOOD: "Bom estado",
    NEEDS_REPAIR: "Precisa de reparo",
};
export const statuses: Record<Status, string> = {
    RESERVED: "Reservada",
    COLLECTED: "Retirada",
    CANCELLED: "Cancelada",
    EXPIRED: "Expirada",
};
export function dateTime(date: string) {
    return new Intl.DateTimeFormat("pt-BR", {
        dateStyle: "short",
        timeStyle: "short",
    }).format(new Date(date));
}
