import { AbsoluteFill, Sequence, interpolate, spring, useCurrentFrame } from "remotion";
import { CARD_W, DP, heightFor } from "./DayScene";
import { End, TAGLINE } from "./End";
import { APRICOT, JUICY, SCHEMES, Scheme, scheme } from "./schemes";
import { fontFace } from "./theme";
import { Caption, Card, E, Header, Lang, dayTitle, layout } from "./kit";

/**
 * Вариант «Цвета» — монтаж в ритм, в духе Apple. Стержень — один и тот же день: те же события,
 * то же «сейчас»; меняются только ракурс и цвета. Открытие: камера под углом, карточки падают
 * на день с весом. Цвет тоже падает — капля краски заливает день новой темой. Дальше нарезка:
 * макро на червячка, бок, взгляд сверху, звезда, дата, подъём «сейчас» — на каждой склейке
 * новая тема, светлые и тёмные. Реприза — «дождь» карточек в тёмной. Финал: камера ровно,
 * по карточкам волнами пробегают цвета.
 */
export const COLORS_FRAMES = 600;
const END_AT = 486;

const LAV = SCHEMES[0];
const FOREST = SCHEMES[1];
const CORAL = SCHEMES[2];
const SEA = SCHEMES[3];
const BERRY = SCHEMES[5];
const PINK = scheme(0xffff6f91, false, "content");
const SKY = scheme(0xff4fc3f7, false, "content");

// Один день на весь ролик. «Сейчас» — урок гитары.
const DAY: Record<Lang, E[]> = {
  en: [
    { t: "🧘 Morning yoga", at: "7:30", min: 45 },
    { t: "💻 Working on a Project", at: "9:30", min: 180 },
    { t: "🥗 Lunch", at: "13:30", min: 45 },
    { t: "📞 Call", at: "15:00", min: 15 },
    { t: "🎸 Guitar lesson", at: "17:00", min: 60 },
    { t: "🍝 Dinner with friends", at: "19:30", min: 120 },
  ],
  ru: [
    { t: "🧘 Утренняя йога", at: "7:30", min: 45 },
    { t: "💻 Работа над проектом", at: "9:30", min: 180 },
    { t: "🥗 Обед", at: "13:30", min: 45 },
    { t: "📞 Созвон", at: "15:00", min: 15 },
    { t: "🎸 Урок гитары", at: "17:00", min: 60 },
    { t: "🍝 Ужин с друзьями", at: "19:30", min: 120 },
  ],
};
const NOW = 4;
const { ys } = layout(DAY.en);
const hs = DAY.en.map((e) => heightFor(e.min));
const nowCenter = { x: 540, y: ys[NOW] + hs[NOW] / 2 };
const worm = { x: 16 * DP + 16 * DP + 30 * DP, y: ys[NOW] + 10 * DP + 34 * DP };
const star = { x: 1080 - 60 * DP, y: ys[1] + hs[1] / 2 };
const pill = { x: 540, y: 70 * DP + 24 * DP };

const CAPTIONS: Record<Lang, [string, number, number][]> = {
  en: [
    ["Your day.", 22, 82],
    ["Your colors.", 104, 164],
    ["Light.", 172, 220],
    ["Dark.", 228, 278],
    ["New every time.", 396, 476],
  ],
  ru: [
    ["Ваш день.", 22, 82],
    ["Ваши цвета.", 104, 164],
    ["Светлые.", 172, 220],
    ["Тёмные.", 228, 278],
    ["Каждый раз — новые.", 396, 476],
  ],
};

// --- Камера: точка страницы в центре кадра, масштаб, повороты.
type Cam = { cx: number; cy: number; s: number; rx: number; ry: number; rz: number };
const TILT: Cam = { cx: 540, cy: 980, s: 0.78, rx: 50, ry: 0, rz: -16 };

type Shot = {
  from: number;
  to: number;
  s: Scheme;
  cam: [Cam, Cam];
  /** Как входим: склейка, проезд с размытием (направление) или капля краски. */
  enter: "cut" | "whip" | "drop";
  dir?: 1 | -1;
  /** Карточки падают: кадр первой и шаг между ними. */
  rain?: [number, number];
  /** «Сейчас» поднимается с этого кадра (иначе уже поднято). */
  lift?: number;
  /** Кадр, когда дата в шапке «перещёлкивается». */
  date?: number;
};

