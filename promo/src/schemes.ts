import {
  Hct,
  MaterialDynamicColors as M,
  SchemeTonalSpot,
  hexFromArgb,
} from "@material/material-color-utilities";

/**
 * Динамическая схема Material You из «цвета обоев» — тот же алгоритм (Tonal Spot), что строит
 * dynamicLight/DarkColorScheme на телефоне. Никаких выдуманных градиентов: только роли темы.
 */
export type Scheme = {
  dark: boolean;
  background: string;
  surfaceContainer: string;
  surfaceContainerHigh: string;
  inverseSurface: string;
  inverseOnSurface: string;
  onSurface: string;
  onSurfaceVariant: string;
  primary: string;
  onPrimary: string;
  primaryContainer: string;
  onPrimaryContainer: string;
  secondary: string;
  secondaryContainer: string;
  onSecondaryContainer: string;
  tertiary: string;
  tertiaryContainer: string;
  onTertiaryContainer: string;
};

const ROLES = {
  background: M.background,
  surfaceContainer: M.surfaceContainer,
  surfaceContainerHigh: M.surfaceContainerHigh,
  inverseSurface: M.inverseSurface,
  inverseOnSurface: M.inverseOnSurface,
  onSurface: M.onSurface,
  onSurfaceVariant: M.onSurfaceVariant,
  primary: M.primary,
  onPrimary: M.onPrimary,
  primaryContainer: M.primaryContainer,
  onPrimaryContainer: M.onPrimaryContainer,
  secondary: M.secondary,
  secondaryContainer: M.secondaryContainer,
  onSecondaryContainer: M.onSecondaryContainer,
  tertiary: M.tertiary,
  tertiaryContainer: M.tertiaryContainer,
  onTertiaryContainer: M.onTertiaryContainer,
};

export const scheme = (seed: number, dark: boolean): Scheme => {
  const s = new SchemeTonalSpot(Hct.fromInt(seed), dark, 0);
  const out: Record<string, string | boolean> = { dark };
  for (const [k, role] of Object.entries(ROLES)) out[k] = hexFromArgb(role.getArgb(s));
  return out as unknown as Scheme;
};

/** «Обои»: лаванда как на эмуляторе, а дальше — то, что бывает у людей на телефонах. */
export const SCHEMES: Scheme[] = [
  scheme(0xff5b6aae, false), // лаванда
  scheme(0xff4c7a4f, true), // лес, тёмная
  scheme(0xffc0665a, false), // коралл
  scheme(0xff2e7d9a, true), // море, тёмная
  scheme(0xffa8873a, false), // песок
  scheme(0xff8e4a8f, true), // ягода, тёмная
  scheme(0xff5b6aae, false), // обратно к лаванде
];

const hex = (c: string) => [1, 3, 5].map((i) => parseInt(c.slice(i, i + 2), 16));

/** Плавный переход между двумя схемами — все роли разом. */
export const mixScheme = (a: Scheme, b: Scheme, t: number): Scheme => {
  const out: Record<string, string | boolean> = { dark: t < 0.5 ? a.dark : b.dark };
  for (const k of Object.keys(ROLES)) {
    const pa = hex(a[k as keyof Scheme] as string);
    const pb = hex(b[k as keyof Scheme] as string);
    const c = pa.map((v, i) => Math.round(v + (pb[i] - v) * t).toString(16).padStart(2, "0"));
    out[k] = `#${c.join("")}`;
  }
  return out as unknown as Scheme;
};
