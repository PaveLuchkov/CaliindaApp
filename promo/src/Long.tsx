import { AbsoluteFill, Sequence, interpolate, spring, useCurrentFrame } from "remotion";
import { CARD_W, DAY_OF, DP, GAP, HEADER_H, Project, heightFor, ProjectCard, RIBBON_H, TOP, mixHex, projectHeight } from "./DayScene";
import { End, TAGLINE } from "./End";
import { MorphShape, Wave, easeInOut } from "./parts";
import { JUICY, Scheme } from "./schemes";
import { flex, fontFace } from "./theme";
import { Caption, Card, E, Header, Lang, MONTH_NAME, Touch, dayTitle, lagOffset, lagSeries, layout } from "./kit";

/**
 * Вариант «Всё, что дольше дня»: привычный месячный вид наполняется, как пузырь, — короткие и
 * длинные вперемешку, — раздувается и лопается. После хлопка — тишина: долгие события спокойно
 * собираются на своей странице, осколки тают. Свайп вбок — чистый день, свайпы — месяцы.
 */
export const LONG_FRAMES = 600;
const END_AT = 486;
// Светлая и сочная: порядок и чистота — это белый, акцент — «ядовито»-жёлтый как есть.
const s = JUICY;

const POP = 150; // пузырь лопается

type Long = Project & { from: number; to: number; tint: string };

// Октябрь 2026 начинается с четверга: в сетке с понедельника три пустые клетки.
const OCT_START = 3;
const LONGS: Record<Lang, Long[]> = {
  en: [
    { title: "✈️ Trip to Lisbon", days: 7, day: 3, dates: "6 – 12 October", left: "4 days left", shape: "clover4", ongoing: true, from: 6, to: 12, tint: "#4C8DF6" },
    { title: "📖 Reading challenge", days: 30, dates: "1 – 30 October", left: "22 days left", shape: "flower", ongoing: true, from: 1, to: 30, tint: "#33B679" },
    { title: "🏠 Moving house", days: 5, dates: "14 – 18 October", shape: "gem", ongoing: false, from: 14, to: 18, tint: "#F4511E" },
    { title: "🎤 Design conference", days: 2, dates: "24 – 25 October", shape: "puffy", ongoing: false, from: 24, to: 25, tint: "#8E24AA" },
  ],
  ru: [
    { title: "✈️ Поездка в Лиссабон", days: 7, day: 3, dates: "6 – 12 октября", left: "осталось 4 дня", shape: "clover4", ongoing: true, from: 6, to: 12, tint: "#4C8DF6" },
    { title: "📖 Книжный марафон", days: 30, dates: "1 – 30 октября", left: "осталось 22 дня", shape: "flower", ongoing: true, from: 1, to: 30, tint: "#33B679" },
    { title: "🏠 Переезд", days: 5, dates: "14 – 18 октября", shape: "gem", ongoing: false, from: 14, to: 18, tint: "#F4511E" },
    { title: "🎤 Конференция", days: 2, dates: "24 – 25 октября", shape: "puffy", ongoing: false, from: 24, to: 25, tint: "#8E24AA" },
  ],
};

// Следующие месяцы — для вертикального листания.
const LATER: Record<Lang, Project[][]> = {
  en: [
    [
      { title: "🎓 Online course", days: 26, dates: "2 – 27 November", shape: "flower", ongoing: false },
      { title: "🏔️ Mountains", days: 9, dates: "7 – 15 November", shape: "clover4", ongoing: false },
      { title: "🧳 Business trip", days: 3, dates: "19 – 21 November", shape: "gem", ongoing: false },
    ],
    [
      { title: "🎁 Holiday prep", days: 23, dates: "1 – 23 December", shape: "sunny", ongoing: false },
      { title: "🎄 Winter break", days: 16, dates: "24 Dec – 8 Jan", shape: "cookie12", ongoing: false },
      { title: "⛷️ Ski trip", days: 5, dates: "27 – 31 December", shape: "puffy", ongoing: false },
    ],
  ],
  ru: [
    [
      { title: "🎓 Онлайн-курс", days: 26, dates: "2 – 27 ноября", shape: "flower", ongoing: false },
      { title: "🏔️ Горы", days: 9, dates: "7 – 15 ноября", shape: "clover4", ongoing: false },
      { title: "🧳 Командировка", days: 3, dates: "19 – 21 ноября", shape: "gem", ongoing: false },
    ],
    [
      { title: "🎁 Подготовка к праздникам", days: 23, dates: "1 – 23 декабря", shape: "sunny", ongoing: false },
      { title: "🎄 Каникулы", days: 16, dates: "24 дек. – 8 янв.", shape: "cookie12", ongoing: false },
      { title: "⛷️ Лыжи", days: 5, dates: "27 – 31 декабря", shape: "puffy", ongoing: false },
    ],
  ],
};

