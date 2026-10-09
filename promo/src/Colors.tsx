import { AbsoluteFill, Sequence, spring, useCurrentFrame } from "remotion";
import { DP, HEADER_H, TOP, heightFor } from "./DayScene";
import { End, TAGLINE } from "./End";
import { ShapeName } from "./parts";
import { APRICOT, JUICY, SCHEMES, Scheme, scheme } from "./schemes";
import shapes from "./shapes.json";
import { fontFace } from "./theme";
import { Caption, Card, E, Header, Lang, Touch, dayTitle, layout } from "./kit";

/**
 * Вариант «Цвета». Всё плоское и чёткое, один и тот же день; переходов мало, но каждый — со
 * смыслом и живой: цвет приходит фронтом, и каждая карточка, до которой он докатился,
 * пружинисто откликается.
 *  1. Ваш день: карточки въезжают, «сейчас» поднимается, фоновые фигуры вращаются.
 *  2. Светлая → тёмная: касание кнопки настроек — тёмная тема расходится от неё кругом.
 *  3. Фоновый цветок распускается на весь экран — сочная жёлтая тема со своей фигурой.
 *  4. Так же сердечко — розовая. У каждой темы в приложении своя фоновая фигура.
 *  5. Финал: волны цвета сверху вниз, всё чаще.
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

// Фоновая фигура темы — как BackgroundShapes: динамическая — цветок, «Солнечная» — солнышко,
// «Розовая» — сердечко.
const BG = new Map<Scheme, ShapeName>([
  [JUICY, "sunny"],
  [PINK, "heart"],
  [SKY, "softBurst"],
]);
const bgShape = (s: Scheme): ShapeName => BG.get(s) ?? "flower";

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
const centers = ys.map((y, i) => ({ x: 540, y: y + hs[i] / 2 }));

// Фоновые фигуры: левая (большая) и правая — как в приложении, за карточками.
const BG_LEFT = { x: -420 + 550, y: ys[2] - 120 + 550, size: 1100 };
const BG_RIGHT = { x: 560 + 450, y: ys[5] - 60 + 450, size: 900 };
const SETTINGS = { x: 1080 - 12 * DP - 20 * DP, y: TOP + HEADER_H / 2 };

/** Переход темы: фронт фигуры [shape] растёт из точки [at]. «circle» — ровный круг. */
type Wave = { at: number; from: Scheme; to: Scheme; shape: ShapeName | "circle"; origin: { x: number; y: number }; dur: number; max: number; base: number; side?: "left" | "right" };
const WAVES: Wave[] = [
  { at: 84, from: LAV, to: SEA, shape: "circle", origin: SETTINGS, dur: 30, max: 2300, base: 0 },
  { at: 156, from: SEA, to: JUICY, shape: "flower", origin: BG_LEFT, dur: 40, max: 6400, base: BG_LEFT.size, side: "left" },
  { at: 234, from: JUICY, to: PINK, shape: "heart", origin: BG_RIGHT, dur: 40, max: 7200, base: BG_RIGHT.size, side: "right" },
];
const TAP_SETTINGS = 80;
// Финал: волны цвета по карточкам сверху вниз — всё чаще.
const PASSES: [number, Scheme][] = [
  [318, FOREST],
  [346, SKY],
  [370, BERRY],
  [390, CORAL],
  [406, APRICOT],
  [419, SEA],
  [430, JUICY],
];

const CAPTIONS: Record<Lang, [string, number, number][]> = {
  en: [
    ["Your day.", 12, 66],
    ["Light and dark.", 90, 146],
    ["Your colors.", 164, 222],
    ["Every theme\nhas its own shape.", 242, 304],
    ["New every time.", 326, 476],
  ],
  ru: [
    ["Ваш день.", 12, 66],
    ["Светлая и тёмная.", 90, 146],
    ["Ваши цвета.", 164, 222],
    ["У каждой темы\nсвоя фигура.", 242, 304],
    ["Каждый раз — новые.", 326, 476],
  ],
};

/** Удар/толчок: затухающие колебания. */
const impact = (t: number) => (t < 0 ? 0 : Math.exp(-t / 6) * Math.sin(t * 0.85));
/** Резкий старт, мягкий хвост — фронт «выплёскивается». */
const expoOut = (x: number) => (x <= 0 ? 0 : x >= 1 ? 1 : 1 - Math.pow(2, -7 * x));

