import { AbsoluteFill, Easing, Sequence, interpolate, useCurrentFrame, useVideoConfig } from "remotion";
import { EventCard, MorphShape, NumberInShape, Shape, ShapeName, Wave, easeInOut, useSpring } from "./parts";
import { DAY_FRAMES, DayScene } from "./DayScene";
import { Intro } from "./Intros";
import { Logo } from "./Logo";
import { C, CARD_COLORS, flex, fontFace, mix } from "./theme";

export const PROMO_FRAMES = 806;

type Lang = "en" | "ru";

// Монтаж: начало спокойное (3,3 с), день с проектами, виджеты, три слова, финал.
const INTRO_FRAMES = 100;
const DAY_AT = 92;
const WIDGETS_FRAMES = 140;
const WIDGETS_AT = DAY_AT + DAY_FRAMES - 6;
const BEATS_AT = WIDGETS_AT + WIDGETS_FRAMES - 4;

const T = {
  en: {
    tagline: "A calendar that's a joy to open",
    events: [
      ["☕ Breakfast", "8:00 – 8:30"],
      ["🎨 Design review", "14:00 – 15:00"],
      ["🏋️ Gym", "18:00 – 19:30"],
      ["🍝 Dinner with friends", "20:00 – 21:30"],
    ],
    left: "25 min left",
    month: "October",
    daysUntil: "days until",
    vacation: "✈️ Vacation",
    week: ["M", "T", "W", "T", "F", "S", "S"],
    beats: ["Simple.", "Beautiful.", "Friendly."],
    widgets: "Widgets that feel alive",
  },
  ru: {
    tagline: "Календарь, который приятно открывать",
    events: [
      ["☕ Завтрак", "8:00 – 8:30"],
      ["🎨 Дизайн-ревью", "14:00 – 15:00"],
      ["🏋️ Зал", "18:00 – 19:30"],
      ["🍝 Ужин с друзьями", "20:00 – 21:30"],
    ],
    left: "ещё 25 мин",
    month: "Октябрь",
    daysUntil: "дней до",
    vacation: "✈️ Отпуска",
    week: ["П", "В", "С", "Ч", "П", "С", "В"],
    beats: ["Просто.", "Красиво.", "По-доброму."],
    widgets: "Виджеты, которые живут",
  },
};

export const Promo = ({ lang, wide = false }: { lang: Lang; wide?: boolean }) => {
  const t = T[lang];
  return (
    <AbsoluteFill style={{ background: C.bg }}>
      <style>{fontFace}</style>
      <FloatingShapes />
      <Sequence from={DAY_AT} durationInFrames={DAY_FRAMES}>
        {wide ? <WideDay lang={lang} /> : <DayScene lang={lang} />}
      </Sequence>
      {/* Начало поверх дня: фигура «пролетает» в камеру и открывает ленту. */}
      <Sequence durationInFrames={INTRO_FRAMES}>
        <IntroLayer />
      </Sequence>
      <Sequence from={WIDGETS_AT} durationInFrames={WIDGETS_FRAMES}>
        {/* В горизонтальном кадре виджеты по высоте не влезают — чуть уменьшаем. */}
        {/* Фон — снаружи уменьшения, иначе виден край прямоугольника. */}
        <AbsoluteFill style={{ background: C.bg }}>
          <AbsoluteFill style={{ transform: wide ? "scale(0.78)" : undefined }}>
            <Widgets t={t} />
          </AbsoluteFill>
        </AbsoluteFill>
      </Sequence>
      <Sequence from={BEATS_AT} durationInFrames={75}>
        <Beats words={t.beats} />
      </Sequence>
      <Sequence from={BEATS_AT + 72}>
        <Outro tagline={t.tagline} />
      </Sequence>
    </AbsoluteFill>
  );
};

/** Фон: крупные полупрозрачные фигуры медленно плывут и вращаются — «абстракция» на заднем плане. */
const FloatingShapes = () => {
  const f = useCurrentFrame();
  // Позиции заданы для вертикального кадра — растягиваем на любой.
  const { width, height } = useVideoConfig();
  const sx = width / 1080;
  const sy = height / 1920;
  const items: [ShapeName, number, number, number, string][] = [
    ["flower", 120, 260, 520, C.primaryContainer],
    ["sunny", 860, 640, 460, C.tertiaryContainer],
    ["clover4", 200, 1380, 600, C.secondaryContainer],
    ["cookie12", 900, 1700, 420, C.green],
  ];
  return (
    <AbsoluteFill>
      {items.map(([name, x, y, size, color], i) => (
        <Shape
          key={name}
          name={name}
          size={size}
          color={color}
          rotate={f * (i % 2 ? -0.25 : 0.3)}
          style={{
            position: "absolute",
            left: x * sx - size / 2 + Math.sin(f / 70 + i) * 40,
            top: y * sy - size / 2 + Math.cos(f / 90 + i * 2) * 50,
            opacity: 0.55,
          }}
        />
      ))}
    </AbsoluteFill>
  );
};