// Короткие события в клетках месяца: подписи обрезаны, как в тесной сетке.
const SHORT = {
  en: ["Standup", "Gym", "Lunch", "Call", "Dentist", "1:1", "Sync", "Dinner", "Yoga", "Review", "Groceries", "Pay rent", "Haircut", "Movie", "Run", "Piano"],
  ru: ["Созвон", "Зал", "Обед", "Звонок", "Стоматолог", "1:1", "Синк", "Ужин", "Йога", "Ревью", "Продукты", "Аренда", "Стрижка", "Кино", "Бег", "Пианино"],
};
const SHORT_TINTS = ["#039BE5", "#7986CB", "#33B679", "#E67C73", "#F6BF26", "#8E24AA", "#616161", "#F4511E"];

const DAY_EVENTS: Record<Lang, E[]> = {
  en: [
    { t: "🥐 Pastel de nata", at: "9:00", min: 30 },
    { t: "🚋 Tram 28", at: "10:00", min: 60 },
    { t: "🏰 São Jorge Castle", at: "11:30", min: 150 },
    { t: "🌇 Miradouro", at: "19:00", min: 45 },
  ],
  ru: [
    { t: "🥐 Паштел-де-ната", at: "9:00", min: 30 },
    { t: "🚋 Трамвай 28", at: "10:00", min: 60 },
    { t: "🏰 Замок Святого Георгия", at: "11:30", min: 150 },
    { t: "🌇 Смотровая", at: "19:00", min: 45 },
  ],
};

const CAPTIONS: Record<Lang, [string, number, number][]> = {
  en: [
    ["A month view\nusually looks like this", 12, 66],
    ["One more…\nand one more…", 76, 140],
    ["Anything longer than a day\ngets its own page", 204, 262],
    ["And a day is just a day", 286, 350],
    ["Flip through\nthe months", 382, 474],
  ],
  ru: [
    ["Обычно месяц\nвыглядит так", 12, 66],
    ["Ещё одно…\nи ещё…", 76, 140],
    ["Всё, что дольше дня, —\nна своей странице", 204, 262],
    ["А день — это просто день", 286, 350],
    ["Листайте\nмесяцы", 382, 474],
  ],
};

// --- Сетка «обычного» месяца.
const GRID_X = 24;
const GRID_Y = 300;
const CELL_W = (1080 - 2 * GRID_X) / 7;
const CELL_H = 250;
const BAR_H = 34;

const rnd = (n: number) => {
  const x = Math.sin(n * 127.1 + 311.7) * 43758.5453;
  return x - Math.floor(x);
};
const cellOf = (day: number) => {
  const idx = day - 1 + OCT_START;
  return { col: idx % 7, row: Math.floor(idx / 7) };
};

/** Отрезки многодневного события по строкам сетки. */
const segments = (from: number, to: number) => {
  const out: { row: number; c0: number; c1: number }[] = [];
  let d = from;
  while (d <= to) {
    const { col, row } = cellOf(d);
    const len = Math.min(7 - col, to - d + 1);
    out.push({ row, c0: col, c1: col + len - 1 });
    d += len;
  }
  return out;
};

/** Полосы многодневных по строкам: в каждой строке своя «дорожка», короткие идут под ними. */
const longLayout = (longs: Long[]) => {
  const lanes: number[] = Array(6).fill(0);
  const segs = longs.map((l) => segments(l.from, l.to).map((sg) => ({ ...sg, lane: lanes[sg.row]++ })));
  return { lanes, segs };
};

const PROJ_TOP = TOP + HEADER_H + 12 * DP;
const projYs = (list: Project[], grow: (i: number) => number = () => 1) => {
  const ys: number[] = [];
  let y = PROJ_TOP;
  list.forEach((p, i) => {
    ys.push(y);
    y += (projectHeight(p.days) + GAP) * grow(i);
  });
  return ys;
};

// Свайпы: вбок к дню и обратно, потом вниз по месяцам.
const H = 1920;
const SIDE_TO_DAY = 268;
const SIDE_BACK = 352;
const MONTH_SWIPES = [384, 432];