/**
 * Размер фронта на кадре [f]: для круга — радиус (выплёскивается сразу), для фигуры — сторона
 * квадрата: фигура вырастает из фоновой, сначала неспешно — чтобы узнать цветок или сердечко, —
 * потом разгоняется на весь экран.
 */
const frontSize = (w: Wave, f: number) => {
  const t = Math.max(0, Math.min(1, (f - w.at) / w.dur));
  return w.shape === "circle" ? w.max * expoOut(t) : w.base + (w.max - w.base) * Math.pow(t, 2.6);
};
/** Перед ростом фоновая фигура чуть сжимается — «набирает воздух». */
const anticipation = (side: "left" | "right", f: number) => {
  const w = WAVES.find((x) => x.side === side && f >= x.at - 10 && f < x.at);
  return w ? 1 - 0.12 * Math.sin((Math.PI * (f - (w.at - 10))) / 10) : 1;
};
/** Докуда фронт уже «достал»: у фигуры вписанный радиус меньше половины стороны. */
const reach = (w: Wave, f: number) => (w.shape === "circle" ? frontSize(w, f) : frontSize(w, f) * 0.33);
/** Кадр, когда фронт докатился до точки [p]. */
const reachedAt = (w: Wave, p: { x: number; y: number }) => {
  const d = Math.hypot(p.x - w.origin.x, p.y - w.origin.y);
  for (let t = 0; t < 120; t++) if (reach(w, w.at + t) >= d) return w.at + t;
  return w.at + 120;
};
const REACH = WAVES.map((w) => centers.map((c) => reachedAt(w, c)));

/** Тема в финале: волна сверху вниз за пару кадров, фон — посередине, чтобы не рябило. */
const passScheme = (f: number, i: number) => {
  let s = PINK;
  const delay = i < 0 ? 3 : i;
  PASSES.forEach(([at, sc]) => {
    if (f >= at + delay) s = sc;
  });
  return s;
};

export const Colors = ({ lang }: { lang: Lang }) => {
  const f = useCurrentFrame();
  // Толчки карточек: от каждого прохода фронта и от каждой волны финала.
  const pops = centers.map((_, i) => {
    let p = 0;
    REACH.forEach((r) => (p += impact(f - r[i])));
    PASSES.forEach(([at]) => (p += impact(f - at - i)));
    return p;
  });
  // Какая волна идёт сейчас: старая тема — снизу, новая — поверх, под маской фронта.
  const active = WAVES.find((w) => f >= w.at && f < w.at + w.dur + 4);
  const settled = [...WAVES].reverse().find((w) => f >= w.at + w.dur + 4);
  const base = f >= PASSES[0][0] ? null : settled ? settled.to : LAV;

  return (
    <AbsoluteFill style={{ background: "#000" }}>
      <style>{fontFace}</style>
      {base && !active && <Page s={base} f={f} lang={lang} pops={pops} />}
      {active && (
        <>
          <Page s={active.from} f={f} lang={lang} pops={pops} />
          <AbsoluteFill style={frontMask(active, f)}>
            <Page s={active.to} f={f} lang={lang} pops={pops} />
          </AbsoluteFill>
        </>
      )}
      {!base && !active && <Page s={passScheme(f, -1)} f={f} lang={lang} pops={pops} cardScheme={(i) => passScheme(f, i)} />}
      {/* Касание кнопки настроек перед тёмной темой. */}
      {f >= TAP_SETTINGS - 10 && f < TAP_SETTINGS + 14 && (
        <Touch x={SETTINGS.x} y={SETTINGS.y + 10} down={Math.abs(f - TAP_SETTINGS) <= 2 ? 1 : 0} show={Math.min(1, (f - TAP_SETTINGS + 10) / 6, (TAP_SETTINGS + 14 - f) / 6)} tap={f >= TAP_SETTINGS ? f - TAP_SETTINGS : -1} />
      )}
      {CAPTIONS[lang].map(([text, from, to]) => (
        <Caption key={text} text={text} from={from} to={to} s={captionScheme(from)} y={1640} size={66} />
      ))}
      <Sequence from={END_AT}>
        <End s={JUICY} tagline={TAGLINE[lang]} />
      </Sequence>
    </AbsoluteFill>
  );
};

const captionScheme = (f: number) => {
  if (f >= PASSES[0][0]) return LAV;
  const w = [...WAVES].reverse().find((x) => f >= x.at);
  return w ? w.to : LAV;
};

