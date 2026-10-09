import { AbsoluteFill, Sequence, interpolate, spring, useCurrentFrame } from "remotion";
import { CARD_W, DP, heightFor, mixHex } from "./DayScene";
import { End, TAGLINE } from "./End";
import { easeInOut } from "./parts";
import { APRICOT } from "./schemes";
import { flex, fontFace } from "./theme";
import { Caption, Card, E, Header, Lang, TOOLBAR_Y, Toolbar, Touch, dayTitle, layout } from "./kit";

/**
 * Вариант «Чипы» — про лень. Раньше календарь не вёлся, потому что каждый раз печатать:
 * палец ползает по клавиатуре, ошибается, стирает… Клавиатура уезжает, буквы осыпаются — и
 * одно касание чипа: он влетает в поле, «Сохранить» бросает карточку в день, она приземляется,
 * соседи вздрагивают. Чипы сами знают время: перескакивают по местам. В финале — лоток чипов:
 * тап, тап, тап — день наполнился. Ни одной буквы с клавиатуры.
 */
export const CHIPS_FRAMES = 600;
const END_AT = 486;
const s = APRICOT;

// --- Монтаж (кадры).
const PLUS1 = 14;
const TYPE_AT = 32;
const KB_OUT = 104; // клавиатура уезжает, буквы осыпаются
const WAVE = 114; // чипы «здороваются»
const TAP_GYM = 134;
const SAVE1 = 166;
const PLUS2 = 214;
const T13 = 240; // время перекручивается — чипы перескакивают
const T20 = 268;
const TAP_MOVIE = 292;
const TAP_DUR = 304;
const SAVE2 = 316;
const TRAY_IN = 344;
const TRAY_TAPS = [362, 374, 386];
const TRAY_OUT = 400;
const ZOOM_AT = 404;
const NOW_AT = 416;
const FLY = 9; // полёт чипа в поле

const T = {
  en: {
    field: "Title",
    save: "Save",
    date: "Thu, Oct 8",
    durs: ["15 min", "30 min", "1 h", "2 h"],
    keys: ["qwertyuiop", "asdfghjkl", "⇧zxcvbnm⌫"],
    // Печатаем «Going t…» с опечаткой: m вместо n, стираем.
    strokes: ["g", "o", "i", "m", "⌫", "n", "g", " ", "t"],
    caps: [
      ["Typing it all out\nevery time?..", 30, 100],
      ["One tap —\nand it's done", 140, 204],
      ["Chips know\nthe time of day", 234, 300],
      ["Not a single letter typed", 420, 480],
    ] as [string, number, number][],
  },
  ru: {
    field: "Название события",
    save: "Сохранить",
    date: "Чт, 8 окт.",
    durs: ["15 мин", "30 мин", "1 ч", "2 ч"],
    keys: ["йцукенгшщзх", "фывапролджэ", "⇧ячсмитьбю⌫"],
    // Печатаем «Поход в…» с опечаткой: л вместо о, стираем.
    strokes: ["п", "о", "х", "л", "⌫", "о", "д", " ", "в"],
    caps: [
      ["Каждый раз\nпечатать?..", 30, 100],
      ["Одно касание —\nи готово", 140, 204],
      ["Чипы знают,\nкакое сейчас время", 234, 300],
      ["Ни одной буквы\nс клавиатуры", 420, 480],
    ] as [string, number, number][],
  },
};

// Чипы: подпись → полное название (strings.xml). Порядок — как orderChips для 7:00, 13:00, 20:00.
const CHIPS: Record<Lang, Record<string, string>> = {
  en: { Gym: "🏋️ Going to the Gym", Walking: "🚶 Walking", Doctor: "🩺 Doctor Appointment", Breakfast: "🍳 Breakfast", Project: "💻 Working on a Project", Work: "💼 Working", Meeting: "🤝 Meeting", Lunch: "🥪 Lunch Break", Dinner: "🍽️ Dinner", Party: "🎉 Party", Movie: "🎬 Movie Night", Relax: "🧘 Relaxing", Reading: "📖 Reading Time", Coffee: "☕ Coffee Break" },
  ru: { Спортзал: "🏋️ Поход в спортзал", Прогулка: "🚶 Прогулка", Врач: "🩺 Приём у врача", Завтрак: "🍳 Завтрак", Проект: "💻 Работа над проектом", Работа: "💼 Работа", Встреча: "🤝 Встреча", Обед: "🥪 Обед", Ужин: "🍽️ Ужин", Вечеринка: "🎉 Вечеринка", Фильм: "🎬 Вечер кино", Отдых: "🧘 Отдых", Чтение: "📖 Время для чтения", Кофе: "☕ Кофе-брейк" },
};
const ORDERS: Record<Lang, string[][]> = {
  en: [
    ["Gym", "Walking", "Doctor", "Breakfast", "Project", "Work", "Dinner", "Meeting", "Lunch", "Party", "Movie", "Relax"],
    ["Project", "Work", "Meeting", "Lunch", "Dinner", "Party", "Movie", "Gym", "Relax", "Walking", "Doctor", "Breakfast"],
    ["Dinner", "Party", "Movie", "Relax", "Project", "Work", "Meeting", "Lunch", "Gym", "Walking", "Doctor", "Breakfast"],
  ],
  ru: [
    ["Спортзал", "Прогулка", "Врач", "Завтрак", "Проект", "Работа", "Ужин", "Встреча", "Обед", "Вечеринка", "Фильм", "Отдых"],
    ["Проект", "Работа", "Встреча", "Обед", "Ужин", "Вечеринка", "Фильм", "Спортзал", "Отдых", "Прогулка", "Врач", "Завтрак"],
    ["Ужин", "Вечеринка", "Фильм", "Отдых", "Проект", "Работа", "Встреча", "Обед", "Спортзал", "Прогулка", "Врач", "Завтрак"],
  ],
};
const TRAY: Record<Lang, string[]> = { en: ["Lunch", "Walking", "Reading", "Coffee"], ru: ["Обед", "Прогулка", "Чтение", "Кофе"] };
const PICKS: Record<Lang, [string, string]> = { en: ["Gym", "Movie"], ru: ["Спортзал", "Фильм"] };

