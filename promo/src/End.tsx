import { AbsoluteFill, interpolate, useCurrentFrame } from "remotion";
import { Logo } from "./Logo";
import { MorphShape, useSpring } from "./parts";
import { Scheme } from "./schemes";
import { flex } from "./theme";

/**
 * Финал вариантов: иконка выпрыгивает, имя растягивается по оси ширины, слоган. Цвета — из
 * схемы ролика, чтобы финал не «выпадал» из его темы. За иконкой медленно дышит фигура.
 */
export const End = ({ s, tagline }: { s: Scheme; tagline: string }) => {
  const f = useCurrentFrame();
  const bg = useSpring(0, { damping: 16, stiffness: 120, mass: 0.9 });
  const logo = useSpring(4, { damping: 11, stiffness: 110, mass: 0.9 });
  const glyph = useSpring(14, { damping: 14, stiffness: 140, mass: 0.8 });
  const name = useSpring(16, { damping: 14, stiffness: 130, mass: 0.8 });
  const tag = useSpring(24, { damping: 15, stiffness: 140, mass: 0.8 });
  return (
    <AbsoluteFill style={{ background: s.background, alignItems: "center", justifyContent: "center", opacity: interpolate(bg, [0, 0.4], [0, 1], { extrapolateRight: "clamp" }) }}>
      <MorphShape
        names={["cookie12", "flower", "sunny", "clover4"]}
        t={f / 30}
        size={1500}
        color={s.surfaceContainerHigh}
        rotate={f * 0.4}
        style={{ position: "absolute", transform: `scale(${0.5 + 0.5 * bg})` }}
      />
      <div style={{ transform: `scale(${logo}) rotate(${(1 - logo) * -35}deg)` }}>
        <Logo size={360} bg={logo} glyph={glyph} rotate={f * 0.4} />
      </div>
      <div style={{ ...flex(1000, 25 + 100 * name), fontSize: 140, color: s.onSurface, marginTop: 56, opacity: Math.min(1, name * 1.5), transform: `translateY(${(1 - name) * 60}px)` }}>Caliinda</div>
      <div style={{ ...flex(520), fontSize: 50, color: s.onSurfaceVariant, marginTop: 16, opacity: tag, transform: `translateY(${(1 - tag) * 30}px)`, textAlign: "center", width: 920 }}>{tagline}</div>
    </AbsoluteFill>
  );
};

export const TAGLINE = { en: "A calendar that's a joy to open", ru: "Календарь, который приятно открывать" };
