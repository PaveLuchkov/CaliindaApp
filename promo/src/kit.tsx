import { AbsoluteFill, interpolate, spring, useCurrentFrame, useVideoConfig } from "remotion";
import { CARD_W, Circle, DP, Ev, GAP, HEADER_H, MICRO_H, Star, TOP, fmt, heightFor, mixHex } from "./DayScene";
import { Wave } from "./parts";
import { Scheme } from "./schemes";
import { flex } from "./theme";

// Общие детали вариантов ролика: лента дня как в приложении, палец, подписи, физика «веса».

export type Lang = "en" | "ru";
/** Событие дня: название, начало «Ч:ММ», длительность в минутах. */
export type E = { t: string; at: string; min: number };

// Звёзды привязаны к месту в ленте — как форма к id события.
export const STARS: Ev[] = [
  { star: "star5", rot: -20, off: 0.2 },
  { star: "star3", rot: 0, off: 0 },
  { star: "star4", rot: 25, off: 0.3 },
  { star: "star6", rot: -35, off: 0.2 },
  { star: "star7", rot: 10, off: 0.4 },
  { star: "star3", rot: -40, off: 0.1 },
  { star: "star5", rot: 30, off: 0.25 },
];

export const LIST_TOP = TOP + HEADER_H + 16 * DP;

/** Позиции карточек: [open] — доля «раскрытия» каждой (0 — ещё не вставлена, места не занимает). */
export const layout = (events: E[], top = LIST_TOP, open?: number[]) => {
  const ys: number[] = [];
  const hs: number[] = [];
  let y = top;
  events.forEach((e, i) => {
    const o = open ? open[i] : 1;
    const h = heightFor(e.min);
    ys.push(y);
    hs.push(h);
    y += (h + GAP) * Math.max(0, o);
  });
  return { ys, hs, bottom: y };
};

export const Header = ({ s, text, style }: { s: Scheme; text: string; style?: React.CSSProperties }) => (
  <div style={{ position: "absolute", top: TOP, left: 12 * DP, right: 12 * DP, height: HEADER_H, display: "flex", alignItems: "center", gap: 10 * DP, ...style }}>
    <Circle color={s.primary} />
    <div style={{ flex: 1, height: 40 * DP, borderRadius: 20 * DP, background: s.inverseSurface, color: s.inverseOnSurface, display: "flex", alignItems: "center", justifyContent: "center", ...flex(700), fontSize: 16 * DP, overflow: "hidden" }}>
      {text}
    </div>
    <Circle color={s.primary} />
  </div>
);

/**
 * Карточка события как CalendarEventCard: высота от длительности, звезда справа с внутренней
 * тенью, короткие (≤ 25 мин) — в строку. [now] 0..1 — «текущее»: третичный цвет, узкий наклонный шрифт.
 */
export const Card = ({
  e,
  i,
  s,
  top,
  now = 0,
  lang,
  f = 0,
  style,
}: {
  e: E;
  i: number;
  s: Scheme;
  top: number;
  now?: number;
  lang?: Lang;
  f?: number;
  style?: React.CSSProperties;
}) => {
  const h = heightFor(e.min);
  const micro = e.min <= 25;
  const bg = now ? mixHex(s.primaryContainer, s.tertiaryContainer, now) : s.primaryContainer;
  const fg = now ? mixHex(s.onPrimaryContainer, s.onTertiaryContainer, now) : s.onPrimaryContainer;
  const shadow = s.dark ? "255,255,255" : "0,0,0";
  return (
    <div
      style={{
        position: "absolute",
        left: 16 * DP,
        width: CARD_W,
        top,
        height: h,
        borderRadius: 20 * DP,
        background: bg,
        overflow: "hidden",
        boxShadow: now ? `0 ${now * 10 * DP}px ${now * 24 * DP}px rgba(${shadow},${0.28 * now})` : undefined,
        ...style,
      }}
    >
      {!micro && <Star e={STARS[i % STARS.length]} minutes={e.min} bg={bg} />}
      <div
        style={{
          position: "absolute",
          left: 16 * DP,
          right: 16 * DP,
          top: micro ? 0 : 10 * DP,
          bottom: micro ? 0 : undefined,
          display: "flex",
          flexDirection: micro ? "row" : "column",
          alignItems: micro ? "center" : "flex-start",
          gap: micro ? 8 * DP : 2 * DP,
          color: fg,
        }}
      >
        <div style={{ ...flex(600 + 100 * now, 100 - 35 * now, `'slnt' ${-5 * now}, 'GRAD' ${70 * now}`), fontSize: (micro ? 16 : 24) * DP, whiteSpace: "nowrap" }}>{e.t}</div>
        <div style={{ ...flex(450), fontSize: (micro ? 13 : 14) * DP, display: "flex", alignItems: "center", gap: 8 * DP, opacity: 0.9 }}>
          {now > 0.05 && (
            <div style={{ opacity: now }}>
              <Wave width={52 * DP} height={9 * DP} progress={0.4 + f / 900} phase={f / 4} color={fg} />
            </div>
          )}
          {now > 0.5 && lang ? `${lang === "en" ? "25 min left" : "ещё 25 мин"} · ${fmt(e.at, e.min)}` : fmt(e.at, e.min)}
        </div>
      </div>
    </div>
  );
};