const BASE: Record<Lang, E[]> = {
  en: [
    { t: "🚇 Commute", at: "8:40", min: 20 },
    { t: "💻 Deep work", at: "9:00", min: 180 },
    { t: "📞 Call Mom", at: "15:30", min: 15 },
    { t: "🎨 Design review", at: "16:00", min: 60 },
  ],
  ru: [
    { t: "🚇 Дорога", at: "8:40", min: 20 },
    { t: "💻 Глубокая работа", at: "9:00", min: 180 },
    { t: "📞 Позвонить маме", at: "15:30", min: 15 },
    { t: "🎨 Дизайн-ревью", at: "16:00", min: 60 },
  ],
};

// --- Геометрия шторки (dp → px). Те же отступы, что в EventFormContent.
const SHEET_H = 513 * DP;
const SHEET_TOP = 1920 - SHEET_H;
const PAD = 16 * DP;
const IN = 10 * DP;
const SEC1_Y = 36 * DP;
const FIELD_H = 56 * DP;
const CHIPS_Y = SEC1_Y + IN + FIELD_H + 8 * DP;
const CHIP_H = 35 * DP;
const SEC2_Y = SEC1_Y + 127 * DP + 8 * DP;
const DUR_Y = SEC2_Y + IN + 48 * DP + 2 * DP + 40 * DP + 8 * DP;
const SEC3_Y = SEC2_Y + 158 * DP + 8 * DP;
const SAVE_Y = SEC3_Y + 60 * DP + 16 * DP + IN;
const INNER_W = 1080 - 2 * PAD - 2 * IN;
const FIELD = { x: PAD + IN, y: SHEET_TOP + SEC1_Y + IN, w: INNER_W, h: FIELD_H };

const chipW = (label: string) => (label.length * 14 * 0.58 + 32) * DP;
const rowX = (order: string[], label: string) => {
  let x = PAD + IN + 8 * DP;
  for (const l of order) {
    if (l === label) return x;
    x += chipW(l) + 8 * DP;
  }
  return 1200;
};

// --- Лоток финала.
const TRAY_Y = 1920 - 40 * DP - 64 * DP;
const trayPositions = (labels: string[]) => {
  const xs: number[] = [];
  let x = PAD + 14 * DP;
  labels.forEach((l) => {
    xs.push(x);
    x += chipW(l) + 10 * DP;
  });
  return xs;
};

// --- Клавиатура.
const KB_H = 280 * DP;
const KB_TOP = 1920 - KB_H;
const ROW_H = (KB_H - 50) / 4;
const keyRect = (rows: string[], ch: string) => {
  const n = rows[0].length;
  const w = (1080 - 20 - (n - 1) * 10) / n;
  for (let r = 0; r < rows.length; r++) {
    const row = Array.from(rows[r]);
    const i = row.indexOf(ch);
    if (i < 0) continue;
    // В нижнем ряду ⇧ и ⌫ шире; ряд центрируем — у QWERTY средний ряд короче.
    const kw = (j: number) => (r === 2 && (j === 0 || j === row.length - 1) ? w * 1.4 : w);
    const total = row.reduce((a, _, j) => a + kw(j), 0) + (row.length - 1) * 10;
    let x = (1080 - total) / 2;
    for (let j = 0; j < i; j++) x += kw(j) + 10;
    return { x, y: KB_TOP + 14 + r * (ROW_H + 10), w: kw(i), h: ROW_H };
  }
  // Пробел — нижний ряд по центру.
  return { x: 260, y: KB_TOP + 14 + 3 * (ROW_H + 10), w: 560, h: ROW_H };
};
// Печатаем всё медленнее — скучно же.
const strokeAt = (i: number) => TYPE_AT + [0, 7, 14, 22, 32, 41, 49, 58, 68][i];

