import { AbsoluteFill, interpolate, spring, useCurrentFrame, useVideoConfig } from "remotion";
import shapes from "./shapes.json";
import stars from "./stars.json";
import { Scheme, SCHEMES, mixScheme } from "./schemes";
import { Wave, easeInOut } from "./parts";
import { flex } from "./theme";

// Масштаб: 1 dp приложения = 3 px ролика (экран 360 dp → 1080 px).
const DP = 3;
const CARD_W = 1080 - 2 * 16 * DP;
const GAP = 3 * DP * 2;
const MIN_H = 65 * DP;
const MAX_H = 200 * DP;
const MICRO_H = 30 * DP;

/** Высота карточки — та же сигмоида по длительности, что в EventUiModelMapper. */
const heightFor = (minutes: number) => {
  if (minutes <= 25) return MICRO_H;
  const x = (minutes - 180) / 100;
  return MIN_H + (MAX_H - MIN_H) / (1 + Math.exp(-4.5 * x));
};

/** Размер звезды — calculateShapeContainerSize. */
const starFor = (minutes: number) => {
  const x = (minutes - 180) / 100;
  return (120 + 240 / (1 + Math.exp(-4.5 * x))) * DP;
};

type Ev = { minutes: number; time: string; star: keyof typeof stars; rot: number; off: number };

// Один день: короткие, микро и одно длинное — видно, что высота = длительность.
const DAY: Ev[] = [
  { minutes: 30, time: "8:00 – 8:30", star: "star5", rot: -20, off: 0.2 },
  { minutes: 20, time: "8:40 – 9:00", star: "star3", rot: 0, off: 0 },
  { minutes: 180, time: "9:00 – 12:00", star: "star4", rot: 25, off: 0.3 },
  { minutes: 60, time: "14:00 – 15:00", star: "star6", rot: -35, off: 0.2 },
  { minutes: 15, time: "15:30 – 15:45", star: "star3", rot: 0, off: 0 },
  { minutes: 90, time: "18:00 – 19:30", star: "star7", rot: 10, off: 0.4 },
  { minutes: 120, time: "20:00 – 22:00", star: "star3", rot: -40, off: 0.1 },
];
const DEEP = 2;
const NOW = 3;

// Несколько наборов названий: при смене темы меняется и день — «у каждого свой».
const TITLES = {
  en: [
    ["☕ Breakfast", "🚇 Commute", "💻 Deep work", "🎨 Design review", "📞 Call Mom", "🏋️ Gym", "🍝 Dinner with friends"],
    ["🥐 Croissant run", "🐕 Walk the dog", "📚 Study session", "🧪 Lab meeting", "💊 Vitamins", "🎸 Guitar lesson", "🎬 Movie night"],
    ["🧘 Morning yoga", "🚲 Ride to work", "🏗️ Sprint planning", "🍣 Lunch with Ana", "☎️ Dentist call", "🛒 Groceries", "🎮 Game night"],
  ],
  ru: [
    ["☕ Завтрак", "🚇 Дорога", "💻 Глубокая работа", "🎨 Дизайн-ревью", "📞 Позвонить маме", "🏋️ Зал", "🍝 Ужин с друзьями"],
    ["🥐 За круассаном", "🐕 Выгул собаки", "📚 Учёба", "🧪 Встреча в лабе", "💊 Витамины", "🎸 Урок гитары", "🎬 Кино вечером"],
    ["🧘 Утренняя йога", "🚲 На работу", "🏗️ Планирование", "🍣 Обед с Аней", "☎️ Звонок врачу", "🛒 Продукты", "🎮 Вечер игр"],
  ],
};

const RIBBONS = {
  en: [["✈️ Trip to Lisbon", 7], ["📖 Reading challenge", 30]] as const,
  ru: [["✈️ Поездка в Лиссабон", 7], ["📖 Книжный марафон", 30]] as const,
};

const DAY_OF = { en: (d: number, n: number) => `day ${d} of ${n}`, ru: (d: number, n: number) => `день ${d} из ${n}` };
const LEFT = { en: "25 min left", ru: "ещё 25 мин" };
const HEADER = { en: "Thu, 8 October", ru: "Чт, 8 октября" };

