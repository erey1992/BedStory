// Build-time conversion only. Android runs Kotlin and packaged vectors, never JavaScript.
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const root = path.resolve(__dirname, '..');
const dest = path.join(root, 'app/src/main/assets');
fs.mkdirSync(path.join(dest, 'scenes'), { recursive: true });
const context = vm.createContext({ console, QUIZ: {} });
let source = fs.readFileSync(path.join(__dirname, 'content/illustrations.js'), 'utf8');
source = source.replace('return { scene, CAST, PROPS, BUILDERS, BG };',
  'return { scene, CAST, PROPS, BUILDERS, BG, drawItem };');
const read = files => files.map(f => fs.readFileSync(path.join(__dirname, 'content', f), 'utf8')).join('\n');
// primero los clásicos (stories.js a stories-8.js); el resto son cuentos originales de la app
vm.runInContext(source + '\n' + read(['stories.js', 'stories-2.js', 'stories-3.js', 'stories-4.js', 'stories-5.js', 'stories-6.js',
  'stories-7.js', 'stories-8.js']), context);
vm.runInContext(read(['stories-9.js', 'stories-10.js', 'stories-11.js', 'stories-12.js', 'stories-13.js', 'stories-14.js', 'stories-15.js',
  'stories-16.js', 'stories-17.js', 'stories-18.js', 'tags.js', 'sensitive.js',
  ...fs.readdirSync(path.join(__dirname, 'content')).filter(f => /^quiz-\d+\.js$/.test(f))]), context);
/**
 * Orden de la biblioteca: los 9 primeros clásicos y, a partir del décimo, los originales repartidos entre los clásicos
 * (en vez de todos al final). El reparto es fijo: siempre sale el mismo orden.
 */
// Los 9 primeros quedan fijos; desde el cuento 10, clásicos y originales se barajan al azar.
// La semilla es fija para que el orden no cambie de una exportación a otra (cámbiala para volver a mezclar).
const SHUFFLE_SEED = 20261106;
function interleave(all) {
  let seed = SHUFFLE_SEED;
  const rand = () => (seed = (seed * 16807) % 2147483647) / 2147483647;
  const out = all.slice(0, 9), rest = all.slice(9);
  for (let i = rest.length - 1; i > 0; i--) {
    const j = Math.floor(rand() * (i + 1));
    [rest[i], rest[j]] = [rest[j], rest[i]];
  }
  return out.concat(rest);
}
const stories = interleave(vm.runInContext('STORIES', context));
const art = vm.runInContext('Art', context);
const tags = vm.runInContext('TAGS', context);
const sensitive = vm.runInContext('SENSITIVE', context);
const quiz = vm.runInContext('QUIZ', context);
for (const [topic, { ids: list }] of Object.entries(sensitive)) for (const id of list) assert(stories.some(s => s.id === id), `${topic}: no existe el cuento ${id}`);
for (const id of Object.keys(quiz)) assert(stories.some(s => s.id === id), `preguntas de un cuento que no existe: ${id}`);
// cada id de las categorías debe existir
for (const [tag, list] of Object.entries(tags)) for (const id of list) assert(stories.some(s => s.id === id), `${tag}: no existe el cuento ${id}`);
const wrap = content => `<svg xmlns="http://www.w3.org/2000/svg" width="600" height="500" viewBox="0 0 600 500">${content}</svg>`;
let count = 0;
function scene(sc, key) {
  if (!sc) return null; // cuento sin escena vectorial: espera su ilustración pintada
  assert(art.BG[sc.bg], `Unknown background ${sc.bg}`);
  const layers = (sc.it || []).map((item, index) => {
    const [name, , , , options = {}] = item;
    assert(art.CAST[name] || art.BUILDERS[name] || art.PROPS[name], `Unknown illustration ${name}`);
    return {
      svg: wrap(art.drawItem(item, index)),
      animation: options.anim || (art.CAST[name] || art.BUILDERS[name] ? 'idle' : ''),
      phase: index * 0.37,
    };
  });
  const tint = sc.time === 'night' && sc.bg !== 'room'
    ? wrap('<rect width="600" height="500" fill="#0b1440" opacity=".12"/>') : null;
  fs.writeFileSync(path.join(dest, 'scenes', key + '.json'), JSON.stringify({
    background: wrap(art.BG[sc.bg](sc, 'bg')), layers, tint,
  }));
  count++;
  return 'scenes/' + key + '.json';
}
// Ilustraciones pintadas (de tools/crop-illustrations.py): reemplazan a la escena vectorial cuando existen.
const painted = (id, n) => fs.existsSync(path.join(dest, 'art', id, n + '.webp')) ? `art/${id}/${n}.webp` : null;
// tres preguntas de comprensión; la primera respuesta es la correcta (la app las baraja)
const quizFor = id => {
  const q = quiz[id];
  if (!q) { console.warn('Sin preguntas:', id); return []; }
  assert(q.length === 3 && q.every(e => e.length === 4 && e.every(t => typeof t === 'string' && t.trim())), `Preguntas mal formadas: ${id}`);
  return q.map(([question, ...answers]) => ({ question, answers }));
};
const ids = new Set();
const catalog = stories.map(story => {
  assert(!ids.has(story.id)); ids.add(story.id);
  return {
    id: story.id, title: story.title, description: story.desc, color: story.color, poem: !!story.poem,
    tags: Object.keys(tags).filter(tag => tags[tag].includes(story.id)),
    sensitive: Object.keys(sensitive).filter(topic => sensitive[topic].ids.includes(story.id)),
    quiz: quizFor(story.id),
    cover: scene(story.cover, story.id + '-cover'), coverImage: painted(story.id, 0),
    pages: story.pages.map((page, index) => ({
      scene: scene(page.sc, story.id + '-' + (index + 1)), image: painted(story.id, index + 1),
      text: page.tx.map(segment => typeof segment === 'string'
        ? { speaker: null, text: segment } : { speaker: segment.s, text: segment.t }),
    })),
  };
});
fs.writeFileSync(path.join(dest, 'stories.json'), JSON.stringify(catalog));
assert(catalog.every(s => s.pages.length && s.pages.every(p => p.text.length)), 'Cuento o página sin texto');
fs.writeFileSync(path.join(dest, 'topics.json'), JSON.stringify(Object.entries(sensitive).map(([id, t]) => ({ id, label: t.label }))));

