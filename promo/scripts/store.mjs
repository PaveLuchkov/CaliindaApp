// node scripts/store.mjs <outDir> [lang...] — все скриншоты Play в полном размере, один бандл.
import { bundle } from "@remotion/bundler";
import { getCompositions, renderStill } from "@remotion/renderer";
import path from "node:path";
const [out, ...langs] = process.argv.slice(2);
const serveUrl = await bundle({ entryPoint: path.join(process.cwd(), "src/index.ts") });
const comps = (await getCompositions(serveUrl)).filter((c) => c.id.startsWith("Store-"));
for (const c of comps) {
  const lang = c.id.split("-").pop();
  if (langs.length && !langs.includes(lang)) continue;
  await renderStill({ composition: c, serveUrl, output: path.join(out, lang, `${c.id.slice(6)}.png`) });
}
console.log("done");