export const DAY_FRAMES = 390;

const HEADER_H = 48 * DP;
const RIBBON_H = 40 * DP;
const TOP = 70 * DP;

export const DayScene = ({ lang }: { lang: "en" | "ru" }) => {
  const f = useCurrentFrame();
  const { fps } = useVideoConfig();
  const sp = (from: number, cfg = { damping: 13, stiffness: 110, mass: 0.9 }) =>
    spring({ frame: f - from, fps, config: cfg });

  // --- Тема: с кадра 200 перебираем «обои», светлые и тёмные, монтажными склейками.
  const SWITCH_FROM = 200;
  const STEP = 20;
  const k = Math.max(0, (f - SWITCH_FROM) / STEP);
  const idx = Math.min(SCHEMES.length - 1, Math.floor(k));
  const next = Math.min(SCHEMES.length - 1, idx + 1);
  const local = k - Math.floor(k);
  // Переход — последние 6 кадров шага, остальное время тема стоит.
  const blend = f < SWITCH_FROM ? 0 : Math.max(0, (local - 0.7) / 0.3);
  const s: Scheme = mixScheme(SCHEMES[idx], SCHEMES[next], easeInOut(Math.min(1, blend)));
  const titleSet = f < SWITCH_FROM ? 0 : (idx + (blend > 0.5 ? 1 : 0)) % TITLES[lang].length;
  const titles = TITLES[lang][titleSet];

  // --- Раскладка: высоты на пружинах, позиции считаем сами, чтобы камера знала, куда смотреть.
  const appear = DAY.map((_, i) => sp(i * 4));
  const deepGrow = sp(52, { damping: 11, stiffness: 90, mass: 1 });
  const nowOn = sp(104, { damping: 10, stiffness: 120, mass: 0.9 });
  const heights = DAY.map((e, i) => {
    let h = e.minutes <= 25 ? MICRO_H : MIN_H;
    // Длинное событие сначала обычное, потом на пружине вырастает до своей высоты.
    if (i === DEEP) h = MIN_H + (heightFor(e.minutes) - MIN_H) * deepGrow;
    // Текущее раскрывается — как карточка, на которую нажали.
    if (i === NOW) h = MIN_H + 105 * DP * nowOn;
    return h;
  });
  const ribbonsTop = TOP + HEADER_H + 12 * DP;
  const listTop = ribbonsTop + 2 * (RIBBON_H + 4 * DP) + 8 * DP;
  const ys: number[] = [];
  let y = listTop;
  heights.forEach((h) => {
    ys.push(y);
    y += h + GAP;
  });
  const total = y + 40 * DP;

  // --- Камера: [кадр, центр x, центр y, зум]. Наезд на длинное, переход к текущему, общий план,
  // проекты. При наезде центр сдвинут влево: в кадре остаётся текст, а не край со звездой.
  const centerOf = (i: number) => ys[i] + heights[i] / 2;
  const keys: [number, number, number, number][] = [
    [0, 540, total / 2, 0.6],
    [36, 540, total / 2, 0.62],
    [62, 430, centerOf(DEEP), 1.25],
    [92, 430, centerOf(DEEP) + 60, 1.22],
    [118, 430, centerOf(NOW), 1.32],
    [172, 430, centerOf(NOW), 1.38],
    [205, 540, total / 2, 0.66],
    [330, 540, total / 2, 0.7],
    [355, 540, ribbonsTop + RIBBON_H, 1.08],
    [390, 540, ribbonsTop + RIBBON_H, 1.12],
  ];
  let ki = 0;
  while (ki < keys.length - 2 && f > keys[ki + 1][0]) ki++;
  const [f0, x0, y0, z0] = keys[ki];
  const [f1, x1, y1, z1] = keys[ki + 1];
  const p = easeInOut(Math.max(0, Math.min(1, (f - f0) / (f1 - f0))));
  const camX = x0 + (x1 - x0) * p;
  const camY = y0 + (y1 - y0) * p;
  let zoom = z0 + (z1 - z0) * p;
  // На каждой смене темы — короткий «удар» камеры, держит ритм.
  if (f >= SWITCH_FROM && f < 330) zoom *= 1 + 0.05 * Math.exp(-local * 6);

  const fade = interpolate(f, [370, 390], [1, 0], { extrapolateLeft: "clamp" });
  const progressDay = Math.round(interpolate(f, [340, 385], [3, 7], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }));

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

        {/* Шапка: кнопки и дата в «таблетке». */}
        <div style={{ position: "absolute", top: TOP, left: 12 * DP, right: 12 * DP, height: HEADER_H, display: "flex", alignItems: "center", gap: 10 * DP }}>
          <Circle color={s.primary} />
          <div style={{ flex: 1, height: 40 * DP, borderRadius: 20 * DP, background: s.inverseSurface, color: s.inverseOnSurface, display: "flex", alignItems: "center", justifyContent: "center", ...flex(700), fontSize: 16 * DP }}>
            {HEADER[lang]}
          </div>
          <Circle color={s.primary} />
        </div>

        {/* Ленты проектов: «день 3 из 7» с волнистым прогрессом. */}
        {RIBBONS[lang].map(([title, days], i) => {
          const day = i === 0 ? progressDay : 12;
          return (
            <div
              key={i}
              style={{
                position: "absolute",
                left: 16 * DP,
                width: CARD_W,
                top: ribbonsTop + i * (RIBBON_H + 4 * DP),
                height: RIBBON_H,
                borderRadius: RIBBON_H / 2,
                background: s.secondaryContainer,
                color: s.onSecondaryContainer,
                display: "flex",
                alignItems: "center",
                padding: `0 ${16 * DP}px`,
                gap: 12 * DP,
                opacity: appear[0],
              }}
            >
              <div style={{ flex: 1, ...flex(600), fontSize: 14 * DP, whiteSpace: "nowrap", overflow: "hidden" }}>{title}</div>
              <Wave width={56 * DP} height={10 * DP} progress={day / days} phase={0} color={s.secondary} />
              <div style={{ ...flex(500), fontSize: 12 * DP }}>{DAY_OF[lang](day, days)}</div>
            </div>
          );
        })}

        {/* Карточки дня. */}
        {DAY.map((e, i) => {
          const isNow = i === NOW;
          const n = isNow ? nowOn : 0;
          const bg = isNow ? mixHex(s.primaryContainer, s.tertiaryContainer, n) : s.primaryContainer;
          const fg = isNow ? mixHex(s.onPrimaryContainer, s.onTertiaryContainer, n) : s.onPrimaryContainer;
          const micro = e.minutes <= 25;
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
              {!micro && <Star e={e} bg={bg} />}
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
                  {titles[i]}
                </div>
                <div style={{ ...flex(450), fontSize: (micro ? 13 : 14) * DP, display: "flex", alignItems: "center", gap: 8 * DP, opacity: 0.9 }}>
                  {isNow && n > 0.05 && (
                    <div style={{ opacity: n }}>
                      <Wave width={52 * DP} height={9 * DP} progress={0.3 + Math.max(0, f - 110) / 500} phase={f / 4} color={fg} />
                    </div>
                  )}
                  {isNow && n > 0.5 ? `${LEFT[lang]} · ${e.time}` : e.time}
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </AbsoluteFill>
  );
};

const Circle = ({ color }: { color: string }) => (
  <div style={{ width: 40 * DP, height: 40 * DP, borderRadius: 20 * DP, background: color }} />
);

const BigShape = ({ d, size, x, y, color, rot }: { d: string; size: number; x: number; y: number; color: string; rot: number }) => (
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
const Star = ({ e, bg }: { e: Ev; bg: string }) => {
  const size = starFor(e.minutes);
  const d = stars[e.star];
  const id = `clip-${e.star}-${e.minutes}`;
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

const mixHex = (a: string, b: string, t: number) => {
  const pa = [1, 3, 5].map((i) => parseInt(a.slice(i, i + 2), 16));
  const pb = [1, 3, 5].map((i) => parseInt(b.slice(i, i + 2), 16));
  return `rgb(${pa.map((v, i) => Math.round(v + (pb[i] - v) * Math.max(0, Math.min(1, t)))).join(",")})`;
};
