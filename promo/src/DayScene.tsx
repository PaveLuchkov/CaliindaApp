import { AbsoluteFill, interpolate, spring, useCurrentFrame, useVideoConfig } from "remotion";
import shapes from "./shapes.json";
import stars from "./stars.json";
import { Scheme, SCHEMES, mixScheme } from "./schemes";
import { Wave, easeInOut } from "./parts";
import { flex } from "./theme";

// Масштаб как у телефона: экран Pixel — 411 dp на 1080 px. С 360 dp карточки выходили крупнее,
// чем в приложении.
export const DP = 1080 / 411;
export const CARD_W = 1080 - 2 * 16 * DP;
export const GAP = 3 * DP * 2;
export const MIN_H = 65 * DP;
export const MAX_H = 200 * DP;
export const MICRO_H = 30 * DP;

/** Высота карточки — та же сигмоида по длительности, что в EventUiModelMapper. */
export const heightFor = (minutes: number) => {
  if (minutes <= 25) return MICRO_H;
  const x = (minutes - 180) / 100;
  return MIN_H + (MAX_H - MIN_H) / (1 + Math.exp(-4.5 * x));
};

/** Размер звезды — calculateShapeContainerSize. */
export const starFor = (minutes: number) => {
  const x = (minutes - 180) / 100;
  return (120 + 240 / (1 + Math.exp(-4.5 * x))) * DP;
};

export type Ev = { star: keyof typeof stars; rot: number; off: number };

// Звёзды привязаны к месту в ленте — как форма к id события, они не прыгают при смене дня.
const SLOTS: Ev[] = [
  { star: "star5", rot: -20, off: 0.2 },
  { star: "star3", rot: 0, off: 0 },
  { star: "star4", rot: 25, off: 0.3 },
  { star: "star6", rot: -35, off: 0.2 },
  { star: "star3", rot: 0, off: 0 },
  { star: "star7", rot: 10, off: 0.4 },
  { star: "star3", rot: -40, off: 0.1 },
];
const DEEP = 2;
const NOW = 3;

/**
 * Дни разных людей: при смене темы меняется всё — длительности (и высоты на пружинах), ленты
 * проектов, события на весь день. Время — от длительности: начало у слота своё.
 */
type DayPlan = {
  minutes: number[];
  starts: string[];
  ribbons: { title: string; day: number; days: number }[];
  allDay: string[];
  titles: string[];
};

export const fmt = (start: string, minutes: number) => {
  const [h, m] = start.split(":").map(Number);
  const end = h * 60 + m + minutes;
  return `${start} – ${Math.floor(end / 60)}:${String(end % 60).padStart(2, "0")}`;
};

const PLANS: Record<"en" | "ru", DayPlan[]> = {
  en: [
    {
      minutes: [30, 20, 180, 60, 15, 90, 120],
      starts: ["8:00", "8:40", "9:00", "14:00", "15:30", "18:00", "20:00"],
      ribbons: [{ title: "✈️ Trip to Lisbon", day: 3, days: 7 }, { title: "📖 Reading challenge", day: 12, days: 30 }],
      allDay: [],
      titles: ["☕ Breakfast", "🚇 Commute", "💻 Deep work", "🎨 Design review", "📞 Call Mom", "🏋️ Gym", "🍝 Dinner with friends"],
    },
    {
      minutes: [45, 15, 60, 120, 20, 30, 150],
      starts: ["7:30", "8:30", "10:00", "13:00", "15:30", "17:00", "19:00"],
      ribbons: [{ title: "🏠 Moving house", day: 2, days: 5 }],
      allDay: ["🎂 Mom's birthday"],
      titles: ["🥐 Croissant run", "🐕 Walk the dog", "📚 Study session", "🧪 Lab meeting", "💊 Vitamins", "🏊 Swimming", "🎬 Movie night"],
    },
    {
      minutes: [60, 20, 30, 240, 15, 60, 90],
      starts: ["7:00", "8:10", "9:00", "12:00", "16:30", "18:00", "20:30"],
      ribbons: [],
      allDay: ["🌴 Day off", "🎉 City festival"],
      titles: ["🧘 Morning yoga", "🚲 Ride to the lake", "🧺 Picnic prep", "🏞️ Hike", "☎️ Call Grandma", "🛒 Groceries", "🎮 Game night"],
    },
  ],
  ru: [
    {
      minutes: [30, 20, 180, 60, 15, 90, 120],
      starts: ["8:00", "8:40", "9:00", "14:00", "15:30", "18:00", "20:00"],
      ribbons: [{ title: "✈️ Поездка в Лиссабон", day: 3, days: 7 }, { title: "📖 Книжный марафон", day: 12, days: 30 }],
      allDay: [],
      titles: ["☕ Завтрак", "🚇 Дорога", "💻 Глубокая работа", "🎨 Дизайн-ревью", "📞 Позвонить маме", "🏋️ Зал", "🍝 Ужин с друзьями"],
    },
    {
      minutes: [45, 15, 60, 120, 20, 30, 150],
      starts: ["7:30", "8:30", "10:00", "13:00", "15:30", "17:00", "19:00"],
      ribbons: [{ title: "🏠 Переезд", day: 2, days: 5 }],
      allDay: ["🎂 День рождения мамы"],
      titles: ["🥐 За круассаном", "🐕 Выгул собаки", "📚 Учёба", "🧪 Встреча в лабе", "💊 Витамины", "🏊 Бассейн", "🎬 Кино вечером"],
    },
    {
      minutes: [60, 20, 30, 240, 15, 60, 90],
      starts: ["7:00", "8:10", "9:00", "12:00", "16:30", "18:00", "20:30"],
      ribbons: [],
      allDay: ["🌴 Выходной", "🎉 День города"],
      titles: ["🧘 Утренняя йога", "🚲 На озеро", "🧺 Сборы на пикник", "🏞️ Поход", "☎️ Позвонить бабушке", "🛒 Продукты", "🎮 Вечер игр"],
    },
  ],
};

