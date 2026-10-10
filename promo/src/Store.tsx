import { AbsoluteFill, Img, staticFile } from "remotion";
import { Lang } from "./kit";
import { Shape, ShapeName } from "./parts";
import { JUICY } from "./schemes";
import { flex, fontFace } from "./theme";

/**
 * Скриншоты для Google Play: настоящий экран с эмулятора в телефоне, крупная подпись и фигура
 * темы за ним. Экраны — `public/store/<lang>_<shot>.png` (1080×2400), снимаются с локального
 * демо-календаря. Одна мысль на кадр, как в роликах.
 */

// Экран телефона в кадре: ширина и где начинается (низ уходит за край — так крупнее).
const PHONE_W = 800;
const K = PHONE_W / 1080;
const PHONE_X = (1080 - PHONE_W) / 2;
const PHONE_Y = 560;
const BEZEL = 16;

/** Кусок экрана, вынесенный поверх телефона: координаты в px исходного скриншота. */
type Pop = { x: number; y: number; w: number; h: number; r: number; scale: number };

type Shot = {
  bg: string;
  ink: string;
  shape: ShapeName;
  shapeColor: string;
  title: Record<Lang, string>;
  sub: Record<Lang, string>;
  pop?: Pop;
};

export const SHOTS: Record<string, Shot> = {
  day: {
    bg: JUICY.tertiaryContainer,
    ink: JUICY.onTertiaryContainer,
    shape: "flower",
    shapeColor: "rgba(255,255,255,0.35)",
    title: { en: "Your day\nat a glance", ru: "Весь день\nодним взглядом" },
    sub: { en: "What's happening now always stands out", ru: "То, что идёт сейчас, всегда на виду" },
    // Текущее событие «поднимается к зрителю», как в приложении.
    pop: { x: 42, y: 1195, w: 996, h: 210, r: 44, scale: 1.12 },
  },
  long: {
    bg: JUICY.primaryContainer,
    ink: JUICY.onPrimaryContainer,
    shape: "puffy",
    shapeColor: "rgba(255,255,255,0.4)",
    title: { en: "Anything longer\nthan a day", ru: "Всё, что\nдольше дня" },
    sub: { en: "Trips, courses, moves — and how many days are left", ru: "Поездки, курсы, переезды — и сколько дней осталось" },
  },
};

const Screen = ({ src }: { src: string }) => (
  <Img src={src} style={{ width: PHONE_W, height: 2400 * K, display: "block" }} />
);

export const StoreShot = ({ shot, lang }: { shot: string; lang: Lang }) => {
  const s = SHOTS[shot];
  const src = staticFile(`store/${lang}_${shot}.png`);
  const p = s.pop;
  return (
    <AbsoluteFill style={{ background: s.bg, overflow: "hidden" }}>
      <style>{fontFace}</style>
      <Shape name={s.shape} size={1500} color={s.shapeColor} rotate={14} style={{ position: "absolute", left: -210, top: 520 }} />

      <div style={{ position: "absolute", top: 120, left: 60, right: 60, textAlign: "center", color: s.ink }}>
        {s.title[lang].split("\n").map((line) => (
          <div key={line} style={{ ...flex(900, 112), fontSize: 104, lineHeight: 1.02, letterSpacing: -1.5 }}>
            {line}
          </div>
        ))}
        <div style={{ ...flex(500, 100), fontSize: 40, lineHeight: 1.25, marginTop: 28, opacity: 0.85 }}>{s.sub[lang]}</div>
      </div>

      {/* Телефон: тонкая тёмная рамка, низ уходит за край кадра. */}
      <div
        style={{
          position: "absolute",
          left: PHONE_X - BEZEL,
          top: PHONE_Y - BEZEL,
          padding: BEZEL,
          background: "#1B1B1F",
          borderRadius: 72,
          boxShadow: "0 30px 80px rgba(0,0,0,0.28)",
        }}
      >
        <div style={{ borderRadius: 58, overflow: "hidden" }}>
          <Screen src={src} />
        </div>
      </div>

      {/* Вынесенная карточка — тот же кусок скриншота, крупнее и с плотной тенью. */}
      {p && (
        <div
          style={{
            position: "absolute",
            left: PHONE_X + (p.x + p.w / 2) * K - (p.w * K * p.scale) / 2,
            top: PHONE_Y + (p.y + p.h / 2) * K - (p.h * K * p.scale) / 2,
            width: p.w * K * p.scale,
            height: p.h * K * p.scale,
            borderRadius: p.r * K * p.scale,
            overflow: "hidden",
            boxShadow: "0 14px 22px rgba(0,0,0,0.32)",
          }}
        >
          <div style={{ transform: `scale(${p.scale}) translate(${-p.x * K}px, ${-p.y * K}px)`, transformOrigin: "0 0" }}>
            <Screen src={src} />
          </div>
        </div>
      )}
    </AbsoluteFill>
  );
};