// ---- Premios de los juegos: calcomanías, fondos, dibujos para colorear y rompecabezas ----
// calcomanías pintadas (de tools/crop-stickers.py): cada una es de un cuento y se gana al terminarlo
const paintedStickers = catalog.filter(s => fs.existsSync(path.join(dest, 'stickers', s.id + '.webp')))
  .map(s => ({ id: 'st-' + s.id, image: 'stickers/' + s.id + '.webp', title: s.title, story: s.id }));
// calcomanías de objetos, animales y personas (tools/content/stickers.json): también se ganan al terminar su cuento
const objectStickers = Object.entries(JSON.parse(fs.readFileSync(path.join(__dirname, 'content', 'stickers.json'), 'utf8')).stickers)
  .map(([id, [story, name]]) => {
    const s = catalog.find(c => c.id === story);
    assert(s, `calcomanía ${id}: no existe el cuento ${story}`);
    assert(fs.existsSync(path.join(dest, 'stickers', 'obj', id + '.webp')), `falta la imagen de la calcomanía ${id} (corre tools/crop-stickers.py)`);
    return { id: 'st-obj-' + id, image: 'stickers/obj/' + id + '.webp', title: s.title, name, story };
  });
const withArt = catalog.filter(s => s.coverImage);
const firstArt = withArt.slice(0, 2).map(s => s.id);
// fondos para las calcomanías (de tools/crop-sheets.py): no son de ningún cuento
const bgTitles = JSON.parse(fs.readFileSync(path.join(dest, 'backgrounds', 'titles.json'), 'utf8'));
const BG_STARTER = new Set(['bosque', 'playa']);
const rewards = {
  stickers: [...paintedStickers, ...objectStickers],
  backgrounds: Object.entries(bgTitles).map(([name, title]) => ({ id: 'bg-' + name, image: 'backgrounds/' + name + '.webp', title, starter: BG_STARTER.has(name) })),
  // dibujos para colorear (de tools/crop-sheets.py): cada uno es de su cuento
  coloring: catalog.filter(s => fs.existsSync(path.join(dest, 'coloring', s.id + '.webp')))
    .map(s => ({ id: 'c-' + s.id, image: 'coloring/' + s.id + '.webp', title: s.title, story: s.id, starter: firstArt.includes(s.id) })),
  puzzles: withArt.map(s => ({ id: 'p-' + s.id, image: s.coverImage, title: s.title, story: s.id, starter: firstArt.includes(s.id) })),
};
fs.writeFileSync(path.join(dest, 'rewards.json'), JSON.stringify(rewards));
console.log(`Premios: ${paintedStickers.length + objectStickers.length} calcomanías, ${rewards.backgrounds.length} fondos, ${rewards.coloring.length} dibujos, ${rewards.puzzles.length} rompecabezas.`);
console.log(`Exported ${catalog.length} stories, ${catalog.reduce((n, s) => n + s.pages.length, 0)} pages and ${count} layered scenes.`);
