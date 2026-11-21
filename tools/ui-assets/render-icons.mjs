// Rasteriza os ícones Lucide da lista em PNG branco (o jogo pinta com a cor do tema).
// Uso: node render-icons.mjs <pasta-node_modules> <saida>
import fs from 'node:fs';
import path from 'node:path';
import { createRequire } from 'node:module';

const [modules, out] = process.argv.slice(2);
const { Resvg } = createRequire(path.resolve(modules) + '/')('@resvg/resvg-js');
const SIZES = [24, 40, 64];
const STROKE = 1.75;
fs.mkdirSync(out, { recursive: true });

const names = fs.readFileSync(new URL('./icons.txt', import.meta.url), 'utf8')
  .split('\n').map(s => s.trim()).filter(s => s && !s.startsWith('#'));

for (const entry of names) {
  const [name, variant] = entry.split(':');
  let svg = fs.readFileSync(path.join(modules, 'lucide-static/icons', name + '.svg'), 'utf8')
    .replace(/<!--.*?-->/s, '')
    .replaceAll('currentColor', '#ffffff')
    .replace(/stroke-width="2"/, `stroke-width="${STROKE}"`);
  if (variant === 'filled') svg = svg.replace('fill="none"', 'fill="#ffffff"');
  for (const size of SIZES) {
    const png = new Resvg(svg, { fitTo: { mode: 'width', value: size } }).render().asPng();
    fs.writeFileSync(path.join(out, `${name}${variant ? '-' + variant : ''}@${size}.png`), png);
  }
}
console.log(`${names.length} ícones x ${SIZES.length} tamanhos`);
