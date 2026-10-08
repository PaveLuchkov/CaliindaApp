import { flex } from "./theme";

// Иконка приложения из ic_launcher_foreground/background (viewport 108): чёрное «печенье» и
// календарь с карандашом на зелёном градиенте. В иконке календарь «прорезан» — виден градиент.
export const LOGO_COOKIE =
  "M44.61,17.5C49.81,12.17 58.44,12.17 63.64,17.5C66.17,20.09 69.67,21.54 73.32,21.49C80.76,21.4 86.82,27.39 86.73,34.75L86.73,34.98C86.69,38.59 88.14,42.07 90.74,44.62C96.09,49.85 96.09,58.4 90.74,63.63C88.14,66.18 86.69,69.66 86.73,73.27L86.73,73.5C86.82,80.86 80.76,86.85 73.32,86.76C69.67,86.71 66.17,88.16 63.64,90.75C58.44,96.08 49.81,96.08 44.61,90.75C42.08,88.16 38.58,86.71 34.94,86.76C27.49,86.85 21.43,80.86 21.52,73.5L21.52,73.27C21.56,69.66 20.11,66.18 17.51,63.63C12.16,58.4 12.16,49.85 17.51,44.62C20.11,42.07 21.56,38.59 21.52,34.98L21.52,34.75C21.43,27.39 27.49,21.4 34.94,21.49C38.58,21.54 42.08,20.09 44.61,17.5Z";
export const LOGO_GLYPH =
  "M39.22,78C37.26,78 35.61,77.33 34.27,75.98C32.92,74.64 32.25,72.99 32.25,71.03V41.39C32.25,39.44 32.92,37.79 34.27,36.44C35.61,35.1 37.26,34.42 39.22,34.42H40.77V30H46.46V34.42H61.61V30H67.31V34.42H68.86C70.81,34.42 72.46,35.1 73.81,36.44C75.15,37.79 75.83,39.44 75.83,41.39V51.46H68.86V49.58H39.22V71.03H53.71V78H39.22ZM58.13,78V69.65L70.68,57.15C71.13,56.7 71.62,56.38 72.16,56.18C72.7,55.98 73.24,55.88 73.78,55.88C74.37,55.88 74.93,55.99 75.47,56.21C76.01,56.43 76.49,56.76 76.93,57.21L78.98,59.25C79.38,59.7 79.69,60.19 79.91,60.73C80.14,61.27 80.25,61.81 80.25,62.35C80.25,62.94 80.15,63.5 79.95,64.04C79.74,64.57 79.42,65.06 78.98,65.5L66.48,78H58.13ZM74.33,63.95L75.83,62.35L73.78,60.3L72.23,61.85L74.33,63.95Z";
export const LOGO_GREEN_A = "#C6FF94";
export const LOGO_GREEN_B = "#43FFA7";

/**
 * Иконка как на рабочем столе (круглая маска). [bg] — раскрытие зелёного круга 0..1, [glyph] —
 * проявление календаря, [cookieD] — можно подменить контур печенья (для морфа из фигуры).
 */
export const Logo = ({
  size,
  bg = 1,
  glyph = 1,
  cookieD = LOGO_COOKIE,
  cookieColor = "#000000",
  rotate = 0,
}: {
  size: number;
  bg?: number;
  glyph?: number;
  cookieD?: string;
  cookieColor?: string;
  rotate?: number;
}) => (
  <svg width={size} height={size} viewBox="0 0 108 108" style={{ overflow: "visible" }}>
    <defs>
      <linearGradient id="logoGrad" x1="0" y1="0" x2="1" y2="1">
        <stop offset="0.02" stopColor={LOGO_GREEN_A} />
        <stop offset="0.99" stopColor={LOGO_GREEN_B} />
      </linearGradient>
    </defs>
    {bg > 0 && <circle cx="54" cy="54" r={54 * bg} fill="url(#logoGrad)" />}
    {/* Слой foreground: scale 0.64 вокруг центра, как в ic_launcher_foreground. */}
    <g transform="translate(19.44 19.44) scale(0.64)">
      <path d={cookieD} fill={cookieColor} transform={`rotate(${rotate} 54 54)`} />
      <path d={LOGO_GLYPH} fill="url(#logoGrad)" opacity={glyph} transform={`translate(54 54) scale(${0.6 + 0.4 * glyph}) translate(-54 -54)`} />
    </g>
  </svg>
);

export const Wordmark = ({ size, color, wght = 1000, wdth = 125, style }: { size: number; color: string; wght?: number; wdth?: number; style?: React.CSSProperties }) => (
  <div style={{ ...flex(wght, wdth), fontSize: size, color, letterSpacing: -size * 0.01, ...style }}>Caliinda</div>
);