export const DAY_OF = { en: (d: number, n: number) => `day ${d} of ${n}`, ru: (d: number, n: number) => `день ${d} из ${n}` };
export const LEFT = { en: "25 min left", ru: "ещё 25 мин" };
const HEADER = { en: "Thu, 8 October", ru: "Чт, 8 октября" };
const MAX_RIBBONS = 2;
const MAX_ALLDAY = 2;
export const ALLDAY_H = 42 * DP;

export const DAY_FRAMES = 420;

export const HEADER_H = 48 * DP;
export const RIBBON_H = 40 * DP;
export const TOP = 70 * DP;

export const DayScene = ({ lang }: { lang: "en" | "ru" }) => {
  const f = useCurrentFrame();
  const { fps } = useVideoConfig();
  const sp = (from: number, cfg = { damping: 13, stiffness: 110, mass: 0.9 }) =>
    spring({ frame: f - from, fps, config: cfg });

  // --- Тема: с кадра 200 перебираем «обои», светлые и тёмные, монтажными склейками.
  const SWITCH_FROM = 150;
  const STEP = 18;
  const LAST = SCHEMES.length - 1;
  // После последней темы счётчик стоит — иначе названия дёргались бы каждые 20 кадров.
  const k = Math.min(LAST, Math.max(0, (f - SWITCH_FROM) / STEP));
  const idx = Math.min(LAST, Math.floor(k));
  const next = Math.min(LAST, idx + 1);
  const local = k - Math.floor(k);
  // Переход — последние 6 кадров шага, остальное время тема стоит.
  const blend = idx >= LAST ? 0 : Math.max(0, (local - 0.7) / 0.3);
  const s: Scheme = mixScheme(SCHEMES[idx], SCHEMES[next], easeInOut(Math.min(1, blend)));

  // Моменты смены дня — середины переходов тем. День меняется вместе с цветами.
  const switches = Array.from({ length: LAST }, (_, j) => SWITCH_FROM + j * STEP + 0.85 * STEP);
  const plans = PLANS[lang];
  const planAt = (j: number) => plans[j % plans.length];
  const shown = switches.filter((t) => f >= t).length;
  const plan = planAt(shown);

  /** Величина, которая на каждой смене дня пружинит к новому значению — и толкает соседей. */
  const springy = (value: (p: DayPlan) => number, base?: number) => {
    let v = base ?? value(planAt(0));
    switches.forEach((t, j) => {
      const d = value(planAt(j + 1)) - value(planAt(j));
      if (d !== 0) v += d * sp(t, { damping: 13, stiffness: 120, mass: 0.9 });
    });
    return v;
  };

  // --- Раскладка: высоты на пружинах, позиции считаем сами, чтобы камера знала, куда смотреть.
  const appear = SLOTS.map((_, i) => sp(i * 4, { damping: 15, stiffness: 120, mass: 0.9 }));
  const deepGrow = sp(48, { damping: 13, stiffness: 100, mass: 1 });
  const nowOn = sp(96, { damping: 13, stiffness: 110, mass: 0.9 });
  const heights = SLOTS.map((_, i) => {
    const hOf = (p: DayPlan) => heightFor(p.minutes[i]);
    // Длинное событие сначала обычное, потом на пружине вырастает до своей высоты.
    const base = i === DEEP ? MIN_H + (hOf(planAt(0)) - MIN_H) * deepGrow : undefined;
    // Текущее чуть раскрывается — заметно, но в пределах приложения (не выше 200 dp + ряд).
    return springy(hOf, base) + (i === NOW ? 40 * DP * nowOn : 0);
  });
  // Ленты проектов и события на весь день появляются и исчезают, раздвигая ленту.
  const ribbonOpen = Array.from({ length: MAX_RIBBONS }, (_, r) => springy((p) => (p.ribbons.length > r ? 1 : 0)));
  const allDayOpen = Array.from({ length: MAX_ALLDAY }, (_, r) => springy((p) => (p.allDay.length > r ? 1 : 0)));
  // Текст слота держим от последнего дня, где он был, — пока лента «закрывается», он не пропадает.
  const lastWith = <T,>(pick: (p: DayPlan) => T | undefined): T | undefined => {
    for (let n = shown; n >= 0; n--) {
      const v = pick(planAt(n));
      if (v !== undefined) return v;
    }
    return undefined;
  };

  // Лента «Поездки» растягивается в карточку проекта и на пружине раздвигает всё, что ниже.
  const expand = sp(300, { damping: 16, stiffness: 90, mass: 1 });
  const push = (projectHeight(PROJECTS[lang][0].days) - RIBBON_H) * expand;
  const ribbonsTop = TOP + HEADER_H + 12 * DP;
  let y = ribbonsTop;
  const ribbonYs = ribbonOpen.map((o, r) => {
    const top = y;
    y += (RIBBON_H + 4 * DP) * Math.max(0, o) + (r === 0 ? push : 0);
    return top;
  });
  y += 4 * DP;
  const allDayYs = allDayOpen.map((o) => {
    const top = y;
    y += (ALLDAY_H + GAP) * Math.max(0, o);
    return top;
  });
  const listTop = y + 4 * DP;
  const ys: number[] = [];
  y = listTop;
  heights.forEach((h) => {
    ys.push(y);
    y += h + GAP;
  });
  const total = y + 40 * DP;

  // --- Камера: [кадр, центр x, центр y, зум]. Наезд на длинное, переход к текущему, общий план,
  // проекты. При наезде центр сдвинут влево: в кадре остаётся текст, а не край со звездой.
  const centerOf = (i: number) => ys[i] + heights[i] / 2;
  // В конце камера возвращается к экрану 1:1 — на нём лента проекта превращается в карточку.
  const screen = 960;
  const keys: [number, number, number, number][] = [
    [0, 540, total / 2, 0.62],
    [26, 540, total / 2, 0.64],
    [52, 440, centerOf(DEEP), 1.2],
    [80, 440, centerOf(DEEP) + 30, 1.18],
    [100, 440, centerOf(NOW), 1.28],
    [140, 440, centerOf(NOW), 1.32],
    [164, 540, total / 2, 0.68],
    [262, 540, total / 2, 0.7],
    [296, 540, screen, 1],
    [420, 540, screen, 1],
  ];
  let ki = 0;
  while (ki < keys.length - 2 && f > keys[ki + 1][0]) ki++;
  const [f0, x0, y0, z0] = keys[ki];
  const [f1, x1, y1, z1] = keys[ki + 1];
  // Баланс резкости и плавности: быстрый, но не рваный разгон и мягкая посадка.
  const p = easeInOut(Math.max(0, Math.min(1, (f - f0) / (f1 - f0))));
  const camX = x0 + (x1 - x0) * p;
  const camY = y0 + (y1 - y0) * p;
  let zoom = z0 + (z1 - z0) * p;
  // На каждой смене темы — лёгкий «удар» камеры, держит ритм.
  if (f >= SWITCH_FROM && idx < LAST) zoom *= 1 + 0.03 * Math.exp(-local * 5);

  // --- Проекты: лента «Поездки» растягивается в карточку проекта, потом свайп на страницу
  // проектов — как горизонтальный пейджер в приложении (проекты слева от дней).
  const slide = easeInOut(interpolate(f, [330, 362], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }));
  const project = PROJECTS[lang];
  const projHeights = project.map((pr, i) => (i === 0 ? 1 : sp(352 + i * 7, { damping: 13, stiffness: 110, mass: 0.9 })) * projectHeight(pr.days));
  const projYs: number[] = [];
  let py = ribbonsTop;
  projHeights.forEach((h) => {
    projYs.push(py);
    py += h + GAP;
  });
  const fade = interpolate(f, [408, 420], [1, 0], { extrapolateLeft: "clamp" });

  return (
    <AbsoluteFill style={{ background: s.background, opacity: fade }}>
      <div
        style={{
          position: "absolute",
          left: 0,
          top: 0,
          width: 1080,
          height: total,
          transformOrigin: "0 0",
          transform: `translate(540px, 960px) scale(${zoom}) translate(${-camX}px, ${-camY}px)`,
        }}
      >
        {/* Фоновые фигуры, как BackgroundShapes в приложении. */}
        <BigShape d={shapes.flower} size={1100} x={-420} y={ys[3] - 200} color={s.surfaceContainerHigh} rot={f * 0.15} />
        <BigShape d={shapes.clover4} size={900} x={560} y={ys[5]} color={s.surfaceContainerHigh} rot={-f * 0.12} />

        <div style={{ position: "absolute", inset: 0, transform: `translateX(${slide * 1080}px)`, opacity: 1 - slide * 0.3 }}>
        {/* Шапка: кнопки и дата в «таблетке». */}
        <div style={{ position: "absolute", top: TOP, left: 12 * DP, right: 12 * DP, height: HEADER_H, display: "flex", alignItems: "center", gap: 10 * DP }}>
          <Circle color={s.primary} />
          <div style={{ flex: 1, height: 40 * DP, borderRadius: 20 * DP, background: s.inverseSurface, color: s.inverseOnSurface, display: "flex", alignItems: "center", justifyContent: "center", ...flex(700), fontSize: 16 * DP }}>
            {HEADER[lang]}
          </div>
          <Circle color={s.primary} />
        </div>

        {/* Ленты проектов: «день 3 из 7» с волнистым прогрессом. */}
        {ribbonOpen.map((o, r) => {
          const rib = lastWith((p) => p.ribbons[r]);
          if (!rib || o <= 0.01) return null;
          // В конце камера смотрит на поездку — счётчик дней бежит.
          const day = rib.day;
          // Первая лента в конце становится карточкой проекта — рисуется отдельно, поверх.
          if (r === 0 && f >= 296) return null;
          return (
            <div
              key={`r${r}`}
              style={{
                position: "absolute",
                left: 16 * DP,
                width: CARD_W,
                top: ribbonYs[r],
                height: RIBBON_H,
                borderRadius: RIBBON_H / 2,
                background: s.secondaryContainer,
                color: s.onSecondaryContainer,
                display: "flex",
                alignItems: "center",
                padding: `0 ${16 * DP}px`,
                gap: 12 * DP,
                opacity: appear[0] * Math.min(1, o),
                transform: `scale(${0.85 + 0.15 * Math.min(1, o)})`,
              }}
            >
              <div style={{ flex: 1, ...flex(600), fontSize: 14 * DP, whiteSpace: "nowrap", overflow: "hidden" }}>{rib.title}</div>
              <Wave width={56 * DP} height={10 * DP} progress={day / rib.days} phase={0} color={s.secondary} />
              <div style={{ ...flex(500), fontSize: 12 * DP }}>{DAY_OF[lang](day, rib.days)}</div>
            </div>
          );
        })}

        {/* События на весь день — во всю ширину, третичным цветом, название по центру. */}
        {allDayOpen.map((o, r) => {
          const title = lastWith((p) => p.allDay[r]);
          if (!title || o <= 0.01) return null;
          return (
            <div
              key={`a${r}`}
              style={{
                position: "absolute",
                left: 16 * DP,
                width: CARD_W,
                top: allDayYs[r],
                height: ALLDAY_H,
                borderRadius: 20 * DP,
                background: s.tertiaryContainer,
                color: s.onTertiaryContainer,
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                ...flex(650),
                fontSize: 16 * DP,
                opacity: Math.min(1, o),
                transform: `scale(${0.85 + 0.15 * Math.min(1, o)})`,
              }}
            >
              {title}
            </div>
          );
        })}

        {/* Карточки дня. */}
        {SLOTS.map((e, i) => {
          const isNow = i === NOW;
          const n = isNow ? nowOn : 0;
          const bg = isNow ? mixHex(s.primaryContainer, s.tertiaryContainer, n) : s.primaryContainer;
          const fg = isNow ? mixHex(s.onPrimaryContainer, s.onTertiaryContainer, n) : s.onPrimaryContainer;
          const minutes = plan.minutes[i];
          const time = fmt(plan.starts[i], minutes);
          const micro = minutes <= 25;
          const shadow = s.dark ? "255,255,255" : "0,0,0";
          return (
            <div
              key={i}
              style={{
                position: "absolute",
                left: 16 * DP,
                width: CARD_W,
                top: ys[i],
                height: heights[i],
                borderRadius: 20 * DP,
                background: bg,
                overflow: "hidden",
                opacity: appear[i],
                // Текущее «левитирует»: поднимается, растёт тень.
                transform: `translateY(${(1 - appear[i]) * 600 - n * 8 * DP}px) scale(${1 + n * 0.02})`,
                boxShadow: `0 ${n * 10 * DP}px ${n * 24 * DP}px rgba(${shadow},${0.28 * n})`,
              }}
            >
              {!micro && <Star e={e} minutes={minutes} bg={bg} />}
              <div
                style={{
                  position: "absolute",
                  left: 16 * DP,
                  right: 16 * DP,
                  top: micro ? 0 : 10 * DP,
                  bottom: micro ? 0 : undefined,
                  display: "flex",
                  flexDirection: micro ? "row" : "column",
                  alignItems: micro ? "center" : "flex-start",
                  gap: micro ? 8 * DP : 2 * DP,
                  color: fg,
                }}
              >
                <div
                  style={{
                    // Шрифт текущего — узкий и наклонный, как CaliindaFonts.CardCurrent.
                    ...flex(600 + 100 * n, 100 - 35 * n, `'slnt' ${-5 * n}, 'GRAD' ${70 * n}`),
                    fontSize: (micro ? 16 : 24) * DP,
                    whiteSpace: "nowrap",
                  }}
                >
                  {plan.titles[i]}
                </div>
                <div style={{ ...flex(450), fontSize: (micro ? 13 : 14) * DP, display: "flex", alignItems: "center", gap: 8 * DP, opacity: 0.9 }}>
                  {isNow && n > 0.05 && (
                    <div style={{ opacity: n }}>
                      <Wave width={52 * DP} height={9 * DP} progress={0.3 + Math.max(0, f - 70) / 400} phase={f / 4} color={fg} />
                    </div>
                  )}
                  {isNow && n > 0.5 ? `${LEFT[lang]} · ${time}` : time}
                </div>
              </div>
            </div>
          );
        })}
        </div>

        {/* Страница проектов заезжает слева, как свайп пейджера. */}
        {slide > 0 && (
          <div style={{ position: "absolute", inset: 0, transform: `translateX(${(slide - 1) * 1080}px)` }}>
            <div style={{ position: "absolute", top: TOP, left: 12 * DP, right: 12 * DP, height: HEADER_H, display: "flex", alignItems: "center", gap: 10 * DP }}>
              <Circle color={s.primary} />
              <div style={{ flex: 1, height: 40 * DP, borderRadius: 20 * DP, background: s.inverseSurface, color: s.inverseOnSurface, display: "flex", alignItems: "center", justifyContent: "center", ...flex(700), fontSize: 16 * DP }}>
                {MONTH[lang]}
              </div>
              <Circle color={s.primary} />
            </div>
            {project.slice(1).map((pr, j) => {
              const i = j + 1;
              return <ProjectCard key={i} pr={pr} s={s} top={projYs[i]} height={projHeights[i]} grow={Math.min(1, projHeights[i] / projectHeight(pr.days))} lang={lang} />;
            })}
          </div>
        )}

        {/* «Поездка»: лента растягивается в карточку проекта и остаётся на месте при свайпе. */}
        {f >= 296 && (
          <ProjectCard
            pr={project[0]}
            s={s}
            top={ribbonYs[0]}
            height={RIBBON_H + (projectHeight(project[0].days) - RIBBON_H) * expand}
            grow={expand}
            ribbon={1 - expand}
            lang={lang}
          />
        )}
      </div>
    </AbsoluteFill>
  );
};

