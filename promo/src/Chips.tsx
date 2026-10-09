import { AbsoluteFill, Sequence, interpolate, spring, useCurrentFrame } from "remotion";
import { DP, mixHex } from "./DayScene";
import { End, TAGLINE } from "./End";
import { easeInOut } from "./parts";
import { SCHEMES, Scheme } from "./schemes";
import { flex, fontFace } from "./theme";
import { Caption, Card, E, Header, Lang, TOOLBAR_Y, Toolbar, Touch, dayTitle, layout } from "./kit";

/**
 * Вариант «Чипы»: три события подряд, каждое быстрее. «+» → шторка → чип → (длительность) →
 * «Сохранить» → карточка падает в ленту и раздвигает соседей. Ни одной буквы с клавиатуры.
 */
export const CHIPS_FRAMES = 600;
const END_AT = 486;
const s = SCHEMES[0]; // лаванда, светлая

type Round = {
  at: number;
  k: number; // растяжение темпа: 1.6 — неспешно, 0.8 — бегом
  start: string;
  chips: [string, string][]; // [подпись чипа, полное название]
  tap: number; // какой чип нажать
  dur: number | null; // какую длительность нажать (индекс в DURS), null — оставить 1 ч
  min: number;
};

const DURS = { en: ["15 min", "30 min", "1 h", "2 h"], ru: ["15 мин", "30 мин", "1 ч", "2 ч"] };
const DUR_MIN = [15, 30, 60, 120];

// Порядок чипов — как orderChips для этого часа: сначала те, чьи часы его захватывают.
const ROUNDS: Record<Lang, Round[]> = {
  en: [
    { at: 30, k: 1.6, start: "7:00", tap: 0, dur: null, min: 60, chips: [["Gym", "🏋️ Going to the Gym"], ["Walking", "🚶 Walking"], ["Doctor", "🩺 Doctor Appointment"], ["Breakfast", "🍳 Breakfast"], ["Pet", "🦮 Walking the pet"]] },
    { at: 172, k: 1.15, start: "13:00", tap: 3, dur: 1, min: 30, chips: [["Project", "💻 Working on a Project"], ["Work", "💼 Working"], ["Meeting", "🤝 Meeting"], ["Lunch", "🥪 Lunch Break"], ["Shopping", "🛍️ Shopping"]] },
    { at: 295, k: 0.8, start: "20:00", tap: 2, dur: 3, min: 120, chips: [["Dinner", "🍽️ Dinner"], ["Party", "🎉 Party"], ["Movie", "🎬 Movie Night"], ["Relax", "🧘 Relaxing"], ["Hobby", "🎨 Hobby Time"]] },
  ],
  ru: [
    { at: 30, k: 1.6, start: "7:00", tap: 0, dur: null, min: 60, chips: [["Спортзал", "🏋️ Поход в спортзал"], ["Прогулка", "🚶 Прогулка"], ["Врач", "🩺 Приём у врача"], ["Завтрак", "🍳 Завтрак"], ["Питомец", "🦮 Выгул питомца"]] },
    { at: 172, k: 1.15, start: "13:00", tap: 3, dur: 1, min: 30, chips: [["Проект", "💻 Работа над проектом"], ["Работа", "💼 Работа"], ["Встреча", "🤝 Встреча"], ["Обед", "🥪 Обед"], ["Покупки", "🛍️ Покупки"]] },
    { at: 295, k: 0.8, start: "20:00", tap: 2, dur: 3, min: 120, chips: [["Ужин", "🍽️ Ужин"], ["Вечеринка", "🎉 Вечеринка"], ["Фильм", "🎬 Вечер кино"], ["Отдых", "🧘 Отдых"], ["Хобби", "🎨 Время для хобби"]] },
  ],
};

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

const T = {
  en: { title: "Title", place: "Place", notes: "Notes", remind: "10 min", save: "Save", date: "Thu, Oct 8" },
  ru: { title: "Название события", place: "Место", notes: "Описание", remind: "10 мин", save: "Сохранить", date: "Чт, 8 окт." },
};