/** Что набрано к кадру [f]: первая буква — заглавная. */
const typed = (lang: Lang, f: number) => {
  let out = "";
  T[lang].strokes.forEach((k, i) => {
    if (f < strokeAt(i)) return;
    out = k === "⌫" ? out.slice(0, -1) : out + k;
  });
  return out ? out[0].toUpperCase() + out.slice(1) : "";
};

// --- Брошенные в день события: когда бросили и откуда (прямоугольник на экране).
type Added = { e: E; launch: number; from: { x: number; y: number; w: number; h: number } };
const added = (lang: Lang): Added[] => {
  const c = CHIPS[lang];
  const tray = TRAY[lang];
  const xs = trayPositions(tray);
  const fromTray = (i: number) => ({ x: xs[i], y: TRAY_Y + (64 * DP - CHIP_H) / 2, w: chipW(tray[i]), h: CHIP_H });
  const [gym, movie] = PICKS[lang];
  return [
    { e: { t: c[gym], at: "7:00", min: 60 }, launch: SAVE1 + 3, from: FIELD },
    { e: { t: c[movie], at: "20:00", min: 120 }, launch: SAVE2 + 3, from: FIELD },
    { e: { t: c[tray[0]], at: "13:00", min: 60 }, launch: TRAY_TAPS[0] + 1, from: fromTray(0) },
    { e: { t: c[tray[1]], at: "18:00", min: 30 }, launch: TRAY_TAPS[1] + 1, from: fromTray(1) },
    { e: { t: c[tray[2]], at: "22:00", min: 45 }, launch: TRAY_TAPS[2] + 1, from: fromTray(2) },
  ];
};

const THROW = { damping: 13, stiffness: 95, mass: 1 };
const landOf = (launch: number) => {
  for (let t = 0; t < 120; t++) if (spring({ frame: t, fps: 30, config: THROW }) >= 0.97) return launch + t;
  return launch + 40;
};
/** Удар при посадке: затухающие колебания. */
const impact = (t: number) => (t < 0 ? 0 : Math.exp(-t / 6) * Math.sin(t * 0.85));

const toMin = (at: string) => {
  const [h, m] = at.split(":").map(Number);
  return h * 60 + m;
};

