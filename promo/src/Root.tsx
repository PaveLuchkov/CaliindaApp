import { Composition } from "remotion";
import { INTRO_LEN, IntroPreview } from "./Intros";
import { Promo, PROMO_FRAMES } from "./Promo";

export const Root = () => (
  <>
    <Composition
      id="Promo"
      component={Promo}
      durationInFrames={PROMO_FRAMES}
      fps={30}
      width={1080}
      height={1920}
      defaultProps={{ lang: "en" as const }}
    />
    <Composition
      id="PromoRu"
      component={Promo}
      durationInFrames={PROMO_FRAMES}
      fps={30}
      width={1080}
      height={1920}
      defaultProps={{ lang: "ru" as const }}
    />
    {(["A", "B", "C"] as const).map((v) => (
      <Composition
        key={v}
        id={`Intro${v}`}
        component={IntroPreview}
        durationInFrames={INTRO_LEN + 20}
        fps={30}
        width={1080}
        height={1920}
        defaultProps={{ variant: v }}
      />
    ))}
  </>
);