/** Высота карточки проекта — projectHeightFraction: сигмоида по дням, от 1 до 31. */
export const projectHeight = (days: number) => {
  const sig = (d: number) => 1 / (1 + Math.exp(-(d - 10) / 4));
  const t = (sig(days) - sig(1)) / (sig(31) - sig(1));
  return MIN_H + (MAX_H - MIN_H) * Math.max(0, Math.min(1, t));
};

export type Project = { title: string; days: number; dates: string; left?: string; shape: keyof typeof shapes; ongoing: boolean; day?: number };

const PROJECTS: Record<"en" | "ru", Project[]> = {
  en: [
    { title: "✈️ Trip to Lisbon", days: 7, day: 3, dates: "6 – 12 October", left: "4 days left", shape: "clover4", ongoing: true },
    { title: "📖 Reading challenge", days: 30, dates: "1 – 30 October", left: "22 days left", shape: "flower", ongoing: true },
    { title: "🏠 Moving house", days: 5, dates: "14 – 18 October", shape: "gem", ongoing: false },
    { title: "🎤 Design conference", days: 2, dates: "24 – 25 October", shape: "puffy", ongoing: false },
  ],
  ru: [
    { title: "✈️ Поездка в Лиссабон", days: 7, day: 3, dates: "6 – 12 октября", left: "осталось 4 дня", shape: "clover4", ongoing: true },
    { title: "📖 Книжный марафон", days: 30, dates: "1 – 30 октября", left: "осталось 22 дня", shape: "flower", ongoing: true },
    { title: "🏠 Переезд", days: 5, dates: "14 – 18 октября", shape: "gem", ongoing: false },
    { title: "🎤 Конференция", days: 2, dates: "24 – 25 октября", shape: "puffy", ongoing: false },
  ],
};
export const MONTH = { en: "October", ru: "Октябрь" };

