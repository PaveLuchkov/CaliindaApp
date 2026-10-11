import { AbsoluteFill, Img, staticFile } from "remotion";
import { Shape, ShapeName } from "./parts";
import { Logo, Wordmark } from "./Logo";
import { JUICY } from "./schemes";
import { flex, fontFace } from "./theme";

/**
 * Скриншоты для Google Play: настоящие экраны с эмулятора в телефонах, крупная подпись и фигура
 * темы за ними. Экраны — `public/store/<lang>_<screen>.png` (1080×2400), снимаются с локального
 * демо-календаря (`scripts/demo_calendar.py`). Одна мысль на кадр, как в роликах.
 */

/** Языки листинга: все, на которые переведено приложение. */
export const STORE_LANGS = ["en", "ru", "de", "es", "fr", "hi"] as const;
export type StoreLang = (typeof STORE_LANGS)[number];
type Text = Record<StoreLang, string>;

const BEZEL = 16;

/** Кусок экрана, вынесенный поверх телефона: координаты в px исходного скриншота. */
type Pop = { x: number; y: number; w: number; h: number; r: number; scale: number };

/** Телефон в кадре: [x], [y] — левый верхний угол экрана, [w] — ширина экрана. */
type Phone = { screen: string; x: number; y: number; w: number; rotate?: number; pop?: Pop };

type Shot = {
  bg: string;
  ink: string;
  shape: ShapeName;
  shapeColor: string;
  title: Text;
  sub: Text;
  phones: Phone[];
};

// Один телефон по центру, низ уходит за край — так экран крупнее.
const single = (screen: string, pop?: Pop): Phone[] => [{ screen, x: 140, y: 560, w: 800, pop }];