/** Маска фронта: круг — clip-path, фигура — векторная маска (чёткая на любом размере). */
const frontMask = (w: Wave, f: number): React.CSSProperties => {
  const size = frontSize(w, f);
  if (w.shape === "circle") return { clipPath: `circle(${size}px at ${w.origin.x}px ${w.origin.y}px)` };
  const rot = (f - w.at) * 1.6 + (w.side === "left" ? f * 0.25 : -f * 0.2) - (f - w.at) * (w.side === "left" ? 0.25 : -0.2);
  const svg = `<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><path d='${shapes[w.shape]}' transform='rotate(${rot.toFixed(1)} 50 50)' fill='black'/></svg>`;
  const url = `url("data:image/svg+xml;utf8,${encodeURIComponent(svg)}")`;
  return {
    WebkitMaskImage: url,
    maskImage: url,
    WebkitMaskSize: `${size}px ${size}px`,
    maskSize: `${size}px ${size}px`,
    WebkitMaskPosition: `${w.origin.x - size / 2}px ${w.origin.y - size / 2}px`,
    maskPosition: `${w.origin.x - size / 2}px ${w.origin.y - size / 2}px`,
    WebkitMaskRepeat: "no-repeat",
    maskRepeat: "no-repeat",
  };
};

/** Страница дня в теме [s]: фоновые фигуры темы, шапка, карточки на пружинах. */
const Page = ({ s, f, lang, pops, cardScheme }: { s: Scheme; f: number; lang: Lang; pops: number[]; cardScheme?: (i: number) => Scheme }) => {
  const events = DAY[lang];
  const nowP = spring({ frame: f - 40, fps: 30, config: { damping: 11, stiffness: 120, mass: 0.9 } });
  const press = Math.max(0, 1 - Math.abs(f - TAP_SETTINGS) / 5);
  const shape = bgShape(s);
  return (
    <AbsoluteFill style={{ background: s.background, overflow: "hidden" }}>
      {/* Фоновые фигуры темы медленно вращаются, как BackgroundShapes. */}
      <svg width={BG_LEFT.size} height={BG_LEFT.size} viewBox="0 0 100 100" style={{ position: "absolute", left: BG_LEFT.x - BG_LEFT.size / 2, top: BG_LEFT.y - BG_LEFT.size / 2, transform: `scale(${anticipation("left", f)})` }}>
        <path d={shapes[shape]} fill={s.surfaceContainerHigh} transform={`rotate(${f * 0.25} 50 50)`} />
      </svg>
      <svg width={BG_RIGHT.size} height={BG_RIGHT.size} viewBox="0 0 100 100" style={{ position: "absolute", left: BG_RIGHT.x - BG_RIGHT.size / 2, top: BG_RIGHT.y - BG_RIGHT.size / 2, transform: `scale(${anticipation("right", f)})` }}>
        <path d={shapes[shape === "flower" ? "clover4" : shape]} fill={s.surfaceContainerHigh} transform={`rotate(${-f * 0.2} 50 50)`} />
      </svg>
      <Header s={s} text={dayTitle(lang, 2)} />
      {/* Нажатая кнопка настроек — круг справа в шапке — чуть проседает. */}
      {press > 0 && <div style={{ position: "absolute", left: SETTINGS.x - 20 * DP, top: SETTINGS.y - 20 * DP, width: 40 * DP, height: 40 * DP, borderRadius: 20 * DP, background: "rgba(255,255,255,0.35)", transform: `scale(${1 + 0.15 * press})` }} />}
      {events.map((e, i) => {
        // Въезд в начале — как анимация появления в списке приложения.
        const a = spring({ frame: f - 2 - i * 4, fps: 30, config: { damping: 14, stiffness: 120, mass: 0.9 } });
        const p = pops[i];
        return (
          <Card
            key={i}
            e={e}
            i={i}
            s={cardScheme ? cardScheme(i) : s}
            top={ys[i]}
            now={i === NOW ? nowP : 0}
            lang={lang}
            f={f}
            style={{
              opacity: Math.min(1, a * 2),
              transform: `translateY(${(1 - a) * 600}px) scale(${1 + 0.05 * p}, ${1 + 0.05 * p})`,
            }}
          />
        );
      })}
    </AbsoluteFill>
  );
};