export const Chips = ({ lang }: { lang: Lang }) => {
  const f = useCurrentFrame();
  const t = T[lang];
  const sp = (from: number, cfg = { damping: 14, stiffness: 120, mass: 0.9 }) => spring({ frame: f - from, fps: 30, config: cfg });

  // --- День: базовые и брошенные карточки. Место под брошенную раздвигается, пока она летит.
  const adds = added(lang);
  const all = [...BASE[lang].map((e) => ({ e, add: null as Added | null })), ...adds.map((a) => ({ e: a.e, add: a }))].sort((a, z) => toMin(a.e.at) - toMin(z.e.at));
  const open = all.map((x) => (x.add ? sp(x.add.launch + 3, { damping: 13, stiffness: 140, mass: 0.8 }) : 1));
  const { ys } = layout(all.map((x) => x.e), undefined, open);
  const lands = adds.map((a) => landOf(a.launch));

  // --- Шторки: первая — с клавиатурой, вторая — без.
  const sheetIn = (at: number, out: number) => {
    if (f < at) return 0;
    const i = sp(at, { damping: 17, stiffness: 150, mass: 0.9 });
    const o = interpolate(f, [out, out + 8], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp", easing: (x) => x * x });
    return i * (1 - o);
  };
  const sheetP = Math.max(sheetIn(PLUS1 + 2, SAVE1 + 4), sheetIn(PLUS2 + 2, SAVE2 + 4));
  const round: 1 | 2 = f < PLUS2 ? 1 : 2;
  const sheetY = SHEET_TOP + (1 - sheetP) * (SHEET_H + 80);
  const kb = f < KB_OUT ? sp(PLUS1 + 6, { damping: 18, stiffness: 160, mass: 0.8 }) : 1 - easeInOut(Math.min(1, (f - KB_OUT) / 10));

  // --- Финал: камера отъезжает, чтобы видно весь день; дизайн-ревью становится «сейчас».
  const zoom = 1 - 0.25 * easeInOut(interpolate(f, [ZOOM_AT, ZOOM_AT + 26], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }));
  const nowP = sp(NOW_AT, { damping: 11, stiffness: 120, mass: 0.9 });
  // После лотка «+» не возвращается: в финале весь экран — день.
  const hideBar = f < TRAY_IN ? 0 : sp(TRAY_IN, { damping: 14, stiffness: 160, mass: 0.8 });
  const tray = f < TRAY_IN ? 0 : f < TRAY_OUT ? sp(TRAY_IN, { damping: 14, stiffness: 160, mass: 0.8 }) : 1 - easeInOut(Math.min(1, (f - TRAY_OUT) / 10));

  return (
    <AbsoluteFill style={{ background: s.background }}>
      <style>{fontFace}</style>
      <AbsoluteFill style={{ transform: `scale(${zoom})`, transformOrigin: "50% 0%" }}>
        <Header s={s} text={dayTitle(lang, 0)} />
        {all.map((x, i) => {
          const a = x.add;
          const isNow = x.e.at === "16:00" ? nowP : 0;
          // Посадки соседей отдаются толчком — как будто все стоят на одной пружинной полке.
          let nudge = 0;
          adds.forEach((o, j) => {
            if (o === a) return;
            const dist = Math.abs(all.findIndex((y) => y.add === o) - i);
            nudge += impact(f - lands[j]) / (1 + dist * 0.6);
          });
          const shake = `translateY(${nudge * 10 * DP}px)`;
          if (!a) return <Card key={x.e.t} e={x.e} i={i} s={s} top={ys[i]} now={isNow} lang={lang} f={f} style={{ transform: shake }} />;
          if (f < a.launch) return null;
          const land = lands[adds.indexOf(a)];
          if (f < land) return <Thrown key={x.e.t} a={a} e={x.e} i={i} top={ys[i]} f={f} zoom={zoom} />;
          // Приземлилась: сплющивается от удара и отпружинивает.
          const sq = impact(f - land);
          return (
            <Card
              key={x.e.t}
              e={x.e}
              i={i}
              s={s}
              top={ys[i]}
              now={isNow}
              lang={lang}
              f={f}
              style={{ transform: `${shake} scale(${1 + 0.05 * sq}, ${1 - 0.16 * sq})`, transformOrigin: "50% 100%" }}
            />
          );
        })}
        <Toolbar s={s} press={Math.max(0, 1 - Math.abs(f - PLUS1) / 5, 1 - Math.abs(f - PLUS2) / 5)} style={{ opacity: 1 - hideBar, translate: `0 ${hideBar * 300}px` }} />
      </AbsoluteFill>

      <AbsoluteFill style={{ background: `rgba(0,0,0,${0.32 * sheetP})` }} />
      {sheetP > 0.001 && <Sheet lang={lang} f={f} y={sheetY} round={round} />}
      {kb > 0.001 && <Keyboard lang={lang} f={f} y={KB_TOP + (1 - kb) * (KB_H + 40)} />}
      {/* Набранное зря осыпается вниз. */}
      {f >= KB_OUT && f < KB_OUT + 40 && <Falling text={typed(lang, KB_OUT)} f={f - KB_OUT} y={sheetY} />}
      {tray > 0.001 && <Tray lang={lang} f={f} p={tray} />}
      <Fingers lang={lang} f={f} />

      {t.caps.map(([text, from, to]) => (
        <Caption key={text} text={text} from={from} to={to} s={s} y={from > 400 ? 1640 : 150} size={58} />
      ))}
      <Sequence from={END_AT}>
        <End s={s} tagline={TAGLINE[lang]} />
      </Sequence>
    </AbsoluteFill>
  );
};

/**
 * Карточка в полёте: стартует размером с поле (или с чип из лотка), летит по дуге к своему
 * месту, в воздухе ближе к зрителю — крупнее, с большой тенью, кувыркается.
 */
const Thrown = ({ a, e, i, top, f, zoom }: { a: Added; e: E; i: number; top: number; f: number; zoom: number }) => {
  const q = spring({ frame: f - a.launch, fps: 30, config: THROW });
  const h = heightFor(e.min);
  // Откуда — в координатах экрана; карточка живёт внутри «камеры» с зумом от верха по центру.
  const fx = (a.from.x - 540) / zoom + 540;
  const fy = a.from.y / zoom;
  const toX = 16 * DP;
  const sx = a.from.w / zoom / CARD_W;
  const sy = a.from.h / zoom / h;
  const air = Math.sin(Math.PI * Math.min(1, q));
  const x = fx + (toX - fx) * q;
  const y = fy + (top - fy) * q - air * 260;
  const scx = sx + (1 - sx) * q;
  const scy = sy + (1 - sy) * q;
  return (
    <div
      style={{
        position: "absolute",
        inset: 0,
        transform: `translate(${x - toX}px, ${y - top}px) rotate(${(1 - q) * -14}deg) scale(${scx * (1 + 0.12 * air)}, ${scy * (1 + 0.12 * air)})`,
        transformOrigin: `${toX}px ${top}px`,
        filter: `drop-shadow(0 ${60 * air}px ${50 * air}px rgba(80,30,0,${0.35 * air}))`,
        zIndex: 30,
      }}
    >
      <Card e={e} i={i} s={s} top={top} />
    </div>
  );
};