export const SHOTS: Record<string, Shot> = {
  day: {
    bg: JUICY.tertiaryContainer,
    ink: JUICY.onTertiaryContainer,
    shape: "flower",
    shapeColor: "rgba(255,255,255,0.35)",
    title: {
      en: "Your day\nat a glance",
      ru: "Весь день\nодним взглядом",
      de: "Dein Tag\nauf einen Blick",
      es: "Tu día\nde un vistazo",
      fr: "Votre journée\nd'un coup d'œil",
      hi: "आपका दिन\nएक नज़र में",
    },
    sub: {
      en: "What's happening now always stands out",
      ru: "То, что идёт сейчас, всегда на виду",
      de: "Was gerade läuft, fällt sofort ins Auge",
      es: "Lo que está pasando ahora siempre destaca",
      fr: "Ce qui se passe maintenant saute aux yeux",
      hi: "जो अभी चल रहा है, वह हमेशा सामने रहता है",
    },
    // Текущее событие «поднимается к зрителю», как в приложении.
    phones: single("day", { x: 42, y: 1195, w: 996, h: 210, r: 44, scale: 1.12 }),
  },
  chips: {
    bg: "#FFB98A",
    ink: "#4A1F00",
    shape: "clover4",
    shapeColor: "rgba(255,255,255,0.35)",
    title: {
      en: "One tap\nand it's named",
      ru: "Одно касание —\nи готово",
      de: "Ein Tippen —\nfertig",
      es: "Un toque\ny listo",
      fr: "Une touche,\nc'est nommé",
      hi: "एक टैप\nऔर नाम तैयार",
    },
    sub: {
      en: "Suggestion chips fill in the title for you",
      ru: "Чипы сами заполняют название",
      de: "Vorschlags-Chips füllen den Titel für dich aus",
      es: "Las sugerencias rellenan el título por ti",
      fr: "Les suggestions remplissent le titre pour vous",
      hi: "सुझाव चिप्स आपके लिए शीर्षक भर देते हैं",
    },
    phones: single("chips", { x: 42, y: 885, w: 996, h: 387, r: 60, scale: 1.1 }),
  },
  long: {
    bg: JUICY.primaryContainer,
    ink: JUICY.onPrimaryContainer,
    shape: "puffy",
    shapeColor: "rgba(255,255,255,0.4)",
    title: {
      en: "Anything longer\nthan a day",
      ru: "Всё, что\nдольше дня",
      de: "Länger\nals ein Tag",
      es: "Todo lo que dura\nmás de un día",
      fr: "Tout ce qui dure\nplus d'un jour",
      hi: "एक दिन से\nलंबा सब कुछ",
    },
    sub: {
      en: "Trips, courses, moves — and how many days are left",
      ru: "Поездки, курсы, переезды — и сколько осталось",
      de: "Reisen, Kurse, Umzüge — und wie viele Tage noch bleiben",
      es: "Viajes, cursos, mudanzas — y cuántos días quedan",
      fr: "Voyages, cours, déménagements — et les jours restants",
      hi: "यात्राएँ, कोर्स, घर बदलना — और कितने दिन बाकी हैं",
    },
    phones: single("long"),
  },
  widgets: {
    bg: "#C9D4FF",
    ink: "#13204F",
    shape: "cookie12",
    shapeColor: "rgba(255,255,255,0.4)",
    title: {
      en: "Widgets\nfor every taste",
      ru: "Виджеты\nна любой вкус",
      de: "Widgets\nnach Geschmack",
      es: "Widgets\npara cada gusto",
      fr: "Des widgets\nà votre goût",
      hi: "हर पसंद के\nविजेट",
    },
    sub: {
      en: "Date, countdown, today's events and more",
      ru: "Дата, обратный отсчёт, события дня и не только",
      de: "Datum, Countdown, die Termine des Tages und mehr",
      es: "Fecha, cuenta atrás, los eventos del día y más",
      fr: "Date, compte à rebours, événements du jour et plus",
      hi: "तारीख, उलटी गिनती, आज के इवेंट और भी बहुत कुछ",
    },
    phones: [
      { screen: "homeA", x: 36, y: 590, w: 488, rotate: -3 },
      { screen: "homeB", x: 556, y: 640, w: 488, rotate: 3 },
    ],
  },
  colors: {
    bg: "#FFF1E8",
    ink: "#3B1D12",
    shape: "softBurst",
    shapeColor: "rgba(255,170,130,0.22)",
    title: { en: "Your colors", ru: "Ваши цвета", de: "Deine Farben", es: "Tus colores", fr: "Vos couleurs", hi: "आपके रंग" },
    sub: {
      en: "Taken from your wallpaper — light or dark",
      ru: "Из ваших обоев — в светлой и тёмной теме",
      de: "Aus deinem Hintergrund — hell oder dunkel",
      es: "Sacados de tu fondo de pantalla — en claro u oscuro",
      fr: "Tirées de votre fond d'écran — en clair ou en sombre",
      hi: "आपके वॉलपेपर से — लाइट या डार्क",
    },
    phones: [
      { screen: "t_coral", x: 20, y: 560, w: 470, rotate: -7 },
      { screen: "t_berry", x: 590, y: 560, w: 470, rotate: 7 },
      { screen: "t_sea", x: 290, y: 500, w: 500 },
    ],
  },
  private: {
    bg: "#1E2A20",
    ink: "#D6F0CF",
    shape: "gem",
    shapeColor: "rgba(214,240,207,0.07)",
    title: {
      en: "No account.\nNo servers.",
      ru: "Без аккаунта.\nБез серверов.",
      de: "Kein Konto.\nKeine Server.",
      es: "Sin cuenta.\nSin servidores.",
      fr: "Sans compte.\nSans serveurs.",
      hi: "न अकाउंट।\nन सर्वर।",
    },
    sub: {
      en: "Works with the calendars already on your phone",
      ru: "Работает с календарями на вашем телефоне",
      de: "Arbeitet mit den Kalendern auf deinem Handy",
      es: "Funciona con los calendarios de tu teléfono",
      fr: "Utilise les agendas déjà présents sur votre téléphone",
      hi: "आपके फ़ोन में पहले से मौजूद कैलेंडर के साथ काम करता है",
    },
    phones: single("t_forest"),
  },
};

const Screen = ({ src, w }: { src: string; w: number }) => (
  <Img src={src} style={{ width: w, height: (2400 * w) / 1080, display: "block" }} />
);