const SHOTS: Shot[] = [
  // Открытие: под углом, карточки падают на день.
  { from: 0, to: 96, s: LAV, enter: "cut", rain: [10, 9], lift: 72, cam: [{ ...TILT, cx: 470 }, { ...TILT, cx: 600, rz: -12 }] },
  // Капля — и тот же день в сочном жёлтом; камера наезжает на «сейчас».
  { from: 96, to: 142, s: JUICY, enter: "drop", cam: [{ ...TILT, cx: 600, rz: -12 }, { cx: nowCenter.x, cy: nowCenter.y, s: 1.5, rx: 18, ry: 0, rz: -4 }] },
  // Макро: червячок ползёт.
  { from: 142, to: 168, s: SEA, enter: "cut", cam: [{ cx: worm.x - 30, cy: worm.y, s: 4.4, rx: 0, ry: 10, rz: 0 }, { cx: worm.x + 70, cy: worm.y, s: 4.0, rx: 0, ry: -4, rz: 0 }] },
  // Сбоку: лента повёрнута, камера едет слева направо.
  { from: 168, to: 198, s: APRICOT, enter: "whip", dir: 1, cam: [{ cx: 380, cy: 900, s: 0.95, rx: 6, ry: -42, rz: 0 }, { cx: 700, cy: 1000, s: 1.0, rx: 6, ry: -34, rz: 0 }] },
  // Сверху: проезд вниз по карточкам.
  { from: 198, to: 224, s: CORAL, enter: "whip", dir: 1, cam: [{ cx: 540, cy: 500, s: 1.35, rx: 0, ry: 0, rz: 6 }, { cx: 540, cy: 1250, s: 1.35, rx: 0, ry: 0, rz: 2 }] },
  // Макро: звезда в карточке с внутренней тенью.
  { from: 224, to: 252, s: BERRY, enter: "cut", cam: [{ cx: star.x, cy: star.y, s: 3.0, rx: 0, ry: 0, rz: -10 }, { cx: star.x - 60, cy: star.y + 30, s: 3.4, rx: 0, ry: 0, rz: 4 }] },
  // Таблетка даты перещёлкивается.
  { from: 252, to: 282, s: FOREST, enter: "whip", dir: -1, date: 258, cam: [{ cx: pill.x - 120, cy: pill.y + 40, s: 2.4, rx: 20, ry: 14, rz: 0 }, { cx: pill.x + 60, cy: pill.y + 40, s: 2.2, rx: 10, ry: -6, rz: 0 }] },
  // Капля — «сейчас» поднимается к зрителю.
  { from: 282, to: 332, s: PINK, enter: "drop", lift: 292, cam: [{ cx: nowCenter.x, cy: nowCenter.y, s: 1.25, rx: 34, ry: 0, rz: 8 }, { cx: nowCenter.x, cy: nowCenter.y, s: 1.6, rx: 22, ry: 10, rz: 2 }] },
  // Реприза: в тёмной теме карточки падают дождём; потом камера выравнивается в обычный экран.
  { from: 332, to: 600, s: SEA, enter: "whip", dir: 1, rain: [336, 4], lift: 366, cam: [{ ...TILT, cx: 540, rz: 14 }, { cx: 540, cy: 960, s: 1, rx: 0, ry: 0, rz: 0 }] },
];
const FLAT_AT = 360;
// Финал: волны цвета по карточкам — всё чаще.
const PASSES: [number, Scheme][] = [
  [394, CORAL],
  [412, BERRY],
  [427, SKY],
  [440, FOREST],
  [451, PINK],
  [460, APRICOT],
];

const lerpCam = (a: Cam, b: Cam, t: number): Cam => ({
  cx: a.cx + (b.cx - a.cx) * t,
  cy: a.cy + (b.cy - a.cy) * t,
  s: a.s + (b.s - a.s) * t,
  rx: a.rx + (b.rx - a.rx) * t,
  ry: a.ry + (b.ry - a.ry) * t,
  rz: a.rz + (b.rz - a.rz) * t,
});
/** Резкий старт, долгая мягкая посадка — камера «прилетает». */
const expoOut = (x: number) => (x >= 1 ? 1 : 1 - Math.pow(2, -8 * x));
/** Удар при посадке: затухающие колебания. */
const impact = (t: number) => (t < 0 ? 0 : Math.exp(-t / 6) * Math.sin(t * 0.85));

const FALL = 11; // кадров на падение
const Z0 = 1100;