/** Шторка создания: секции на surfaceContainer, как EventFormContent + SaveBar. */
const Sheet = ({ lang, f, y, round }: { lang: Lang; f: number; y: number; round: 1 | 2 }) => {
  const t = T[lang];
  const order = ORDERS[lang];
  const tapAt = round === 1 ? TAP_GYM : TAP_MOVIE;
  const tapped = PICKS[lang][round - 1];
  const title = CHIPS[lang][tapped];
  const saveAt = round === 1 ? SAVE1 : SAVE2;
  const press = (at: number) => Math.max(0, 1 - Math.abs(f - at - 1) / 5);
  const sheetBg = mixHex(s.background, s.surfaceContainer, 0.5);
  const R = 28 * DP;
  const r4 = 4 * DP;
  const halfW = (INNER_W - 2 * DP) / 2;

  // Время: 7:00 в первом раунде; во втором перекручивается 7 → 13 → 20.
  const times = ["7:00", "13:00", "20:00"];
  const changes = [T13, T20];
  const stage = round === 1 ? 0 : changes.filter((c) => f >= c).length;
  const durSel = round === 2 && f >= TAP_DUR ? 3 : 2;
  const durPop = round === 2 ? spring({ frame: f - TAP_DUR, fps: 30, config: { damping: 11, stiffness: 220, mass: 0.6 } }) : 1;

  // Порядок чипов: при смене времени каждый чип по дуге перескакивает на новое место —
  // кто вправо, тот ныряет вниз, кто влево — взлетает, чтобы не сталкивались.
  const delay = (k: number, label: string) => changes[k] + 2 + (order[k + 1].indexOf(label) % 6);
  const chipX = (label: string) => {
    let x = rowX(order[0], label);
    if (round === 1) return x;
    changes.forEach((_, k) => {
      const p = spring({ frame: f - delay(k, label), fps: 30, config: { damping: 12, stiffness: 110, mass: 0.8 } });
      x += (rowX(order[k + 1], label) - rowX(order[k], label)) * p;
    });
    return x;
  };
  const chipArc = (label: string) => {
    if (round === 1) return 0;
    let dy = 0;
    changes.forEach((_, k) => {
      const d = rowX(order[k + 1], label) - rowX(order[k], label);
      const p = Math.min(1, Math.max(0, (f - delay(k, label)) / 12));
      dy += -Math.sin(Math.PI * p) * Math.sign(d) * Math.min(90, Math.abs(d) * 0.25);
    });
    return dy;
  };

  const startTime = times[stage];
  const endMin = toMin(startTime) + (durSel === 3 ? 120 : 60);
  const endTime = `${Math.floor(endMin / 60) % 24}:${String(endMin % 60).padStart(2, "0")}`;
  const arrived = f >= tapAt + FLY;
  const section = (top: number, h: number, children: React.ReactNode) => (
    <div style={{ position: "absolute", left: PAD, right: PAD, top, height: h, borderRadius: 25 * DP, background: s.surfaceContainer, overflow: "hidden" }}>{children}</div>
  );
  const cell = (text: string, x: number, w: number, h: number, radius: string, size: number, key: string, shift = 0) => (
    <div key={key} style={{ position: "absolute", left: x, width: w, height: h, borderRadius: radius, background: s.secondaryContainer, color: s.onSecondaryContainer, overflow: "hidden", ...flex(600), fontSize: size }}>
      <div style={{ position: "absolute", inset: 0, display: "flex", alignItems: "center", justifyContent: "center", transform: `translateY(${shift * h}px)` }}>{text}</div>
    </div>
  );
  // Перекрутка времени: старое уезжает вверх, новое въезжает снизу на пружине.
  const roll = stage > 0 ? spring({ frame: f - changes[stage - 1], fps: 30, config: { damping: 13, stiffness: 170, mass: 0.7 } }) : 1;
  const typedText = round === 1 && f < KB_OUT ? typed(lang, f) : "";
  const fieldBump = arrived ? impact(f - tapAt - FLY) : 0;

  return (
    <div style={{ position: "absolute", left: 0, right: 0, top: y, height: SHEET_H + 200, background: sheetBg, borderRadius: `${R}px ${R}px 0 0`, boxShadow: "0 -10px 40px rgba(0,0,0,0.15)" }}>
      <div style={{ position: "absolute", left: 540 - 16 * DP, top: 16 * DP, width: 32 * DP, height: 4 * DP, borderRadius: 2 * DP, background: s.onSurfaceVariant, opacity: 0.4 }} />
      {section(
        SEC1_Y,
        127 * DP,
        <>
          <div style={{ position: "absolute", left: IN, right: IN, top: IN, height: FIELD_H, borderRadius: 25 * DP, border: `${2 * DP}px solid ${s.primary}`, boxSizing: "border-box", transform: `scale(${1 + 0.03 * fieldBump})` }}>
            <div style={{ position: "absolute", left: 14 * DP, top: -9 * DP, padding: `0 ${4 * DP}px`, background: s.surfaceContainer, ...flex(500), fontSize: 12 * DP, color: s.primary }}>{t.field}</div>
            <div style={{ position: "absolute", left: 16 * DP, top: 0, bottom: 0, display: "flex", alignItems: "center", ...flex(450), fontSize: 16 * DP, color: s.onSurface, whiteSpace: "pre" }}>
              {typedText}
              {arrived &&
                Array.from(title).map((ch, i) => {
                  const p = spring({ frame: f - tapAt - FLY - i * 0.6, fps: 30, config: { damping: 12, stiffness: 260, mass: 0.5 } });
                  return (
                    <span key={i} style={{ display: "inline-block", opacity: Math.min(1, p * 2), transform: `translateY(${(1 - p) * 14}px) scale(${0.6 + 0.4 * p})` }}>
                      {ch}
                    </span>
                  );
                })}
              <span style={{ display: "inline-block", width: 2 * DP, height: 20 * DP, marginLeft: 1 * DP, background: s.primary, opacity: Math.floor(f / 8) % 2 ? 0 : 1 }} />
            </div>
          </div>
          {order[0].map((label, idx) => {
            const isTap = label === tapped;
            // «Привет» волной: каждый чип подпрыгивает по очереди.
            const wl = f - WAVE - idx * 3;
            const hop = round === 1 && wl > 0 && wl < 12 ? Math.sin((Math.PI * wl) / 12) * 22 * DP : 0;
            const chosen = isTap && f >= tapAt;
            const p = isTap ? press(tapAt) : 0;
            return (
              <div
                key={label}
                style={{
                  position: "absolute",
                  left: chipX(label) - PAD,
                  top: CHIPS_Y - SEC1_Y + chipArc(label) - hop,
                  width: chipW(label),
                  height: CHIP_H,
                  borderRadius: 8 * DP,
                  border: `${1 * DP}px solid ${chosen ? s.primary : mixHex(s.onSurfaceVariant, s.surfaceContainer, 0.55)}`,
                  background: chosen ? mixHex(s.surfaceContainer, s.primaryContainer, 0.7) : "transparent",
                  boxSizing: "border-box",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  ...flex(550),
                  fontSize: 14 * DP,
                  color: chosen ? s.onPrimaryContainer : s.onSurfaceVariant,
                  transform: `scale(${1 - 0.1 * p})`,
                  opacity: isTap && f >= tapAt && f < tapAt + FLY + 6 ? 0.35 : 1,
                }}
              >
                {label}
              </div>
            );
          })}
        </>,
      )}
      {section(
        SEC2_Y,
        158 * DP,
        <>
          <div style={{ position: "absolute", left: IN, top: IN }}>
            {stage > 0 && roll < 1 && cell(times[stage - 1], 0, halfW, 48 * DP, `${R}px ${r4}px ${r4}px ${r4}px`, 22 * DP, "prev", -roll)}
            {cell(startTime, 0, halfW, 48 * DP, `${R}px ${r4}px ${r4}px ${r4}px`, 22 * DP, "start", stage > 0 ? 1 - roll : 0)}
            {cell(endTime, halfW + 2 * DP, halfW, 48 * DP, `${r4}px ${R}px ${r4}px ${r4}px`, 22 * DP, "end")}
          </div>
          <div style={{ position: "absolute", left: IN, top: IN + 50 * DP }}>
            {cell(t.date, 0, halfW, 40 * DP, `${r4}px ${r4}px ${r4}px ${R}px`, 14 * DP, "d1")}
            {cell(t.date, halfW + 2 * DP, halfW, 40 * DP, `${r4}px ${r4}px ${R}px ${r4}px`, 14 * DP, "d2")}
          </div>
          {t.durs.map((label, i) => {
            const sel = i === durSel;
            const outer = 20 * DP;
            // Выбранная кнопка группы скругляется целиком — как ButtonGroup в M3 Expressive.
            const inner = sel ? 20 * DP * (0.6 + 0.4 * durPop) : 6 * DP;
            const rad = [i === 0 ? outer : inner, i === 3 ? outer : inner, i === 3 ? outer : inner, i === 0 ? outer : inner];
            return (
              <div
                key={label}
                style={{
                  position: "absolute",
                  left: (INNER_W / 4) * i + IN + 1 * DP,
                  width: INNER_W / 4 - 2 * DP,
                  top: DUR_Y - SEC2_Y,
                  height: 40 * DP,
                  borderRadius: rad.map((r) => `${r}px`).join(" "),
                  background: sel ? s.primary : s.surfaceContainerHigh,
                  color: sel ? s.onPrimary : s.onSurfaceVariant,
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  ...flex(sel ? 700 : 500),
                  fontSize: 14 * DP,
                  transform: `scale(${sel && i === 3 ? 0.88 + 0.12 * durPop : 1})`,
                }}
              >
                {label}
              </div>
            );
          })}
        </>,
      )}
      {/* «Сохранить»: при нажатии скругление уходит — кнопка «сжимается». */}
      <div
        style={{
          position: "absolute",
          left: PAD,
          right: PAD,
          top: SAVE_Y,
          height: 56 * DP,
          borderRadius: (28 - 16 * press(saveAt)) * DP,
          background: s.primary,
          color: s.onPrimary,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          ...flex(650),
          fontSize: 18 * DP,
          transform: `scale(${1 - 0.05 * press(saveAt)})`,
        }}
      >
        {t.save}
      </div>
      {f >= tapAt && f < tapAt + FLY + 2 && <FlyingChip label={tapped} x0={chipX(tapped)} f={f - tapAt} />}
    </div>
  );
};