// Иконка AddCircle из Material Icons (Apache 2.0).
const ADD_CIRCLE = "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm5 11h-4v4h-2v-4H7v-2h4V7h2v4h4v2z";

export const AddIcon = ({ size, color }: { size: number; color: string }) => (
  <svg width={size} height={size} viewBox="0 0 24 24">
    <path d={ADD_CIRCLE} fill={color} />
  </svg>
);

/** Нижняя панель — HorizontalFloatingToolbar с кнопкой «+». [press] 0..1 — нажатие. */
export const TOOLBAR_Y = 1920 - 40 * DP - 64 * DP;
export const Toolbar = ({ s, press = 0, style }: { s: Scheme; press?: number; style?: React.CSSProperties }) => (
  <div
    style={{
      position: "absolute",
      left: 540 - 36 * DP,
      top: TOOLBAR_Y,
      width: 72 * DP,
      height: 64 * DP,
      borderRadius: 32 * DP,
      background: s.surfaceContainerHigh,
      display: "flex",
      alignItems: "center",
      justifyContent: "center",
      boxShadow: "0 8px 24px rgba(0,0,0,0.18)",
      transform: `scale(${1 - 0.1 * press})`,
      ...style,
    }}
  >
    <div style={{ width: 48 * DP, height: 48 * DP, borderRadius: 24 * DP, background: `rgba(127,127,127,${0.25 * press})`, display: "flex", alignItems: "center", justifyContent: "center" }}>
      <AddIcon size={24 * DP} color={s.onSurface} />
    </div>
  </div>
);

/**
 * Палец: мягкий круг, при касании сжимается и пускает кольцо. [down] 0..1 — прижат,
 * [tap] — кадры с момента касания (для кольца), [show] — видимость.
 */
export const Touch = ({ x, y, down, show = 1, tap = -1 }: { x: number; y: number; down: number; show?: number; tap?: number }) => {
  const ring = tap >= 0 && tap < 18 ? tap / 18 : -1;
  return (
    <div style={{ position: "absolute", left: x, top: y, width: 0, height: 0, opacity: show, pointerEvents: "none", zIndex: 50 }}>
      {ring >= 0 && (
        <div
          style={{
            position: "absolute",
            left: -70 - ring * 60,
            top: -70 - ring * 60,
            width: 140 + ring * 120,
            height: 140 + ring * 120,
            borderRadius: "50%",
            border: `6px solid rgba(255,255,255,${0.9 * (1 - ring)})`,
            boxShadow: `0 0 0 3px rgba(0,0,0,${0.15 * (1 - ring)})`,
          }}
        />
      )}
      <div
        style={{
          position: "absolute",
          left: -55,
          top: -55,
          width: 110,
          height: 110,
          borderRadius: "50%",
          background: "rgba(255,255,255,0.55)",
          border: "4px solid rgba(255,255,255,0.95)",
          boxShadow: "0 10px 30px rgba(0,0,0,0.25)",
          transform: `scale(${(1 - 0.22 * down) * show})`,
          backdropFilter: "blur(2px)",
        }}
      />
    </div>
  );
};

/**
 * Подпись: таблетка inverseSurface (как дата в шапке), слова выпрыгивают по одному.
 * Держится на кадрах [from, to).
 */