export const Colors = ({ lang }: { lang: Lang }) => {
  const f = useCurrentFrame();
  const idx = SHOTS.findIndex((sh) => f >= sh.from && f < sh.to);
  const shot = SHOTS[idx];
  const prev = SHOTS[idx - 1];
  const next = SHOTS[idx + 1];
  // Капля: пока новая тема растекается, под ней ещё виден прошлый план.
  const reveal = shot.enter === "drop" ? interpolate(f - shot.from, [0, 14], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp", easing: (x) => 1 - Math.pow(1 - x, 3) }) : 1;
  const capScheme = (from: number) => SHOTS.find((sh) => from >= sh.from && from < sh.to)!.s;

  return (
    <AbsoluteFill style={{ background: "#000" }}>
      <style>{fontFace}</style>
      {prev && reveal < 1 && <ShotView shot={prev} f={f} lang={lang} />}
      <AbsoluteFill style={{ clipPath: reveal < 1 ? `circle(${reveal * 2300}px at 540px 900px)` : undefined }}>
        <ShotView shot={shot} f={f} lang={lang} exitWhip={next?.enter === "whip" ? next.dir : undefined} />
      </AbsoluteFill>
      {/* Капля падает перед своим планом, на всплеске — кольцо и брызги. */}
      {next?.enter === "drop" && f >= next.from - 8 && <Drop s={next.s} t={f - (next.from - 8)} />}
      {shot.enter === "drop" && f - shot.from < 16 && <Splash s={shot.s} t={f - shot.from} />}
      {CAPTIONS[lang].map(([text, from, to]) => (
        <Caption key={text} text={text} from={from} to={to} s={capScheme(from)} y={1660} size={72} />
      ))}
      <Sequence from={END_AT}>
        <End s={APRICOT} tagline={TAGLINE[lang]} />
      </Sequence>
    </AbsoluteFill>
  );
};

/** Один план: день в теме плана, камера, проезды с размытием на входе и выходе. */
const ShotView = ({ shot, f, lang, exitWhip }: { shot: Shot; f: number; lang: Lang; exitWhip?: 1 | -1 }) => {
  const len = shot.to - shot.from;
  const t = f - shot.from;
  const last = shot === SHOTS[SHOTS.length - 1];
  // Последний план: сначала держим угол (дождь), потом камера на пружине выравнивается.
  const p = last ? spring({ frame: f - FLAT_AT, fps: 30, config: { damping: 16, stiffness: 90, mass: 1 } }) : expoOut(Math.max(0, t) / len);
  const cam = lerpCam(shot.cam[0], shot.cam[1], p);
  let whipX = 0;
  let blur = 0;
  if (shot.enter === "whip" && t < 5) {
    const k = 1 - t / 5;
    whipX = -(shot.dir ?? 1) * k * k * 900;
    blur = k * 26;
  }
  if (exitWhip && t > len - 5) {
    const k = (t - (len - 5)) / 5;
    whipX = exitWhip * k * k * 900;
    blur = k * 26;
  }
  const s = last ? finalScheme(f, -1) : shot.s;
  // День вздрагивает от каждой посадки.
  const dip = shot.rain ? DAY[lang].reduce((a, _, i) => a + impact(f - (shot.rain![0] + i * shot.rain![1] + FALL)), 0) : 0;
  return (
    <AbsoluteFill style={{ background: s.dark ? "#0c0d10" : "#ece9f0", perspective: 2200, overflow: "hidden" }}>
      <div
        style={{
          position: "absolute",
          left: 0,
          top: 0,
          width: 1080,
          height: 1920,
          transformOrigin: "0 0",
          transformStyle: "preserve-3d",
          transform: `translate(${540 + whipX}px, 960px) rotateX(${cam.rx}deg) rotateY(${cam.ry}deg) rotateZ(${cam.rz}deg) scale(${cam.s}) translate(${-cam.cx}px, ${-cam.cy}px) translateZ(${-dip * 14}px)`,
          filter: blur > 0.5 ? `blur(${blur}px)` : undefined,
        }}
      >
        <Page shot={shot} f={f} lang={lang} s={s} last={last} />
      </div>
    </AbsoluteFill>
  );
};

/**
 * Тема в финале: волна сверху вниз пробегает за пару кадров (фон — посередине), чтобы каждая
 * тема успела постоять целиком, а не рябила вперемешку.
 */
const finalScheme = (f: number, i: number) => {
  let s = SEA;
  const delay = i < 0 ? 3 : i;
  PASSES.forEach(([at, sc]) => {
    if (f >= at + delay) s = sc;
  });
  return s;
};

/** Страница дня: шапка, карточки — падают, приземляются, «сейчас» поднимается. */
const Page = ({ shot, f, lang, s, last }: { shot: Shot; f: number; lang: Lang; s: Scheme; last: boolean }) => {
  const events = DAY[lang];
  const nowP = shot.lift === undefined ? 1 : spring({ frame: f - shot.lift, fps: 30, config: { damping: 11, stiffness: 120, mass: 0.9 } });
  // Дата перещёлкивается: вчерашняя уезжает вверх, сегодняшняя въезжает на пружине.
  const roll = shot.date !== undefined ? spring({ frame: f - shot.date, fps: 30, config: { damping: 12, stiffness: 170, mass: 0.7 } }) : 1;
  return (
    <div style={{ position: "absolute", inset: 0, background: s.background, borderRadius: 60, transformStyle: "preserve-3d", boxShadow: "0 60px 160px rgba(0,0,0,0.35)" }}>
      <div style={{ position: "absolute", inset: 0, overflow: "hidden", borderRadius: 60 }}>
        {roll < 1 && <Header s={s} text={dayTitle(lang, 1)} style={{ transform: `translateY(${-roll * 80}px)`, opacity: 1 - roll }} />}
        <Header s={s} text={dayTitle(lang, 2)} style={{ transform: `translateY(${(1 - roll) * 80}px)`, opacity: roll }} />
      </div>
      {events.map((e, i) => {
        let z = 0;
        let near = 1; // 0 — высоко, 1 — на земле
        let sq = 0;
        if (shot.rain) {
          const t0 = shot.rain[0] + i * shot.rain[1];
          const u = (f - t0) / FALL;
          if (u < 0) return null;
          if (u < 1) {
            z = Z0 * (1 - u * u); // ускоряется к земле
            near = u;
          }
          sq = impact(f - t0 - FALL);
        }
        // В финале карточка «вспыхивает» новой темой — короткий пружинный толчок.
        const pop = last ? PASSES.reduce((a, [at]) => a + impact(f - at - i), 0) : 0;
        const now = i === NOW ? nowP : 0;
        return (
          <div key={i} style={{ transformStyle: "preserve-3d" }}>
            {/* Тень на «полу» дня: сжимается и темнеет, пока карточка подлетает. */}
            {z > 0 && (
              <div
                style={{
                  position: "absolute",
                  left: 16 * DP,
                  top: ys[i],
                  width: CARD_W,
                  height: hs[i],
                  borderRadius: 20 * DP,
                  background: "rgba(0,0,0,0.5)",
                  filter: `blur(${10 + (1 - near) * 40}px)`,
                  opacity: 0.15 + 0.5 * near,
                  transform: `scale(${0.85 + 0.15 * near})`,
                }}
              />
            )}
            <Card
              e={e}
              i={i}
              s={last ? finalScheme(f, i) : s}
              top={ys[i]}
              now={now}
              lang={lang}
              f={f}
              style={{
                transform: `translateZ(${z + now * 24}px) scale(${1 + 0.04 * sq + 0.03 * pop}, ${1 - 0.12 * sq + 0.03 * pop})`,
                transformOrigin: "50% 100%",
              }}
            />
          </div>
        );
      })}
    </div>
  );
};

/** Капля краски цвета следующей темы падает сверху в точку всплеска. */
const Drop = ({ s, t }: { s: Scheme; t: number }) => {
  if (t > 8) return null;
  const u = t / 8;
  const y = -120 + (900 + 120) * u * u;
  return (
    <div
      style={{
        position: "absolute",
        left: 540 - 50,
        top: y - 70,
        width: 100,
        height: 140,
        borderRadius: "50% 50% 50% 50% / 60% 60% 40% 40%",
        background: s.primary,
        boxShadow: "0 20px 40px rgba(0,0,0,0.3), inset -10px -14px 0 rgba(0,0,0,0.12), inset 10px 10px 0 rgba(255,255,255,0.25)",
        transform: `scaleY(${1 + u * 0.4})`,
      }}
    />
  );
};

/** Всплеск: кольцо и брызги цвета новой темы. */
const Splash = ({ s, t }: { s: Scheme; t: number }) => {
  const k = t / 16;
  return (
    <AbsoluteFill style={{ pointerEvents: "none" }}>
      <div
        style={{
          position: "absolute",
          left: 540 - k * 700,
          top: 900 - k * 700,
          width: k * 1400,
          height: k * 1400,
          borderRadius: "50%",
          border: `${30 * (1 - k) + 2}px solid ${s.primary}`,
          opacity: 1 - k,
          boxSizing: "border-box",
        }}
      />
      {Array.from({ length: 10 }, (_, i) => {
        const a = (i / 10) * Math.PI * 2 + 0.3;
        const r = 120 + k * 620;
        return (
          <div
            key={i}
            style={{
              position: "absolute",
              left: 540 + Math.cos(a) * r - 18,
              top: 900 + Math.sin(a) * r * 0.8 + k * k * 500 - 18,
              width: 36 * (1 - k * 0.6),
              height: 36 * (1 - k * 0.6),
              borderRadius: "50%",
              background: s.primary,
              opacity: 1 - k,
            }}
          />
        );
      })}
    </AbsoluteFill>
  );
};
