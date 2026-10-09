import { AbsoluteFill, Sequence, interpolate, spring, useCurrentFrame } from "remotion";
import { End, TAGLINE } from "./End";
import { Logo } from "./Logo";
import { MorphShape, ShapeName, easeInOut } from "./parts";
import { SCHEMES, Scheme } from "./schemes";
import { flex, fontFace } from "./theme";
import { DAYS } from "./Swipe";
import { Caption, Card, Header, Lang, Touch, dayTitle, layout } from "./kit";

/**
 * Вариант «Каждый раз новые цвета»: рабочий стол → касание иконки → приложение раскрывается
 * кругом в цветах этих обоев → обратно → новые обои… Всё быстрее. В конце — шесть телефонов
 * рядом, в шести схемах, светлых и тёмных.
 */
export const COLORS_FRAMES = 600;
const END_AT = 486;

// Шесть «обоев»: лаванда, лес (тёмная), коралл, море (тёмная), песок, ягода (тёмная).
const PALETTE = SCHEMES.slice(0, 6);

// Цикл: [длительность рабочего стола, длительность приложения]. Темп разгоняется.
const CYCLES: [number, number][] = [
  [26, 78],
  [12, 60],
  [9, 44],
  [7, 32],
  [6, 26],
  [6, 24],
];
const starts = (() => {
  const out: { home: number; app: number; close: number }[] = [];
  let t = 0;
  for (const [h, a] of CYCLES) {
    out.push({ home: t, app: t + h, close: t + h + a });
    t += h + a;
  }
  return out;
})();
const GRID_AT = starts[starts.length - 1].close + 4;

const ICON = { x: 540, y: 1180, size: 210 };

const CAPTIONS: Record<Lang, [string, number, number][]> = {
  en: [
    ["Colors follow\nyour wallpaper", 40, 150],
    ["Light and dark", 168, 252],
    ["A new look\nevery time", 262, 316],
    ["Your colors.\nEvery time.", GRID_AT + 30, END_AT - 6],
  ],
  ru: [
    ["Цвета —\nиз ваших обоев", 40, 150],
    ["Светлая и тёмная", 168, 252],
    ["Каждый раз —\nпо-новому", 262, 316],
    ["Ваши цвета.\nКаждый раз.", GRID_AT + 30, END_AT - 6],
  ],
};

export const Colors = ({ lang }: { lang: Lang }) => {
  const f = useCurrentFrame();
  let ci = 0;
  starts.forEach((st, i) => {
    if (f >= st.home) ci = i;
  });
  const st = starts[ci];
  const s = PALETTE[ci];
  const prev = PALETTE[(ci + PALETTE.length - 1) % PALETTE.length];
  const inGrid = f >= GRID_AT;

  // Открытие: круг раскрывается от иконки; закрытие — обратно в неё.
  const open = spring({ frame: f - st.app, fps: 30, config: { damping: 20, stiffness: 130, mass: 0.8 } });
  const close = easeInOut(interpolate(f, [st.close - 7, st.close], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }));
  const reveal = f < st.app ? 0 : open * (1 - close);
  // Новые обои «распускаются» фигурой из центра поверх старых.
  const bloom = ci === 0 ? 1 : easeInOut(interpolate(f, [st.home, st.home + Math.max(5, CYCLES[ci][0] - 2)], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }));
  const tapAt = st.app - 3;

  return (
    <AbsoluteFill style={{ background: "#000" }}>
      <style>{fontFace}</style>
      {!inGrid && (
        <>
          <Wallpaper s={ci === 0 ? s : prev} f={f} />
          {/* Новые обои раскрываются кругом от иконки. */}
          {ci > 0 && (
            <AbsoluteFill style={{ clipPath: `circle(${bloom * 2300}px at ${ICON.x}px ${ICON.y}px)` }}>
              <Wallpaper s={s} f={f} />
            </AbsoluteFill>
          )}
          <HomeIcons s={bloom >= 0.5 ? s : prev} f={f} press={Math.max(0, 1 - Math.abs(f - tapAt) / 5)} />
          {reveal > 0.001 && (
            <AbsoluteFill
              style={{
                clipPath: `circle(${ICON.size / 2 + reveal * 2300}px at ${ICON.x}px ${ICON.y}px)`,
                transform: `scale(${0.85 + 0.15 * reveal})`,
                transformOrigin: `${ICON.x}px ${ICON.y}px`,
              }}
            >
              <AppDay s={s} lang={lang} day={ci} f={f - st.app} />
            </AbsoluteFill>
          )}
          {f >= tapAt - 6 && f < tapAt + 10 && <Touch x={ICON.x + 20} y={ICON.y + 30} down={f >= tapAt - 1 && f < tapAt + 4 ? 1 : 0} show={interpolate(f, [tapAt - 6, tapAt - 2, tapAt + 4, tapAt + 10], [0, 1, 1, 0], { extrapolateLeft: "clamp", extrapolateRight: "clamp" })} tap={f - tapAt} />}
          {CAPTIONS[lang].slice(0, 3).map(([text, from, to]) => (
            <Caption key={text} text={text} from={from} to={to} s={s} y={1600} size={58} />
          ))}
        </>
      )}
      {inGrid && <Grid lang={lang} f={f - GRID_AT} />}
      {inGrid && <Caption text={CAPTIONS[lang][3][0]} from={CAPTIONS[lang][3][1]} to={CAPTIONS[lang][3][2]} s={PALETTE[0]} y={1600} size={60} />}
      <Sequence from={END_AT}>
        <End s={PALETTE[0]} tagline={TAGLINE[lang]} />
      </Sequence>
    </AbsoluteFill>
  );
};

