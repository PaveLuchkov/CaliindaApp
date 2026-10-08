import { interpolate as flubber } from "flubber";
import { useMemo } from "react";
import { AbsoluteFill, Easing, interpolate, useCurrentFrame } from "remotion";
import { LOGO_COOKIE, Logo } from "./Logo";
import { MorphShape, useSpring } from "./parts";
import shapes from "./shapes.json";
import { C, flex, fontFace, mix } from "./theme";

export const INTRO_LEN = 100;

const NAME = "Caliinda";

/** Общий уход: всё разгоняется в камеру и растворяется — склейка в ленту дня. */
const useExit = () => {
  const f = useCurrentFrame();
  const scale = interpolate(f, [80, 100], [1, 7], { extrapolateLeft: "clamp", extrapolateRight: "clamp", easing: Easing.in(Easing.cubic) });
  const opacity = interpolate(f, [90, 100], [1, 0], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
  return { scale, opacity };
};

/** A. Иконка выпрыгивает, буквы вылетают по одной — у каждой своя пружина и ширина. */
export const IntroA = () => {
  const f = useCurrentFrame();
  const logo = useSpring(0, { damping: 11, stiffness: 110, mass: 0.9 });
  const glyph = useSpring(14, { damping: 14, stiffness: 140, mass: 0.8 });
  const letters = NAME.split("").map((_, i) => useSpring(26 + i * 4, { damping: 12, stiffness: 150, mass: 0.7 }));
  const exit = useExit();
  return (
    <AbsoluteFill style={{ alignItems: "center", justifyContent: "center", opacity: exit.opacity, transform: `scale(${exit.scale})` }}>
      <div style={{ transform: `scale(${logo}) rotate(${(1 - logo) * -40}deg)` }}>
        <Logo size={380} bg={logo} glyph={glyph} rotate={f * 0.6} />
      </div>
      <div style={{ display: "flex", marginTop: 70, height: 200, alignItems: "flex-end" }}>
        {NAME.split("").map((ch, i) => {
          const s = letters[i];
          return (
            <span
              key={i}
              style={{
                display: "inline-block",
                ...flex(300 + 700 * s, 25 + 100 * s),
                fontSize: 170,
                color: C.onPrimaryContainer,
                opacity: Math.min(1, s * 1.5),
                transform: `translateY(${(1 - s) * 140}px)`,
              }}
            >
              {ch}
            </span>
          );
        })}
      </div>
    </AbsoluteFill>
  );
};

/** B. Название во весь экран сверхузким, затем на пружине раздаётся до широкого; иконка падает сверху. */
export const IntroB = () => {
  const f = useCurrentFrame();
  const appear = interpolate(f, [0, 12], [0, 1], { extrapolateRight: "clamp" });
  const stretch = useSpring(20, { damping: 13, stiffness: 80, mass: 1.1 });
  const logo = useSpring(46, { damping: 11, stiffness: 130, mass: 0.8 });
  const exit = useExit();
  return (
    <AbsoluteFill style={{ alignItems: "center", justifyContent: "center", opacity: exit.opacity, transform: `scale(${exit.scale})` }}>
      <div style={{ transform: `translateY(${(1 - logo) * -900}px)`, marginBottom: 40 }}>
        <Logo size={300} rotate={-f * 0.5} />
      </div>
      <div
        style={{
          ...flex(100 + 900 * stretch, 25 + 126 * stretch),
          fontSize: 520 - 340 * stretch,
          lineHeight: 1,
          color: C.onPrimaryContainer,
          opacity: appear,
          whiteSpace: "nowrap",
          transform: `translateY(${(1 - logo) * -180}px)`,
        }}
      >
        {NAME}
      </div>
    </AbsoluteFill>
  );
};

/** Переводит путь из координат 0..100 в место печенья логотипа (viewport 108). */
const toLogoSpace = (d: string) => {
  let i = 0;
  return d.replace(/-?\d+(\.\d+)?/g, (n) => String(14 + Number(n) * 0.8 + (i++ % 2) * 0));
};

/** C. Фигура морфится и перетекает в печенье логотипа, под ним раскрывается круг, выезжает имя. */
export const IntroC = () => {
  const f = useCurrentFrame();
  const toCookie = useMemo(() => flubber(toLogoSpace(shapes.cookie9), LOGO_COOKIE, { maxSegmentLength: 2 }), []);
  const morphT = easeOut(interpolate(f, [34, 54], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }));
  const pop = useSpring(0, { damping: 11, stiffness: 90, mass: 1 });
  const bg = useSpring(52, { damping: 12, stiffness: 120, mass: 0.8 });
  const glyph = useSpring(60, { damping: 14, stiffness: 140, mass: 0.8 });
  const name = useSpring(64, { damping: 14, stiffness: 110, mass: 0.9 });
  const exit = useExit();
  const size = 600 - 220 * name;
  return (
    <AbsoluteFill style={{ alignItems: "center", justifyContent: "center", opacity: exit.opacity, transform: `scale(${exit.scale})` }}>
      <div style={{ transform: `translateY(${-name * 110}px) scale(${pop})`, width: size, height: size, position: "relative" }}>
        {f < 34 ? (
          <MorphShape names={["sunny", "flower", "softBurst", "cookie9"]} t={f / 11.5} size={size} color={C.primary} rotate={f * 1.5} />
        ) : (
          // Печенье в иконке — половина круга: сначала увеличиваем, чтобы морф шёл без скачка,
          // и сжимаем вместе с раскрытием зелёного круга.
          <div style={{ transform: `scale(${2.05 - 1.05 * bg})` }}>
            <Logo size={size} bg={bg} glyph={glyph} cookieD={toCookie(morphT)} cookieColor={mix(C.primary, "#000000", morphT)} />
          </div>
        )}
      </div>
      <div
        style={{
          position: "absolute",
          top: 1180,
          ...flex(400 + 600 * name, 40 + 85 * name),
          fontSize: 170,
          color: C.onPrimaryContainer,
          opacity: name,
          clipPath: `inset(0 ${(1 - name) * 50}% 0 ${(1 - name) * 50}%)`,
        }}
      >
        {NAME}
      </div>
    </AbsoluteFill>
  );
};

const easeOut = (x: number) => 1 - Math.pow(1 - x, 3);

/** Обёртка для сравнения вариантов: тот же фон, что в ролике. */
export const IntroPreview = ({ variant }: { variant: "A" | "B" | "C" }) => (
  <AbsoluteFill style={{ background: C.bg }}>
    <style>{fontFace}</style>
    {variant === "A" ? <IntroA /> : variant === "B" ? <IntroB /> : <IntroC />}
  </AbsoluteFill>
);
