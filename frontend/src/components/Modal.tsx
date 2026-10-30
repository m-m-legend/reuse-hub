import { useEffect, useRef, type ReactNode } from "react";
import { X } from "lucide-react";
export function Modal({
    title,
    children,
    onClose,
    wide = false,
}: {
    title: string;
    children: ReactNode;
    onClose: () => void;
    wide?: boolean;
}) {
    const ref = useRef<HTMLDialogElement>(null);
    useEffect(() => {
        const element = ref.current!;
        element.showModal();
        return () => element.close();
    }, []);
    return (
        <dialog
            ref={ref}
            className={wide ? "modal wide" : "modal"}
            aria-label={title}
            onCancel={(e) => {
                e.preventDefault();
                onClose();
            }}
            onClick={(e) => {
                if (e.target === ref.current) {
                    const r = ref.current.getBoundingClientRect();
                    if (
                        e.clientX < r.left ||
                        e.clientX > r.right ||
                        e.clientY < r.top ||
                        e.clientY > r.bottom
                    )
                        onClose();
                }
            }}
        >
            <div className="modal-header">
                <h2>{title}</h2>
                <button
                    className="icon-button"
                    aria-label="Fechar"
                    onClick={onClose}
                >
                    <X size={22} />
                </button>
            </div>
            {children}
        </dialog>
    );
}