const side = (f: number) => {
  const go = spring({ frame: f - SIDE_TO_DAY - 8, fps: 30, config: { damping: 28, stiffness: 190, mass: 1 } });
  const back = spring({ frame: f - SIDE_BACK - 8, fps: 30, config: { damping: 28, stiffness: 190, mass: 1 } });
  const drag = (at: number) => easeInOut(interpolate(f, [at, at + 8], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }));
  // Пока палец тянет — страница идёт за ним на треть, дальше доезжает сама.
  const there = f < SIDE_TO_DAY + 8 ? drag(SIDE_TO_DAY) * 0.3 : 0.3 + 0.7 * go;
  const ret = f < SIDE_BACK + 8 ? drag(SIDE_BACK) * 0.3 : 0.3 + 0.7 * back;
  return f < SIDE_BACK ? there : 1 - ret;
};
const month = (f: number) => {
  let m = 0;
  MONTH_SWIPES.forEach((at) => {
    if (f < at) return;
    const d = easeInOut(Math.min(1, (f - at) / 8)) * 0.28;
    m += f < at + 8 ? d : 0.28 + 0.72 * spring({ frame: f - at - 8, fps: 30, config: { damping: 28, stiffness: 190, mass: 1 } });
  });
  return m;
};

export const LongEvents = ({ lang }: { lang: Lang }) => {
  const f = useCurrentFrame();
  const longs = LONGS[lang];
  const sp = (from: number, cfg = { damping: 14, stiffness: 120, mass: 0.9 }) => spring({ frame: f - from, fps: 30, config: cfg });

  const sideP = side(f);
  const m = month(f);
  const lag = lagSeries("months", LONG_FRAMES, (q) => -month(q) * H)[f] ?? 0;
  const pages: Project[][] = [longs, ...LATER[lang]];
  // После хлопка камера ещё трясётся, затухая.
  const shakeAmp = f >= POP ? 34 * Math.exp(-(f - POP) / 5) : 0;
  const shake = `translate(${Math.sin(f * 2.9) * shakeAmp}px, ${Math.cos(f * 3.7) * shakeAmp}px)`;

  return (
    <AbsoluteFill style={{ background: f < POP ? "#ECEEF1" : s.background }}>
      <style>{fontFace}</style>
      {f < POP && <Bubble lang={lang} f={f} />}

      {/* Страница долгих событий: после хлопка полоски ловятся и встают карточками. */}
      {f >= POP && (
        <AbsoluteFill style={{ transform: `${shake} translateX(${-sideP * 1080}px)` }}>
          {pages.map((list, pi) => {
            const top = (pi - m) * H;
            if (Math.abs(top) >= H) return null;
            const ys = projYs(list);
            const head = sp(POP + 30, { damping: 15, stiffness: 100, mass: 0.9 });
            return (
              <div key={pi} style={{ position: "absolute", left: 0, top, width: 1080, height: H, background: s.background }}>
                <Header s={s} text={MONTH_NAME[lang][9 + pi]} style={{ opacity: head, transform: `translateY(${(1 - head) * -60}px)` }} />
                {list.map((p, i) => {
                  const h = projectHeight(p.days);
                  const off = lagOffset(lag, ys[i] + h / 2);
                  if (pi === 0) return <Flying key={i} p={longs[i]} i={i} s={s} top={ys[i]} f={f} lang={lang} />;
                  return <ProjectCard key={i} pr={p} s={s} top={ys[i] + off} height={h} grow={1} lang={lang} />;
                })}
              </div>
            );
          })}
          {/* День — справа от страницы долгих событий, как в горизонтальном пейджере. */}
          {f >= SIDE_TO_DAY && <DayPage lang={lang} f={f} longs={longs} />}
        </AbsoluteFill>
      )}
      {f >= POP && f < POP + 70 && (
        <AbsoluteFill style={{ transform: shake }}>
          <Pop lang={lang} f={f} />
        </AbsoluteFill>
      )}

      <Fingers f={f} />
      {CAPTIONS[lang].map(([text, from, to]) => (
        <Caption key={text} text={text} from={from} to={to} s={s} y={1600} size={58} />
      ))}
      <Sequence from={END_AT}>
        <End s={s} tagline={TAGLINE[lang]} />
      </Sequence>
    </AbsoluteFill>
  );
};

