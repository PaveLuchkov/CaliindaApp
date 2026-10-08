import { interpolate as flubber } from "flubber";
import { useMemo } from "react";
import { spring, useCurrentFrame, useVideoConfig } from "remotion";
import shapes from "./shapes.json";
import { C, flex } from "./theme";

export type ShapeName = keyof typeof shapes;

/** Фигура из MaterialShapes; координаты 0..100, рисуем в квадрат [size]. */
export const Shape = ({
  name,
  size,
  color,
  rotate = 0,
  style,
}: {
  name: ShapeName;
  size: number;
  color: string;
  rotate?: number;
  style?: React.CSSProperties;
}) => (
  <svg width={size} height={size} viewBox="0 0 100 100" style={{ overflow: "visible", ...style }}>
    <path d={shapes[name]} fill={color} transform={`rotate(${rotate} 50 50)`} />
  </svg>
);

/**
 * Фигура, перетекающая по списку [names]: [t] — непрерывный «номер фигуры» (1.5 — середина между
 * второй и третьей). Морф — flubber, тот же приём, что Morph в приложении.
 */
export const MorphShape = ({
  names,
  t,
  size,
  color,
  rotate = 0,
  style,
}: {
  names: ShapeName[];
  t: number;
  size: number;
  color: string;
  rotate?: number;
  style?: React.CSSProperties;
}) => {
  const morphs = useMemo(
    () =>
      names.map((n, i) =>
        flubber(shapes[n], shapes[names[(i + 1) % names.length]], { maxSegmentLength: 2 }),
      ),
    [names],
  );
  const i = Math.floor(t) % names.length;
  const local = t - Math.floor(t);
  // Задерживаемся на фигуре и быстро перетекаем — морф читается как «вдох».
  const eased = local < 0.55 ? 0 : easeInOut((local - 0.55) / 0.45);
  return (
    <svg width={size} height={size} viewBox="0 0 100 100" style={{ overflow: "visible", ...style }}>
      <path d={morphs[i](eased)} fill={color} transform={`rotate(${rotate} 50 50)`} />
    </svg>
  );
};

export const easeInOut = (x: number) => (x < 0.5 ? 4 * x * x * x : 1 - Math.pow(-2 * x + 2, 3) / 2);

/** Пружина от кадра [delay] — как пружины в приложении. */
export const useSpring = (delay: number, config = { damping: 14, stiffness: 120, mass: 0.9 }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  return spring({ frame: frame - delay, fps, config });
};

/** Волнистый прогресс; [phase] — бег волны, чтобы «червячок» полз. */
export const Wave = ({
  width,
  height,
  progress,
  phase,
  color,
}: {
  width: number;
  height: number;
  progress: number;
  phase: number;
  color: string;
}) => {
  const stroke = height * 0.34;
  const amp = (height - stroke) / 2;
  const wl = height * 2.6;
  const start = stroke / 2;
  const end = width - stroke / 2;
  const split = start + (end - start) * Math.max(0, Math.min(1, progress));
  const gap = stroke * 1.4;
  let d = "";
  for (let x = start; x <= split; x += 2) {
    const y = height / 2 + amp * Math.sin(((x - start) / wl) * Math.PI * 2 - phase);
    d += `${d ? "L" : "M"}${x.toFixed(1)},${y.toFixed(1)} `;
  }
  return (
    <svg width={width} height={height} style={{ overflow: "visible" }}>
      {split + gap < end && (
        <line
          x1={split + gap}
          y1={height / 2}
          x2={end}
          y2={height / 2}
          stroke={color}
          strokeOpacity={0.3}
          strokeWidth={stroke}
          strokeLinecap="round"
        />
      )}
      <path d={d} fill="none" stroke={color} strokeWidth={stroke} strokeLinecap="round" />
    </svg>
  );
};

/**
 * Карточка события как в ленте: крупное название RobotoFlex, время, фигура выглядывает из-за
 * правого края — видна только её «тень» по кромке.
 */
export const EventCard = ({
  width,
  height,
  title,
  time,
  bg,
  fg,
  shape,
  now,
  children,
  style,
}: {
  width: number;
  height: number;
  title: string;
  time: string;
  bg: string;
  fg: string;
  shape: ShapeName;
  now?: boolean;
  children?: React.ReactNode;
  style?: React.CSSProperties;
}) => {
  const s = height * 1.5;
  return (
    <div
      style={{
        position: "relative",
        width,
        height,
        borderRadius: 40,
        background: bg,
        overflow: "hidden",
        boxShadow: "0 18px 40px rgba(40,30,80,0.12)",
        ...style,
      }}
    >
      <svg
        width={s}
        height={s}
        viewBox="0 0 100 100"
        style={{ position: "absolute", right: -s * 0.42, top: -s * 0.22, overflow: "visible" }}
      >
        <path d={shapes[shape]} fill={fg} fillOpacity={0.3} transform="rotate(-18 50 50)" />
        <path d={shapes[shape]} fill={bg} transform="translate(4.5 6.5) rotate(-18 50 50)" />
      </svg>
      <div style={{ position: "absolute", left: 44, top: 0, bottom: 0, display: "flex", flexDirection: "column", justifyContent: "center" }}>
        <div
          style={{
            ...flex(now ? 820 : 780, now ? 70 : 125, now ? "'slnt' -6" : ""),
            fontSize: height * 0.3,
            color: fg,
            lineHeight: 1.05,
            whiteSpace: "nowrap",
          }}
        >
          {title}
        </div>
        <div style={{ ...flex(450), fontSize: height * 0.14, color: fg, marginTop: 10, display: "flex", alignItems: "center", gap: 16 }}>
          {children}
          {time}
        </div>
      </div>
    </div>
  );
};

/** Число в фигуре — сердце виджета даты и отсчёта. */
export const NumberInShape = ({
  value,
  size,
  shape,
  t,
  color = C.primary,
  textColor = C.onPrimary,
}: {
  value: string;
  size: number;
  shape?: ShapeName;
  t?: number;
  color?: string;
  textColor?: string;
}) => (
  <div style={{ position: "relative", width: size, height: size }}>
    {t !== undefined ? (
      <MorphShape names={["cookie9", "sunny", "flower", "cookie12"]} t={t} size={size} color={color} />
    ) : (
      <Shape name={shape ?? "cookie9"} size={size} color={color} />
    )}
    <div
      style={{
        position: "absolute",
        inset: 0,
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        ...flex(1000, value.length === 1 ? 151 : value.length === 2 ? 100 : 75),
        fontSize: size * (value.length > 2 ? 0.36 : 0.52),
        color: textColor,
        paddingBottom: size * 0.03,
      }}
    >
      {value}
    </div>
  </div>
);