/** Чип отрывается от ряда и по дуге летит в поле названия — там рассыпается буквами. */
const FlyingChip = ({ label, x0, f }: { label: string; x0: number; f: number }) => {
  const p = easeInOut(Math.min(1, f / FLY));
  const arc = Math.sin(Math.PI * p);
  const y0 = CHIPS_Y;
  const y1 = SEC1_Y + IN + (FIELD_H - CHIP_H) / 2;
  const x1 = PAD + IN + 14 * DP;
  return (
    <div
      style={{
        position: "absolute",
        left: x0 + (x1 - x0) * p,
        top: y0 + (y1 - y0) * p - arc * 120,
        width: chipW(label),
        height: CHIP_H,
        borderRadius: 8 * DP,
        background: s.primaryContainer,
        color: s.onPrimaryContainer,
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        ...flex(650),
        fontSize: 14 * DP,
        transform: `scale(${1 + 0.35 * arc}) rotate(${arc * -10}deg)`,
        boxShadow: `0 ${30 * arc}px 40px rgba(80,30,0,0.3)`,
        opacity: f > FLY ? 1 - (f - FLY) / 2 : 1,
        zIndex: 10,
      }}
    >
      {label}
    </div>
  );
};

/** Клавиатура: клавиши проседают под пальцем, над нажатой всплывает буква. */
const Keyboard = ({ lang, f, y }: { lang: Lang; f: number; y: number }) => {
  const rows = T[lang].keys;
  const strokes = T[lang].strokes;
  const pressedKey = (ch: string) => {
    let p = 0;
    strokes.forEach((k, i) => {
      if (k === ch) p = Math.max(p, 1 - Math.abs(f - strokeAt(i)) / 4);
    });
    return Math.max(0, p);
  };
  const keys = [...rows.flatMap((r) => Array.from(r)), " "];
  return (
    <div style={{ position: "absolute", left: 0, right: 0, top: y, height: KB_H + 60, background: mixHex(s.surfaceContainerHigh, "#D9D9DE", 0.5) }}>
      {keys.map((ch) => {
        const r = keyRect(rows, ch);
        const p = pressedKey(ch);
        const special = ch === "⇧" || ch === "⌫";
        return (
          <div key={ch}>
            <div
              style={{
                position: "absolute",
                left: r.x,
                top: r.y - KB_TOP,
                width: r.w,
                height: r.h,
                borderRadius: 14,
                background: special ? "#C9CBD1" : mixHex("#FFFFFF", s.primaryContainer, p),
                boxShadow: "0 3px 0 rgba(0,0,0,0.12)",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                ...flex(450),
                fontSize: ch === " " ? 0 : 52,
                color: "#202124",
                transform: `translateY(${p * 4}px) scale(${1 - 0.06 * p})`,
              }}
            >
              {ch}
            </div>
            {p > 0.05 && !special && ch !== " " && (
              <div
                style={{
                  position: "absolute",
                  left: r.x - 14,
                  top: r.y - KB_TOP - r.h - 20,
                  width: r.w + 28,
                  height: r.h + 10,
                  borderRadius: 18,
                  background: "#FFFFFF",
                  boxShadow: "0 8px 20px rgba(0,0,0,0.2)",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  ...flex(600),
                  fontSize: 70,
                  color: "#202124",
                  transform: `scale(${0.6 + 0.4 * p})`,
                  transformOrigin: "50% 100%",
                  opacity: p,
                }}
              >
                {ch}
              </div>
            )}
          </div>
        );
      })}
    </div>
  );
};