/** Палец: свайп влево (к дню), вправо (обратно), два свайпа вверх по месяцам. */
const Fingers = ({ f }: { f: number }) => {
  const swipes: [number, number, number, number, number][] = [
    [SIDE_TO_DAY, 860, 1100, 380, 1100],
    [SIDE_BACK, 220, 1100, 700, 1100],
    ...MONTH_SWIPES.map((at) => [at, 760, 1380, 760, 900] as [number, number, number, number, number]),
  ];
  for (const [at, x0, y0, x1, y1] of swipes) {
    const l = f - at;
    if (l < -4 || l > 16) continue;
    const m = easeInOut(Math.max(0, Math.min(1, l / 8)));
    const show = l < 0 ? (l + 4) / 4 : l > 8 ? Math.max(0, 1 - (l - 8) / 8) : 1;
    return <Touch x={x0 + (x1 - x0) * m + (l > 8 ? (x1 - x0) * (l - 8) * 0.04 : 0)} y={y0 + (y1 - y0) * m} down={l >= 0 && l <= 8 ? 1 : 0} show={show} tap={l >= 0 ? l : -1} />;
  }
  return null;
};

// --- Пузырь: сетка месяца наполняется, раздувается и лопается.

/** Кусочек сетки: число дня, короткое событие, отрезок многодневного или счётчик «+N». */
type Piece = {
  id: number;
  kind: "num" | "short" | "long" | "more";
  x: number;
  y: number;
  w: number;
  h: number;
  at: number; // кадр появления
  label: string;
  tint: string;
  li?: number; // индекс многодневного
  first?: boolean; // первый отрезок — из него вырастет карточка
  hidden?: number[]; // для «+N»: кадры появления не влезших событий
};

const GRID_TOP = 130;
const BUBBLE = { x: GRID_X - 12, y: GRID_TOP, w: 1080 - 2 * GRID_X + 24, h: GRID_Y + 6 * CELL_H - GRID_TOP + 12 };
const CX = BUBBLE.x + BUBBLE.w / 2;
const CY = BUBBLE.y + BUBBLE.h / 2;

/** Всё содержимое сетки с координатами на экране и моментом появления. Детерминированно. */
const piecesCache = new Map<Lang, Piece[]>();
const pieces = (lang: Lang): Piece[] => {
  const hit = piecesCache.get(lang);
  if (hit) return hit;
  const longs = LONGS[lang];
  const out: Piece[] = [];
  let id = 0;
  for (let d = 1; d <= 31; d++) {
    const { col, row } = cellOf(d);
    out.push({ id: id++, kind: "num", x: GRID_X + col * CELL_W, y: GRID_Y + row * CELL_H + 6, w: CELL_W, h: 32, at: 0, label: String(d), tint: "" });
  }
  // Многодневные появляются по ходу наполнения — каждая полоса добавляет «давления».
  const { lanes, segs } = longLayout(longs);
  const longAt = [24, 52, 84, 112];
  segs.forEach((list, li) =>
    list.forEach((sg, k) =>
      out.push({
        id: id++,
        kind: "long",
        x: GRID_X + sg.c0 * CELL_W + 3,
        y: GRID_Y + sg.row * CELL_H + 44 + sg.lane * (BAR_H + 4),
        w: (sg.c1 - sg.c0 + 1) * CELL_W - 6,
        h: BAR_H,
        at: longAt[li] + k * 3,
        label: longs[li].title,
        tint: longs[li].tint,
        li,
        first: k === 0,
      }),
    ),
  );
  // Короткие: сыплются всё чаще. Что не влезло в клетку — уходит в счётчик «+N».
  for (let d = 1; d <= 31; d++) {
    const { col, row } = cellOf(d);
    const top = GRID_Y + row * CELL_H + 44 + lanes[row] * (BAR_H + 4);
    const fit = Math.max(1, Math.floor((GRID_Y + (row + 1) * CELL_H - 34 - top) / 30));
    const n = 3 + Math.floor(rnd(d) * 6);
    const hidden: number[] = [];
    for (let k = 0; k < n; k++) {
      const seed = d * 7 + k;
      const at = 8 + Math.pow(rnd(seed + 1), 0.55) * 128;
      if (k < fit) {
        out.push({
          id: id++,
          kind: "short",
          x: GRID_X + col * CELL_W + 6,
          y: top + k * 30,
          w: CELL_W - 12,
          h: 26,
          at,
          label: SHORT[lang][Math.floor(rnd(seed + 3) * SHORT[lang].length)],
          tint: SHORT_TINTS[Math.floor(rnd(seed + 9) * SHORT_TINTS.length)],
        });
      } else hidden.push(at);
    }
    if (hidden.length) out.push({ id: id++, kind: "more", x: GRID_X + col * CELL_W + 8, y: top + fit * 30 - 2, w: 60, h: 28, at: Math.min(...hidden), label: "", tint: "", hidden });
  }
  piecesCache.set(lang, out);
  return out;
};

