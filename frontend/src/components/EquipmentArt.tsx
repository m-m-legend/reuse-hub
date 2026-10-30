import type { Category } from "../lib/types";
// Ilustrações de categoria, sem representar fotos dos equipamentos anunciados.
export function EquipmentArt({
    category,
    hero = false,
}: {
    category: Category;
    hero?: boolean;
}) {
    return (
        <svg
            className={`equipment-art ${hero ? "hero-art" : ""}`}
            viewBox="0 0 360 200"
            fill="none"
            aria-hidden="true"
        >
            <ellipse
                cx="180"
                cy="170"
                rx="112"
                ry="12"
                fill="currentColor"
                opacity=".08"
            />
            {category === "COMPUTERS" && (
                <>
                    <path
                        d="M97 48q0-9 9-9h148q9 0 9 9v100H97Z"
                        fill="#344b46"
                    />
                    <rect
                        x="106"
                        y="48"
                        width="148"
                        height="91"
                        rx="3"
                        fill="#d9e9d8"
                    />
                    <path d="m65 149 32-6h166l32 6-12 14H77Z" fill="#71847d" />
                    <path d="M65 149h230l-12 5H77Z" fill="#a4b5ab" />
                    <path
                        d="M132 116v-30m15 30V74m15 42V95"
                        stroke="#7e9e84"
                        strokeWidth="8"
                    />
                    <circle cx="218" cy="83" r="17" fill="#bfd591" />
                    <path
                        d="m211 84 5 5 10-12"
                        stroke="#466a43"
                        strokeWidth="3"
                    />
                </>
            )}
            {category === "ELECTRONICS" && (
                <>
                    <path d="m106 60 129-20 26 104-129 20Z" fill="#477b68" />
                    <path d="m114 66 115-18 22 90-115 18Z" stroke="#91bba0" />
                    <path d="m159 75 47-8 12 49-47 8Z" fill="#283d37" />
                    <path d="m166 81 34-6 9 35-34 6Z" fill="#567267" />
                    <g stroke="#d5dbb5" strokeWidth="4">
                        <path d="m149 79-20 3m23 8-20 3m23 8-20 3m23 8-20 3m53-48-3-13m15 11-3-13m15 11-3-13m14 29 20-3m-17 14 20-3m-17 14 20-3m-55 16 3 13m9-15 3 13" />
                    </g>
                    <rect
                        x="223"
                        y="120"
                        width="20"
                        height="22"
                        rx="2"
                        fill="#c1c9c0"
                        transform="rotate(-10 223 120)"
                    />
                    <circle cx="126" cy="69" r="3" fill="#d5dbb5" />
                </>
            )}
            {category === "TOOLS" && (
                <>
                    <path d="M110 92h142v67H110Z" fill="#bd7e4d" />
                    <path d="M104 87h154v25H104Z" fill="#dba16a" />
                    <path
                        d="M153 88V69h52v19"
                        stroke="#5a6559"
                        strokeWidth="9"
                    />
                    <rect
                        x="171"
                        y="106"
                        width="18"
                        height="19"
                        rx="2"
                        fill="#f4d28c"
                    />
                    <path d="m251 67-28 49" stroke="#4d5f54" strokeWidth="14" />
                    <path d="m240 42 19 4 8 17-16 13-15-9Z" fill="#7d8c7e" />
                    <path
                        d="m120 42 12 14-8 11-8-4-19 30-9-6 19-30-3-9Z"
                        fill="#899888"
                    />
                </>
            )}
            {category === "EDUCATION" && (
                <>
                    <path d="m102 126 123-22 36 39-122 23Z" fill="#6e8c70" />
                    <path d="m110 129 113-19 26 29-112 22Z" fill="#f3eddb" />
                    <path d="m102 116 123-22 36 39-122 23Z" fill="#a2b88b" />
                    <path d="m111 94 116-17 23 28-116 18Z" fill="#bd895f" />
                    <path d="m115 97 109-15 18 19-109 18Z" fill="#fff4db" />
                    <path d="m111 85 116-17 23 28-116 18Z" fill="#dfab73" />
                    <path d="m143 55 83 7-5 34-83-7Z" fill="#6b8c88" />
                    <path
                        d="m155 67 54 5m-55 6 36 3"
                        stroke="#b6cfbc"
                        strokeWidth="3"
                    />
                </>
            )}
            {category === "OTHER" && (
                <>
                    <path d="m104 87 73 25 78-27-76-24Z" fill="#dbc6a0" />
                    <path d="m104 87 73 25v61l-73-28Z" fill="#b9996c" />
                    <path d="m177 112 78-27v59l-78 29Z" fill="#ceb187" />
                    <path d="m145 73 74 24-13 5-74-25Z" fill="#eee3c4" />
                    <path d="M201 107v25l14-5v-25" fill="#eee3c4" />
                    <path
                        d="m136 119-12 8m0-8 12 14"
                        stroke="#7f7258"
                        strokeWidth="3"
                    />
                </>
            )}
        </svg>
    );
}
