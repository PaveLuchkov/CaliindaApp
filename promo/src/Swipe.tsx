import { AbsoluteFill, Sequence, interpolate, spring, useCurrentFrame } from "remotion";
import { End, TAGLINE } from "./End";
import { easeInOut } from "./parts";
import { SCHEMES } from "./schemes";
import { fontFace } from "./theme";
import { Caption, Card, E, Header, Lang, Touch, dayTitle, lagOffset, lagSeries, layout } from "./kit";

/**
 * Вариант «Листай»: вертикальный пейджер по дням. Свайп — и завтра; карточки отстают от
 * страницы на пружинках и догоняют с перелётом (SpringyScroll). Потом отъезд камеры: дни
 * стопкой, лента бежит — и финал.
 */
export const SWIPE_FRAMES = 600;
const END_AT = 486;
const H = 1920;

const s = SCHEMES[2]; // коралл, светлая

export const DAYS: Record<Lang, E[][]> = {
  en: [
    [["☕ Breakfast", "8:00", 30], ["💻 Deep work", "9:00", 180], ["🎨 Design review", "14:00", 60], ["📞 Call Mom", "15:30", 15], ["🏋️ Gym", "18:00", 90]],
    [["🥐 Croissant run", "8:00", 20], ["🧪 Lab meeting", "10:00", 120], ["🥪 Lunch Break", "13:00", 60], ["🎸 Guitar lesson", "17:00", 45]],
    [["🧘 Morning yoga", "7:00", 60], ["🤝 Meeting", "11:00", 60], ["💊 Vitamins", "13:00", 10], ["🎬 Movie Night", "20:00", 150]],
    [["🐕 Walk the dog", "7:30", 30], ["🏞️ Hike", "10:00", 240], ["🍝 Dinner with friends", "19:00", 120]],
    [["🛒 Groceries", "10:00", 45], ["🍳 Cooking a Meal", "12:00", 60], ["🎮 Game night", "19:00", 180]],
    [["🚲 Ride to the lake", "9:00", 120], ["🧺 Picnic", "12:00", 90], ["📖 Reading Time", "21:00", 45]],
    [["💼 Working", "9:00", 240], ["☕ Coffee Break", "13:30", 20], ["🤝 Meeting", "15:00", 60], ["🧘 Relaxing", "19:00", 60]],
    [["🩺 Doctor", "8:30", 45], ["📚 Studying", "10:00", 180], ["🎉 Party", "20:00", 180]],
    [["🦮 Walking the pet", "7:00", 30], ["✈️ Flight to Lisbon", "11:00", 210], ["🍷 Tapas", "20:00", 90]],
    [["🌅 Sunrise", "6:30", 20], ["🏖️ Beach", "10:00", 180], ["🎶 Fado night", "21:00", 120]],
  ].map((d) => d.map(([t, at, min]) => ({ t, at, min }) as E)),
  ru: [
    [["☕ Завтрак", "8:00", 30], ["💻 Глубокая работа", "9:00", 180], ["🎨 Дизайн-ревью", "14:00", 60], ["📞 Позвонить маме", "15:30", 15], ["🏋️ Зал", "18:00", 90]],
    [["🥐 За круассаном", "8:00", 20], ["🧪 Встреча в лабе", "10:00", 120], ["🥪 Обед", "13:00", 60], ["🎸 Урок гитары", "17:00", 45]],
    [["🧘 Утренняя йога", "7:00", 60], ["🤝 Встреча", "11:00", 60], ["💊 Витамины", "13:00", 10], ["🎬 Вечер кино", "20:00", 150]],
    [["🐕 Выгул собаки", "7:30", 30], ["🏞️ Поход", "10:00", 240], ["🍝 Ужин с друзьями", "19:00", 120]],
    [["🛒 Продукты", "10:00", 45], ["🍳 Готовка", "12:00", 60], ["🎮 Вечер игр", "19:00", 180]],
    [["🚲 На озеро", "9:00", 120], ["🧺 Пикник", "12:00", 90], ["📖 Время для чтения", "21:00", 45]],
    [["💼 Работа", "9:00", 240], ["☕ Кофе-брейк", "13:30", 20], ["🤝 Встреча", "15:00", 60], ["🧘 Отдых", "19:00", 60]],
    [["🩺 Приём у врача", "8:30", 45], ["📚 Учёба", "10:00", 180], ["🎉 Вечеринка", "20:00", 180]],
    [["🦮 Выгул питомца", "7:00", 30], ["✈️ Рейс в Лиссабон", "11:00", 210], ["🍷 Тапас", "20:00", 90]],
    [["🌅 Рассвет", "6:30", 20], ["🏖️ Пляж", "10:00", 180], ["🎶 Вечер фаду", "21:00", 120]],
  ].map((d) => d.map(([t, at, min]) => ({ t, at, min }) as E)),
};

const CAPTIONS: Record<Lang, [string, number, number][]> = {
  en: [
    ["Swipe up — it's tomorrow", 12, 104],
    ["Every day gets its own page", 116, 222],
    ["Flip through just for fun", 236, 336],
    ["Your whole week,\none swipe away", 392, 474],
  ],
  ru: [
    ["Свайп вверх — и завтра", 12, 104],
    ["У каждого дня своя страница", 116, 222],
    ["Листать — одно удовольствие", 236, 336],
    ["Вся неделя —\nна расстоянии свайпа", 392, 474],
  ],
};