/** Набранное зря осыпается: каждая буква подлетает и падает, кувыркаясь. */
const Falling = ({ text, f, y }: { text: string; f: number; y: number }) => {
  const top = y + SEC1_Y + IN + FIELD_H / 2 - 12 * DP;
  let x = FIELD.x + 16 * DP;
  return (
    <>
      {Array.from(text).map((ch, i) => {
        const cx = x;
        x += ch === " " ? 5 * DP : 9 * DP;
        const t = Math.max(0, f - i * 1.5) / 30;
        const vy = -300 - ((i * 37) % 5) * 60;
        return (
          <span
            key={i}
            style={{
              position: "absolute",
              left: cx + t * ((i % 3) - 1) * 160,
              top: top + vy * t + 0.5 * 4200 * t * t,
              ...flex(450),
              fontSize: 16 * DP,
              color: s.onSurface,
              transform: `rotate(${t * ((i % 2) * 2 - 1) * 400}deg)`,
              zIndex: 20,
            }}
          >
            {ch}
          </span>
        );
      })}
    </>
  );
};

/** Лоток чипов в финале: выезжает снизу, чипы здороваются, по касанию улетают в день. */
const Tray = ({ lang, f, p }: { lang: Lang; f: number; p: number }) => {
  const labels = TRAY[lang];
  const xs = trayPositions(labels);
  return (
    <div style={{ position: "absolute", left: PAD, right: PAD, top: TRAY_Y + (1 - p) * 300, height: 64 * DP, borderRadius: 32 * DP, background: s.surfaceContainerHigh, boxShadow: "0 12px 30px rgba(80,30,0,0.2)" }}>
      {labels.map((l, i) => {
        const tap = TRAY_TAPS[i];
        if (tap !== undefined && f > tap) return null;
        const press = tap !== undefined ? Math.max(0, 1 - Math.abs(f - tap) / 4) : 0;
        const wl = f - TRAY_IN - 6 - i * 3;
        const hop = wl > 0 && wl < 10 ? Math.sin((Math.PI * wl) / 10) * 18 * DP : 0;
        return (
          <div
            key={l}
            style={{
              position: "absolute",
              left: xs[i] - PAD,
              top: (64 * DP - CHIP_H) / 2 - hop,
              width: chipW(l),
              height: CHIP_H,
              borderRadius: 8 * DP,
              border: `${1 * DP}px solid ${mixHex(s.onSurfaceVariant, s.surfaceContainerHigh, 0.5)}`,
              boxSizing: "border-box",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              ...flex(550),
              fontSize: 14 * DP,
              color: s.onSurfaceVariant,
              background: press > 0 ? mixHex(s.surfaceContainerHigh, s.primaryContainer, press) : "transparent",
              transform: `scale(${1 - 0.1 * press})`,
            }}
          >
            {l}
          </div>
        );
      })}
    </div>
  );
};

