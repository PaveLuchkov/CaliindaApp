import { AbsoluteFill, Easing, interpolate, useCurrentFrame } from "remotion";
import { Logo } from "./Logo";
import { useSpring } from "./parts";
import { C, flex } from "./theme";

export const INTRO_LEN = 100;

const NAME = "Caliinda";

/** Вступление: иконка выпрыгивает, буквы вылетают по одной — у каждой своя пружина и ширина. */
export const Intro = () => {
  const f = useCurrentFrame();
  const logo = useSpring(0, { damping: 11, stiffness: 110, mass: 0.9 });
  const glyph = useSpring(14, { damping: 14, stiffness: 140, mass: 0.8 });
  const letters = NAME.split("").map((_, i) => useSpring(26 + i * 4, { damping: 12, stiffness: 150, mass: 0.7 }));
  // Уход: буквы гаснут, иконка встаёт в центр и «пролетает» в камеру — зелёный круг на миг
  // заливает экран, и из него проявляется лента дня.
  const lettersOut = interpolate(f, [70, 80], [1, 0], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
  const toCenter = interpolate(f, [72, 88], [0, 135], { extrapolateLeft: "clamp", extrapolateRight: "clamp", easing: Easing.inOut(Easing.cubic) });
  const through = interpolate(f, [80, 100], [1, 9], { extrapolateLeft: "clamp", extrapolateRight: "clamp", easing: Easing.in(Easing.cubic) });
  const fade = interpolate(f, [93, 100], [1, 0], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
  return (
    <AbsoluteFill style={{ alignItems: "center", justifyContent: "center", opacity: fade }}>
      <div style={{ transform: `translateY(${toCenter}px) scale(${logo * through}) rotate(${(1 - logo) * -40}deg)`, zIndex: 1 }}>
        <Logo size={380} bg={logo} glyph={glyph} rotate={f * 0.6} />
      </div>
      <div style={{ display: "flex", marginTop: 70, height: 200, alignItems: "flex-end", opacity: lettersOut }}>
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