/** Свайп: с кадра [at] палец тянет [drag] кадров на [dist] px, потом страница доезжает до [to]. */
type Swipe = { at: number; to: number; drag: number; dist: number };
const SWIPES: Swipe[] = [
  { at: 40, to: 1, drag: 14, dist: 560 },
  { at: 120, to: 2, drag: 12, dist: 520 },
  { at: 182, to: 3, drag: 10, dist: 480 },
  // Серия быстрых флингов.
  { at: 240, to: 4, drag: 5, dist: 300 },
  { at: 258, to: 5, drag: 5, dist: 300 },
  { at: 274, to: 6, drag: 4, dist: 280 },
  { at: 288, to: 7, drag: 4, dist: 280 },
  { at: 300, to: 8, drag: 4, dist: 280 },
  // И назад — вниз, на день раньше.
  { at: 346, to: 7, drag: 10, dist: -500 },
];

const SNAP = { damping: 28, stiffness: 190, mass: 1 };

/** Положение пейджера (номер страницы, дробный) и палец на кадре [f]. */
const pager = (f: number) => {
  let p = 0;
  let finger = { y: 0, down: 0, show: 0, tap: -1 };
  for (let k = 0; k < SWIPES.length; k++) {
    const sw = SWIPES[k];
    if (f < sw.at) break;
    const from = k === 0 ? 0 : SWIPES[k - 1].to;
    const local = f - sw.at;
    const dragged = (sw.dist / H) * easeInOut(Math.min(1, local / sw.drag));
    const startY = sw.dist > 0 ? 1380 : 560;
    if (local <= sw.drag) {
      p = from + dragged;
      finger = { y: startY - sw.dist * easeInOut(Math.min(1, local / sw.drag)), down: 1, show: Math.min(1, local / 3 + 0.4), tap: local };
    } else {
      const sp = spring({ frame: local - sw.drag, fps: 30, config: SNAP });
      const rel = from + sw.dist / H;
      p = rel + (sw.to - rel) * sp;
      const after = local - sw.drag;
      finger = { y: startY - sw.dist - Math.sign(sw.dist) * after * 18, down: Math.max(0, 1 - after / 4), show: Math.max(0, 1 - after / 7), tap: -1 };
    }
  }
  return { p, finger };
};

// Финальный «отъезд»: дни стопкой, лента бежит назад к началу недели.
const PULL_AT = 384;
const pull = (f: number) => easeInOut(interpolate(f, [PULL_AT, PULL_AT + 30], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }));
const runBack = (f: number) => easeInOut(interpolate(f, [PULL_AT + 20, PULL_AT + 80], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }));
const position = (f: number) => {
  const { p } = pager(f);
  return p + (2 - p) * runBack(f);
};

export const Swipe = ({ lang }: { lang: Lang }) => {
  const f = useCurrentFrame();
  const days = DAYS[lang];
  const p = position(f);
  const { finger } = pager(f);
  const lag = lagSeries("swipe", SWIPE_FRAMES, (q) => -position(q) * H)[f] ?? 0;
  const k = pull(f);
  // В отъезде страницы расходятся с зазором и скругляются — видно, что это стопка дней.
  const gap = 90 * k;
  const zoom = 1 - 0.5 * k;
  const first = Math.max(0, Math.floor(p) - 3);
  const last = Math.min(days.length - 1, Math.floor(p) + 3);
  const appear = (i: number) => spring({ frame: f - 2 - i * 4, fps: 30, config: { damping: 14, stiffness: 120, mass: 0.9 } });

  return (
    <AbsoluteFill style={{ background: s.surfaceContainerHigh }}>
      <style>{fontFace}</style>
      <AbsoluteFill style={{ transform: `scale(${zoom})` }}>
        {Array.from({ length: last - first + 1 }, (_, n) => first + n).map((d) => {
          const top = (d - p) * (H + gap);
          if (top > H * 3 || top < -H * 3) return null;
          const { ys, hs } = layout(days[d]);
          return (
            <div key={d} style={{ position: "absolute", left: 0, top, width: 1080, height: H, background: s.background, borderRadius: 110 * k, overflow: "hidden" }}>
              <Header s={s} text={dayTitle(lang, d)} />
              {days[d].map((e, i) => {
                const off = lagOffset(lag, ys[i] + hs[i] / 2);
                const a = d === 0 ? appear(i) : 1;
                // «Сейчас» — на сегодняшнем дне, дизайн-ревью: поднимается и меняет цвет.
                const now = d === 0 && i === 2 ? spring({ frame: f - 26, fps: 30, config: { damping: 13, stiffness: 110, mass: 0.9 } }) : 0;
                return (
                  <Card
                    key={i}
                    e={e}
                    i={i + d}
                    s={s}
                    top={ys[i]}
                    now={now}
                    lang={lang}
                    f={f}
                    style={{ transform: `translateY(${off + (1 - a) * 700 - now * 8}px) scale(${1 + now * 0.02})`, opacity: a }}
                  />
                );
              })}
            </div>
          );
        })}
      </AbsoluteFill>
      {finger.show > 0 && <Touch x={760} y={finger.y} down={finger.down} show={finger.show} tap={finger.tap} />}
      {CAPTIONS[lang].map(([text, from, to]) => (
        <Caption key={text} text={text} from={from} to={to} s={s} y={1630} size={60} />
      ))}
      <Sequence from={END_AT}>
        <End s={s} tagline={TAGLINE[lang]} />
      </Sequence>
    </AbsoluteFill>
  );
};