export const Caption = ({ text, from, to, s, y = 1560, size = 64 }: { text: string; from: number; to: number; s: Scheme; y?: number; size?: number }) => {
  const f = useCurrentFrame();
  const { fps } = useVideoConfig();
  if (f < from - 1 || f > to + 12) return null;
  const pill = spring({ frame: f - from, fps, config: { damping: 14, stiffness: 160, mass: 0.8 } });
  const out = interpolate(f, [to, to + 10], [1, 0], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
  const lines = text.split("\n");
  let w = 0;
  return (
    <div style={{ position: "absolute", left: 0, right: 0, top: y, display: "flex", justifyContent: "center", zIndex: 40, pointerEvents: "none" }}>
      <div
        style={{
          background: s.inverseSurface,
          color: s.inverseOnSurface,
          borderRadius: 60,
          padding: "26px 52px 30px",
          transform: `translateY(${(1 - pill) * 80 + (1 - out) * 40}px) scale(${(0.7 + 0.3 * pill) * (0.9 + 0.1 * out)})`,
          opacity: Math.min(1, pill * 2) * out,
          boxShadow: "0 20px 50px rgba(0,0,0,0.25)",
          textAlign: "center",
          maxWidth: 960,
        }}
      >
        {lines.map((line, li) => (
          <div key={li} style={{ whiteSpace: "nowrap" }}>
            {line.split(" ").map((word, wi) => {
              const k = w++;
              const p = spring({ frame: f - from - 3 - k * 3, fps, config: { damping: 11, stiffness: 180, mass: 0.6 } });
              return (
                <span
                  key={wi}
                  style={{
                    display: "inline-block",
                    ...flex(400 + 500 * p, 60 + 50 * p),
                    fontSize: size,
                    lineHeight: 1.15,
                    marginRight: "0.25em",
                    opacity: Math.min(1, p * 1.6),
                    transform: `translateY(${(1 - p) * 30}px)`,
                  }}
                >
                  {word}
                </span>
              );
            })}
          </div>
        ))}
      </div>
    </div>
  );
};

/**
 * «Вес» карточек — SpringyScroll из приложения: грузик на пружине (жёсткость 280, затухание
 * 0.5) отстаёт от прокрутки. [scrollAt] — положение содержимого в px по кадрам. Возвращает
 * отставание на каждом кадре. Считаем честно с нуля: Remotion рендерит кадры вразнобой.
 */
const lagCache = new Map<string, number[]>();
export const lagSeries = (key: string, frames: number, scrollAt: (f: number) => number, fps = 30) => {
  const hit = lagCache.get(key);
  if (hit && hit.length >= frames) return hit;
  const STIFF = 280;
  const DAMP = 2 * 0.5 * Math.sqrt(STIFF);
  const SUB = 8;
  const out: number[] = [];
  let x = 0;
  let v = 0;
  let last = scrollAt(0);
  for (let f = 0; f < frames; f++) {
    const pos = scrollAt(f);
    x -= pos - last;
    last = pos;
    const dt = 1 / fps / SUB;
    for (let k = 0; k < SUB; k++) {
      v += (-STIFF * x - DAMP * v) * dt;
      x += v * dt;
    }
    out.push(x);
  }
  lagCache.set(key, out);
  return out;
};

/**
 * Сдвиг карточки по отставанию [x]: отстают те, что позади переднего края страницы, и тем
 * сильнее, чем дальше. Для промо связь чуть сильнее, чем в приложении, — чтобы читалась.
 */
export const lagOffset = (x: number, center: number, viewport = 1920, coupling = 0.45, max = 130 * DP) => {
  if (x === 0) return 0;
  const anchor = x > 0 ? 0 : viewport;
  const distance = Math.max(0, Math.min(1, ((center - anchor) * Math.sign(x)) / viewport));
  return Math.max(-max, Math.min(max, x * coupling * distance));
};

/** Фон-подложка: крупные фигуры приложения медленно вращаются. */
export const Backdrop = ({ s, children }: { s: Scheme; children?: React.ReactNode }) => <AbsoluteFill style={{ background: s.background }}>{children}</AbsoluteFill>;

export const MICRO = MICRO_H;

const WD = {
  en: ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"],
  ru: ["Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"],
};
const MON = {
  en: ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"],
  ru: ["января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря"],
};
export const MONTH_NAME = {
  en: MON.en,
  ru: ["Январь", "Февраль", "Март", "Апрель", "Май", "Июнь", "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"],
};

/** Подпись дня в шапке: [offset] — дней от четверга, 8 октября 2026. */
export const dayTitle = (lang: Lang, offset: number) => {
  const d = new Date(Date.UTC(2026, 9, 8 + offset));
  const wd = WD[lang][(d.getUTCDay() + 6) % 7];
  return `${wd}, ${d.getUTCDate()} ${MON[lang][d.getUTCMonth()]}`;
};
