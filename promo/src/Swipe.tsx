import { AbsoluteFill, Sequence, interpolate, spring, useCurrentFrame } from "remotion";
import { DP } from "./DayScene";
import { End, TAGLINE } from "./End";
import { easeInOut } from "./parts";
import { scheme } from "./schemes";
import { fontFace } from "./theme";
import { Caption, Card, E, Header, Lang, Touch, dayTitle, lagOffset, lagSeries, layout } from "./kit";

/**
 * Вариант «Листай» — про удовольствие листать. Палец тянет день и возвращает — карточки
 * тянутся за ним, как на резинках; отпустил — завтра; играет туда-сюда — карточки колышутся,
 * как желе; серия флингов — неделя за пару движений. Отставание — SpringyScroll из приложения,
 * пейджер — честная пружина со скоростью пальца. В конце — отъезд: дни стопкой.
 */
export const SWIPE_FRAMES = 600;
const END_AT = 486;
const H = 1920;

const PINK = scheme(0xffff6f91, false, "content");

// Сочная розовая: карточки #FF6F91, «сейчас» — оранжевое.
const s = PINK;

export const DAYS: Record<Lang, E[][]> = {
  en: [
    [["☕ Breakfast", "8:00", 30], ["💻 Deep work", "9:00", 180], ["🎨 Design review", "14:00", 60], ["📞 Call Mom", "15:30", 15], ["🏋️ Gym", "18:00", 90]],
    [["🥐 Croissant run", "8:00", 20], ["🧪 Lab meeting", "10:00", 120], ["🥪 Lunch Break", "13:00", 60], ["🏊 Swimming", "17:00", 45]],
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
    [["🥐 За круассаном", "8:00", 20], ["🧪 Встреча в лабе", "10:00", 120], ["🥪 Обед", "13:00", 60], ["🏊 Бассейн", "17:00", 45]],
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
    ["Stretches\nlike it's alive", 22, 78],
    ["Swipe up —\nit's tomorrow", 86, 142],
    ["You'll swipe\njust for fun", 150, 214],
    ["A week\nin a few flicks", 226, 300],
    ["Every day\nhas its own page", 368, 476],
  ],
  ru: [
    ["Тянется,\nкак живое", 22, 78],
    ["Свайп вверх —\nи завтра", 86, 142],
    ["Хочется листать\nпросто так", 150, 214],
    ["Неделя —\nза пару движений", 226, 300],
    ["У каждого дня\nсвоя страница", 368, 476],
  ],
};

/**
 * Жест: [кадр, y пальца] — пока палец прижат, страница едет за ним; [кадр, null] — отпустил.
 * Сценарий: медленно тянем и возвращаем; отпускаем — завтра; играем пальцем туда-сюда; серия
 * флингов; длинный тяжёлый свайп.
 */
type Key = [number, number | null];
const fling = (at: number): Key[] => [[at, 1420], [at + 5, 1040], [at + 6, null]];
const GESTURE: Key[] = [
  [18, 1420], [46, 920], [62, 1300], [74, 1180], [82, 640], [83, null],
  // Игра: палец мелко дёргает вверх-вниз — карточки колышутся, страница возвращается.
  ...Array.from({ length: 13 }, (_, k) => [150 + k * 4, 1260 + (k % 2 ? -150 : 150) * (1 - k / 16)] as Key),
  [204, 1260], [205, null],
  ...fling(226), ...fling(240), ...fling(253), ...fling(264), ...fling(274),
  // Тяжёлый финальный свайп: медленно тянем далеко и отпускаем.
  [298, 1500], [324, 560], [325, null],
];

/** Палец на кадре [f]: y или null (не касается), и когда коснулся. */
const finger = (f: number) => {
  let i = GESTURE.findIndex((k) => k[0] > f);
  if (i < 0) i = GESTURE.length;
  const prev = GESTURE[i - 1];
  const next = GESTURE[i];
  if (!prev || prev[1] === null) return { y: null as number | null, prevUp: prev?.[0] ?? -100, nextDown: next?.[0] ?? 1e9 };
  // Равномерно, без замедления к концу: иначе на отпускании скорость ~0 и флинг не срабатывает.
  const y = next && next[1] !== null ? prev[1] + ((next[1] - prev[1]) * (f - prev[0])) / (next[0] - prev[0]) : prev[1];
  return { y, prevUp: -100, nextDown: -100 };
};

/**
 * Пейджер — честная физика: пока палец держит, страница идёт за ним; отпустил — пружина
 * ведёт к ближайшей странице по направлению движения, стартуя со скоростью пальца. Резкий
 * флинг уезжает дальше, медленный — мягче.
 */