const PhoneView = ({ p, lang }: { p: Phone; lang: StoreLang }) => {
  const src = staticFile(`store/${lang}_${p.screen}.png`);
  const k = p.w / 1080;
  const r = (72 * p.w) / 800;
  const pop = p.pop;
  return (
    <div style={{ position: "absolute", left: p.x - BEZEL, top: p.y - BEZEL, transform: `rotate(${p.rotate ?? 0}deg)`, transformOrigin: "50% 30%" }}>
      {/* Тонкая тёмная рамка; низ уходит за край кадра. */}
      <div style={{ padding: BEZEL, background: "#1B1B1F", borderRadius: r, boxShadow: "0 30px 80px rgba(0,0,0,0.28)" }}>
        <div style={{ borderRadius: r - BEZEL, overflow: "hidden" }}>
          <Screen src={src} w={p.w} />
        </div>
      </div>
      {/* Вынесенный кусок — тот же скриншот, крупнее и с плотной тенью. */}
      {pop && (
        <div
          style={{
            position: "absolute",
            left: BEZEL + (pop.x + pop.w / 2) * k - (pop.w * k * pop.scale) / 2,
            top: BEZEL + (pop.y + pop.h / 2) * k - (pop.h * k * pop.scale) / 2,
            width: pop.w * k * pop.scale,
            height: pop.h * k * pop.scale,
            borderRadius: pop.r * k * pop.scale,
            overflow: "hidden",
            boxShadow: "0 14px 22px rgba(0,0,0,0.32)",
          }}
        >
          <div style={{ transform: `scale(${pop.scale}) translate(${-pop.x * k}px, ${-pop.y * k}px)`, transformOrigin: "0 0" }}>
            <Screen src={src} w={p.w} />
          </div>
        </div>
      )}
    </div>
  );
};

export const StoreShot = ({ shot, lang }: { shot: string; lang: StoreLang }) => {
  const s = SHOTS[shot];
  return (
    <AbsoluteFill style={{ background: s.bg, overflow: "hidden" }}>
      <style>{fontFace}</style>
      <Shape name={s.shape} size={1500} color={s.shapeColor} rotate={14} style={{ position: "absolute", left: -210, top: 520 }} />

      {/* fontWeight — для хинди: в Roboto Flex нет деванагари, запасной шрифт берёт вес отсюда. */}
      <div style={{ position: "absolute", top: 120, left: 60, right: 60, textAlign: "center", color: s.ink }}>
        {s.title[lang].split("\n").map((line) => (
          <div key={line} style={{ ...flex(900, 112), fontWeight: 800, fontSize: 104, lineHeight: 1.02, letterSpacing: -1.5 }}>
            {line}
          </div>
        ))}
        <div style={{ ...flex(500, 100), fontWeight: 500, fontSize: 40, lineHeight: 1.25, marginTop: 28, opacity: 0.85 }}>{s.sub[lang]}</div>
      </div>

      {s.phones.map((p) => (
        <PhoneView key={p.screen} p={p} lang={lang} />
      ))}
    </AbsoluteFill>
  );
};

const TAGLINE: Text = {
  en: "A calendar that's a joy to open",
  ru: "Календарь, который приятно открывать",
  de: "Ein Kalender, den man gern öffnet",
  es: "Un calendario que da gusto abrir",
  fr: "Un agenda qu'on a plaisir à ouvrir",
  hi: "एक कैलेंडर जिसे खोलना अच्छा लगता है",
};

/** Обложка Play (feature graphic) 1024×500: знак, название и кусок настоящего дня в телефоне. */
export const FeatureGraphic = ({ lang }: { lang: StoreLang }) => {
  const w = 430;
  const k = w / 1080;
  return (
    <AbsoluteFill style={{ background: JUICY.primaryContainer, overflow: "hidden" }}>
      <style>{fontFace}</style>
      <Shape name="flower" size={760} color="rgba(255,255,255,0.35)" rotate={10} style={{ position: "absolute", left: 470, top: -150 }} />
      <div style={{ position: "absolute", left: 64, top: 0, bottom: 0, width: 440, display: "flex", flexDirection: "column", justifyContent: "center", color: JUICY.onPrimaryContainer }}>
        <Logo size={112} />
        <Wordmark size={96} color={JUICY.onPrimaryContainer} style={{ marginTop: 14 }} />
        <div style={{ ...flex(560, 100), fontSize: 34, lineHeight: 1.2, marginTop: 10 }}>{TAGLINE[lang]}</div>
      </div>
      {/* Телефон наклонён и уходит за нижний край: виден верх дня с текущим событием. */}
      <div style={{ position: "absolute", left: 560, top: 50, transform: "rotate(8deg)", transformOrigin: "50% 0" }}>
        <div style={{ padding: 10, background: "#1B1B1F", borderRadius: 46, boxShadow: "0 24px 60px rgba(0,0,0,0.3)" }}>
          <div style={{ borderRadius: 36, overflow: "hidden", width: w, height: 1500 * k }}>
            <Img src={staticFile(`store/${lang}_day.png`)} style={{ width: w, display: "block" }} />
          </div>
        </div>
      </div>
    </AbsoluteFill>
  );
};