const CAPTIONS: Record<Lang, [string, number, number][]> = {
  en: [
    ["Tap a chip —\nthe title is done", 86, 160],
    ["Chips know\nthe time of day", 196, 286],
    ["Not a single letter typed", 384, 476],
  ],
  ru: [
    ["Нажали чип —\nназвание готово", 86, 160],
    ["Чипы знают,\nкакое сейчас время", 196, 286],
    ["Ни одной буквы\nс клавиатуры", 384, 476],
  ],
};

// --- Геометрия шторки (dp → px). Те же отступы, что в EventFormContent.
const SHEET_H = 513 * DP;
const SHEET_TOP = 1920 - SHEET_H;
const PAD = 16 * DP; // поля формы
const IN = 10 * DP; // ContainerPadding
const SEC1_Y = 36 * DP;
const FIELD_H = 56 * DP;
const CHIPS_Y = SEC1_Y + IN + FIELD_H + 8 * DP;
const CHIP_H = 35 * DP;
const SEC2_Y = SEC1_Y + 127 * DP + 8 * DP;
const DUR_Y = SEC2_Y + IN + 48 * DP + 2 * DP + 40 * DP + 8 * DP;
const SEC3_Y = SEC2_Y + 158 * DP + 8 * DP;
const SAVE_Y = SEC3_Y + 60 * DP + 16 * DP + IN;
const INNER_W = 1080 - 2 * PAD - 2 * IN;

/** Ширина чипа — по длине подписи (RobotoFlex 14 sp, средняя буква ≈ 0.56 em) плюс поля. */
const chipW = (label: string) => (label.length * 14 * 0.58 + 32) * DP;
const chipX = (chips: [string, string][], i: number) => {
  let x = PAD + IN + 8 * DP;
  for (let j = 0; j < i; j++) x += chipW(chips[j][0]) + 8 * DP;
  return x;
};
const durX = (i: number) => PAD + IN + (INNER_W / 4) * i;

/** Ключевые моменты раунда (в кадрах от его начала, уже с темпом). */
const beats = (r: Round) => {
  const k = r.k;
  const plus = 6 * k;
  const sheet = 8 * k;
  const chip = 34 * k;
  const dur = r.dur !== null ? 50 * k : null;
  const save = (r.dur !== null ? 66 : 52) * k;
  return { plus, sheet, chip, dur, save, out: save + 4, insert: save + 7, end: save + 30 * k };
};

/** Где палец в момент касания — по порядку раунда. */
const targets = (r: Round, sheetY: number) => {
  const b = beats(r);
  const list: [number, number, number][] = [[b.plus, 540, TOOLBAR_Y + 32 * DP]];
  list.push([b.chip, chipX(r.chips, r.tap) + chipW(r.chips[r.tap][0]) / 2, sheetY + CHIPS_Y + CHIP_H / 2]);
  if (b.dur !== null && r.dur !== null) list.push([b.dur, durX(r.dur) + INNER_W / 8, sheetY + DUR_Y + 20 * DP]);
  list.push([b.save, 540, sheetY + SAVE_Y + 28 * DP]);
  return list;
};