/** Палец ходит по всем касаниям ролика: «+», клавиши, чипы, длительность, «Сохранить», лоток. */
const Fingers = ({ lang, f }: { lang: Lang; f: number }) => {
  const rows = T[lang].keys;
  const order = ORDERS[lang];
  const [gym, movie] = PICKS[lang];
  const rowY = SHEET_TOP + CHIPS_Y + CHIP_H / 2;
  const xs = trayPositions(TRAY[lang]);
  const taps: [number, number, number][] = [
    [PLUS1, 540, TOOLBAR_Y + 32 * DP],
    ...T[lang].strokes.map((k, i) => {
      const r = keyRect(rows, k);
      return [strokeAt(i), r.x + r.w / 2 + 10, r.y + r.h / 2 + 20] as [number, number, number];
    }),
    [TAP_GYM, rowX(order[0], gym) + chipW(gym) / 2, rowY],
    [SAVE1, 540, SHEET_TOP + SAVE_Y + 28 * DP],
    [PLUS2, 540, TOOLBAR_Y + 32 * DP],
    [TAP_MOVIE, rowX(order[2], movie) + chipW(movie) / 2, rowY],
    [TAP_DUR, PAD + IN + (INNER_W / 4) * 3.5, SHEET_TOP + DUR_Y + 20 * DP],
    [SAVE2, 540, SHEET_TOP + SAVE_Y + 28 * DP],
    ...TRAY_TAPS.map((at, i) => [at, xs[i] + chipW(TRAY[lang][i]) / 2, TRAY_Y + 32 * DP] as [number, number, number]),
  ];
  let k = taps.findIndex((tp) => tp[0] > f);
  if (k < 0) k = taps.length;
  const prev = taps[k - 1];
  const next = taps[k];
  const near = prev && next && next[0] - prev[0] <= 40;
  let x = 0;
  let y = 0;
  let show = 0;
  if (near) {
    // Касания рядом — палец переезжает от одного к другому.
    const lead = Math.min(10, next[0] - prev[0] - 2);
    const m = easeInOut(Math.min(1, Math.max(0, (f - (next[0] - lead)) / lead)));
    x = prev[1] + (next[1] - prev[1]) * m;
    y = prev[2] + (next[2] - prev[2]) * m;
    show = 1;
  } else if (next && f >= next[0] - 10) {
    // Далеко — палец заходит снизу.
    const m = easeInOut((f - (next[0] - 10)) / 10);
    x = next[1];
    y = next[2] + (1 - m) * 200;
    show = m;
  } else if (prev && f < prev[0] + 12) {
    // И уходит.
    const m = (f - prev[0]) / 12;
    x = prev[1];
    y = prev[2] + m * 120;
    show = 1 - m;
  }
  if (show <= 0) return null;
  const since = prev ? f - prev[0] : -1;
  const down = (next && next[0] - f <= 1) || (since >= 0 && since < 3) ? 1 : 0;
  return <Touch x={x} y={y} down={down} show={show} tap={since >= 0 && since < 18 ? since : -1} />;
};