/**
 * Карточка проекта как на странице проектов: идущий — третичным цветом, будущий — основным,
 * фигура проекта выглядывает справа. [ribbon] > 0 — ещё наполовину лента дня (для перехода).
 */
export const ProjectCard = ({
  pr,
  s,
  top,
  height,
  grow,
  ribbon = 0,
  lang,
}: {
  pr: Project;
  s: Scheme;
  top: number;
  height: number;
  grow: number;
  ribbon?: number;
  lang: "en" | "ru";
}) => {
  const cardBg = pr.ongoing ? s.tertiaryContainer : s.primaryContainer;
  const cardFg = pr.ongoing ? s.onTertiaryContainer : s.onPrimaryContainer;
  const bg = mixHex(cardBg, s.secondaryContainer, ribbon);
  const fg = mixHex(cardFg, s.onSecondaryContainer, ribbon);
  const radius = RIBBON_H / 2 + (20 * DP - RIBBON_H / 2) * (1 - ribbon);
  const shapeSize = Math.max(height * 1.15, 110 * DP);
  const id = `pclip-${pr.shape}`;
  return (
    <div
      style={{
        position: "absolute",
        left: 16 * DP,
        width: CARD_W,
        top,
        height,
        borderRadius: radius,
        background: bg,
        color: fg,
        overflow: "hidden",
        opacity: Math.min(1, grow * 2 + ribbon),
      }}
    >
      <svg
        width={shapeSize}
        height={shapeSize}
        viewBox="0 0 100 100"
        style={{ position: "absolute", right: -shapeSize * 0.18, top: -shapeSize * 0.12, opacity: 1 - ribbon, overflow: "visible" }}
      >
        <defs>
          <clipPath id={id}>
            <path d={shapes[pr.shape]} transform="rotate(-15 50 50)" />
          </clipPath>
        </defs>
        <g clipPath={`url(#${id})`}>
          <path d={shapes[pr.shape]} fill="rgba(0,0,0,0.22)" transform="rotate(-15 50 50)" />
          <path d={shapes[pr.shape]} fill={bg} transform="translate(-2.6 -3.6) rotate(-15 50 50)" />
        </g>
      </svg>
      {/* Лента: название, волна, «день 3 из 7». */}
      {ribbon > 0.01 && (
        <div style={{ position: "absolute", inset: 0, display: "flex", alignItems: "center", padding: `0 ${16 * DP}px`, gap: 12 * DP, opacity: ribbon }}>
          <div style={{ flex: 1, ...flex(600), fontSize: 14 * DP, whiteSpace: "nowrap" }}>{pr.title}</div>
          <Wave width={56 * DP} height={10 * DP} progress={(pr.day ?? 1) / pr.days} phase={0} color={s.secondary} />
          <div style={{ ...flex(500), fontSize: 12 * DP }}>{DAY_OF[lang](pr.day ?? 1, pr.days)}</div>
        </div>
      )}
      {/* Карточка: крупное название, даты и сколько осталось. */}
      <div style={{ position: "absolute", left: 16 * DP, top: 8 * DP, opacity: 1 - ribbon }}>
        <div style={{ ...flex(600), fontSize: 24 * DP, whiteSpace: "nowrap" }}>{pr.title}</div>
        <div style={{ ...flex(450), fontSize: 11 * DP, marginTop: 2 * DP, display: "flex", gap: 10 * DP }}>
          <span>{pr.dates}</span>
          {pr.left && <span>{pr.left}</span>}
        </div>
      </div>
    </div>
  );
};

