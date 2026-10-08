import { staticFile } from "remotion";

// Цвета — как у приложения в динамической теме с лавандовыми обоями.
export const C = {
  bg: "#F4F1FA",
  surface: "#FBF8FF",
  primary: "#4E5A91",
  onPrimary: "#FFFFFF",
  primaryContainer: "#DDE1FF",
  onPrimaryContainer: "#0B1A4A",
  tertiary: "#7B5470",
  tertiaryContainer: "#FFD7F1",
  onTertiaryContainer: "#30112A",
  secondaryContainer: "#E2E1F6",
  green: "#CDEFD4",
  onGreen: "#0F3A1E",
  peach: "#FFDCC7",
  onPeach: "#3A1A07",
  dark: "#121318",
};

// Карточки «переливаются» по этим парам: контейнер и текст на нём.
export const CARD_COLORS: [string, string][] = [
  [C.primaryContainer, C.onPrimaryContainer],
  [C.tertiaryContainer, C.onTertiaryContainer],
  [C.green, C.onGreen],
  [C.peach, C.onPeach],
];

export const fontFace = `
@font-face {
  font-family: "RobotoFlex";
  src: url("${staticFile("RobotoFlex.ttf")}") format("truetype");
  font-weight: 100 1000;
  font-stretch: 25% 151%;
}`;

export const flex = (wght: number, wdth = 100, extra = "") => ({
  fontFamily: "RobotoFlex",
  fontVariationSettings: `'wght' ${wght}, 'wdth' ${wdth}${extra ? ", " + extra : ""}`,
});

/** Смешивание двух hex-цветов: карточки плавно перетекают из одного в другой. */
export const mix = (a: string, b: string, t: number) => {
  const pa = [1, 3, 5].map((i) => parseInt(a.slice(i, i + 2), 16));
  const pb = [1, 3, 5].map((i) => parseInt(b.slice(i, i + 2), 16));
  const c = pa.map((v, i) => Math.round(v + (pb[i] - v) * t));
  return `rgb(${c[0]},${c[1]},${c[2]})`;
};
