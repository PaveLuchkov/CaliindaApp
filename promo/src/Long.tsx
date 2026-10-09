import { AbsoluteFill, Sequence, interpolate, spring, useCurrentFrame } from "remotion";
import { CARD_W, DAY_OF, DP, GAP, HEADER_H, Project, ProjectCard, RIBBON_H, TOP, mixHex, projectHeight } from "./DayScene";
import { End, TAGLINE } from "./End";
import { Wave, easeInOut } from "./parts";
import { SCHEMES, Scheme } from "./schemes";
import { flex, fontFace } from "./theme";
import { Caption, Card, E, Header, Lang, MONTH_NAME, Touch, dayTitle, lagOffset, lagSeries, layout } from "./kit";

/**
 * Вариант «Всё, что дольше дня»: привычный месячный вид забит до отказа — короткие и длинные
 * вперемешку. Сетка рассыпается: долгие события собираются на своей странице, короткие уходят
 * в дни. Свайп вбок — чистый день, свайпы вниз — следующие месяцы.
 */
export const LONG_FRAMES = 600;
const END_AT = 486;
const s = SCHEMES[1]; // лес, тёмная

const BREAK = 112; // сетка рассыпается
const SETTLE = 150;

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
    ["A month view\nusually looks like this", 14, 104],
    ["Anything longer than a day\ngets its own page", 160, 250],
    ["And a day is just a day", 286, 350],
    ["Flip through\nthe months", 382, 474],
  ],
  ru: [
    ["Обычно месяц\nвыглядит так", 14, 104],
    ["Всё, что дольше дня, —\nна своей странице", 160, 250],
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

  // Сцена 1 → 2: [brk] 0..1 — сетка рассыпается, фон темнеет до темы приложения.
  const brk = interpolate(f, [BREAK, BREAK + 30], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
  const bg = mixHex("#FFFFFF", s.background, easeInOut(brk));
  const sideP = side(f);
  const m = month(f);
  const lag = lagSeries("months", LONG_FRAMES, (q) => -month(q) * H)[f] ?? 0;
  const pages: Project[][] = [longs, ...LATER[lang]];

  return (
    <AbsoluteFill style={{ background: bg }}>
      <style>{fontFace}</style>
      {f < BREAK + 40 && <MonthGrid lang={lang} f={f} brk={brk} />}

      {/* Страница долгих событий: в октябре карточки «прилетают» из полосок сетки. */}
      {f >= BREAK && (
        <AbsoluteFill style={{ transform: `translateX(${-sideP * 1080}px)` }}>
          {pages.map((list, pi) => {
            const top = (pi - m) * H;
            if (Math.abs(top) >= H) return null;
            const ys = projYs(list);
            const head = sp(BREAK + 14, { damping: 15, stiffness: 120, mass: 0.9 });
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

      <Fingers f={f} />
      {CAPTIONS[lang].map(([text, from, to]) => (
        <Caption key={text} text={text} from={from} to={to} s={from < BREAK ? SCHEMES[0] : s} y={1600} size={58} />
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

/** Сетка месяца: события сыплются всё гуще, сетка подрагивает, потом рассыпается. */
const MonthGrid = ({ lang, f, brk }: { lang: Lang; f: number; brk: number }) => {
  const longs = LONGS[lang];
  const shake = f > 70 && f < BREAK ? Math.sin(f * 2.3) * Math.min(1, (f - 70) / 30) * 4 : 0;
  const head = spring({ frame: f, fps: 30, config: { damping: 15, stiffness: 120, mass: 0.9 } });
  const short: { day: number; slot: number; label: string; tint: string; at: number }[] = [];
  for (let d = 1; d <= 31; d++) {
    const n = 1 + Math.floor(rnd(d) * 4);
    for (let k = 0; k < n; k++) {
      const seed = d * 7 + k;
      // Появляются всё чаще: первые — медленно, последние — лавиной.
      short.push({ day: d, slot: k, label: SHORT[lang][Math.floor(rnd(seed + 3) * SHORT[lang].length)], tint: SHORT_TINTS[Math.floor(rnd(seed + 9) * SHORT_TINTS.length)], at: 6 + Math.pow(rnd(seed + 1), 0.7) * 90 });
    }
  }
  const { lanes, segs: longSegs } = longLayout(longs);
  return (
    <AbsoluteFill style={{ opacity: 1 - brk, transform: `translate(${shake}px, ${shake * 0.6}px)` }}>
      <div style={{ position: "absolute", left: GRID_X, top: 150, ...flex(500), fontSize: 64, color: "#3C4043", opacity: head }}>{lang === "en" ? "October 2026" : "Октябрь 2026"}</div>
      {/* Линии сетки. */}
      {Array.from({ length: 6 }, (_, row) =>
        Array.from({ length: 7 }, (_, col) => {
          const day = row * 7 + col - OCT_START + 1;
          return (
            <div key={`${row}-${col}`} style={{ position: "absolute", left: GRID_X + col * CELL_W, top: GRID_Y + row * CELL_H, width: CELL_W, height: CELL_H, border: "1px solid #DADCE0", boxSizing: "border-box", opacity: head }}>
              {day >= 1 && day <= 31 && <div style={{ textAlign: "center", ...flex(500), fontSize: 26, color: day === 8 ? "#FFFFFF" : "#3C4043", marginTop: 6 }}>{day === 8 ? <span style={{ background: "#1A73E8", borderRadius: 20, padding: "2px 10px" }}>{day}</span> : day}</div>}
            </div>
          );
        }),
      )}
      {/* Многодневные полосы. */}
      {longSegs.map((segs, li) =>
        segs.map((sg, k) => {
          const p = spring({ frame: f - 10 - li * 8 - k * 2, fps: 30, config: { damping: 14, stiffness: 140, mass: 0.8 } });
          return (
            <div
              key={`${li}-${k}`}
              style={{
                position: "absolute",
                left: GRID_X + sg.c0 * CELL_W + 3,
                top: GRID_Y + sg.row * CELL_H + 44 + sg.lane * (BAR_H + 4),
                width: (sg.c1 - sg.c0 + 1) * CELL_W - 6,
                height: BAR_H,
                borderRadius: 6,
                background: longs[li].tint,
                color: "#fff",
                ...flex(500),
                fontSize: 22,
                lineHeight: `${BAR_H}px`,
                paddingLeft: 8,
                whiteSpace: "nowrap",
                overflow: "hidden",
                transformOrigin: "0 50%",
                transform: `scaleX(${p})`,
                opacity: Math.min(1, p * 2),
              }}
            >
              {longs[li].title}
            </div>
          );
        }),
      )}
      {/* Короткие: не влезают — обрезаются и прячутся под «+N». */}
      {short.map((ev, n) => {
        const { col, row } = cellOf(ev.day);
        const lane = lanes[row];
        const y = GRID_Y + row * CELL_H + 44 + lane * (BAR_H + 4) + ev.slot * 30;
        const overflow = y + 28 > GRID_Y + (row + 1) * CELL_H - 4;
        const p = spring({ frame: f - ev.at, fps: 30, config: { damping: 12, stiffness: 180, mass: 0.6 } });
        if (p <= 0.01) return null;
        // При распаде короткие падают вниз — «уходят в дни».
        const fall = Math.max(0, f - BREAK - rnd(n) * 10);
        if (overflow)
          return ev.slot === Math.ceil((GRID_Y + (row + 1) * CELL_H - 4 - (GRID_Y + row * CELL_H + 44 + lane * (BAR_H + 4)) - 28) / 30) ? (
            <div key={n} style={{ position: "absolute", left: GRID_X + col * CELL_W + 8, top: y - 4, ...flex(600), fontSize: 20, color: "#5F6368", opacity: p }}>
              +{2 + Math.floor(rnd(n) * 3)}
            </div>
          ) : null;
        return (
          <div
            key={n}
            style={{
              position: "absolute",
              left: GRID_X + col * CELL_W + 6,
              top: y + fall * fall * 1.6,
              width: CELL_W - 12,
              height: 26,
              display: "flex",
              alignItems: "center",
              gap: 4,
              ...flex(450),
              fontSize: 19,
              color: "#3C4043",
              whiteSpace: "nowrap",
              overflow: "hidden",
              opacity: p,
              transform: `scale(${0.6 + 0.4 * p}) rotate(${fall * (rnd(n + 5) - 0.5) * 3}deg)`,
            }}
          >
            <div style={{ width: 10, height: 10, borderRadius: 5, background: ev.tint, flex: "none" }} />
            {ev.label}
          </div>
        );
      })}
    </AbsoluteFill>
  );
};

/**
 * Долгое событие октября: из первой полоски сетки «вылетает» на своё место и раскрывается в
 * карточку — цвет, высота, скругление и фигура перетекают на пружине.
 */
const Flying = ({ p, i, s, top, f, lang }: { p: Long; i: number; s: Scheme; top: number; f: number; lang: Lang }) => {
  const seg = longLayout(LONGS[lang]).segs[i][0];
  const fromRect = {
    x: GRID_X + seg.c0 * CELL_W + 3,
    y: GRID_Y + seg.row * CELL_H + 44 + seg.lane * (BAR_H + 4),
    w: (seg.c1 - seg.c0 + 1) * CELL_W - 6,
    h: BAR_H,
  };
  const toRect = { x: 16 * DP, y: top, w: CARD_W, h: projectHeight(p.days) };
  const k = spring({ frame: f - BREAK - 4 - i * 5, fps: 30, config: { damping: 13, stiffness: 95, mass: 1 } });
  const lerp = (a: number, b: number) => a + (b - a) * k;
  const card = p.ongoing ? s.tertiaryContainer : s.primaryContainer;
  if (k > 0.98) return <ProjectCard pr={p} s={s} top={top} height={toRect.h} grow={1} lang={lang} />;
  const reveal = interpolate(k, [0.55, 0.98], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
  return (
    <>
      <div
        style={{
          position: "absolute",
          left: lerp(fromRect.x, toRect.x),
          top: lerp(fromRect.y, toRect.y),
          width: lerp(fromRect.w, toRect.w),
          height: lerp(fromRect.h, toRect.h),
          borderRadius: lerp(6, 20 * DP),
          background: mixHex(p.tint, card, Math.min(1, k * 1.3)),
          transform: `rotate(${Math.sin(k * Math.PI) * (i % 2 ? 4 : -4)}deg)`,
          opacity: 1 - reveal,
          ...flex(500),
          color: "#fff",
          fontSize: lerp(22, 24 * DP),
          whiteSpace: "nowrap",
          overflow: "hidden",
          paddingLeft: lerp(8, 16 * DP),
          paddingTop: lerp(4, 8 * DP),
          boxSizing: "border-box",
        }}
      >
        {p.title}
      </div>
      {reveal > 0 && (
        <div style={{ opacity: reveal }}>
          <ProjectCard pr={p} s={s} top={top} height={toRect.h} grow={1} lang={lang} />
        </div>
      )}
    </>
  );
};

/** Страница дня справа: ленты идущих долгих событий сверху, под ними — обычный день. */
const DayPage = ({ lang, f, longs }: { lang: Lang; f: number; longs: Long[] }) => {
  const ongoing = longs.filter((l) => l.ongoing);
  const ribbonTop = TOP + HEADER_H + 12 * DP;
  const listTop = ribbonTop + ongoing.length * (RIBBON_H + 4 * DP) + 8 * DP;
  const events = DAY_EVENTS[lang];
  const { ys } = layout(events, listTop);
  const now = spring({ frame: f - 300, fps: 30, config: { damping: 13, stiffness: 110, mass: 0.9 } });
  return (
    <div style={{ position: "absolute", left: 1080, top: 0, width: 1080, height: H, background: s.background }}>
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
        return <Card key={i} e={e} i={i + 2} s={s} top={ys[i]} now={isNow} lang={lang} f={f} style={{ transform: `translateY(${-isNow * 8}px) scale(${1 + isNow * 0.02})` }} />;
      })}
    </div>
  );
};

