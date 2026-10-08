import { Composition } from "remotion";
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
      defaultProps={{ lang: "en" as const, wide: false }}
    />
    <Composition
      id="PromoRu"
      component={Promo}
      durationInFrames={PROMO_FRAMES}
      fps={30}
      width={1080}
      height={1920}
      defaultProps={{ lang: "ru" as const, wide: false }}
    />
    {/* Горизонтальная — для YouTube и промо-видео в Google Play. */}
    <Composition
      id="PromoWide"
      component={Promo}
      durationInFrames={PROMO_FRAMES}
      fps={30}
      width={1920}
      height={1080}
      defaultProps={{ lang: "en" as const, wide: true }}
    />
    <Composition
      id="PromoWideRu"
      component={Promo}
      durationInFrames={PROMO_FRAMES}
      fps={30}
      width={1920}
      height={1080}
      defaultProps={{ lang: "ru" as const, wide: true }}
    />
  </>
);