/** Давление 0..1 к моменту хлопка — от него зависит всё: размер, дрожь, румянец. */
const pressure = (f: number) => Math.min(1, Math.max(0, f / POP));
const bubbleScale = (f: number) => {
  const p = pressure(f);
  // Перед самым хлопком — короткий «вдох»: чуть сжимается и сразу раздувается сильнее.
  const breath = f > POP - 10 ? Math.sin(((f - (POP - 10)) / 10) * Math.PI) * -0.03 : 0;
  return 0.8 + 0.24 * Math.pow(p, 2.2) + breath;
};
const wobble = (f: number) => {
  const p = pressure(f);
  return Math.sin(f * 0.8) * 0.012 * p + Math.sin(f * 2.9) * 0.01 * Math.pow(p, 4);
};

/** Положение кусочка на экране в момент хлопка — с учётом раздутого пузыря. */
const atPop = (pc: Piece) => {
  const k = bubbleScale(POP);
  return { x: CX + (pc.x + pc.w / 2 - CX) * k, y: CY + (pc.y + pc.h / 2 - CY) * k, k };
};

/**
 * После хлопка: кусочек летит от центра и вязко тормозит — как в замедленной съёмке.
 * Возвращает центр, поворот и «время после хлопка» в секундах.
 */
const flight = (pc: Piece, f: number) => {
  const p0 = atPop(pc);
  const t = Math.max(0, (f - POP) / 30);
  const dx = p0.x - CX;
  const dy = p0.y - CY;
  const len = Math.hypot(dx, dy) || 1;
  const speed = 700 + rnd(pc.id + 17) * 1300 + len * 0.6;
  const tang = (rnd(pc.id + 29) - 0.5) * 500;
  const vx = (dx / len) * speed - (dy / len) * tang;
  const vy = (dy / len) * speed + (dx / len) * tang - 300;
  // Вязкое торможение: путь растёт как 1 − e^(−t/τ).
  const tau = 0.32;
  const travel = tau * (1 - Math.exp(-t / tau));
  // Медленное оседание вниз — «уходят в дни».
  const sink = 160 * t * t;
  return {
    x: p0.x + vx * travel,
    y: p0.y + vy * travel + sink,
    rot: (rnd(pc.id + 41) - 0.5) * 540 * travel,
    k: p0.k,
    t,
  };
};

/** Внешний вид кусочка — общий для пузыря и для полёта. */
const PieceView = ({ pc, f, style }: { pc: Piece; f: number; style?: React.CSSProperties }) => {
  const base: React.CSSProperties = { position: "absolute", left: 0, top: 0, width: pc.w, height: pc.h, ...style };
  if (pc.kind === "num")
    return <div style={{ ...base, textAlign: "center", ...flex(500), fontSize: 26, color: pc.label === "8" ? "#1A73E8" : "#3C4043" }}>{pc.label}</div>;
  if (pc.kind === "long")
    return <div style={{ ...base, borderRadius: 6, background: pc.tint, color: "#fff", ...flex(500), fontSize: 22, lineHeight: `${BAR_H}px`, paddingLeft: 8, boxSizing: "border-box", whiteSpace: "nowrap", overflow: "hidden" }}>{pc.label}</div>;
  if (pc.kind === "more") {
    const n = (pc.hidden ?? []).filter((a) => a <= Math.min(f, POP)).length;
    // Каждое новое скрытое событие — счётчик подпрыгивает.
    const last = Math.max(...(pc.hidden ?? [0]).filter((a) => a <= f), -100);
    const bump = spring({ frame: f - last, fps: 30, config: { damping: 9, stiffness: 300, mass: 0.5 } });
    return (
      <div style={{ ...base, ...flex(800), fontSize: 21, color: n > 4 ? "#D93025" : "#5F6368", transform: `${style?.transform ?? ""} scale(${1 + 0.35 * (1 - bump)})`, transformOrigin: "0 50%" }}>
        +{n}
      </div>
    );
  }
  return (
    <div style={{ ...base, display: "flex", alignItems: "center", gap: 4, ...flex(450), fontSize: 19, color: "#3C4043", whiteSpace: "nowrap", overflow: "hidden" }}>
      <div style={{ width: 10, height: 10, borderRadius: 5, background: pc.tint, flex: "none" }} />
      {pc.label}
    </div>
  );
};

