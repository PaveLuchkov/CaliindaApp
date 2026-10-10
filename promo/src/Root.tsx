import { Composition, Still } from "remotion";
import { Promo, PROMO_FRAMES } from "./Promo";
import { SWIPE_FRAMES, Swipe } from "./Swipe";
import { CHIPS_FRAMES, Chips } from "./Chips";
import { LONG_FRAMES, LongEvents } from "./Long";
import { COLORS_FRAMES, Colors } from "./Colors";
import { FeatureGraphic, SHOTS, StoreShot } from "./Store";

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
    {/* Варианты: у каждого своя идея, вертикальные, ~20 с. */}
    <Composition id="Swipe" component={Swipe} durationInFrames={SWIPE_FRAMES} fps={30} width={1080} height={1920} defaultProps={{ lang: "en" as const }} />
    <Composition id="SwipeRu" component={Swipe} durationInFrames={SWIPE_FRAMES} fps={30} width={1080} height={1920} defaultProps={{ lang: "ru" as const }} />
    <Composition id="Chips" component={Chips} durationInFrames={CHIPS_FRAMES} fps={30} width={1080} height={1920} defaultProps={{ lang: "en" as const }} />
    <Composition id="ChipsRu" component={Chips} durationInFrames={CHIPS_FRAMES} fps={30} width={1080} height={1920} defaultProps={{ lang: "ru" as const }} />
    <Composition id="Long" component={LongEvents} durationInFrames={LONG_FRAMES} fps={30} width={1080} height={1920} defaultProps={{ lang: "en" as const }} />
    <Composition id="LongRu" component={LongEvents} durationInFrames={LONG_FRAMES} fps={30} width={1080} height={1920} defaultProps={{ lang: "ru" as const }} />
    <Composition id="Colors" component={Colors} durationInFrames={COLORS_FRAMES} fps={30} width={1080} height={1920} defaultProps={{ lang: "en" as const }} />
    <Composition id="ColorsRu" component={Colors} durationInFrames={COLORS_FRAMES} fps={30} width={1080} height={1920} defaultProps={{ lang: "ru" as const }} />
    {/* Скриншоты для Google Play: Store-<кадр>-<язык>. */}
    {Object.keys(SHOTS).flatMap((shot) =>
      (["en", "ru"] as const).map((lang) => (
        <Still key={shot + lang} id={`Store-${shot}-${lang}`} component={StoreShot} width={1080} height={1920} defaultProps={{ shot, lang }} />
      )),
    )}
    <Still id="Store-feature-en" component={FeatureGraphic} width={1024} height={500} defaultProps={{ lang: "en" as const }} />
    <Still id="Store-feature-ru" component={FeatureGraphic} width={1024} height={500} defaultProps={{ lang: "ru" as const }} />
  </>
);