export const Chips = ({ lang }: { lang: Lang }) => {
  const f = useCurrentFrame();
  const rounds = ROUNDS[lang];
  const t = T[lang];
  const sp = (from: number, cfg = { damping: 14, stiffness: 120, mass: 0.9 }) => spring({ frame: f - from, fps: 30, config: cfg });

  // Текущий раунд — последний начавшийся.
  let ri = -1;
  rounds.forEach((r, i) => {
    if (f >= r.at) ri = i;
  });
  const r = ri >= 0 ? rounds[ri] : null;
  const local = r ? f - r.at : 0;
  const b = r ? beats(r) : null;

  // --- Лента дня: базовые события и вставленные. Вставленное раскрывается на пружине.
  const added = rounds.map((rr, i) => {
    const ab = beats(rr);
    const title = rr.chips[rr.tap][1];
    return { e: { t: title, at: rr.start, min: rr.min } as E, at: rr.at + ab.insert, i };
  });
  const all = [...BASE[lang].map((e) => ({ e, at: -100, i: -1 })), ...added].sort((a, z) => toMin(a.e.at) - toMin(z.e.at));
  const open = all.map((x) => (x.at < 0 ? 1 : sp(x.at, { damping: 12, stiffness: 150, mass: 0.8 })));
  const { ys } = layout(all.map((x) => x.e), undefined, open);

  // --- Шторка.
  let sheetP = 0;
  if (r && b) {
    const inP = sp(r.at + b.sheet, { damping: 17, stiffness: 150, mass: 0.9 });
    const outP = interpolate(local, [b.out, b.out + 9 * Math.min(1, r.k)], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp", easing: (x) => x * x });
    sheetP = local >= b.out ? inP * (1 - outP) : inP;
  }
  const sheetY = SHEET_TOP + (1 - sheetP) * (SHEET_H + 60);

  // --- Палец: перелетает между целями, у цели прижимается.
  let finger = { x: 540, y: 2100, down: 0, show: 0, tap: -1 };
  if (r && b && local < b.save + 12) {
    const list = targets(r, SHEET_TOP);
    let x = list[0][1];
    let y = list[0][2] + 260;
    let down = 0;
    let tap = -1;
    for (let j = 0; j < list.length; j++) {
      const [at, tx, ty] = list[j];
      const lead = Math.max(5, 12 * r.k);
      const prev: [number, number] = j === 0 ? [tx, ty + 260] : [list[j - 1][1], list[j - 1][2]];
      if (local >= at - lead) {
        const m = easeInOut(Math.min(1, (local - (at - lead)) / lead));
        x = prev[0] + (tx - prev[0]) * m;
        y = prev[1] + (ty - prev[1]) * m;
      }
      if (local >= at - 2 && local < at + 5) {
        down = 1;
        tap = local - at;
      }
    }
    const show = Math.min(1, local / 4) * interpolate(local, [b.save + 4, b.save + 12], [1, 0], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
    finger = { x, y, down, show, tap };
  }

  // Нажатия — для отклика кнопок.
  const pressed = (at: number | null | undefined) => (r && at != null ? Math.max(0, 1 - Math.abs(local - at - 1) / 5) : 0);

  // --- Финал: отъезд, чтобы видно весь день; дизайн-ревью становится «сейчас».
  const zoom = 1 - 0.1 * easeInOut(interpolate(f, [380, 410], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }));
  const nowP = sp(392, { damping: 13, stiffness: 110, mass: 0.9 });

  return (
    <AbsoluteFill style={{ background: s.background }}>
      <style>{fontFace}</style>
      <AbsoluteFill style={{ transform: `scale(${zoom})`, transformOrigin: "50% 40%" }}>
        <Header s={s} text={dayTitle(lang, 0)} />
        {all.map((x, i) => {
          const fresh = x.at >= 0;
          const o = open[i];
          if (fresh && f < x.at) return null;
          const isNow = x.e.at === "16:00" ? nowP : 0;
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
              style={{
                opacity: fresh ? Math.min(1, o * 2) : 1,
                transform: fresh
                  ? `translateY(${(1 - o) * 220}px) scale(${0.7 + 0.3 * o}) rotate(${(1 - o) * -4}deg)`
                  : undefined,
                transformOrigin: "50% 0%",
              }}
            />
          );
        })}
        <Toolbar s={s} press={b ? pressed(b.plus) : 0} />
      </AbsoluteFill>

      {/* Затемнение под шторкой. */}
      <AbsoluteFill style={{ background: `rgba(0,0,0,${0.32 * sheetP})` }} />

      {r && sheetP > 0.001 && (
        <Sheet s={s} y={sheetY} r={r} local={local} b={beats(r)} t={t} lang={lang} pressed={pressed} />
      )}

      {finger.show > 0 && <Touch x={finger.x} y={finger.y} down={finger.down} show={finger.show} tap={finger.tap} />}

      {CAPTIONS[lang].map(([text, from, to]) => (
        <Caption key={text} text={text} from={from} to={to} s={s} y={from > 380 ? 1580 : 150} size={58} />
      ))}

      <Sequence from={END_AT}>
        <End s={s} tagline={TAGLINE[lang]} />
      </Sequence>
    </AbsoluteFill>
  );
};

const toMin = (at: string) => {
  const [h, m] = at.split(":").map(Number);
  return h * 60 + m;
};