/** Пузырь: сетка в белой «плёнке», наполняется, круглеет, дрожит и краснеет от давления. */
const Bubble = ({ lang, f }: { lang: Lang; f: number }) => {
  const p = pressure(f);
  const k = bubbleScale(f);
  const w = wobble(f);
  const head = spring({ frame: f, fps: 30, config: { damping: 15, stiffness: 120, mass: 0.9 } });
  const shake = Math.pow(p, 3) * 6;
  const sx = Math.sin(f * 3.1) * shake;
  const sy = Math.cos(f * 2.7) * shake;
  return (
    <AbsoluteFill style={{ background: "#ECEEF1" }}>
      <div
        style={{
          position: "absolute",
          left: BUBBLE.x,
          top: BUBBLE.y,
          width: BUBBLE.w,
          height: BUBBLE.h,
          borderRadius: 36 + Math.pow(p, 1.6) * 460,
          background: "#FFFFFF",
          overflow: "hidden",
          transform: `translate(${sx}px, ${sy}px) scale(${k * (1 + w)}, ${k * (1 - w)})`,
          transformOrigin: `${CX - BUBBLE.x}px ${CY - BUBBLE.y}px`,
          boxShadow: `0 ${30 + 40 * p}px ${80 + 80 * p}px rgba(60,64,67,${0.18 + 0.2 * p}), inset 0 0 ${120 + 200 * p}px rgba(217,48,37,${0.4 * Math.pow(p, 3)})`,
          opacity: head,
        }}
      >
        <div style={{ position: "absolute", left: GRID_X - BUBBLE.x + 8, top: 26, ...flex(500), fontSize: 60, color: "#3C4043" }}>{lang === "en" ? "October 2026" : "Октябрь 2026"}</div>
        {Array.from({ length: 6 }, (_, row) =>
          Array.from({ length: 7 }, (_, col) => (
            <div key={`${row}-${col}`} style={{ position: "absolute", left: GRID_X - BUBBLE.x + col * CELL_W, top: GRID_Y - BUBBLE.y + row * CELL_H, width: CELL_W, height: CELL_H, border: "1px solid #DADCE0", boxSizing: "border-box" }} />
          )),
        )}
        {pieces(lang).map((pc) => {
          if (f < pc.at) return null;
          const a = spring({ frame: f - pc.at, fps: 30, config: { damping: 11, stiffness: 200, mass: 0.6 } });
          // Под давлением кусочки мелко трясутся, каждый по-своему.
          const j = Math.pow(p, 3) * 3;
          const jx = Math.sin(f * 1.7 + pc.id) * j;
          const jy = Math.cos(f * 2.3 + pc.id * 2) * j;
          return (
            <PieceView
              key={pc.id}
              pc={pc}
              f={f}
              style={{
                left: pc.x - BUBBLE.x + jx,
                top: pc.y - BUBBLE.y + jy,
                opacity: Math.min(1, a * 2),
                transform: pc.kind === "long" ? `scaleX(${a})` : `scale(${0.4 + 0.6 * a})`,
                transformOrigin: pc.kind === "long" ? "0 50%" : "50% 50%",
              }}
            />
          );
        })}
        {/* Блик плёнки — проступает, когда пузырь круглеет. */}
        <div style={{ position: "absolute", inset: 0, background: "radial-gradient(ellipse 38% 22% at 28% 16%, rgba(255,255,255,0.95), rgba(255,255,255,0) 70%)", opacity: Math.pow(p, 1.5), mixBlendMode: "screen" }} />
        <div style={{ position: "absolute", inset: 0, borderRadius: "inherit", boxShadow: `inset 0 0 0 ${2 + 6 * p}px rgba(255,255,255,${0.8 * p})` }} />
      </div>
    </AbsoluteFill>
  );
};