/** Обои: мягкие пятна из цветов схемы — как будто их и взяли с фото. */
const Wallpaper = ({ s, f }: { s: Scheme; f: number }) => (
  <AbsoluteFill
    style={{
      background: `radial-gradient(circle at 20% 15%, ${s.tertiaryContainer} 0%, transparent 55%),
        radial-gradient(circle at 85% 70%, ${s.primaryContainer} 0%, transparent 60%),
        radial-gradient(circle at ${40 + Math.sin(f / 40) * 10}% 95%, ${s.secondaryContainer} 0%, transparent 50%),
        ${s.background}`,
    }}
  />
);

/** Рабочий стол: сетка безымянных иконок-фигур и наша — с подписью. */
const HomeIcons = ({ s, f, press }: { s: Scheme; f: number; press: number }) => {
  const others: [number, number, ShapeName][] = [
    [180, 880, "cookie9"], [420, 880, "clover4"], [660, 880, "sunny"], [900, 880, "puffy"],
    [180, 1180, "gem"], [900, 1180, "flower"],
    [180, 1480, "cookie12"], [420, 1480, "pill"], [660, 1480, "heart"], [900, 1480, "clover4"],
  ];
  return (
    <AbsoluteFill>
      <div style={{ position: "absolute", top: 200, left: 0, right: 0, textAlign: "center", ...flex(300, 110), fontSize: 220, color: s.onSurface, opacity: 0.9 }}>
        {`${9 + Math.floor(f / 120)}:${String(41 + (Math.floor(f / 30) % 18)).padStart(2, "0")}`}
      </div>
      {others.map(([x, y, sh], i) => (
        <MorphShape key={i} names={[sh, sh]} t={0} size={170} color={i % 2 ? s.secondaryContainer : s.surfaceContainerHigh} rotate={i * 20 + f * 0.1} style={{ position: "absolute", left: x - 85, top: y - 85, opacity: 0.9 }} />
      ))}
      <div style={{ position: "absolute", left: ICON.x - ICON.size / 2, top: ICON.y - ICON.size / 2, transform: `scale(${1 - 0.1 * press})` }}>
        <Logo size={ICON.size} rotate={f * 0.5} />
      </div>
      <div style={{ position: "absolute", left: 0, right: 0, top: ICON.y + ICON.size / 2 + 18, textAlign: "center", ...flex(600), fontSize: 38, color: s.onSurface }}>Caliinda</div>
    </AbsoluteFill>
  );
};

/** День в приложении: карточки прилетают снизу, «сейчас» поднимается. */
const AppDay = ({ s, lang, day, f, still = false }: { s: Scheme; lang: Lang; day: number; f: number; still?: boolean }) => {
  const events = DAYS[lang][day % DAYS[lang].length];
  const { ys } = layout(events);
  return (
    <AbsoluteFill style={{ background: s.background }}>
      <Header s={s} text={dayTitle(lang, day)} />
      {events.map((e, i) => {
        const a = still ? 1 : spring({ frame: f - 2 - i * 3, fps: 30, config: { damping: 14, stiffness: 140, mass: 0.8 } });
        const nowIdx = Math.min(1, events.length - 1);
        const now = i === nowIdx ? spring({ frame: f - 10, fps: 30, config: { damping: 13, stiffness: 110, mass: 0.9 } }) : 0;
        return (
          <Card
            key={i}
            e={e}
            i={i + day}
            s={s}
            top={ys[i]}
            now={now}
            lang={lang}
            f={f}
            style={{ transform: `translateY(${(1 - a) * 500}px)`, opacity: Math.min(1, a * 2) }}
          />
        );
      })}
    </AbsoluteFill>
  );
};

/** Финал: шесть телефонов в шести схемах выпрыгивают по очереди. */
const Grid = ({ lang, f }: { lang: Lang; f: number }) => {
  const W = 300;
  const Hh = (W * 1920) / 1080;
  const gap = 36;
  const left0 = (1080 - 3 * W - 2 * gap) / 2;
  const top0 = 250;
  const bg = spring({ frame: f, fps: 30, config: { damping: 16, stiffness: 120, mass: 0.9 } });
  return (
    <AbsoluteFill style={{ background: PALETTE[0].background }}>
      <MorphShape names={["cookie12", "flower", "sunny"]} t={f / 30} size={1600} color={PALETTE[0].surfaceContainerHigh} rotate={f * 0.3} style={{ position: "absolute", left: -260, top: 160, transform: `scale(${bg})` }} />
      {PALETTE.map((s, i) => {
        const col = i % 3;
        const row = Math.floor(i / 3);
        const p = spring({ frame: f - 4 - i * 4, fps: 30, config: { damping: 11, stiffness: 120, mass: 0.8 } });
        // Телефоны чуть «качаются» — живые.
        const sway = Math.sin((f + i * 13) / 18) * 2;
        return (
          <div
            key={i}
            style={{
              position: "absolute",
              left: left0 + col * (W + gap),
              top: top0 + row * (Hh + gap),
              width: W,
              height: Hh,
              borderRadius: 40,
              overflow: "hidden",
              boxShadow: "0 24px 60px rgba(0,0,0,0.25)",
              transform: `translateY(${(1 - p) * 500}px) scale(${0.6 + 0.4 * p}) rotate(${(1 - p) * (col - 1) * 12 + sway}deg)`,
              opacity: Math.min(1, p * 2),
            }}
          >
            <div style={{ width: 1080, height: 1920, transform: `scale(${W / 1080})`, transformOrigin: "0 0", position: "relative" }}>
              <AppDay s={s} lang={lang} day={i} f={f - 4 - i * 4} />
            </div>
          </div>
        );
      })}
    </AbsoluteFill>
  );
};