const pagerCache: number[] = [];
const simulate = () => {
  if (pagerCache.length) return pagerCache;
  const SUB = 8;
  const K = 230; // жёсткость, 1/с²
  const C = 2 * 0.72 * Math.sqrt(K); // лёгкий перелёт
  let p = 0;
  let v = 0;
  let target = 0;
  let grab: { y: number; p: number } | null = null;
  for (let f = 0; f < SWIPE_FRAMES; f++) {
    const { y } = finger(f);
    if (y !== null) {
      if (!grab) grab = { y, p };
      const np = grab.p + (grab.y - y) / H;
      v = (np - p) * 30;
      p = np;
    } else {
      if (grab) {
        // Отпустили: куда дальше — по направлению скорости и пройденному пути.
        const moved = p - Math.round(grab.p);
        const dir = Math.abs(v) > 0.6 ? Math.sign(v) : Math.abs(moved) > 0.25 ? Math.sign(moved) : 0;
        target = Math.round(grab.p) + dir;
        grab = null;
      }
      for (let k = 0; k < SUB; k++) {
        const dt = 1 / 30 / SUB;
        v += (-K * (p - target) - C * v) * dt;
        p += v * dt;
      }
    }
    pagerCache.push(p);
  }
  return pagerCache;
};

// Финальный отъезд: дни стопкой, лента бежит назад с отставанием карточек.
const PULL_AT = 352;
const pull = (f: number) => easeInOut(interpolate(f, [PULL_AT, PULL_AT + 30], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }));
const runBack = (f: number) => easeInOut(interpolate(f, [PULL_AT + 24, PULL_AT + 96], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }));
const position = (f: number) => {
  const p = simulate()[Math.min(f, SWIPE_FRAMES - 1)];
  return p + (1 - p) * runBack(f);
};

export const Swipe = ({ lang }: { lang: Lang }) => {
  const f = useCurrentFrame();
  const days = DAYS[lang];
  const p = position(f);
  const fg = finger(f);
  // Отставание — как SpringyScroll, но связь сильнее: в ролике «желе» должно читаться.
  const lag = lagSeries("swipe", SWIPE_FRAMES, (q) => -position(q) * H)[f] ?? 0;
  const k = pull(f);
  const gap = 90 * k;
  const zoom = 1 - 0.5 * k;
  const first = Math.max(0, Math.floor(p) - 3);
  const last = Math.min(days.length - 1, Math.floor(p) + 3);
  const appear = (i: number) => spring({ frame: f - 2 - i * 4, fps: 30, config: { damping: 14, stiffness: 120, mass: 0.9 } });
  const nowP = spring({ frame: f - 8, fps: 30, config: { damping: 11, stiffness: 120, mass: 0.9 } });
  // Палец: появляется к касанию, уходит после отпускания.
  const touchY = fg.y;
  const lastTouch = (() => {
    let y = 1200;
    for (const [at, yy] of GESTURE) if (at <= f && yy !== null) y = yy;
    return y;
  })();
  const sinceUp = f - fg.prevUp;
  const untilDown = fg.nextDown - f;
  const show = touchY !== null ? 1 : Math.max(0, 1 - sinceUp / 7, 1 - untilDown / 6);
  const fy = touchY ?? (sinceUp < 8 ? lastTouch - sinceUp * 14 : (GESTURE.find((g) => g[0] === fg.nextDown)?.[1] ?? 1300) + untilDown * 20);

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
                const off = lagOffset(lag, ys[i] + hs[i] / 2, H, 0.6, 170 * DP);
                const a = d === 0 ? appear(i) : 1;
                // «Сейчас» — на сегодняшнем дне, завтрак.
                const now = d === 0 && i === 0 ? nowP : 0;
                return <Card key={i} e={e} i={i + d} s={s} top={ys[i]} now={now} lang={lang} f={f} style={{ transform: `translateY(${off + (1 - a) * 700}px)`, opacity: a }} />;
              })}
            </div>
          );
        })}
      </AbsoluteFill>
      {show > 0 && f < PULL_AT && <Touch x={760} y={fy} down={touchY !== null ? 1 : 0} show={show} tap={touchY !== null && f - (GESTURE.filter((g) => g[0] <= f && g[1] !== null).slice(-1)[0]?.[0] ?? f) < 18 ? 0 : -1} />}
      {CAPTIONS[lang].map(([text, from, to]) => (
        <Caption key={text} text={text} from={from} to={to} s={s} y={1620} size={62} />
      ))}
      <Sequence from={END_AT}>
        <End s={s} tagline={TAGLINE[lang]} />
      </Sequence>
    </AbsoluteFill>
  );
};
