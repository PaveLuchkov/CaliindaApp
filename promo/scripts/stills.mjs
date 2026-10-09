// node stills.mjs <compId> <outDir> <frame...> — один бандл, много кадров.
import { bundle } from "@remotion/bundler";
import { renderStill, selectComposition } from "@remotion/renderer";
import path from "node:path";
const [id, out, ...frames] = process.argv.slice(2);
const root = process.cwd();
const serveUrl = await bundle({ entryPoint: path.join(root, "src/index.ts") });
const composition = await selectComposition({ serveUrl, id, inputProps: {} });
for (const f of frames) {
  await renderStill({ composition, serveUrl, output: path.join(out, `${id}-${f}.png`), frame: Number(f), scale: 0.3 });
}
console.log("done");