/** Лента дня: карточки прилетают, камера наезжает на текущее событие, отъезжает, цвета перетекают. */
const Day = ({ t }: { t: (typeof T)["en"] }) => {
  const f = useCurrentFrame();
  // Камера: общий план → наезд на «сейчас» → обратно, чуть с поворотом.
  const zoom = interpolate(f, [0, 40, 95, 150, 200], [0.75, 0.9, 1.3, 1.3, 0.95], {
    easing: Easing.bezier(0.65, 0, 0.35, 1),
    extrapolateRight: "clamp",
  });
  const focusY = interpolate(f, [40, 95, 150, 200], [0, -160, -160, 0], {
    easing: Easing.bezier(0.65, 0, 0.35, 1),
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });
  const tilt = interpolate(f, [150, 230], [0, -6], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
  const fade = interpolate(f, [270, 300], [1, 0], { extrapolateLeft: "clamp" });
  // После отъезда карточки начинают «переливаться» цветами.
  const flow = Math.max(0, f - 170) / 45;

  const shapes: ShapeName[] = ["clover4", "flower", "gem", "puffy"];
  return (
    <AbsoluteFill style={{ alignItems: "center", justifyContent: "center", opacity: fade }}>
      <div
        style={{
          display: "flex",
          flexDirection: "column",
          gap: 28,
          transform: `translateY(${focusY}px) scale(${zoom}) rotate(${tilt}deg)`,
        }}
      >
        {t.events.map(([title, time], i) => {
          const s = useSpring(i * 7);
          const isNow = i === 1;
          const k = (flow + i * 0.7) % CARD_COLORS.length;
          const a = CARD_COLORS[Math.floor(k)];
          const b = CARD_COLORS[(Math.floor(k) + 1) % CARD_COLORS.length];
          const m = easeInOut(k - Math.floor(k));
          const base = isNow ? [C.tertiaryContainer, C.onTertiaryContainer] : [C.primaryContainer, C.onPrimaryContainer];
          const bg = flow > 0 ? mix(a[0], b[0], m) : base[0];
          const fg = flow > 0 ? mix(a[1], b[1], m) : base[1];
          // Разъезд «веером» в конце — карточки перекрывают друг друга.
          const fan = interpolate(f, [200, 260], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp", easing: Easing.out(Easing.cubic) });
          return (
            <EventCard
              key={i}
              width={900}
              height={isNow ? 300 : 210}
              title={title}
              time={isNow ? `${t.left} · ${time}` : time}
              bg={bg}
              fg={fg}
              shape={shapes[i]}
              now={isNow}
              style={{
                transform: `translateY(${(1 - s) * 900 + fan * (1.5 - i) * 60}px) rotate(${fan * (i - 1.5) * 5}deg)`,
                opacity: s,
              }}
            >
              {isNow && <Wave width={210} height={36} progress={0.35 + f / 700} phase={f / 4} color={fg} />}
            </EventCard>
          );
        })}
      </div>
    </AbsoluteFill>
  );
};

/** Виджеты выпрыгивают по очереди, камера медленно дышит. */
const Widgets = ({ t }: { t: (typeof T)["en"] }) => {
  const f = useCurrentFrame();
  const title = useSpring(0, { damping: 15, stiffness: 120, mass: 0.9 });
  const delays = [8, 18, 28, 38];
  const pops = delays.map((d) => useSpring(d, { damping: 12, stiffness: 140, mass: 0.85 }));
  // Камера медленно и ровно наезжает — без «ударов»: они дёргали кадр не в такт виджетам.
  const breathe = interpolate(f, [0, 140], [0.97, 1.04], { easing: Easing.inOut(Easing.sin) });
  const out = interpolate(f, [126, 140], [1, 0], { extrapolateLeft: "clamp" });
  const outScale = interpolate(f, [124, 140], [1, 1.15], { extrapolateLeft: "clamp", easing: Easing.in(Easing.cubic) });
  const card = (p: number, extra: React.CSSProperties = {}) => ({
    transform: `scale(${p})`,
    opacity: Math.min(1, p * 1.5),
    borderRadius: 56,
    boxShadow: "0 24px 60px rgba(40,30,80,0.14)",
    ...extra,
  });
  return (
    <AbsoluteFill style={{ alignItems: "center", justifyContent: "center", opacity: out, background: C.bg }}>
      <div style={{ transform: `scale(${breathe * outScale})`, display: "flex", flexDirection: "column", alignItems: "center", gap: 40 }}>
        <div style={{ ...flex(900, 110), fontSize: 76, color: C.onPrimaryContainer, opacity: title, marginBottom: 20, textAlign: "center", width: 900 }}>
          {t.widgets}
        </div>
        <div style={{ display: "flex", gap: 40 }}>
          {/* Дата: число в фигуре дня, за ним — осенний лист. */}
          <div style={{ ...card(pops[0]), width: 430, height: 430, background: C.primaryContainer, position: "relative", overflow: "hidden", display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center" }}>
            <div style={{ position: "absolute", right: -40, top: -50, fontSize: 210, opacity: 0.3, transform: `rotate(${18 + Math.sin(f / 20) * 6}deg)` }}>🍂</div>
            <NumberInShape value="8" size={250} t={f / 18} />
            <div style={{ ...flex(800, 125), fontSize: 52, color: C.onPrimaryContainer, marginTop: 10 }}>{t.month}</div>
          </div>
          {/* Отсчёт. */}
          <div style={{ ...card(pops[1]), width: 430, height: 430, background: C.tertiaryContainer, display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center" }}>
            <NumberInShape value="12" size={230} shape="sunny" color={C.tertiary} />
            <div style={{ ...flex(450), fontSize: 34, color: C.onTertiaryContainer, marginTop: 16 }}>{t.daysUntil}</div>
            <div style={{ ...flex(800, 110), fontSize: 46, color: C.onTertiaryContainer }}>{t.vacation}</div>
          </div>
        </div>
        {/* Неделя. */}
        <div style={{ ...card(pops[2]), width: 900, height: 230, background: C.surface, display: "flex", alignItems: "center", justifyContent: "space-around", padding: "0 20px" }}>
          {t.week.map((d, i) => {
            const today = i === 3;
            const dots = [1, 2, 2, 3, 1, 0, 1][i];
            return (
              <div key={i} style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: 8 }}>
                <div style={{ ...flex(500), fontSize: 26, color: today ? C.primary : "#5F5E6E" }}>{d}</div>
                {today ? (
                  <NumberInShape value="8" size={84} shape="cookie9" />
                ) : (
                  <div style={{ ...flex(900, 100), fontSize: 50, height: 84, lineHeight: "84px", color: i < 3 ? "#9A98A8" : "#1B1B24" }}>{5 + i}</div>
                )}
                <div style={{ display: "flex", gap: 5, height: 12 }}>
                  {Array.from({ length: dots }).map((_, j) => (
                    <div key={j} style={{ width: 12, height: 12, borderRadius: 6, background: i < 3 ? "#9A98A8" : C.primary }} />
                  ))}
                </div>
              </div>
            );
          })}
        </div>
        {/* Кнопка «+». */}
        <div style={{ transform: `scale(${pops[3]}) rotate(${(1 - pops[3]) * -90}deg)`, position: "relative", width: 200, height: 200 }}>
          <MorphShape names={["cookie9", "softBurst"]} t={f / 40} size={200} color={C.primary} rotate={f} />
          <div style={{ position: "absolute", inset: 0, display: "flex", alignItems: "center", justifyContent: "center", ...flex(400), fontSize: 130, color: C.onPrimary, paddingBottom: 12 }}>+</div>
        </div>
      </div>
    </AbsoluteFill>
  );
};

/** Три слова — каждое на своей фигуре и своём цвете, монтажными склейками. */
const Beats = ({ words }: { words: string[] }) => {
  const f = useCurrentFrame();
  const i = Math.min(2, Math.floor(f / 25));
  const local = f - i * 25;
  const looks: [ShapeName[], string, string][] = [
    [["pill", "cookie12"], C.primary, C.onPrimary],
    [["flower", "sunny"], C.tertiary, C.onPrimary],
    [["heart", "clover4"], "#3F6B4A", C.onPrimary],
  ];
  const [names, bg, fg] = looks[i];
  const pop = useSpring(i * 25, { damping: 13, stiffness: 170, mass: 0.7 });
  return (
    <AbsoluteFill style={{ background: bg, alignItems: "center", justifyContent: "center" }}>
      <MorphShape names={names} t={local / 12} size={1300} color="rgba(255,255,255,0.12)" rotate={local * 2} style={{ position: "absolute" }} />
      <div
        style={{
          ...flex(1000, interpolate(local, [0, 25], [151, 60], { extrapolateRight: "clamp" })),
          // Длинное слово («По-доброму.») мельче, чтобы при самой широкой оси влезло в строку.
          fontSize: Math.min(150, 1350 / words[i].length),
          whiteSpace: "nowrap",
          color: fg,
          transform: `scale(${0.6 + pop * 0.4})`,
        }}
      >
        {words[i]}
      </div>
    </AbsoluteFill>
  );
};

const Outro = ({ tagline }: { tagline: string }) => {
  const f = useCurrentFrame();
  const logo = useSpring(0, { damping: 12, stiffness: 100, mass: 1 });
  const glyph = useSpring(12, { damping: 14, stiffness: 140, mass: 0.8 });
  const text = useSpring(14, { damping: 14, stiffness: 140, mass: 0.8 });
  return (
    <AbsoluteFill style={{ alignItems: "center", justifyContent: "center", background: C.bg }}>
      {/* Иконка приложения: выпрыгивает, раскрывается зелёный круг, проявляется календарь. */}
      <div style={{ transform: `scale(${logo}) rotate(${(1 - logo) * -30}deg)`, position: "relative", width: 380, height: 380 }}>
        <Logo size={380} bg={logo} glyph={glyph} rotate={f * 0.4} />
      </div>
      <div style={{ ...flex(1000, interpolate(text, [0, 1], [40, 125])), fontSize: 130, color: C.onPrimaryContainer, marginTop: 60, opacity: text }}>
        Caliinda
      </div>
      <div style={{ ...flex(500), fontSize: 46, color: "#5F5E6E", marginTop: 20, opacity: text, textAlign: "center", width: 900 }}>
        {tagline}
      </div>
    </AbsoluteFill>
  );
};

/** Вступление поверх ленты дня: свой фон, пока иконка не «пролетела» в камеру. */
const IntroLayer = () => {
  const f = useCurrentFrame();
  return (
    <AbsoluteFill style={{ background: f < 92 ? C.bg : "transparent" }}>
      {f < 92 && <FloatingShapes />}
      <Intro />
    </AbsoluteFill>
  );
};

// Подписи сбоку от «телефона» в горизонтальной версии — по ходу сцены дня.
const CAPTIONS: Record<Lang, [number, number, string][]> = {
  en: [
    [0, 44, "Your whole day\nat a glance"],
    [44, 92, "Longer events\ntake more space"],
    [92, 160, "What\u2019s now\nstands out"],
    [160, 290, "Your colors,\nlight and dark"],
    [290, 420, "Projects that\nspan days"],
  ],
  ru: [
    [0, 44, "Весь день\nодним взглядом"],
    [44, 92, "Длинное —\nдлиннее"],
    [92, 160, "Сейчас —\nв фокусе"],
    [160, 290, "Ваши цвета,\nсветлая и тёмная"],
    [290, 420, "Проекты\nна много дней"],
  ],
};

/** Горизонтальная версия: сцена дня в «телефоне» по центру, по бокам — подписи. */
const WideDay = ({ lang }: { lang: Lang }) => {
  const f = useCurrentFrame();
  const enter = useSpring(0, { damping: 15, stiffness: 120, mass: 0.9 });
  const exit = interpolate(f, [DAY_FRAMES - 12, DAY_FRAMES], [1, 0], { extrapolateLeft: "clamp" });
  const scale = 1000 / 1920;
  return (
    <AbsoluteFill style={{ background: C.bg, opacity: exit }}>
      <div
        style={{
          position: "absolute",
          left: 960 - (1080 * scale) / 2,
          top: 40,
          width: 1080,
          height: 1920,
          transformOrigin: "0 0",
          transform: `scale(${scale * (0.9 + 0.1 * enter)}) translateY(${(1 - enter) * 300}px)`,
          borderRadius: 90,
          overflow: "hidden",
          boxShadow: "0 40px 120px rgba(40,30,80,0.25)",
        }}
      >
        <DayScene lang={lang} />
      </div>
      {CAPTIONS[lang].map(([from, to, text], i) => {
        const local = f - from;
        if (local < 0 || f > to + 10) return null;
        const inS = interpolate(local, [0, 14], [0, 1], { extrapolateRight: "clamp", easing: Easing.out(Easing.cubic) });
        const outS = interpolate(f, [to - 4, to + 8], [1, 0], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
        const left = i % 2 === 0;
        return (
          <div
            key={i}
            style={{
              position: "absolute",
              top: 540,
              [left ? "right" : "left"]: 960 + (1080 * scale) / 2 + 70,
              transform: `translateY(-50%) translateX(${(1 - inS) * (left ? -60 : 60)}px)`,
              opacity: inS * outS,
              ...flex(950, 110),
              fontSize: 84,
              lineHeight: 1.05,
              color: C.onPrimaryContainer,
              textAlign: left ? "right" : "left",
              whiteSpace: "pre-line",
              width: 560,
            }}
          >
            {text}
          </div>
        );
      })}
    </AbsoluteFill>
  );
};