/** Шторка создания: секции на surfaceContainer, как EventFormContent + SaveBar. */
const Sheet = ({
  s,
  y,
  r,
  local,
  b,
  t,
  lang,
  pressed,
}: {
  s: Scheme;
  y: number;
  r: Round;
  local: number;
  b: ReturnType<typeof beats>;
  t: (typeof T)["en"];
  lang: Lang;
  pressed: (at: number | null | undefined) => number;
}) => {
  const title = r.chips[r.tap][1];
  const typed = local >= b.chip ? local - b.chip : -1;
  const sheetBg = mixHex(s.background, s.surfaceContainer, 0.5);
  const durSel = r.dur !== null && b.dur !== null && local >= b.dur ? r.dur : 2;
  const durSwap = r.dur !== null && b.dur !== null ? spring({ frame: local - b.dur, fps: 30, config: { damping: 12, stiffness: 200, mass: 0.6 } }) : 1;
  const endMin = toMin(r.start) + DUR_MIN[durSel];
  const end = `${Math.floor(endMin / 60) % 24}:${String(endMin % 60).padStart(2, "0")}`;
  const saveP = pressed(b.save);
  const section = (top: number, h: number, children: React.ReactNode) => (
    <div style={{ position: "absolute", left: PAD, right: PAD, top, height: h, borderRadius: 25 * DP, background: s.surfaceContainer, overflow: "hidden" }}>{children}</div>
  );
  const cell = (text: string, x: number, w: number, h: number, radius: string, size: number, wght = 600) => (
    <div style={{ position: "absolute", left: x, width: w, height: h, borderRadius: radius, background: s.secondaryContainer, color: s.onSecondaryContainer, display: "flex", alignItems: "center", justifyContent: "center", ...flex(wght), fontSize: size }}>{text}</div>
  );
  const halfW = (INNER_W - 2 * DP) / 2;
  const R = 28 * DP;
  const r4 = 4 * DP;
  return (
    <div style={{ position: "absolute", left: 0, right: 0, top: y, height: SHEET_H + 200, background: sheetBg, borderRadius: `${R}px ${R}px 0 0`, boxShadow: "0 -10px 40px rgba(0,0,0,0.15)" }}>
      <div style={{ position: "absolute", left: 540 - 16 * DP, top: 16 * DP, width: 32 * DP, height: 4 * DP, borderRadius: 2 * DP, background: s.onSurfaceVariant, opacity: 0.4 }} />

      {/* Название и чипы. */}
      {section(
        SEC1_Y,
        127 * DP,
        <>
          <div style={{ position: "absolute", left: IN, right: IN, top: IN, height: FIELD_H, borderRadius: 25 * DP, border: `${2 * DP}px solid ${s.primary}`, boxSizing: "border-box" }}>
            <div style={{ position: "absolute", left: 14 * DP, top: -9 * DP, padding: `0 ${4 * DP}px`, background: s.surfaceContainer, ...flex(500), fontSize: 12 * DP, color: s.primary }}>{t.title}</div>
            <div style={{ position: "absolute", left: 16 * DP, top: 0, bottom: 0, display: "flex", alignItems: "center", ...flex(450), fontSize: 16 * DP, color: s.onSurface, whiteSpace: "pre" }}>
              {typed >= 0 &&
                Array.from(title).map((ch, i) => {
                  const p = spring({ frame: typed - i * 0.6, fps: 30, config: { damping: 12, stiffness: 260, mass: 0.5 } });
                  return (
                    <span key={i} style={{ display: "inline-block", opacity: Math.min(1, p * 2), transform: `translateY(${(1 - p) * 14}px) scale(${0.6 + 0.4 * p})` }}>
                      {ch}
                    </span>
                  );
                })}
              {/* Курсор мигает в конце. */}
              <span style={{ display: "inline-block", width: 2 * DP, height: 20 * DP, marginLeft: 1 * DP, background: s.primary, opacity: Math.floor(local / 8) % 2 ? 0 : 1 }} />
            </div>
          </div>
          {r.chips.map(([label], i) => {
            const isTap = i === r.tap;
            const p = isTap ? pressed(b.chip) : 0;
            const chosen = isTap && local >= b.chip;
            return (
              <div
                key={label}
                style={{
                  position: "absolute",
                  left: chipX(r.chips, i) - PAD,
                  top: CHIPS_Y - SEC1_Y,
                  width: chipW(label),
                  height: CHIP_H,
                  borderRadius: 8 * DP,
                  border: `${1 * DP}px solid ${chosen ? s.primary : mixHex(s.onSurfaceVariant, s.surfaceContainer, 0.55)}`,
                  background: chosen ? mixHex(s.surfaceContainer, s.primary, 0.12 + 0.2 * p) : "transparent",
                  boxSizing: "border-box",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  ...flex(550),
                  fontSize: 14 * DP,
                  color: s.onSurfaceVariant,
                  transform: `scale(${1 - 0.08 * p})`,
                }}
              >
                {label}
              </div>
            );
          })}
        </>,
      )}

      {/* Время, дата и быстрая длительность. */}
      {section(
        SEC2_Y,
        158 * DP,
        <>
          <div style={{ position: "absolute", left: IN, top: IN }}>
            {cell(r.start, 0, halfW, 48 * DP, `${R}px ${r4}px ${r4}px ${r4}px`, 22 * DP, 600)}
            {cell(end, halfW + 2 * DP, halfW, 48 * DP, `${r4}px ${R}px ${r4}px ${r4}px`, 22 * DP, 600)}
          </div>
          <div style={{ position: "absolute", left: IN, top: IN + 50 * DP }}>
            {cell(t.date, 0, halfW, 40 * DP, `${r4}px ${r4}px ${r4}px ${R}px`, 14 * DP, 550)}
            {cell(t.date, halfW + 2 * DP, halfW, 40 * DP, `${r4}px ${r4}px ${R}px ${r4}px`, 14 * DP, 550)}
          </div>
          {DURS[lang].map((label, i) => {
            const sel = i === durSel;
            // Выбранная кнопка группы скругляется целиком — как ButtonGroup в M3 Expressive.
            const round = sel ? 20 * DP * (0.6 + 0.4 * durSwap) : 0;
            const outer = 20 * DP;
            const radius = [
              i === 0 ? outer : Math.max(round, 6 * DP),
              i === 3 ? outer : Math.max(round, 6 * DP),
              i === 3 ? outer : Math.max(round, 6 * DP),
              i === 0 ? outer : Math.max(round, 6 * DP),
            ];
            return (
              <div
                key={label}
                style={{
                  position: "absolute",
                  left: (INNER_W / 4) * i + IN + 1 * DP,
                  width: INNER_W / 4 - 2 * DP,
                  top: DUR_Y - SEC2_Y,
                  height: 40 * DP,
                  borderRadius: `${radius[0]}px ${radius[1]}px ${radius[2]}px ${radius[3]}px`,
                  background: sel ? s.primary : s.surfaceContainerHigh,
                  color: sel ? s.onPrimary : s.onSurfaceVariant,
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  ...flex(sel ? 700 : 500),
                  fontSize: 14 * DP,
                  transform: `scale(${sel && i === r.dur ? 0.9 + 0.1 * durSwap : 1})`,
                }}
              >
                {label}
              </div>
            );
          })}
        </>,
      )}

      {/* Место, описание, напоминание — по запросу. */}
      {section(
        SEC3_Y,
        60 * DP,
        <div style={{ position: "absolute", left: IN, right: IN, top: IN, height: 40 * DP, display: "flex", gap: 2 * DP }}>
          {[t.place, t.notes, `🔔 ${t.remind}`].map((label, i) => (
            <div
              key={label}
              style={{
                flex: 1,
                borderRadius: i === 0 ? `${20 * DP}px ${6 * DP}px ${6 * DP}px ${20 * DP}px` : i === 2 ? `${6 * DP}px ${20 * DP}px ${20 * DP}px ${6 * DP}px` : 6 * DP,
                background: s.surfaceContainerHigh,
                color: s.onSurfaceVariant,
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                ...flex(500),
                fontSize: 14 * DP,
              }}
            >
              {label}
            </div>
          ))}
        </div>,
      )}

      {/* «Сохранить»: при нажатии скругление уходит — кнопка «сжимается» в квадратик. */}
      <div
        style={{
          position: "absolute",
          left: PAD,
          right: PAD,
          top: SAVE_Y,
          height: 56 * DP,
          borderRadius: (28 - 16 * saveP) * DP,
          background: s.primary,
          color: s.onPrimary,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          ...flex(650),
          fontSize: 18 * DP,
          transform: `scale(${1 - 0.04 * saveP})`,
        }}
      >
        {t.save}
      </div>
    </div>
  );
};