/** Хлопок: вспышка, ударная волна, «БАХ!», осколки разлетаются и тают. */
const Pop = ({ lang, f }: { lang: Lang; f: number }) => {
  const t = f - POP;
  const flash = interpolate(t, [0, 7], [0.95, 0], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
  const ring = interpolate(t, [0, 16], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp", easing: (x) => 1 - Math.pow(1 - x, 3) });
  const word = spring({ frame: t, fps: 30, config: { damping: 9, stiffness: 220, mass: 0.6 } });
  const wordOut = interpolate(t, [20, 30], [1, 0], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
  const burst = interpolate(t, [0, 12], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp", easing: (x) => 1 - Math.pow(1 - x, 2) });
  return (
    <AbsoluteFill style={{ pointerEvents: "none" }}>
      {/* Цветная «клякса» хлопка в цветах приложения. */}
      {t < 18 && (
        <AbsoluteFill style={{ alignItems: "center", justifyContent: "center" }}>
          <MorphShape names={["softBurst", "sunny"]} t={t / 8} size={900} color={s.tertiaryContainer} rotate={t * 6} style={{ transform: `scale(${0.3 + burst * 2.2})`, opacity: 1 - burst }} />
        </AbsoluteFill>
      )}
      {/* Осколки сетки. Карточками станут только первые отрезки многодневных — их рисует Flying. */}
      {pieces(lang).map((pc) => {
        if (pc.at > POP || (pc.kind === "long" && pc.first)) return null;
        const fl = flight(pc, f);
        const fade = interpolate(fl.t, [0.5, 1.7], [1, 0], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
        if (fade <= 0) return null;
        return (
          <PieceView
            key={pc.id}
            pc={pc}
            f={f}
            style={{
              left: fl.x - pc.w / 2,
              top: fl.y - pc.h / 2,
              opacity: fade,
              transform: `scale(${fl.k * (1 - 0.4 * (1 - fade))}) rotate(${fl.rot}deg)`,
              filter: pc.kind === "num" ? undefined : `saturate(${fade})`,
            }}
          />
        );
      })}
      {t < 18 && (
        <div
          style={{
            position: "absolute",
            left: CX - ring * 1300,
            top: CY - ring * 1300,
            width: ring * 2600,
            height: ring * 2600,
            borderRadius: "50%",
            border: `${40 * (1 - ring) + 2}px solid ${s.primary}`,
            opacity: 0.85 * (1 - ring),
            boxSizing: "border-box",
          }}
        />
      )}
      <AbsoluteFill style={{ background: "#FFFFFF", opacity: flash }} />
      {t < 32 && (
        <AbsoluteFill style={{ alignItems: "center", justifyContent: "center" }}>
          <div
            style={{
              ...flex(1000, 151),
              fontSize: 260,
              color: s.primary,
              WebkitTextStroke: `14px #FFFFFF`,
              paintOrder: "stroke fill",
              transform: `scale(${0.3 + 0.8 * word}) rotate(${-10 + 4 * word}deg)`,
              opacity: wordOut,
              textShadow: "0 20px 60px rgba(120,60,0,0.3)",
            }}
          >
            {lang === "en" ? "POP!" : "БАХ!"}
          </div>
        </AbsoluteFill>
      )}
    </AbsoluteFill>
  );
};

// --- Посадка долгих событий: пружина ловит полоску, карточка приземляется на «полку».
const CATCH = (i: number) => POP + 16 + i * 8;
const CATCH_CFG = { damping: 17, stiffness: 75, mass: 1.1 };
/** Кадр приземления — когда пружина почти довела карточку до места. */
const LAND = [0, 1, 2, 3].map((i) => {
  for (let t = 0; t < 200; t++) if (spring({ frame: t, fps: 30, config: CATCH_CFG }) >= 0.97) return CATCH(i) + t;
  return CATCH(i) + 60;
});
/** Удар при посадке: затухающие колебания. */
const impact = (t: number) => (t < 0 ? 0 : Math.exp(-t / 6) * Math.sin(t * 0.85));
/**
 * Карточка на полке: при своей посадке сплющивается и отпружинивает, а от посадок соседей
 * вздрагивает — тем слабее, чем дальше сосед.
 */
const shelf = (i: number, f: number) => {
  const own = impact(f - LAND[i]);
  let nudge = 0;
  LAND.forEach((l, j) => {
    if (j !== i && f >= LAND[i]) nudge += impact(f - l) / (1 + Math.abs(i - j));
  });
  return { squash: own, nudge: nudge * 9 * DP };
};

/**
 * Многодневное событие октября: после хлопка его полоска летит вместе с осколками, потом
 * пружина ловит её и ведёт на своё место. В полёте она ближе к зрителю — крупнее, с большой
 * мягкой тенью; по пути раскрывается в карточку и приземляется с отскоком.
 */
const Flying = ({ p, i, s, top, f, lang }: { p: Long; i: number; s: Scheme; top: number; f: number; lang: Lang }) => {
  const pc = pieces(lang).find((x) => x.kind === "long" && x.li === i && x.first)!;
  const fl = flight(pc, f);
  const toRect = { x: 16 * DP, y: top, w: CARD_W, h: projectHeight(p.days) };
  const k = spring({ frame: f - CATCH(i), fps: 30, config: CATCH_CFG });
  const lerp = (a: number, b: number) => a + (b - a) * k;
  const card = p.ongoing ? s.tertiaryContainer : s.primaryContainer;
  if (f >= LAND[i]) {
    const { squash, nudge } = shelf(i, f);
    return (
      <div style={{ position: "absolute", inset: 0, transform: `translateY(${nudge}px) scale(${1 + 0.05 * squash}, ${1 - 0.14 * squash})`, transformOrigin: `540px ${top + toRect.h}px` }}>
        <ProjectCard pr={p} s={s} top={top} height={toRect.h} grow={1} lang={lang} />
      </div>
    );
  }
  const w = lerp(pc.w * fl.k, toRect.w);
  const h = lerp(pc.h * fl.k, toRect.h);
  const air = 1 - k;
  const reveal = interpolate(k, [0.84, 0.97], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
  return (
    <div style={{ position: "absolute", inset: 0, transform: `scale(${1 + 0.12 * air * k * 4})`, transformOrigin: `540px ${top + toRect.h / 2}px` }}>
      <div
        style={{
          position: "absolute",
          left: lerp(fl.x - w / 2, toRect.x),
          top: lerp(fl.y - h / 2, toRect.y),
          width: w,
          height: h,
          borderRadius: lerp(6, 20 * DP),
          background: mixHex(p.tint, card, Math.min(1, k * 1.3)),
          transform: `rotate(${fl.rot * air}deg)`,
          opacity: 1 - reveal,
          ...flex(500),
          color: mixHex("#FFFFFF", p.ongoing ? s.onTertiaryContainer : s.onPrimaryContainer, k),
          fontSize: lerp(22, 24 * DP),
          whiteSpace: "nowrap",
          overflow: "hidden",
          paddingLeft: lerp(8, 16 * DP),
          paddingTop: lerp(4, 8 * DP),
          boxSizing: "border-box",
          boxShadow: `0 ${50 * air}px ${90 * air}px rgba(0,0,0,${0.35 * air})`,
        }}
      >
        {p.title}
      </div>
      {reveal > 0 && (
        <div style={{ opacity: reveal, filter: `drop-shadow(0 ${50 * air}px ${45 * air}px rgba(0,0,0,${0.35 * air}))` }}>
          <ProjectCard pr={p} s={s} top={lerp(fl.y - h / 2, toRect.y)} height={toRect.h} grow={1} lang={lang} />
        </div>
      )}
    </div>
  );
};

/** Страница дня справа: ленты идущих долгих событий сверху, под ними — обычный день. */
const DayPage = ({ lang, f, longs }: { lang: Lang; f: number; longs: Long[] }) => {
  const ongoing = longs.filter((l) => l.ongoing);
  const ribbonTop = TOP + HEADER_H + 12 * DP;
  const listTop = ribbonTop + ongoing.length * (RIBBON_H + 4 * DP) + 8 * DP;
  const events = DAY_EVENTS[lang];
  const { ys } = layout(events, listTop);
  // Камера подъезжает к текущему событию: оно поднимается к зрителю, ползёт червячок.
  const now = spring({ frame: f - 302, fps: 30, config: { damping: 11, stiffness: 120, mass: 0.9 } });
  const push = easeInOut(interpolate(f, [292, 312, 334, 350], [0, 1, 1, 0], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }));
  const focus = ys[2] + heightFor(events[2].min) / 2;
  return (
    <div style={{ position: "absolute", left: 1080, top: 0, width: 1080, height: H, background: s.background, overflow: "hidden" }}>
      <div style={{ position: "absolute", inset: 0, transform: `scale(${1 + 0.24 * push})`, transformOrigin: `${16 * DP}px ${focus}px` }}>
      <Header s={s} text={dayTitle(lang, 0)} />
      {ongoing.map((l, r) => (
        <div
          key={r}
          style={{
            position: "absolute",
            left: 16 * DP,
            width: CARD_W,
            top: ribbonTop + r * (RIBBON_H + 4 * DP),
            height: RIBBON_H,
            borderRadius: RIBBON_H / 2,
            background: s.secondaryContainer,
            color: s.onSecondaryContainer,
            display: "flex",
            alignItems: "center",
            padding: `0 ${16 * DP}px`,
            gap: 12 * DP,
            boxSizing: "border-box",
          }}
        >
          <div style={{ flex: 1, ...flex(600), fontSize: 14 * DP, whiteSpace: "nowrap", overflow: "hidden" }}>{l.title}</div>
          <Wave width={56 * DP} height={10 * DP} progress={(r === 0 ? 3 : 8) / l.days} phase={0} color={s.secondary} />
          <div style={{ ...flex(500), fontSize: 12 * DP }}>{DAY_OF[lang](r === 0 ? 3 : 8, l.days)}</div>
        </div>
      ))}
      {events.map((e, i) => {
        const isNow = i === 2 ? now : 0;
        return <Card key={i} e={e} i={i + 2} s={s} top={ys[i]} now={isNow} lang={lang} f={f} />;
      })}
      </div>
    </div>
  );
};