export const Circle = ({ color }: { color: string }) => (
  <div style={{ width: 40 * DP, height: 40 * DP, borderRadius: 20 * DP, background: color }} />
);

export const BigShape = ({ d, size, x, y, color, rot }: { d: string; size: number; x: number; y: number; color: string; rot: number }) => (
  <svg width={size} height={size} viewBox="0 0 100 100" style={{ position: "absolute", left: x, top: y }}>
    <path d={d} fill={color} transform={`rotate(${rot} 50 50)`} />
  </svg>
);

/** Границы пути — звёзды из RoundedPolygon.star меньше своего квадрата, растягиваем по ним. */
const bbox = (d: string) => {
  const n = d.match(/-?\d+(\.\d+)?/g)!.map(Number);
  const xs = n.filter((_, i) => i % 2 === 0);
  const ys = n.filter((_, i) => i % 2 === 1);
  const minX = Math.min(...xs);
  const minY = Math.min(...ys);
  return `${minX} ${minY} ${Math.max(...xs) - minX} ${Math.max(...ys) - minY}`;
};

/**
 * Звезда события: цвета карточки, видна только внутренняя тень по кромке — как innerShadow в
 * CalendarEventCard. Справа, со сдвигом и поворотом от параметров события.
 */
export const Star = ({ e, minutes, bg }: { e: Ev; minutes: number; bg: string }) => {
  const size = starFor(minutes);
  const d = stars[e.star];
  const id = `clip-${e.star}-${e.rot}`;
  return (
    <svg
      width={size}
      height={size}
      viewBox={bbox(d)}
      style={{
        position: "absolute",
        right: 0,
        top: "50%",
        marginTop: -size / 2,
        transform: `translate(${size * -e.off}px, ${size * e.off}px) rotate(${e.rot}deg)`,
        overflow: "visible",
      }}
    >
      <defs>
        <clipPath id={id}>
          <path d={d} />
        </clipPath>
      </defs>
      <g clipPath={`url(#${id})`}>
        <path d={d} fill="rgba(0,0,0,0.22)" />
        <path d={d} fill={bg} transform="translate(-1.6 -2.2)" />
      </g>
    </svg>
  );
};

/** Кривая камеры: быстрый старт, долгая мягкая посадка (expo out с коротким разгоном). */
const snap = (x: number) => (x < 0.15 ? (x / 0.15) ** 2 * 0.15 : 0.15 + 0.85 * (1 - Math.pow(2, -10 * ((x - 0.15) / 0.85))));

export const mixHex = (a: string, b: string, t: number) => {
  const pa = [1, 3, 5].map((i) => parseInt(a.slice(i, i + 2), 16));
  const pb = [1, 3, 5].map((i) => parseInt(b.slice(i, i + 2), 16));
  return `rgb(${pa.map((v, i) => Math.round(v + (pb[i] - v) * Math.max(0, Math.min(1, t)))).join(",")})`;
};
