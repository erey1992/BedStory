/* Ilustraciones vectoriales de Los Cuentos de Mamá Oca.
   Cada escena es un SVG de 600x500 compuesto por un fondo y una lista de elementos:
   [nombre, x, y, escala, opciones]. El origen de cada personaje u objeto está en sus pies (centro inferior). */
const Art = (() => {
  let uid = 0;

  // ---------- utilidades ----------
  const shade = (hex, amt) => {
    let c = hex.replace('#', '');
    if (c.length === 3) c = c.split('').map(x => x + x).join('');
    const n = parseInt(c, 16);
    const f = v => Math.max(0, Math.min(255, Math.round(amt < 0 ? v * (1 + amt) : v + (255 - v) * amt)));
    return '#' + [n >> 16, (n >> 8) & 255, n & 255].map(v => f(v).toString(16).padStart(2, '0')).join('');
  };
  const rng = seed => () => (seed = (seed * 16807) % 2147483647) / 2147483647;
  const star5 = (r, r2 = r * 0.45) => {
    let d = '';
    for (let i = 0; i < 10; i++) {
      const a = -Math.PI / 2 + i * Math.PI / 5, rr = i % 2 ? r2 : r;
      d += (i ? 'L' : 'M') + (Math.cos(a) * rr).toFixed(1) + ',' + (Math.sin(a) * rr).toFixed(1);
    }
    return d + 'Z';
  };
  const sparkle = (x, y, r, c = '#fff7c2') =>
    `<path d="M${x},${y - r} Q${x},${y} ${x + r},${y} Q${x},${y} ${x},${y + r} Q${x},${y} ${x - r},${y} Q${x},${y} ${x},${y - r}Z" fill="${c}"/>`;
  const INK = '#3a2a22';

  // ---------- caras ----------
  function face(cy, o = {}) {
    const gap = o.gap || 11, m = o.mood || 'happy', ey = cy - 2;
    let s = '';
    if (m === 'sleep') {
      s += `<path d="M${-gap - 5},${ey} q5,4 10,0 M${gap - 5},${ey} q5,4 10,0" stroke="${INK}" stroke-width="2.5" fill="none" stroke-linecap="round"/>`;
    } else {
      s += `<g fill="#2b1d16"><ellipse cx="${-gap}" cy="${ey}" rx="4.3" ry="5.3"/><ellipse cx="${gap}" cy="${ey}" rx="4.3" ry="5.3"/></g>` +
        `<g fill="#fff"><circle cx="${-gap + 1.5}" cy="${ey - 2}" r="1.7"/><circle cx="${gap + 1.5}" cy="${ey - 2}" r="1.7"/></g>`;
      if (m === 'grumpy') s += `<path d="M${-gap - 6},${ey - 10} l11,4 M${gap + 6},${ey - 10} l-11,4" stroke="${INK}" stroke-width="2.5" stroke-linecap="round"/>`;
      if (m === 'sad') s += `<path d="M${-gap - 5},${ey - 8} l9,-3 M${gap + 5},${ey - 8} l-9,-3" stroke="${INK}" stroke-width="2" stroke-linecap="round"/>`;
    }
    if (!o.noCheeks) s += `<g fill="#ff8a8a" opacity=".45"><circle cx="${-gap - 7}" cy="${cy + 8}" r="6"/><circle cx="${gap + 7}" cy="${cy + 8}" r="6"/></g>`;
    if (o.noMouth) return s;
    const my = cy + (o.my || 13);
    if (m === 'laugh' || m === 'sing') s += `<path d="M-8,${my - 1} q8,12 16,0z" fill="#7a2e22"/>`;
    else if (m === 'sad') s += `<path d="M-6,${my + 4} q6,-6 12,0" stroke="#7a2e22" stroke-width="2.5" fill="none" stroke-linecap="round"/>`;
    else if (m === 'surprised') s += `<ellipse cx="0" cy="${my + 1}" rx="4.5" ry="5.5" fill="#7a2e22"/>`;
    else if (m === 'grumpy') s += `<path d="M-6,${my + 2} h12" stroke="#7a2e22" stroke-width="2.5" stroke-linecap="round"/>`;
    else if (m === 'blow') s += `<circle cx="0" cy="${my}" r="5" fill="#7a2e22"/>`;
    else s += `<path d="M-7,${my} q7,7 14,0" stroke="#7a2e22" stroke-width="2.5" fill="none" stroke-linecap="round"/>`;
    return s;
  }

  // ---------- objetos que se sostienen ----------
  function held(item, x, y, o = {}) {
    const t = `translate(${x} ${y})`;
    switch (item) {
      case 'basket': return `<g transform="${t}"><path d="M-14,-6 q14,-24 28,0" stroke="#a56a28" stroke-width="3" fill="none"/><circle cx="-5" cy="-6" r="5" fill="#e74c3c"/><circle cx="5" cy="-7" r="4" fill="#f4d35e"/><path d="M-16,-6 h32 l-5,20 h-22z" fill="#c8863a"/><path d="M-14,0 h28 M-12,7 h24" stroke="#a56a28" stroke-width="2"/></g>`;
      case 'axe': return `<g transform="${t}"><rect x="-3" y="-50" width="6" height="64" rx="3" fill="#8a5a33"/><path d="M2,-50 q22,4 20,22 q-10,-6 -20,-4z" fill="#b8c2cc"/></g>`;
      case 'broom': return `<g transform="${t}"><rect x="-2.5" y="-70" width="5" height="80" rx="2" fill="#a0703c"/><path d="M-10,8 h20 l8,30 h-36z" fill="#e8c46a"/><path d="M-12,16 h24" stroke="#b38b3a" stroke-width="2"/></g>`;
      case 'staff': return `<g transform="${t}"><path d="M0,40 V-70 q0,-18 14,-16 q10,2 8,14" stroke="#8a5a33" stroke-width="5" fill="none" stroke-linecap="round"/></g>`;
      case 'wand': return `<g transform="${t}"><path d="M0,0 L-14,-30" stroke="#e9d6ff" stroke-width="4" stroke-linecap="round"/><path d="${star5(11)}" transform="translate(-16 -36)" fill="#ffe066" stroke="#f5b700" stroke-width="1.5"/>${sparkle(-30, -50, 6)}${sparkle(-2, -52, 4)}</g>`;
      case 'book': return `<g transform="${t}"><rect x="-14" y="-12" width="28" height="22" rx="2" fill="#3a86ff"/><rect x="-12" y="-10" width="3" height="18" fill="#fff" opacity=".6"/></g>`;
      case 'apple': return `<g transform="${t}"><circle cx="0" cy="-4" r="10" fill="#e63946"/><path d="M0,-13 q2,-6 6,-7" stroke="#6b4226" stroke-width="2" fill="none"/><ellipse cx="5" cy="-17" rx="4" ry="2" fill="#57cc99"/><circle cx="-4" cy="-7" r="2.5" fill="#fff" opacity=".6"/></g>`;
      case 'flowers': return `<g transform="${t}"><path d="M0,10 L-6,-14 M0,10 L0,-18 M0,10 L6,-14" stroke="#4caf50" stroke-width="2.5"/><circle cx="-6" cy="-16" r="5" fill="#ff6fa5"/><circle cx="0" cy="-20" r="5" fill="#ffd166"/><circle cx="6" cy="-16" r="5" fill="#a29bfe"/></g>`;
      case 'letter': return `<g transform="${t}"><rect x="-14" y="-12" width="28" height="20" rx="2" fill="#fff8e7" stroke="#d4a373" stroke-width="1.5"/><path d="M-14,-12 l14,10 l14,-10" stroke="#d4a373" stroke-width="1.5" fill="none"/><circle cx="0" cy="-2" r="3.5" fill="#e63946"/></g>`;
      case 'violin': return `<g transform="${t} rotate(-30)"><path d="M0,-22 q9,0 8,9 q-4,4 0,8 q3,12 -8,13 q-11,-1 -8,-13 q4,-4 0,-8 q-1,-9 8,-9z" fill="#b5651d"/><rect x="-1.5" y="-40" width="3" height="20" fill="#5d3a1a"/><path d="M-14,-6 L16,10" stroke="#e9d8a6" stroke-width="2"/></g>`;
      case 'soup': return `<g transform="${t}"><path d="M-14,-6 h28 q0,14 -14,14 q-14,0 -14,-14z" fill="#fff"/><ellipse cx="0" cy="-6" rx="14" ry="3.5" fill="#f4a261"/><path d="M-4,-14 q-3,-5 0,-9 M4,-14 q-3,-5 0,-9" stroke="#fff" stroke-width="2" fill="none" opacity=".8"/></g>`;
      case 'coin': return `<g transform="${t}"><circle r="9" fill="#ffd24a" stroke="#e0a320" stroke-width="2"/><path d="M-3,-3 l3,-2 v10" stroke="#e0a320" stroke-width="2" fill="none"/></g>`;
      case 'beans': return `<g transform="${t}"><ellipse cx="-5" cy="-4" rx="4" ry="3" fill="#8bc34a"/><ellipse cx="3" cy="-6" rx="4" ry="3" fill="#cddc39"/><ellipse cx="0" cy="0" rx="4" ry="3" fill="#7cb342"/>${sparkle(10, -14, 6)}${sparkle(-12, -14, 4)}</g>`;
      case 'shell': return `<g transform="${t}"><path d="M0,6 L-12,-8 q12,-16 24,0z" fill="#ffc8dd"/><path d="M0,6 L-6,-12 M0,6 L0,-14 M0,6 L6,-12" stroke="#f48fb1" stroke-width="1.5"/>${sparkle(14, -16, 6)}</g>`;
      case 'slipper': return `<g transform="${t}"><path d="M-14,4 q0,-10 10,-10 q4,-8 10,-4 l8,14z" fill="#bde0fe" stroke="#fff" stroke-width="2" opacity=".95"/>${sparkle(16, -12, 6)}</g>`;
      case 'cheese': return `<g transform="${t}"><path d="M-10,4 L12,4 L12,-6 Z" fill="#ffd54f"/><path d="M-10,4 L12,-6 L4,-10Z" fill="#ffe082"/><circle cx="6" cy="0" r="1.8" fill="#f9a825"/></g>`;
      case 'drum': return `<g transform="${t}"><rect x="-13" y="-10" width="26" height="18" rx="3" fill="#e63946"/><ellipse cx="0" cy="-10" rx="13" ry="4" fill="#fff7e6"/><path d="M-13,-6 l26,12 M13,-6 l-26,12" stroke="#ffd166" stroke-width="2"/></g>`;
      case 'flag': return `<g transform="${t}"><rect x="-2" y="-46" width="4" height="56" fill="#8a5a33"/><path d="M2,-46 h26 l-6,9 l6,9 h-26z" fill="#e63946"/></g>`;
      case 'sack': return `<g transform="${t}"><path d="M-14,-18 q14,-6 28,0 q8,26 -2,30 h-24 q-10,-4 -2,-30z" fill="#c49a6c"/><path d="M-10,-16 q10,4 20,0" stroke="#8a6440" stroke-width="3" fill="none"/><circle cx="-3" cy="-22" r="5" fill="#e63946"/><circle cx="5" cy="-22" r="5" fill="#ff9f1c"/></g>`;
      default: return '';
    }
  }

  // ---------- personas ----------
  function person(o) {
    const skin = o.skin || '#f7cfa8', hair = o.hair || '#7a4a26', top = o.top || '#4f86d9', bottom = o.bottom || '#34508f';
    const dress = o.dress, sleeve = o.sleeve || o.bodice || dress || top, hs = o.hairStyle || 'short';
    let s = '';
    if (o.wings) s += `<g fill="${o.wings}" opacity=".75" stroke="#fff" stroke-width="2"><ellipse cx="-36" cy="-92" rx="30" ry="15" transform="rotate(-30 -36 -92)"/><ellipse cx="36" cy="-92" rx="30" ry="15" transform="rotate(30 36 -92)"/><ellipse cx="-30" cy="-66" rx="20" ry="10" transform="rotate(25 -30 -66)"/><ellipse cx="30" cy="-66" rx="20" ry="10" transform="rotate(-25 30 -66)"/></g>`;
    if (o.cape) s += `<path d="M-22,-98 L22,-98 L42,-6 Q0,2 -42,-6Z" fill="${o.cape}"/>`;
    if (o.hat === 'hood') s += `<path d="M-40,-126 Q-48,-60 -42,-30 Q0,-22 42,-30 Q48,-60 40,-126 Q0,-180 -40,-126Z" fill="${o.hood || '#d93a3a'}"/>`;
    if (hs === 'long') s += `<path d="M-33,-130 Q-44,-82 -36,-60 Q0,-54 36,-60 Q44,-82 33,-130Z" fill="${hair}"/>`;
    if (hs === 'curly') for (const [x, y] of [[-33, -118], [-38, -98], [-33, -78], [33, -118], [38, -98], [33, -78]]) s += `<circle cx="${x}" cy="${y}" r="13" fill="${hair}"/>`;
    if (hs === 'braids') for (const k of [-1, 1]) for (let i = 0; i < 4; i++) s += `<circle cx="${31 * k}" cy="${-112 + i * 13}" r="${8 - i}" fill="${hair}"/>`;
    // piernas o cola de sirena
    if (o.tail) {
      s += `<path d="M-20,-62 Q-26,-26 -6,-14 L-30,2 Q-8,4 0,-8 Q8,4 30,2 L6,-14 Q26,-26 20,-62Z" fill="${o.tail}"/>` +
        `<path d="M-14,-44 q6,4 12,0 M2,-44 q6,4 12,0 M-10,-30 q6,4 12,0" stroke="${shade(o.tail, -0.2)}" stroke-width="2" fill="none"/>`;
    } else if (!o.gown) {
      const lc = o.legs || (dress ? skin : bottom);
      s += `<rect x="-15" y="-42" width="11" height="40" rx="5" fill="${lc}"/><rect x="4" y="-42" width="11" height="40" rx="5" fill="${lc}"/>` +
        `<ellipse cx="-11" cy="-3" rx="10" ry="5.5" fill="${o.shoes || '#5b3a24'}"/><ellipse cx="11" cy="-3" rx="10" ry="5.5" fill="${o.shoes || '#5b3a24'}"/>`;
    }
    // cuerpo
    if (dress) {
      const w = o.gown ? 54 : 36, yb = o.gown ? -2 : -30;
      s += `<path d="M-18,-98 Q0,-102 18,-98 L${w},${yb} Q0,${yb + 8} -${w},${yb}Z" fill="${dress}"/>`;
      if (o.gown) s += `<path d="M-30,-40 Q0,-30 30,-40 M-44,-18 Q0,-6 44,-18" stroke="${shade(dress, 0.35)}" stroke-width="3" fill="none"/>`;
      if (o.bodice) s += `<path d="M-18,-98 Q0,-102 18,-98 L21,-74 Q0,-70 -21,-74Z" fill="${o.bodice}"/>`;
      if (o.apron) s += `<path d="M-14,-76 L14,-76 L22,${yb + 2} Q0,${yb + 7} -22,${yb + 2}Z" fill="${o.apron}"/>`;
      if (o.patches) s += `<rect x="-26" y="-48" width="10" height="9" fill="${o.patches}" transform="rotate(-10 -21 -44)"/><rect x="12" y="-62" width="9" height="8" fill="${o.patches}" transform="rotate(12 16 -58)"/>`;
      s += `<rect x="-18" y="-78" width="36" height="5" rx="2" fill="${shade(o.bodice || dress, -0.25)}"/>`;
    } else {
      if (!o.tail) s += `<rect x="-19" y="-64" width="38" height="28" rx="7" fill="${bottom}"/>`;
      s += `<rect x="-22" y="-100" width="44" height="${o.tail ? 40 : 44}" rx="15" fill="${top}"/>`;
      if (o.plaid) s += `<path d="M-22,-86 h44 M-22,-72 h44 M-8,-100 v44 M8,-100 v44" stroke="${shade(top, -0.3)}" stroke-width="2.5" opacity=".7"/>`;
      if (o.vest) s += `<path d="M-22,-96 Q-20,-70 -16,-58 L-6,-58 L-6,-98Z M22,-96 Q20,-70 16,-58 L6,-58 L6,-98Z" fill="${o.vest}"/>`;
      if (o.belt) s += `<rect x="-22" y="-62" width="44" height="6" fill="${o.belt}"/><rect x="-5" y="-63" width="10" height="8" rx="1" fill="#ffd24a"/>`;
      if (o.patches) s += `<rect x="6" y="-56" width="9" height="9" fill="${o.patches}" transform="rotate(10 10 -52)"/>`;
    }
    // brazos
    const hands = {};
    const arm = (k, pose) => {
      let d, hx, hy;
      if (pose === 'up') { d = `M${20 * k},-92 Q${38 * k},-110 ${36 * k},-134`; hx = 36 * k; hy = -136; }
      else if (pose === 'hold') { d = `M${20 * k},-92 Q${30 * k},-66 ${12 * k},-62`; hx = 12 * k; hy = -62; }
      else if (pose === 'out') { d = `M${20 * k},-92 Q${40 * k},-86 ${52 * k},-98`; hx = 54 * k; hy = -100; }
      else { d = `M${20 * k},-92 Q${32 * k},-74 ${30 * k},-56`; hx = 30 * k; hy = -54; }
      hands[k] = [hx, hy];
      return `<path d="${d}" stroke="${sleeve}" stroke-width="11" fill="none" stroke-linecap="round"/><circle cx="${hx}" cy="${hy}" r="6.5" fill="${skin}"/>`;
    };
    const pose = o.pose || 'down';
    const pl = pose === 'wave' || pose === 'point' ? 'down' : pose;
    const pr = pose === 'wave' ? 'up' : pose === 'point' ? 'out' : pose;
    s += arm(-1, pl) + arm(1, pr);
    // cabeza
    s += `<rect x="-6" y="-106" width="12" height="10" fill="${skin}"/>`;
    s += `<circle cx="-29" cy="-124" r="6" fill="${skin}"/><circle cx="29" cy="-124" r="6" fill="${skin}"/><circle cx="0" cy="-126" r="30" fill="${skin}"/>`;
    if (o.mask) s += `<rect x="-29" y="-136" width="58" height="13" rx="6" fill="#2d2d2d"/>`;
    s += face(-124, { mood: o.mood });
    if (o.nose) s += `<path d="M-1,-123 L${8 + o.nose},-119 Q${11 + o.nose},-117 ${8 + o.nose},-115 L-1,-111Z" fill="#d9925a" stroke="#b5703c" stroke-width="1.5"/>`;
    else s += `<ellipse cx="0" cy="-117" rx="3" ry="2.4" fill="${shade(skin, -0.12)}"/>`;
    if (o.beard) s += `<path d="M-27,-122 Q-28,-80 0,-74 Q28,-80 27,-122 Q16,-102 0,-106 Q-16,-102 -27,-122Z" fill="${o.beard}"/><path d="M-11,-110 Q0,-115 11,-110" stroke="${shade(o.beard, -0.2)}" stroke-width="4" fill="none" stroke-linecap="round"/>`;
    if (o.mustache) s += `<path d="M-14,-110 Q-6,-118 0,-112 Q6,-118 14,-110 Q6,-108 0,-110 Q-6,-108 -14,-110Z" fill="${o.mustache}"/>`;
    // pelo delantero
    const H = c => `fill="${c}"`;
    if (hs === 'bob') s += `<path d="M-33,-106 Q-38,-162 0,-162 Q38,-162 33,-106 L27,-110 Q28,-140 10,-144 Q0,-136 -10,-144 Q-28,-140 -27,-110Z" ${H(hair)}/>`;
    else if (hs === 'bald') s += `<ellipse cx="-27" cy="-136" rx="7" ry="10" ${H(hair)}/><ellipse cx="27" cy="-136" rx="7" ry="10" ${H(hair)}/>`;
    else if (hs !== 'none' && o.hat !== 'hood') {
      s += `<path d="M-31,-124 Q-34,-162 0,-160 Q34,-162 31,-124 Q26,-146 6,-146 Q-2,-138 -12,-146 Q-26,-144 -31,-124Z" ${H(hair)}/>`;
      if (hs === 'curly') s += `<circle cx="-20" cy="-150" r="13" ${H(hair)}/><circle cx="0" cy="-157" r="13" ${H(hair)}/><circle cx="20" cy="-150" r="13" ${H(hair)}/>`;
      if (hs === 'bun') s += `<circle cx="0" cy="-162" r="12" ${H(hair)}/>`;
      if (hs === 'spiky') s += `<path d="M-20,-150 l-4,-16 l12,8 l6,-14 l6,14 l12,-8 l-4,16z" ${H(hair)}/>`;
    }
    if (o.glasses) s += `<g fill="none" stroke="#6b4b2a" stroke-width="2"><circle cx="-11" cy="-126" r="8"/><circle cx="11" cy="-126" r="8"/><path d="M-3,-126h6"/></g>`;
    // sombreros
    switch (o.hat) {
      case 'crown': s += `<g transform="translate(0,-150)"><path d="M-18,0 L-20,-22 L-9,-11 L0,-26 L9,-11 L20,-22 L18,0Z" fill="#ffd24a" stroke="#e0a320" stroke-width="2" stroke-linejoin="round"/><circle cx="0" cy="-7" r="3.5" fill="#e84a5f"/></g>`; break;
      case 'tiara': s += `<g transform="translate(0,-152) scale(.7)"><path d="M-18,0 L-20,-16 L-9,-8 L0,-22 L9,-8 L20,-16 L18,0Z" fill="#ffe8a3" stroke="#e0a320" stroke-width="2"/><circle cx="0" cy="-6" r="4" fill="#7bdff2"/></g>`; break;
      case 'hood': s += `<path d="M-38,-120 Q-40,-172 0,-172 Q40,-172 38,-120 Q36,-158 0,-158 Q-36,-158 -38,-120Z" fill="${o.hood || '#d93a3a'}"/><path d="M-36,-124 Q-34,-160 0,-160 Q34,-160 36,-124 Q30,-150 0,-150 Q-30,-150 -36,-124Z" fill="${hair}"/>`; break;
      case 'cap': { const c = o.capColor || '#e63946'; s += `<path d="M-33,-138 Q0,-154 33,-138 L4,-200 Q0,-204 -4,-200Z" fill="${c}"/><rect x="-35" y="-146" width="70" height="11" rx="5" fill="${shade(c, -0.25)}"/>`; break; }
      case 'witch': s += `<ellipse cx="0" cy="-150" rx="48" ry="9" fill="${o.hatColor || '#2d1b4e'}"/><path d="M-24,-152 Q-8,-190 10,-214 Q8,-190 24,-152Z" fill="${o.hatColor || '#2d1b4e'}"/><rect x="-23" y="-160" width="46" height="7" fill="#8e44ad"/>`; break;
      case 'nightcap': s += `<path d="M-32,-136 Q-10,-182 30,-170 Q50,-160 44,-140 Q30,-158 32,-136Z" fill="#fff"/><circle cx="46" cy="-138" r="7" fill="#fff"/><path d="M-34,-136 Q0,-128 34,-136" stroke="#f0c8d8" stroke-width="7" fill="none" stroke-linecap="round"/>`; break;
      case 'straw': s += `<ellipse cx="0" cy="-148" rx="46" ry="10" fill="#e8c46a"/><path d="M-24,-150 Q-24,-178 0,-178 Q24,-178 24,-150Z" fill="#e8c46a"/><rect x="-24" y="-157" width="48" height="6" fill="#c0392b"/>`; break;
      case 'tophat': s += `<rect x="-20" y="-196" width="40" height="42" rx="4" fill="#2d2d2d"/><rect x="-32" y="-158" width="64" height="8" rx="4" fill="#2d2d2d"/><rect x="-20" y="-166" width="40" height="6" fill="#e63946"/>`; break;
      case 'feather': s += `<ellipse cx="0" cy="-150" rx="44" ry="10" fill="#5d3a8a"/><path d="M-22,-152 Q0,-182 22,-152Z" fill="#5d3a8a"/><path d="M14,-160 Q40,-196 50,-176 Q36,-170 16,-156Z" fill="#ff6b6b"/>`; break;
      case 'bow': s += `<g transform="translate(20,-150)" fill="${o.bowColor || '#ff6fa5'}"><path d="M0,0 L-14,-10 L-14,10Z M0,0 L14,-10 L14,10Z"/><circle r="4.5"/></g>`; break;
      case 'headband': s += `<path d="M-30,-136 Q0,-162 30,-136" stroke="${o.bowColor || '#e63946'}" stroke-width="6" fill="none"/><g transform="translate(22,-146)" fill="${o.bowColor || '#e63946'}"><path d="M0,0 L-12,-9 L-12,9Z M0,0 L12,-9 L12,9Z"/><circle r="4"/></g>`; break;
      case 'chef': s += `<rect x="-22" y="-160" width="44" height="14" fill="#fff"/><circle cx="-14" cy="-170" r="14" fill="#fff"/><circle cx="0" cy="-176" r="15" fill="#fff"/><circle cx="14" cy="-170" r="14" fill="#fff"/>`; break;
    }
    if (o.item) {
      const k = o.hand === 'left' ? -1 : 1, [hx, hy] = hands[k];
      s += held(o.item, hx + 4 * k, hy + 6, o);
    }
    return s;
  }

  // ---------- animales de pie ----------
  function critter(o) {
    const c = o.color || '#c9a27a', bel = o.belly || shade(c, 0.45), dk = shade(c, -0.25), inner = o.inner || '#f5a9b8';
    const hw = o.headW || 1, hy = -118;
    let s = '';
    switch (o.tail) {
      case 'curly': s += `<path d="M30,-46 q16,-4 12,-16 q-4,-10 -12,-2 q-4,6 4,8" stroke="${dk}" stroke-width="4" fill="none" stroke-linecap="round"/>`; break;
      case 'bushy': s += `<g transform="rotate(35 34 -40)"><ellipse cx="44" cy="-56" rx="15" ry="32" fill="${c}"/><ellipse cx="44" cy="-82" rx="9" ry="10" fill="${o.tailTip || bel}"/></g>`; break;
      case 'thin': s += `<path d="M28,-36 Q62,-34 58,-78" stroke="${o.tailColor || c}" stroke-width="5" fill="none" stroke-linecap="round"/>` + (o.tuft ? `<circle cx="58" cy="-82" r="8" fill="${o.tuft}"/>` : ''); break;
      case 'puff': s += `<circle cx="32" cy="-36" r="11" fill="${o.tailColor || '#fff'}"/>`; break;
    }
    if (o.mane) {
      for (let i = 0; i < 12; i++) { const a = i * Math.PI / 6; s += `<circle cx="${(Math.cos(a) * 42).toFixed(1)}" cy="${(hy + Math.sin(a) * 40).toFixed(1)}" r="17" fill="${o.mane}"/>`; }
      s += `<circle cx="0" cy="${hy}" r="44" fill="${o.mane}"/>`;
    }
    if (o.shell) s += `<ellipse cx="0" cy="-54" rx="48" ry="40" fill="${o.shell}"/><ellipse cx="0" cy="-54" rx="48" ry="40" fill="none" stroke="${shade(o.shell, -0.3)}" stroke-width="4"/><path d="M-44,-66 l10,6 M44,-66 l-10,6 M-40,-30 l10,-6 M40,-30 l-10,-6" stroke="${shade(o.shell, -0.3)}" stroke-width="4"/>`;
    // piernas
    const leg = o.boots || c;
    s += `<rect x="-22" y="-30" width="17" height="30" rx="8" fill="${leg}"/><rect x="5" y="-30" width="17" height="30" rx="8" fill="${leg}"/>`;
    if (o.boots) s += `<rect x="-25" y="-34" width="23" height="9" rx="3" fill="${shade(o.boots, -0.25)}"/><rect x="2" y="-34" width="23" height="9" rx="3" fill="${shade(o.boots, -0.25)}"/>`;
    // cuerpo
    if (o.wool) {
      for (const [x, y] of [[-24, -70], [0, -80], [24, -70], [-28, -46], [28, -46], [-14, -28], [14, -28], [0, -54]]) s += `<circle cx="${x}" cy="${y}" r="20" fill="${o.wool}"/>`;
    } else if (o.shell) {
      s += `<ellipse cx="0" cy="-52" rx="26" ry="30" fill="#f2e3a0"/><path d="M-22,-62 h44 M-24,-46 h48 M-20,-30 h40 M0,-80 v54" stroke="#d4c075" stroke-width="2.5"/>`;
    } else {
      s += `<ellipse cx="0" cy="-56" rx="${o.fat ? 40 : 33}" ry="38" fill="${c}"/>`;
      if (!o.noBelly) s += `<ellipse cx="0" cy="-50" rx="${o.fat ? 26 : 20}" ry="25" fill="${bel}"/>`;
    }
    if (o.spots) s += `<g fill="${o.spots}"><ellipse cx="-20" cy="-66" rx="9" ry="7"/><ellipse cx="22" cy="-44" rx="8" ry="10"/></g>`;
    if (o.shirt) s += `<path d="M-31,-78 Q0,-92 31,-78 L33,-46 Q0,-36 -33,-46Z" fill="${o.shirt}"/>`;
    if (o.overalls) s += `<path d="M-26,-70 h52 L30,-30 Q0,-20 -30,-30Z" fill="${o.overalls}"/><path d="M-20,-70 L-24,-90 M20,-70 L24,-90" stroke="${o.overalls}" stroke-width="6"/>`;
    if (o.dress) s += `<path d="M-26,-86 Q0,-94 26,-86 L42,-22 Q0,-12 -42,-22Z" fill="${o.dress}"/>`;
    if (o.apron) s += `<path d="M-16,-72 h32 l6,44 q-22,6 -44,0z" fill="${o.apron}"/>`;
    if (o.vest) s += `<path d="M-30,-80 Q-28,-56 -20,-44 L-8,-44 L-8,-88Z M30,-80 Q28,-56 20,-44 L8,-44 L8,-88Z" fill="${o.vest}"/>`;
    if (o.scarf) s += `<path d="M-26,-92 Q0,-80 26,-92 L26,-82 Q0,-70 -26,-82Z" fill="${o.scarf}"/><path d="M14,-84 l6,26 l8,-2 l-4,-26z" fill="${o.scarf}"/>`;
    if (o.bowtie) s += `<g transform="translate(0,-88)" fill="${o.bowtie}"><path d="M0,0 L-11,-7 L-11,7Z M0,0 L11,-7 L11,7Z"/><circle r="3.5"/></g>`;
    if (o.cape) s += `<path d="M-26,-90 L26,-90 L36,-20 L-36,-20Z" fill="${o.cape}" opacity=".95"/>`;
    // brazos
    const pose = o.pose || 'down', hands = {};
    const arm = (k, p) => {
      let d, px, py;
      if (p === 'up') { d = `M${26 * k},-76 Q${44 * k},-96 ${40 * k},-116`; px = 40 * k; py = -118; }
      else if (p === 'hold') { d = `M${26 * k},-74 Q${30 * k},-52 ${12 * k},-50`; px = 12 * k; py = -50; }
      else if (p === 'out') { d = `M${26 * k},-76 Q${46 * k},-74 ${56 * k},-84`; px = 58 * k; py = -86; }
      else { d = `M${26 * k},-74 Q${40 * k},-60 ${36 * k},-44`; px = 36 * k; py = -42; }
      hands[k] = [px, py];
      const ac = o.wool ? (o.color || '#555') : (o.sleeve || c);
      return `<path d="${d}" stroke="${ac}" stroke-width="13" fill="none" stroke-linecap="round"/><circle cx="${px}" cy="${py}" r="7.5" fill="${o.shell ? c : (o.paw || c)}"/>`;
    };
    const pl = pose === 'wave' || pose === 'point' ? 'down' : pose, pr = pose === 'wave' ? 'up' : pose === 'point' ? 'out' : pose;
    s += arm(-1, pl) + arm(1, pr);
    // orejas
    const ear = k => {
      switch (o.ears) {
        case 'pointy': return `<path d="M${-30 * k},-130 L${-26 * k},-172 L${-6 * k},-148Z" fill="${c}"/><path d="M${-25 * k},-138 L${-23 * k},-162 L${-12 * k},-149Z" fill="${inner}"/>`;
        case 'round': return `<circle cx="${-26 * k * hw}" cy="-146" r="13" fill="${c}"/><circle cx="${-26 * k * hw}" cy="-146" r="7" fill="${dk}"/>`;
        case 'big': return `<circle cx="${-30 * k}" cy="-144" r="21" fill="${c}"/><circle cx="${-30 * k}" cy="-144" r="13" fill="${inner}"/>`;
        case 'long': return `<ellipse cx="${-13 * k}" cy="-172" rx="9" ry="30" fill="${c}" transform="rotate(${-8 * k} ${-13 * k} -172)"/><ellipse cx="${-13 * k}" cy="-170" rx="4" ry="21" fill="${inner}" transform="rotate(${-8 * k} ${-13 * k} -172)"/>`;
        case 'donkey': return `<ellipse cx="${-24 * k}" cy="-162" rx="8" ry="27" fill="${c}" transform="rotate(${-25 * k} ${-24 * k} -162)"/><ellipse cx="${-24 * k}" cy="-160" rx="3.5" ry="18" fill="${dk}" transform="rotate(${-25 * k} ${-24 * k} -162)"/>`;
        case 'floppy': return `<ellipse cx="${-35 * k}" cy="-112" rx="10" ry="23" fill="${o.earColor || dk}" transform="rotate(${15 * k} ${-35 * k} -112)"/>`;
        case 'pig': return `<path d="M${-30 * k},-134 L${-32 * k},-162 L${-12 * k},-146Z" fill="${c}"/><path d="M${-27 * k},-139 L${-28 * k},-154 L${-17 * k},-146Z" fill="${inner}"/>`;
        case 'cow': return `<ellipse cx="${-40 * k}" cy="-128" rx="13" ry="7" fill="${c}" transform="rotate(${-15 * k} ${-40 * k} -128)"/>`;
        default: return '';
      }
    };
    s += ear(1) + ear(-1);
    if (o.horns) s += [1, -1].map(k => `<path d="M${-14 * k},-144 Q${-22 * k},-168 ${-6 * k},-176" stroke="#e9dcc0" stroke-width="7" fill="none" stroke-linecap="round"/>`).join('');
    // cabeza
    s += `<ellipse cx="0" cy="${hy}" rx="${34 * hw}" ry="32" fill="${c}"/>`;
    if (o.mask) s += `<path d="M-32,-114 Q-16,-100 0,-106 Q16,-100 32,-114 Q22,-88 0,-86 Q-22,-88 -32,-114Z" fill="${bel}"/>`;
    if (o.headSpot) s += `<ellipse cx="14" cy="-136" rx="9" ry="7" fill="${o.headSpot}"/>`;
    if (o.stripes) s += `<path d="M-6,-150 v10 M0,-150 v12 M6,-150 v10" stroke="${dk}" stroke-width="3" stroke-linecap="round"/>`;
    switch (o.snout) {
      case 'pig':
        s += face(-124, { gap: 13, my: 30, mood: o.mood });
        s += `<ellipse cx="0" cy="-108" rx="15" ry="11" fill="${o.snoutC || shade(c, -0.08)}"/><ellipse cx="-5" cy="-108" rx="3" ry="4" fill="${dk}"/><ellipse cx="5" cy="-108" rx="3" ry="4" fill="${dk}"/>`;
        break;
      case 'muzzle': case 'long': {
        const L = o.snout === 'long';
        s += face(-126, { gap: 13, noMouth: true, mood: o.mood });
        s += `<ellipse cx="0" cy="${L ? -102 : -105}" rx="${L ? 16 : 17}" ry="${L ? 15 : 12}" fill="${o.snoutC || bel}"/><ellipse cx="0" cy="${L ? -110 : -110}" rx="7" ry="5" fill="${INK}"/>`;
        const my = L ? -96 : -99;
        if (o.mood === 'laugh' || o.mood === 'sing') s += `<path d="M-8,${my - 2} q8,12 16,0z" fill="#7a2e22"/>`;
        else if (o.mood === 'surprised') s += `<ellipse cx="0" cy="${my + 1}" rx="4" ry="5" fill="#7a2e22"/>`;
        else if (o.mood === 'blow') s += `<circle cx="0" cy="${my}" r="5" fill="#7a2e22"/>`;
        else if (o.mood === 'sad') s += `<path d="M-6,${my + 3} q6,-5 12,0" stroke="${INK}" stroke-width="2.2" fill="none" stroke-linecap="round"/>`;
        else s += `<path d="M0,${my - 5} v4 M-7,${my - 1} q7,6 14,0" stroke="${INK}" stroke-width="2.2" fill="none" stroke-linecap="round"/>`;
        if (o.teeth) s += `<path d="M-6,${my} l3,5 l3,-5 M1,${my} l3,5 l3,-5" fill="#fff" stroke="#ddd" stroke-width=".8"/>`;
        break;
      }
      case 'small':
        s += face(-124, { gap: 13, noMouth: true, mood: o.mood });
        s += `<path d="M-5,-113 h10 l-5,6z" fill="${o.noseC || inner}"/>`;
        if (o.mood === 'laugh' || o.mood === 'sing') s += `<path d="M-7,-104 q7,10 14,0z" fill="#7a2e22"/>`;
        else if (o.mood === 'surprised') s += `<ellipse cx="0" cy="-102" rx="4" ry="5" fill="#7a2e22"/>`;
        else if (o.mood === 'sad') s += `<path d="M-6,-100 q6,-5 12,0" stroke="${INK}" stroke-width="2" fill="none"/>`;
        else s += `<path d="M0,-107 v3 M-6,-103 q3,4 6,-1 q3,5 6,1" stroke="${INK}" stroke-width="2" fill="none" stroke-linecap="round"/>`;
        s += `<g stroke="${dk}" stroke-width="1.6" opacity=".8"><path d="M-12,-110 h-18 M-12,-106 l-17,5 M12,-110 h18 M12,-106 l17,5"/></g>`;
        break;
      default:
        s += face(-122, { gap: 13, mood: o.mood });
    }
    if (o.glasses) s += `<g fill="none" stroke="#6b4b2a" stroke-width="2"><circle cx="-13" cy="-128" r="9"/><circle cx="13" cy="-128" r="9"/><path d="M-4,-128h8"/></g>`;
    switch (o.hat) {
      case 'straw': s += `<ellipse cx="0" cy="-146" rx="40" ry="9" fill="#e8c46a"/><path d="M-22,-148 Q-22,-172 0,-172 Q22,-172 22,-148Z" fill="#e8c46a"/><rect x="-22" y="-154" width="44" height="6" fill="${o.band || '#c0392b'}"/>`; break;
      case 'cap': s += `<path d="M-30,-140 Q0,-170 30,-140Z" fill="${o.capColor || '#3a86ff'}"/><path d="M14,-142 h26 q4,4 -4,6 h-22z" fill="${o.capColor || '#3a86ff'}"/>`; break;
      case 'feather': s += `<ellipse cx="0" cy="-148" rx="42" ry="9" fill="#5d3a8a"/><path d="M-22,-150 Q0,-178 22,-150Z" fill="#5d3a8a"/><path d="M12,-158 Q42,-196 52,-174 Q36,-168 16,-154Z" fill="#ff6b6b"/>`; break;
      case 'tophat': s += `<rect x="-17" y="-186" width="34" height="34" rx="3" fill="#2d2d2d"/><rect x="-28" y="-156" width="56" height="7" rx="3" fill="#2d2d2d"/><rect x="-17" y="-163" width="34" height="5" fill="#e63946"/>`; break;
      case 'bow': s += `<g transform="translate(22,-146)" fill="${o.bowColor || '#e84393'}"><path d="M0,0 L-15,-10 L-15,10Z M0,0 L15,-10 L15,10Z"/><circle r="5"/></g>`; break;
      case 'nightcap': s += `<path d="M-30,-140 Q-10,-184 30,-172 Q50,-162 44,-142 Q30,-160 30,-140Z" fill="#fff"/><circle cx="46" cy="-140" r="7" fill="#fff"/><path d="M-32,-140 Q0,-130 32,-140" stroke="#f0c8d8" stroke-width="7" fill="none" stroke-linecap="round"/>`; break;
      case 'crown': s += `<g transform="translate(0,-146)"><path d="M-16,0 L-18,-20 L-8,-10 L0,-24 L8,-10 L18,-20 L16,0Z" fill="#ffd24a" stroke="#e0a320" stroke-width="2"/></g>`; break;
      case 'bonnet': s += `<path d="M-38,-112 Q-44,-170 0,-170 Q44,-170 38,-112 Q30,-150 0,-152 Q-30,-150 -38,-112Z" fill="${o.bonnet || '#b28dff'}"/>`; break;
      case 'medal': s += `<path d="M-8,-92 L0,-70 L8,-92" stroke="#e63946" stroke-width="5" fill="none"/><circle cx="0" cy="-66" r="9" fill="#ffd24a" stroke="#e0a320" stroke-width="2"/>`; break;
    }
    if (o.item) { const k = o.hand === 'left' ? -1 : 1, [px, py] = hands[k]; s += held(o.item, px + 3 * k, py + 6, o); }
    return s;
  }

  // ---------- aves (miran a la derecha) ----------
  function bird(o) {
    const c = o.color || '#fff', dk = shade(c, -0.18), bk = o.beak || '#f59e2a';
    let s = '';
    if (!o.swim) s += `<path d="M-8,-22 V-2 M8,-22 V-2" stroke="${bk}" stroke-width="4" stroke-linecap="round"/><path d="M-16,-2 h14 M0,-2 h14" stroke="${bk}" stroke-width="4" stroke-linecap="round"/>`;
    if (o.tailFeathers) s += `<path d="M-30,-50 Q-70,-100 -56,-40Z" fill="#2a9d8f"/><path d="M-32,-48 Q-80,-76 -54,-30Z" fill="#e9c46a"/><path d="M-32,-46 Q-64,-50 -46,-24Z" fill="#e76f51"/>`;
    else s += `<path d="M-34,-52 L-58,-72 L-52,-40Z" fill="${dk}"/>`;
    s += `<ellipse cx="0" cy="-44" rx="40" ry="28" fill="${c}"/>`;
    s += o.flap
      ? `<path d="M-20,-56 Q-14,-110 24,-104 Q8,-70 -20,-56Z" fill="${dk}"/>`
      : `<path d="M-24,-50 Q-2,-68 18,-48 Q0,-30 -24,-50Z" fill="${dk}"/>`;
    let hx = 26, hy = -80, hr = 21;
    if (o.long) { s += `<path d="M24,-56 Q46,-90 28,-118" stroke="${c}" stroke-width="16" fill="none" stroke-linecap="round"/>`; hx = 32; hy = -126; hr = 16; }
    if (o.scarf) s += `<path d="M10,-62 q18,6 30,-6 l0,8 q-14,10 -30,4z" fill="${o.scarf}"/>`;
    s += `<circle cx="${hx}" cy="${hy}" r="${hr}" fill="${c}"/>`;
    if (o.comb) s += `<path d="M${hx - 10},${hy - hr + 4} q2,-16 8,-6 q4,-14 8,-2 q8,-10 6,6Z" fill="${o.comb}"/><ellipse cx="${hx + 15}" cy="${hy + 14}" rx="4" ry="6" fill="${o.comb}"/>`;
    if (o.bonnet) s += `<path d="M${hx - 20},${hy + 6} Q${hx - 24},${hy - 32} ${hx + 4},${hy - 28} Q${hx + 26},${hy - 22} ${hx + 18},${hy - 8} Q${hx + 4},${hy - 22} ${hx - 20},${hy + 6}Z" fill="${o.bonnet}"/>`;
    if (o.crown) s += `<g transform="translate(${hx},${hy - hr + 2}) scale(.6)"><path d="M-16,0 L-18,-20 L-8,-10 L0,-24 L8,-10 L18,-20 L16,0Z" fill="#ffd24a" stroke="#e0a320" stroke-width="3"/></g>`;
    if (o.mood === 'sing' || o.mood === 'laugh') s += `<path d="M${hx + 12},${hy - 3} L${hx + 34},${hy - 10} L${hx + 16},${hy}Z M${hx + 12},${hy + 3} L${hx + 32},${hy + 12} L${hx + 14},${hy + 7}Z" fill="${bk}"/>`;
    else if (o.beakType === 'flat') s += `<ellipse cx="${hx + hr}" cy="${hy + 4}" rx="13" ry="6" fill="${bk}"/>`;
    else s += `<path d="M${hx + hr - 6},${hy - 4} L${hx + hr + 14},${hy + 3} L${hx + hr - 6},${hy + 8}Z" fill="${bk}"/>`;
    if (o.mood === 'sleep') s += `<path d="M${hx + 1},${hy - 4} q4,4 8,0" stroke="${INK}" stroke-width="2.5" fill="none" stroke-linecap="round"/>`;
    else s += `<circle cx="${hx + 5}" cy="${hy - 4}" r="4" fill="#2b1d16"/><circle cx="${hx + 6.5}" cy="${hy - 5.5}" r="1.4" fill="#fff"/>`;
    if (o.mood === 'sad') s += `<path d="M${hx},${hy - 12} l10,3" stroke="${INK}" stroke-width="2" stroke-linecap="round"/><path d="M${hx + 6},${hy + 2} q-2,6 0,8 q2,-2 0,-8z" fill="#74c0fc"/>`;
    s += `<circle cx="${hx + 1}" cy="${hy + 7}" r="5" fill="#ff8a8a" opacity=".45"/>`;
    if (o.item === 'cheese') s += held('cheese', hx + hr + 10, hy + 10);
    if (o.swim) s += `<path d="M-46,-24 Q-30,-30 -16,-24 T14,-24 T44,-24" stroke="#fff" stroke-width="3" fill="none" opacity=".7"/>`;
    return s;
  }

  // ---------- insectos de pie ----------
  function insect(o) {
    const c = o.color || '#7cc242', dk = shade(c, -0.3);
    let s = '';
    if (o.wings) s += `<g fill="#e0f7fa" opacity=".7" stroke="#b2ebf2" stroke-width="1.5"><ellipse cx="-24" cy="-66" rx="12" ry="32" transform="rotate(-20 -24 -66)"/><ellipse cx="24" cy="-66" rx="12" ry="32" transform="rotate(20 24 -66)"/></g>`;
    s += `<path d="M-8,-26 L-14,-2 M8,-26 L14,-2" stroke="${dk}" stroke-width="5" stroke-linecap="round"/><path d="M-20,-2 h10 M10,-2 h10" stroke="${dk}" stroke-width="5" stroke-linecap="round"/>`;
    s += `<ellipse cx="0" cy="-32" rx="20" ry="17" fill="${c}"/><ellipse cx="0" cy="-60" rx="14" ry="15" fill="${c}"/>`;
    if (o.scarf) s += `<path d="M-14,-72 Q0,-64 14,-72 L14,-64 Q0,-56 -14,-64Z" fill="${o.scarf}"/>`;
    if (o.vest) s += `<path d="M-14,-70 Q0,-62 14,-70 L12,-48 Q0,-44 -12,-48Z" fill="${o.vest}"/>`;
    const p = o.pose || 'down';
    s += p === 'up'
      ? `<path d="M-12,-64 Q-28,-76 -30,-94 M12,-64 Q28,-76 30,-94" stroke="${dk}" stroke-width="5" fill="none" stroke-linecap="round"/>`
      : `<path d="M-12,-64 Q-28,-56 -28,-40 M12,-64 Q26,-60 30,-48" stroke="${dk}" stroke-width="5" fill="none" stroke-linecap="round"/>`;
    s += `<path d="M-6,-108 Q-14,-128 -24,-132 M6,-108 Q14,-128 24,-132" stroke="${dk}" stroke-width="3" fill="none"/><circle cx="-24" cy="-132" r="4" fill="${dk}"/><circle cx="24" cy="-132" r="4" fill="${dk}"/>`;
    s += `<circle cx="0" cy="-92" r="22" fill="${c}"/>` + face(-92, { gap: 8, mood: o.mood, my: 11 });
    if (o.hat === 'tophat') s += `<rect x="-12" y="-138" width="24" height="24" rx="3" fill="#2d2d2d"/><rect x="-20" y="-117" width="40" height="6" rx="3" fill="#2d2d2d"/><rect x="-12" y="-122" width="24" height="4" fill="#ffd166"/>`;
    if (o.hat === 'cap') s += `<path d="M-20,-104 Q0,-128 20,-104Z" fill="${o.capColor || '#e63946'}"/>`;
    if (o.item) s += held(o.item, p === 'up' ? 30 : 30, p === 'up' ? -90 : -42, o);
    return s;
  }

  function frog(o) {
    const c = o.color || '#6abf4b';
    return `<ellipse cx="-26" cy="-6" rx="16" ry="7" fill="${shade(c, -0.15)}"/><ellipse cx="26" cy="-6" rx="16" ry="7" fill="${shade(c, -0.15)}"/>` +
      `<ellipse cx="0" cy="-34" rx="40" ry="32" fill="${c}"/><ellipse cx="0" cy="-26" rx="26" ry="20" fill="${shade(c, 0.5)}"/>` +
      `<circle cx="-18" cy="-62" r="14" fill="${c}"/><circle cx="18" cy="-62" r="14" fill="${c}"/><circle cx="-18" cy="-63" r="7" fill="#fff"/><circle cx="18" cy="-63" r="7" fill="#fff"/><circle cx="-17" cy="-63" r="4" fill="#2b1d16"/><circle cx="19" cy="-63" r="4" fill="#2b1d16"/>` +
      `<path d="M-16,-44 q16,12 32,0" stroke="${INK}" stroke-width="2.5" fill="none" stroke-linecap="round"/><g fill="#ff8a8a" opacity=".45"><circle cx="-26" cy="-46" r="5"/><circle cx="26" cy="-46" r="5"/></g>` +
      (o.crown ? `<g transform="translate(0,-74) scale(.7)"><path d="M-16,0 L-18,-20 L-8,-10 L0,-24 L8,-10 L18,-20 L16,0Z" fill="#ffd24a" stroke="#e0a320" stroke-width="3"/></g>` : '');
  }

  function fish(o) {
    const c = o.color || '#ffb703';
    return `<path d="M-24,-30 L-48,-46 L-48,-14Z" fill="${shade(c, -0.15)}"/><ellipse cx="0" cy="-30" rx="30" ry="19" fill="${c}"/><path d="M-6,-48 q8,-10 16,2z" fill="${shade(c, -0.15)}"/>` +
      `<path d="M-6,-40 q-4,10 0,20 M4,-42 q-4,12 0,24" stroke="${shade(c, 0.3)}" stroke-width="3" fill="none"/><circle cx="14" cy="-34" r="5" fill="#fff"/><circle cx="15" cy="-34" r="3" fill="#2b1d16"/><path d="M18,-24 q5,3 9,-2" stroke="${INK}" stroke-width="2" fill="none" stroke-linecap="round"/>`;
  }

  function crab(o) {
    const c = o.color || '#ef476f';
    return `<path d="M-30,-20 l-14,16 M-22,-16 l-10,16 M30,-20 l14,16 M22,-16 l10,16" stroke="${shade(c, -0.2)}" stroke-width="4" stroke-linecap="round"/>` +
      `<path d="M-30,-36 Q-50,-50 -46,-66 M30,-36 Q50,-50 46,-66" stroke="${c}" stroke-width="6" fill="none"/><path d="M-46,-66 q-12,-6 -6,-16 q6,4 12,0 q2,10 -6,16z M46,-66 q12,-6 6,-16 q-6,4 -12,0 q-2,10 6,16z" fill="${c}"/>` +
      `<ellipse cx="0" cy="-28" rx="34" ry="20" fill="${c}"/><path d="M-10,-46 v-14 M10,-46 v-14" stroke="${c}" stroke-width="4"/><circle cx="-10" cy="-62" r="7" fill="#fff"/><circle cx="10" cy="-62" r="7" fill="#fff"/><circle cx="-9" cy="-62" r="3.5" fill="#2b1d16"/><circle cx="11" cy="-62" r="3.5" fill="#2b1d16"/>` +
      `<path d="M-8,-24 q8,7 16,0" stroke="${INK}" stroke-width="2.5" fill="none" stroke-linecap="round"/>`;
  }

  function octopus(o) {
    const c = o.color || '#b185db';
    let s = '';
    for (let i = 0; i < 6; i++) { const x = -40 + i * 16; s += `<path d="M${x},-40 Q${x + (i < 3 ? -10 : 10)},-10 ${x + (i < 3 ? -18 : 18)},-6 q${i < 3 ? -8 : 8},-2 ${i < 3 ? -6 : 6},-10" stroke="${c}" stroke-width="12" fill="none" stroke-linecap="round"/>`; }
    s += `<ellipse cx="0" cy="-74" rx="44" ry="46" fill="${c}"/><g fill="${shade(c, 0.4)}"><circle cx="-26" cy="-96" r="6"/><circle cx="24" cy="-104" r="5"/></g>` + face(-72, { gap: 14, mood: o.mood });
    if (o.glasses) s += `<g fill="none" stroke="#6b4b2a" stroke-width="2.5"><circle cx="-14" cy="-74" r="9"/><circle cx="14" cy="-74" r="9"/><path d="M-5,-74h10"/></g>`;
    if (o.crown) s += `<g transform="translate(0,-116)"><path d="M-16,0 L-18,-20 L-8,-10 L0,-24 L8,-10 L18,-20 L16,0Z" fill="#ffd24a" stroke="#e0a320" stroke-width="2"/></g>`;
    return s;
  }

  // ---------- objetos y escenografía ----------
  function house(o) {
    const k = o.kind || 'cottage';
    const P = {
      straw: { wall: '#f2d16b', roof: '#e3b94a', door: '#c58b3c' },
      sticks: { wall: '#b98150', roof: '#8d5a2b', door: '#6b4226' },
      brick: { wall: '#cf5c44', roof: '#7a3b2e', door: '#5b3a24' },
      cottage: { wall: o.wall || '#fff3dc', roof: o.roof || '#e0604a', door: o.door || '#4f86d9' },
      cabin: { wall: '#c8915a', roof: '#7d4f2a', door: '#5b3a24' },
      candy: { wall: '#f9c5d1', roof: '#c77dff', door: '#8d5a2b' },
    }[k];
    let s = '';
    if (k === 'brick' || k === 'cottage' || k === 'cabin') s += `<rect x="40" y="-200" width="24" height="50" fill="${shade(P.roof, -0.1)}"/>` + (o.smoke ? `<g fill="#fff" opacity=".7" class="a-float"><circle cx="54" cy="-214" r="9"/><circle cx="62" cy="-232" r="12"/><circle cx="56" cy="-254" r="14"/></g>` : '');
    s += `<rect x="-80" y="-112" width="160" height="112" fill="${P.wall}"/>`;
    if (k === 'straw') { let r = rng(7); for (let i = 0; i < 40; i++) { const x = -76 + r() * 150, y = -108 + r() * 100; s += `<path d="M${x.toFixed(0)},${y.toFixed(0)} l${(r() * 6 - 3).toFixed(0)},12" stroke="#d6aa3c" stroke-width="2"/>`; } }
    if (k === 'sticks' || k === 'cabin') for (let x = -72; x < 80; x += 16) s += `<path d="M${x},-110 v108" stroke="${shade(P.wall, -0.25)}" stroke-width="3"/>`;
    if (k === 'brick') for (let y = -100, i = 0; y < 0; y += 14, i++) { s += `<path d="M-80,${y} h160" stroke="#a8432f" stroke-width="2"/>`; for (let x = -80 + (i % 2) * 14; x < 80; x += 28) s += `<path d="M${x},${y} v14" stroke="#a8432f" stroke-width="2"/>`; }
    if (k === 'candy') s += `<g fill="#fff"><circle cx="-56" cy="-90" r="8"/><circle cx="56" cy="-30" r="8"/></g>`;
    s += `<path d="M-100,-106 L0,-188 L100,-106Z" fill="${P.roof}"/>`;
    if (k === 'straw') s += `<path d="M-70,-120 L0,-178 M-40,-110 L0,-150 M70,-120 L0,-178 M40,-110 L0,-150" stroke="#c9a03c" stroke-width="3"/>`;
    if (k === 'cottage') s += `<path d="M-100,-106 L100,-106" stroke="${shade(P.roof, -0.25)}" stroke-width="6"/>`;
    s += `<path d="M-20,0 V-50 Q-20,-70 0,-70 Q20,-70 20,-50 V0Z" fill="${P.door}"/><circle cx="12" cy="-34" r="3" fill="#ffd24a"/>`;
    const win = o.lit ? '#ffe08a' : '#bfe6ff';
    s += `<rect x="36" y="-88" width="30" height="28" rx="3" fill="${win}" stroke="#fff" stroke-width="4"/><path d="M51,-88 v28 M36,-74 h30" stroke="#fff" stroke-width="3"/>`;
    s += `<rect x="-66" y="-88" width="30" height="28" rx="3" fill="${win}" stroke="#fff" stroke-width="4"/><path d="M-51,-88 v28 M-66,-74 h30" stroke="#fff" stroke-width="3"/>`;
    if (k === 'cottage') s += `<rect x="-70" y="-60" width="38" height="8" fill="#8d5a2b"/><g fill="#ff6fa5"><circle cx="-62" cy="-62" r="4"/><circle cx="-51" cy="-63" r="4"/><circle cx="-40" cy="-62" r="4"/></g>`;
    return s;
  }

  function tree(o) {
    const g = o.color || '#5cb85c', g2 = shade(g, 0.2), t = o.trunk || '#8a5a33';
    if (o.kind === 'pine') return `<rect x="-9" y="-40" width="18" height="40" fill="${t}"/><path d="M-60,-30 L0,-120 L60,-30Z M-50,-80 L0,-170 L50,-80Z M-38,-130 L0,-210 L38,-130Z" fill="${o.color || '#2f7d4f'}"/>` + (o.snow ? `<path d="M-30,-120 L0,-170 L30,-120 Q0,-130 -30,-120Z M-20,-176 L0,-210 L20,-176 Q0,-184 -20,-176Z" fill="#fff"/>` : '');
    if (o.kind === 'bare') return `<path d="M-12,0 L-8,-110 L8,-110 L12,0Z" fill="${t}"/><path d="M-4,-100 L-40,-150 M-6,-80 L-50,-110 M4,-96 L40,-160 M6,-70 L48,-100 M0,-110 L4,-170" stroke="${t}" stroke-width="8" stroke-linecap="round"/>` + (o.snow ? `<path d="M-44,-152 l10,4 M38,-160 l10,2" stroke="#fff" stroke-width="6" stroke-linecap="round"/>` : '');
    let s = `<path d="M-13,0 L-9,-96 L9,-96 L13,0Z" fill="${t}"/><path d="M0,-70 L-26,-96 M2,-80 L24,-104" stroke="${t}" stroke-width="7" stroke-linecap="round"/>`;
    s += `<g fill="${g}"><circle cx="0" cy="-146" r="56"/><circle cx="-44" cy="-114" r="40"/><circle cx="44" cy="-114" r="40"/><circle cx="-22" cy="-180" r="38"/><circle cx="26" cy="-178" r="36"/></g>`;
    s += `<g fill="${g2}" opacity=".6"><circle cx="-20" cy="-170" r="18"/><circle cx="30" cy="-140" r="14"/><circle cx="-46" cy="-122" r="12"/></g>`;
    if (o.fruit) s += `<g fill="${o.fruit}"><circle cx="-30" cy="-130" r="7"/><circle cx="20" cy="-160" r="7"/><circle cx="40" cy="-110" r="7"/><circle cx="-6" cy="-112" r="7"/></g>`;
    if (o.flowers) s += `<g fill="${o.flowers}"><circle cx="-30" cy="-140" r="5"/><circle cx="20" cy="-170" r="5"/><circle cx="44" cy="-118" r="5"/><circle cx="-8" cy="-118" r="5"/><circle cx="0" cy="-186" r="5"/></g>`;
    return s;
  }

  const bush = o => `<g fill="${o.color || '#4caf50'}"><circle cx="-24" cy="-20" r="22"/><circle cx="24" cy="-20" r="22"/><circle cx="0" cy="-34" r="26"/></g>` + (o.flowers ? `<g fill="${o.flowers}"><circle cx="-20" cy="-30" r="5"/><circle cx="10" cy="-44" r="5"/><circle cx="26" cy="-22" r="5"/><circle cx="-4" cy="-16" r="5"/></g>` : '');
  const flower = o => `<path d="M0,0 V-30" stroke="#4caf50" stroke-width="3"/><ellipse cx="-8" cy="-12" rx="7" ry="3" fill="#66bb6a"/><g fill="${o.color || '#ff6fa5'}"><circle cx="0" cy="-40" r="7"/><circle cx="-9" cy="-33" r="7"/><circle cx="9" cy="-33" r="7"/><circle cx="-5" cy="-24" r="7"/><circle cx="5" cy="-24" r="7"/></g><circle cx="0" cy="-31" r="5" fill="#ffd166"/>`;
  const mushroom = o => `<rect x="-8" y="-24" width="16" height="24" rx="6" fill="#fff3e0"/><path d="M-26,-22 Q-26,-52 0,-52 Q26,-52 26,-22Z" fill="${o.color || '#e63946'}"/><g fill="#fff"><circle cx="-12" cy="-36" r="4"/><circle cx="6" cy="-42" r="4"/><circle cx="14" cy="-30" r="3"/></g>`;
  const rock = o => `<path d="M-40,0 Q-44,-30 -16,-40 Q10,-50 30,-30 Q46,-14 40,0Z" fill="${o.color || '#a0a4ad'}"/><path d="M-20,-30 Q-4,-38 10,-34" stroke="#fff" stroke-width="3" opacity=".4" fill="none"/>`;
  const fence = o => { let s = ''; const w = o.w || 220; for (let x = -w / 2; x <= w / 2; x += 36) s += `<path d="M${x - 6},0 V-50 l6,-8 l6,8 V0Z" fill="#fff4e0" stroke="#d9b98c" stroke-width="2"/>`; return `<rect x="${-w / 2 - 6}" y="-40" width="${w + 12}" height="8" fill="#f0dcc0"/><rect x="${-w / 2 - 6}" y="-20" width="${w + 12}" height="8" fill="#f0dcc0"/>` + s; };
  const bed = o => { const c = o.color || '#7ec8e3'; return `<rect x="-80" y="-110" width="16" height="110" rx="6" fill="#a0703c"/><rect x="64" y="-70" width="16" height="70" rx="6" fill="#a0703c"/><rect x="-80" y="-110" width="16" height="40" rx="8" fill="#b9854b"/><rect x="-66" y="-56" width="132" height="30" rx="8" fill="#fff"/><ellipse cx="-40" cy="-60" rx="24" ry="12" fill="#fff" stroke="#eee"/><path d="M-30,-62 h98 v36 h-98z" fill="${c}"/><path d="M-30,-48 h98" stroke="${shade(c, 0.4)}" stroke-width="4"/><rect x="-70" y="-26" width="140" height="8" fill="#8a5a33"/>`; };
  const chair = o => { const c = o.color || '#c58b4f'; return `<rect x="-22" y="-80" width="8" height="80" rx="3" fill="${c}"/><rect x="14" y="-36" width="8" height="36" rx="3" fill="${c}"/><rect x="-22" y="-80" width="8" height="44" fill="${shade(c, -0.1)}"/><rect x="-26" y="-42" width="52" height="10" rx="4" fill="${shade(c, 0.15)}"/>` + (o.broken ? `<path d="M-10,-30 l8,10 l-6,6 M10,-20 l-4,8" stroke="#5b3a24" stroke-width="3"/>` : '') + (o.cushion ? `<rect x="-24" y="-48" width="48" height="8" rx="4" fill="${o.cushion}"/>` : ''); };
  const table = o => { const w = o.w || 180, c = o.color || '#b07a45'; let s = `<rect x="${-w / 2 + 12}" y="-60" width="12" height="60" fill="${shade(c, -0.15)}"/><rect x="${w / 2 - 24}" y="-60" width="12" height="60" fill="${shade(c, -0.15)}"/>`; if (o.cloth) s += `<path d="M${-w / 2 - 4},-66 h${w + 8} l-6,30 h${-w + 4}z" fill="${o.cloth}"/><path d="M${-w / 2},-50 h${w}" stroke="#fff" stroke-width="3" stroke-dasharray="8 8" opacity=".7"/>`; else s += `<rect x="${-w / 2}" y="-70" width="${w}" height="12" rx="4" fill="${c}"/>`; return s + (o.food ? food(w) : ''); };
  const food = w => `<g transform="translate(0,-68)"><ellipse cx="${-w / 4}" cy="0" rx="20" ry="5" fill="#fff"/><path d="M${-w / 4 - 14},-2 q14,-22 28,0z" fill="#f4a261"/><path d="M${w / 4 - 16},0 l14,-20 l14,20z" fill="#ffd54f"/><circle cx="0" cy="-8" r="9" fill="#e63946"/><circle cx="8" cy="-6" r="7" fill="#8bc34a"/></g>`;
  const bowl = o => { const c = o.color || '#ffffff', w = o.w || 22; return `<path d="M${-w},-${w * 0.9} h${w * 2} q0,${w * 0.9} -${w},${w * 0.9} q-${w},0 -${w},-${w * 0.9}z" fill="${c}"/><ellipse cx="0" cy="-${w * 0.9}" rx="${w}" ry="${w * 0.25}" fill="${o.empty ? shade(c, -0.1) : '#f4a261'}"/>` + (o.steam ? `<g class="a-float" stroke="#fff" stroke-width="3" fill="none" opacity=".8"><path d="M-6,-${w + 6} q-6,-8 0,-16 q6,-8 0,-16"/><path d="M8,-${w + 4} q-6,-8 0,-16"/></g>` : '') + (o.spoon ? `<path d="M${w - 4},-${w} l14,-16" stroke="#cfd8dc" stroke-width="4" stroke-linecap="round"/>` : ''); };
  const cheese = () => `<path d="M-26,0 L26,0 L26,-28 Z" fill="#ffd54f"/><path d="M-26,0 L26,-28 L10,-38Z" fill="#ffe082"/><g fill="#f9a825"><circle cx="12" cy="-10" r="4"/><circle cx="20" cy="-20" r="3"/><circle cx="2" cy="-4" r="2.5"/></g>`;
  const egg = () => `<ellipse cx="0" cy="-18" rx="14" ry="18" fill="#ffd24a" stroke="#e0a320" stroke-width="2"/><ellipse cx="-5" cy="-24" rx="4" ry="6" fill="#fff6c2"/>${sparkle(18, -34, 7)}${sparkle(-18, -8, 5)}`;
  const nest = o => { let s = `<ellipse cx="0" cy="-10" rx="60" ry="18" fill="#a47148"/>`; const n = o.eggs ?? 3; for (let i = 0; i < n; i++) s += `<ellipse cx="${-30 + i * (60 / Math.max(1, n - 1))}" cy="-26" rx="${o.big === i ? 18 : 13}" ry="${o.big === i ? 22 : 16}" fill="#fffaf0" stroke="#e8dcc4" stroke-width="2"/>`; return s + `<path d="M-60,-12 Q0,6 60,-12 M-56,-4 Q0,10 56,-4" stroke="#7d5232" stroke-width="3" fill="none"/>`; };
  const coins = () => `<path d="M-24,0 Q-30,-30 -10,-40 L-14,-50 h28 l-4,10 Q30,-30 24,0Z" fill="#c49a6c"/><path d="M-12,-44 h24" stroke="#8a6440" stroke-width="4"/><g fill="#ffd24a" stroke="#e0a320" stroke-width="1.5"><ellipse cx="30" cy="-4" rx="10" ry="4"/><ellipse cx="34" cy="-9" rx="10" ry="4"/><ellipse cx="-34" cy="-4" rx="10" ry="4"/></g>`;
  const harp = () => `<path d="M-24,0 L-24,-90 Q10,-100 24,-60 Q30,-30 20,0Z" fill="none" stroke="#f5b700" stroke-width="7" stroke-linejoin="round"/><g stroke="#fff4c2" stroke-width="1.5"><path d="M-16,-92 V0 M-8,-92 V0 M0,-92 V0 M8,-86 V0 M16,-70 V0"/></g><circle cx="-24" cy="-94" r="6" fill="#f5b700"/>`;
  const beanstalk = o => { const h = o.h || 480; let s = `<path d="M0,0 C-40,-${h * 0.2} 40,-${h * 0.4} 0,-${h * 0.6} S-40,-${h * 0.85} 10,-${h}" stroke="#4caf50" stroke-width="18" fill="none" stroke-linecap="round"/>`; for (let i = 1; i < 9; i++) { const y = -i * h / 9, k = i % 2 ? 1 : -1; s += `<path d="M${k * 6},${y} q${k * 30},-14 ${k * 48},4 q${k * -24},16 ${k * -48},-4z" fill="#66bb6a"/>`; } return s; };
  const cloud = o => `<g fill="${o.color || '#fff'}" opacity="${o.op || 0.95}"><ellipse cx="0" cy="0" rx="60" ry="22"/><circle cx="-24" cy="-12" r="22"/><circle cx="10" cy="-22" r="28"/><circle cx="36" cy="-8" r="18"/></g>`;
  const sun = () => { let s = ''; for (let i = 0; i < 12; i++) s += `<rect x="-3" y="-62" width="6" height="18" rx="3" fill="#ffd166" transform="rotate(${i * 30})"/>`; return `<g class="a-spin">${s}</g><circle r="36" fill="#ffd166"/><circle r="28" fill="#ffe08a"/>`; };
  const moon = () => `<circle r="40" fill="#fff3b0" opacity=".25"/><path d="M10,-30 A32,32 0 1,0 30,16 A26,26 0 1,1 10,-30Z" fill="#fff3b0"/>`;
  const star = o => `<path d="${star5(o.r || 14)}" fill="${o.color || '#ffd166'}" stroke="#f5b700" stroke-width="1.5"/>`;
  const heart = o => `<path d="M0,10 C-30,-10 -20,-34 0,-20 C20,-34 30,-10 0,10Z" fill="${o.color || '#ff4d6d'}"/>`;
  const sparkles = o => `<g class="a-twinkle">${sparkle(-30, -10, 10, o.color)}${sparkle(20, -30, 8, o.color)}${sparkle(30, 10, 6, o.color)}${sparkle(-10, 24, 7, o.color)}${sparkle(0, -50, 5, o.color)}</g>`;
  const notes = () => `<g fill="#6c5ce7"><path d="M-20,0 v-30 l16,-4 v26" stroke="#6c5ce7" stroke-width="3" fill="none"/><ellipse cx="-24" cy="0" rx="6" ry="4.5"/><ellipse cx="-8" cy="-4" rx="6" ry="4.5"/><path d="M20,-20 v-26 q8,4 10,12" stroke="#e84393" stroke-width="3" fill="none"/><ellipse cx="16" cy="-20" rx="6" ry="4.5" fill="#e84393"/></g>`;
  const zzz = () => `<g fill="#6c8ebf" font-family="Fredoka, sans-serif" font-weight="700"><text x="0" y="0" font-size="22">z</text><text x="14" y="-18" font-size="28">z</text><text x="32" y="-42" font-size="34">Z</text></g>`;
  const pumpkin = () => `<g fill="#ff9f1c"><ellipse cx="-18" cy="-30" rx="20" ry="30"/><ellipse cx="18" cy="-30" rx="20" ry="30"/></g><ellipse cx="0" cy="-30" rx="18" ry="31" fill="#ffb347"/><path d="M0,-60 q4,-14 12,-16" stroke="#5d8a3a" stroke-width="5" fill="none"/>`;
  const carriage = o => `<g>${[-60, 60].map(x => `<circle cx="${x}" cy="-28" r="26" fill="none" stroke="#f5b700" stroke-width="6"/><path d="M${x - 24},-28 h48 M${x},-52 v48" stroke="#f5b700" stroke-width="3"/>`).join('')}<path d="M-80,-60 Q-90,-170 0,-176 Q90,-170 80,-60 Q0,-44 -80,-60Z" fill="${o.color || '#ff9f1c'}"/><path d="M-40,-66 Q-50,-160 0,-170 M40,-66 Q50,-160 0,-170" stroke="${shade(o.color || '#ff9f1c', -0.2)}" stroke-width="4" fill="none"/><path d="M-22,-70 V-130 Q-22,-150 0,-150 Q22,-150 22,-130 V-70Z" fill="#fff6d6" stroke="#f5b700" stroke-width="4"/><path d="M-10,-176 Q0,-200 14,-196" stroke="#5d8a3a" stroke-width="6" fill="none"/>${sparkle(-90, -140, 10)}${sparkle(92, -110, 8)}</g>`;
  const mirror = () => `<rect x="-6" y="-60" width="12" height="60" fill="#c79a3a"/><ellipse cx="0" cy="-120" rx="42" ry="56" fill="#e0a320"/><ellipse cx="0" cy="-120" rx="34" ry="48" fill="#cfeaff"/><path d="M-18,-150 q10,-10 22,-8" stroke="#fff" stroke-width="5" fill="none" opacity=".8"/><g transform="translate(0,-116)"><circle cx="-10" cy="-4" r="3" fill="#6c8ebf"/><circle cx="10" cy="-4" r="3" fill="#6c8ebf"/><path d="M-8,8 q8,6 16,0" stroke="#6c8ebf" stroke-width="2.5" fill="none"/></g>`;
  const spinwheel = () => `<path d="M-40,0 L-20,-60 M40,0 L20,-60 M0,0 V-60" stroke="#8a5a33" stroke-width="6"/><circle cx="0" cy="-90" r="40" fill="none" stroke="#a0703c" stroke-width="7"/>${[0, 45, 90, 135].map(a => `<path d="M-40,-90 h80" stroke="#a0703c" stroke-width="3" transform="rotate(${a} 0 -90)"/>`).join('')}<path d="M30,-60 L60,-130" stroke="#5b3a24" stroke-width="4"/><circle cx="60" cy="-130" r="5" fill="#ffd166"/>`;
  const cradle = o => `<path d="M-50,-50 Q0,10 50,-50Z" fill="#f8c8dc"/><path d="M-54,-52 h108" stroke="#d58eaf" stroke-width="6" stroke-linecap="round"/><path d="M-50,-50 Q-60,-100 -20,-104 Q-50,-80 -44,-52" fill="#f3a6c8"/><path d="M-30,0 Q0,-14 30,0" stroke="#a0703c" stroke-width="6" fill="none"/><circle cx="-6" cy="-58" r="14" fill="#f7cfa8"/><path d="M-10,-60 q3,2 6,0 M0,-60 q3,2 6,0" stroke="${INK}" stroke-width="1.8" fill="none"/>`;
  const castle = o => { const c = o.color || '#f1e4ff', r = o.roof || '#8e6cf0'; return `<rect x="-90" y="-180" width="180" height="180" fill="${c}"/><g fill="${c}"><rect x="-140" y="-230" width="60" height="230"/><rect x="80" y="-230" width="60" height="230"/><rect x="-30" y="-260" width="60" height="100"/></g><g fill="${r}"><path d="M-148,-228 L-110,-310 L-72,-228Z"/><path d="M72,-228 L110,-310 L148,-228Z"/><path d="M-38,-258 L0,-340 L38,-258Z"/></g><g fill="#ff6b6b"><path d="M-110,-310 v-22 l18,7 l-18,7"/><path d="M110,-310 v-22 l18,7 l-18,7"/><path d="M0,-340 v-24 l20,8 l-20,8"/></g><path d="M-28,0 V-60 Q-28,-90 0,-90 Q28,-90 28,-60 V0Z" fill="#8d5a2b"/><g fill="${o.lit ? '#ffe08a' : '#9fd3ff'}"><rect x="-122" y="-190" width="24" height="32" rx="12"/><rect x="98" y="-190" width="24" height="32" rx="12"/><rect x="-12" y="-220" width="24" height="32" rx="12"/><rect x="-70" y="-140" width="22" height="30" rx="11"/><rect x="48" y="-140" width="22" height="30" rx="11"/></g><path d="M-90,-180 h180" stroke="${shade(c, -0.12)}" stroke-width="8" stroke-dasharray="16 12"/>`; };
  const tower = o => `<rect x="-50" y="-280" width="100" height="280" fill="${o.color || '#efe3d0'}"/><path d="M-60,-276 L0,-360 L60,-276Z" fill="${o.roof || '#8e6cf0'}"/><rect x="-16" y="-240" width="32" height="44" rx="16" fill="${o.lit ? '#ffe08a' : '#9fd3ff'}"/><path d="M-50,-280 h100" stroke="#d8c8ae" stroke-width="8" stroke-dasharray="14 10"/>`;
  const roses = o => { let s = ''; const w = o.w || 200, r = rng(11); for (let i = 0; i < 12; i++) { const x = -w / 2 + r() * w, y = -r() * 120; s += `<circle cx="${x.toFixed(0)}" cy="${(y - 20).toFixed(0)}" r="26" fill="#2e7d32"/>`; } for (let i = 0; i < 14; i++) { const x = -w / 2 + r() * w, y = -20 - r() * 130; s += `<circle cx="${x.toFixed(0)}" cy="${y.toFixed(0)}" r="7" fill="#ff4d6d"/><circle cx="${x.toFixed(0)}" cy="${y.toFixed(0)}" r="3" fill="#c9184a"/>`; } return s; };
  const boat = o => `<path d="M-70,-30 h140 l-24,30 h-92z" fill="${o.color || '#a0703c'}"/><path d="M-70,-30 h140" stroke="#5b3a24" stroke-width="4"/><path d="M0,-30 V-140" stroke="#5b3a24" stroke-width="5"/><path d="M4,-136 Q54,-90 4,-40Z" fill="#fff"/><path d="M-4,-120 Q-40,-80 -4,-44Z" fill="#ffe8d6"/>`;
  const ship = () => `<path d="M-130,-50 h260 l-40,50 h-180z" fill="#8d5a2b"/><path d="M-130,-50 h260" stroke="#5b3a24" stroke-width="6"/><g fill="#ffd166"><circle cx="-70" cy="-28" r="7"/><circle cx="-20" cy="-28" r="7"/><circle cx="30" cy="-28" r="7"/><circle cx="80" cy="-28" r="7"/></g><path d="M-30,-50 V-230 M50,-50 V-200" stroke="#5b3a24" stroke-width="7"/><path d="M-26,-224 Q30,-170 -26,-80Z M54,-194 Q100,-150 54,-80Z" fill="#fff"/><path d="M-30,-230 v-20 l24,10z" fill="#e63946"/>`;
  const whale = o => `<path d="M-160,-60 Q-150,-170 0,-170 Q140,-170 160,-80 Q170,0 60,0 L-120,0 Q-170,-10 -160,-60Z" fill="${o.color || '#5c7cfa'}"/><path d="M-140,-30 Q-30,30 120,-20 Q60,0 -120,0Z" fill="#c5d4ff"/><path d="M150,-100 Q200,-160 230,-130 Q200,-110 220,-70 Q190,-80 160,-70Z" fill="${o.color || '#5c7cfa'}"/><circle cx="-90" cy="-100" r="9" fill="#2b1d16"/><circle cx="-87" cy="-103" r="3" fill="#fff"/><path d="M-150,-50 Q-120,-30 -80,-46" stroke="#2b1d16" stroke-width="4" fill="none" stroke-linecap="round"/><circle cx="-110" cy="-70" r="10" fill="#ff8a8a" opacity=".4"/>` + (o.spout ? `<g fill="#bde0fe" class="a-float"><path d="M-20,-170 q-10,-40 -30,-60 M-20,-170 q0,-50 0,-70 M-20,-170 q10,-40 30,-60" stroke="#bde0fe" stroke-width="7" fill="none" stroke-linecap="round"/></g>` : '');
  const anthill = () => `<path d="M-90,0 Q-60,-90 0,-96 Q60,-90 90,0Z" fill="#b07a45"/><path d="M-18,0 V-30 Q-18,-46 0,-46 Q18,-46 18,-30 V0Z" fill="#5b3a24"/><circle cx="10" cy="-22" r="2.5" fill="#ffd24a"/><g fill="#c9925a"><circle cx="-50" cy="-30" r="4"/><circle cx="44" cy="-46" r="3"/><circle cx="30" cy="-16" r="4"/></g>`;
  const mousehole = () => `<path d="M-30,0 V-30 Q-30,-56 0,-56 Q30,-56 30,-30 V0Z" fill="#3b2a20"/>`;
  const finish = () => `<rect x="-110" y="-120" width="8" height="120" fill="#fff"/><rect x="102" y="-120" width="8" height="120" fill="#fff"/><path d="M-102,-110 h204 v24 h-204z" fill="#fff"/><g fill="#2d2d2d">${Array.from({ length: 17 }, (_, i) => `<rect x="${-102 + i * 12}" y="${i % 2 ? -110 : -98}" width="12" height="12"/>`).join('')}</g>`;
  const net = o => `<g stroke="${o.color || '#c9a66b'}" stroke-width="3" fill="none" opacity=".95"><path d="M-90,-10 Q0,-230 90,-10"/>${Array.from({ length: 7 }, (_, i) => `<path d="M${-80 + i * 27},-10 Q${-40 + i * 13},-150 ${-10 + i * 3},-190"/>`).join('')}${Array.from({ length: 5 }, (_, i) => `<path d="M-86,${-30 - i * 32} Q0,${-60 - i * 34} 86,${-30 - i * 32}"/>`).join('')}</g>` + (o.broken ? `<g fill="#c9a66b"><rect x="-60" y="-8" width="20" height="4" transform="rotate(20 -50 -6)"/><rect x="40" y="-6" width="24" height="4" transform="rotate(-15 52 -4)"/></g>` : '');
  const clock = () => `<rect x="-30" y="-200" width="60" height="200" rx="6" fill="#8d5a2b"/><path d="M-34,-200 Q0,-230 34,-200Z" fill="#6b4226"/><circle cx="0" cy="-160" r="22" fill="#fff8e7" stroke="#d4a373" stroke-width="4"/><path d="M0,-160 V-176 M0,-160 L10,-154" stroke="${INK}" stroke-width="3" stroke-linecap="round"/><rect x="-16" y="-120" width="32" height="96" rx="4" fill="#5b3a24"/><path d="M0,-118 V-60" stroke="#ffd24a" stroke-width="3"/><circle cx="0" cy="-56" r="9" fill="#ffd24a"/>`;
  const wardrobe = () => `<rect x="-60" y="-200" width="120" height="200" rx="6" fill="#a0703c"/><path d="M0,-196 V-4" stroke="#6b4226" stroke-width="4"/><circle cx="-8" cy="-100" r="5" fill="#ffd24a"/><circle cx="8" cy="-100" r="5" fill="#ffd24a"/><rect x="-66" y="-208" width="132" height="14" rx="4" fill="#8a5a33"/>`;
  const fireplace = o => `<rect x="-80" y="-140" width="160" height="140" fill="#c8553d"/><rect x="-92" y="-150" width="184" height="16" rx="4" fill="#8d5a2b"/><path d="M-50,0 V-80 Q-50,-100 0,-100 Q50,-100 50,-80 V0Z" fill="#3b2a20"/><g class="a-flicker"><path d="M-30,0 Q-36,-40 -14,-60 Q-16,-30 0,-34 Q-4,-66 18,-80 Q16,-40 34,-36 Q40,-14 30,0Z" fill="#ff9f1c"/><path d="M-16,0 Q-18,-24 -4,-36 Q0,-18 10,-24 Q16,-10 14,0Z" fill="#ffd166"/></g>${o.pot ? `<path d="M-30,-46 h60 q0,40 -30,40 q-30,0 -30,-40z" fill="#3d3d3d"/>` : ''}`;
  const cake = () => `<rect x="-40" y="-40" width="80" height="40" rx="6" fill="#f8c8dc"/><rect x="-28" y="-70" width="56" height="32" rx="6" fill="#fff0f6"/><path d="M-40,-36 q10,10 20,0 q10,10 20,0 q10,10 20,0 q10,10 20,0" stroke="#fff" stroke-width="5" fill="none"/><g fill="#ffd166"><rect x="-14" y="-90" width="5" height="20"/><rect x="-2" y="-92" width="5" height="22"/><rect x="10" y="-90" width="5" height="20"/></g><g fill="#ff9f1c" class="a-flicker"><ellipse cx="-11.5" cy="-96" rx="3" ry="5"/><ellipse cx="0.5" cy="-98" rx="3" ry="5"/><ellipse cx="12.5" cy="-96" rx="3" ry="5"/></g><circle cx="0" cy="-74" r="6" fill="#e63946"/>`;
  const throne = () => `<path d="M-40,0 V-150 Q0,-190 40,-150 V0Z" fill="#c0392b"/><path d="M-30,-10 V-140 Q0,-170 30,-140 V-10Z" fill="#e74c3c"/><rect x="-50" y="-60" width="100" height="20" rx="6" fill="#f5b700"/><circle cx="0" cy="-160" r="8" fill="#ffd24a"/>`;
  const rainbow = () => ['#ff595e', '#ffca3a', '#8ac926', '#1982c4', '#6a4c93'].map((c, i) => `<path d="M${-150 + i * 12},0 A${150 - i * 12},${150 - i * 12} 0 0,1 ${150 - i * 12},0" stroke="${c}" stroke-width="12" fill="none" opacity=".75"/>`).join('');
  const seaweed = o => `<g class="a-sway"><path d="M0,0 Q-20,-40 0,-80 Q20,-120 0,-160" stroke="${o.color || '#2a9d8f'}" stroke-width="12" fill="none" stroke-linecap="round"/><path d="M20,0 Q0,-30 18,-70 Q30,-96 16,-120" stroke="${shade(o.color || '#2a9d8f', 0.2)}" stroke-width="9" fill="none" stroke-linecap="round"/></g>`;
  const bubbles = () => `<g fill="none" stroke="#fff" stroke-width="2.5" opacity=".7" class="a-float"><circle cx="0" cy="0" r="8"/><circle cx="12" cy="-26" r="5"/><circle cx="-6" cy="-46" r="6"/><circle cx="8" cy="-70" r="4"/></g>`;
  const wave = o => `<path d="M-120,0 q30,-20 60,0 t60,0 t60,0 t60,0 v20 h-240z" fill="${o.color || '#4cc9f0'}" opacity=".9"/>`;
  const splash = () => `<g fill="#90e0ef"><path d="M-40,0 q10,-40 20,-10 q6,-50 20,-4 q10,-46 20,-2 q8,-36 20,16z"/><circle cx="-50" cy="-40" r="6"/><circle cx="50" cy="-50" r="5"/><circle cx="10" cy="-70" r="6"/></g>`;
  const window_ = o => `<rect x="-50" y="-120" width="100" height="120" rx="8" fill="${o.night ? '#1b2a6b' : '#bfe6ff'}" stroke="#fff" stroke-width="8"/><path d="M0,-120 V0 M-50,-60 h100" stroke="#fff" stroke-width="6"/>` + (o.night ? `<path d="M20,-100 A12,12 0 1,0 30,-82 A10,10 0 1,1 20,-100Z" fill="#fff3b0"/>` : '') + `<path d="M-60,-126 Q-30,-90 -56,-10" fill="#e88aa0" opacity=".9"/><path d="M60,-126 Q30,-90 56,-10" fill="#e88aa0" opacity=".9"/>`;
  const chandelier = () => `<path d="M0,-200 V-110" stroke="#c79a3a" stroke-width="4"/><path d="M-60,-100 Q0,-60 60,-100" stroke="#f5b700" stroke-width="6" fill="none"/><g fill="#ffe08a">${[-60, -30, 0, 30, 60].map(x => `<ellipse cx="${x}" cy="${-110 + Math.abs(x) * 0.2}" rx="5" ry="9" class="a-flicker"/>`).join('')}</g>`;
  const signpost = o => `<rect x="-5" y="-110" width="10" height="110" fill="#8a5a33"/><path d="M-50,-104 h80 l14,14 l-14,14 h-80z" fill="#c8915a"/><text x="-6" y="-84" font-size="16" text-anchor="middle" font-family="Fredoka, sans-serif" font-weight="600" fill="#5b3a24">${o.text || 'Bremen'}</text>`;
  const pot = () => `<path d="M-34,-50 h68 q0,50 -34,50 q-34,0 -34,-50z" fill="#3d3d3d"/><rect x="-40" y="-56" width="80" height="10" rx="5" fill="#555"/><g class="a-float" stroke="#fff" stroke-width="3" fill="none" opacity=".7"><path d="M-10,-62 q-6,-10 0,-20 q6,-10 0,-20"/><path d="M12,-62 q-6,-10 0,-20"/></g>`;
  const thoughtbubble = o => `<ellipse cx="0" cy="-60" rx="70" ry="50" fill="#fff" opacity=".95"/><circle cx="-40" cy="2" r="10" fill="#fff"/><circle cx="-54" cy="22" r="6" fill="#fff"/>`;
  const cookie = () => `<circle r="14" fill="#d4a373"/><g fill="#5b3a24"><circle cx="-4" cy="-4" r="2.5"/><circle cx="5" cy="3" r="2.5"/><circle cx="-3" cy="6" r="2"/></g>`;
  const logs = () => `<g fill="#a0703c"><rect x="-50" y="-20" width="100" height="20" rx="10"/><rect x="-40" y="-40" width="80" height="20" rx="10"/></g><g fill="#e8c49a"><circle cx="-50" cy="-10" r="9"/><circle cx="-40" cy="-30" r="9"/></g>`;

  const PROPS = {
    house, tree, bush, flower, mushroom, rock, fence, bed, chair, table, bowl, cheese, egg, nest, coins, harp, beanstalk, cloud, sun, moon, star, heart, sparkles, notes, zzz,
    pumpkin, carriage, mirror, spinwheel, cradle, castle, tower, roses, boat, ship, whale, anthill, mousehole, finish, net, clock, wardrobe, fireplace, cake, throne, rainbow,
    seaweed, bubbles, wave, splash, window: window_, chandelier, signpost, pot, thoughtbubble, cookie, logs,
    item: o => held(o.what, 0, 0, o),
  };
  const BUILDERS = { person, critter, bird, insect, frog, fish, crab, octopus };

  // ---------- reparto de personajes ----------
  const pig = { color: '#f8b7c4', ears: 'pig', snout: 'pig', tail: 'curly', snoutC: '#f39cb0' };
  const wolf = { color: '#8d99ae', belly: '#e6e9ef', ears: 'pointy', snout: 'long', tail: 'bushy', mask: true, inner: '#c9ced8' };
  const mouse = { color: '#a7a9ac', ears: 'big', snout: 'small', tail: 'thin', tailColor: '#f5a9b8' };
  const cat = { color: '#f39c4a', ears: 'pointy', snout: 'small', tail: 'thin', stripes: true };
  const bear = { ears: 'round', snout: 'muzzle', headW: 1.08 };
  const goat = { color: '#f4ece1', ears: 'floppy', horns: true, snout: 'muzzle', tail: 'puff', earColor: '#e3d2bd', snoutC: '#fff' };
  const P = (b, o) => [b, o];
  const CAST = {
    // tres cerditos
    cerdito1: P('critter', { ...pig, shirt: '#f4d35e', hat: 'straw' }),
    cerdito2: P('critter', { ...pig, shirt: '#7bc47f' }),
    cerdito3: P('critter', { ...pig, overalls: '#5b8def', hat: 'cap', capColor: '#e63946' }),
    mamaCerdita: P('critter', { ...pig, dress: '#c39bd3', apron: '#fff' }),
    lobo: P('critter', { ...wolf }),
    // caperucita
    caperucita: P('person', { hat: 'hood', hood: '#d93a3a', dress: '#d93a3a', apron: '#fff', hair: '#8b4a24', hairStyle: 'braids', item: 'basket' }),
    mamaCaperucita: P('person', { hair: '#8b4a24', hairStyle: 'bun', dress: '#7fb3d5', apron: '#fff' }),
    abuelita: P('person', { hair: '#e0e0e0', hairStyle: 'bun', glasses: true, dress: '#9b7fd1', apron: '#f3e9ff', skin: '#f3c6a0' }),
    lenador: P('person', { beard: '#7a4a26', hair: '#7a4a26', top: '#c0392b', plaid: true, bottom: '#3b5998', item: 'axe' }),
    // ricitos
    ricitos: P('person', { hair: '#f5c542', hairStyle: 'curly', dress: '#7fc8f8', hat: 'bow', bowColor: '#ff6fa5' }),
    papaOso: P('critter', { ...bear, color: '#8b5a2b', fat: true, shirt: '#2e86de' }),
    mamaOsa: P('critter', { ...bear, color: '#a46b3a', dress: '#e17fb0', apron: '#fff' }),
    osito: P('critter', { ...bear, color: '#c58a4f', shirt: '#f6c744' }),
    papaRicitos: P('person', { hair: '#d9a830', beard: '#d9a830', top: '#6ab04c', bottom: '#5d4037' }),
    // liebre y tortuga
    liebre: P('critter', { color: '#d9c8b4', ears: 'long', snout: 'small', tail: 'puff', scarf: '#e74c3c', headW: 0.92 }),
    tortuga: P('critter', { color: '#9ccc65', shell: '#5d8a3a', snout: 'none', headW: 0.85 }),
    zorro: P('critter', { color: '#ef7d32', belly: '#fff', ears: 'pointy', snout: 'long', tail: 'bushy', tailTip: '#fff', mask: true }),
    pajarito: P('bird', { color: '#74c0fc', beakType: 'pointed', beak: '#ffb703' }),
    // patito feo
    mamaPata: P('bird', { color: '#ffffff', beakType: 'flat', bonnet: '#b28dff' }),
    patito: P('bird', { color: '#ffd84d', beakType: 'flat' }),
    patitoFeo: P('bird', { color: '#a9a39b', beakType: 'flat', beak: '#e8913a' }),
    cisne: P('bird', { color: '#ffffff', long: true, beak: '#f28c28' }),
    granjera: P('person', { hair: '#a0522d', hairStyle: 'bun', dress: '#8ecae6', apron: '#fff', skin: '#e8b48a' }),
    // cigarra y hormiga
    cigarra: P('insect', { color: '#7cc242', wings: true, item: 'violin' }),
    hormiga: P('insect', { color: '#c0533a', vest: '#f4d35e' }),
    // león y ratón
    leon: P('critter', { color: '#e9b44c', mane: '#b5651d', ears: 'round', snout: 'muzzle', tail: 'thin', tuft: '#b5651d', headW: 1.05, belly: '#f6dca0' }),
    raton: P('critter', { ...mouse }),
    ratonCiudad: P('critter', { ...mouse, color: '#8e8e93', bowtie: '#e63946', vest: '#2d2d2d', hat: 'tophat' }),
    ratonCampo: P('critter', { ...mouse, color: '#b5a28a', overalls: '#6a994e', hat: 'straw' }),
    gato: P('critter', { ...cat }),
    // juan
    juan: P('person', { hair: '#c0782f', top: '#f4a259', bottom: '#4a6fa5', patches: '#d4a373', hairStyle: 'spiky' }),
    mamaJuan: P('person', { hair: '#6b4226', hairStyle: 'bun', dress: '#90a4ae', apron: '#f1f1f1', patches: '#bcaaa4' }),
    vaca: P('critter', { color: '#ffffff', spots: '#3d3d3d', headSpot: '#3d3d3d', ears: 'cow', horns: true, snout: 'muzzle', snoutC: '#f7c6d0', belly: '#ffffff', tail: 'thin', tuft: '#3d3d3d' }),
    anciano: P('person', { hair: '#eeeeee', hairStyle: 'bald', beard: '#f5f5f5', top: '#6d597a', bottom: '#4a4e69', item: 'beans' }),
    gigante: P('person', { beard: '#a0522d', hair: '#a0522d', top: '#6c8e3e', bottom: '#5b4636', belt: '#3e2723', skin: '#f1bd8e' }),
    gallinaOro: P('bird', { color: '#f6c445', comb: '#e74c3c', beak: '#f39c12' }),
    // pedro
    pedro: P('person', { hat: 'straw', top: '#e9c46a', bottom: '#6d597a', item: 'staff', hair: '#6b4226' }),
    oveja: P('critter', { color: '#5c5c5c', wool: '#fbfbfb', ears: 'floppy', snout: 'none', earColor: '#4a4a4a' }),
    vecino: P('person', { hair: '#3d2b1f', beard: '#3d2b1f', top: '#a3b18a', bottom: '#588157', vest: '#6b4226' }),
    vecina: P('person', { hair: '#5c3d2e', hairStyle: 'bun', dress: '#f4a261', apron: '#fff', item: 'broom' }),
    // cenicienta
    cenicienta: P('person', { hair: '#f2c14e', hairStyle: 'bun', dress: '#a1887f', apron: '#d7ccc8', patches: '#8d6e63' }),
    cenicientaGala: P('person', { hair: '#f2c14e', hairStyle: 'bun', dress: '#8ecae6', gown: true, hat: 'tiara', bodice: '#bde0fe' }),
    madrastra: P('person', { hair: '#4a3b5c', hairStyle: 'bun', dress: '#6a4c93', mood: 'grumpy' }),
    hermanastra1: P('person', { hair: '#c1121f', hairStyle: 'curly', dress: '#f28482', mood: 'grumpy' }),
    hermanastra2: P('person', { hair: '#3d2b1f', hairStyle: 'long', dress: '#84a59d', mood: 'grumpy' }),
    hada: P('person', { hair: '#f0f0f0', hairStyle: 'bun', dress: '#a5b4fc', wings: '#dbeafe', item: 'wand', pose: 'wave', cape: '#818cf8' }),
    principe: P('person', { hair: '#6b4226', top: '#2b59c3', bottom: '#f5f5f5', cape: '#c0392b', hat: 'crown', belt: '#f5b700' }),
    rey: P('person', { beard: '#e8e8e8', hair: '#e8e8e8', hairStyle: 'bald', top: '#8e44ad', bottom: '#5b2c6f', cape: '#c0392b', hat: 'crown' }),
    reina: P('person', { hair: '#8b4a24', hairStyle: 'long', dress: '#e56b6f', gown: true, hat: 'crown' }),
    princesa: P('person', { hair: '#8b4a24', hairStyle: 'long', dress: '#ffafcc', gown: true, hat: 'tiara' }),
    // gato con botas
    gatoBotas: P('critter', { ...cat, boots: '#6b3e1f', hat: 'feather', cape: '#c0392b' }),
    muchacho: P('person', { hair: '#e0a96d', top: '#fff8e7', bottom: '#6d4c41', vest: '#8d6e63' }),
    marques: P('person', { hair: '#e0a96d', top: '#3a86ff', bottom: '#fff', cape: '#6a4c93', belt: '#f5b700' }),
    ogro: P('person', { skin: '#9bc26b', hair: '#4b6e2a', top: '#8d6e63', bottom: '#5d4037', mood: 'grumpy', belt: '#3e2723' }),
    // pinocho
    pinocho: P('person', { skin: '#e8b77d', hair: '#3b2416', top: '#e63946', bottom: '#ffd166', hat: 'cap', capColor: '#ffd166', nose: 8 }),
    pinochoLargo: P('person', { skin: '#e8b77d', hair: '#3b2416', top: '#e63946', bottom: '#ffd166', hat: 'cap', capColor: '#ffd166', nose: 46, mood: 'surprised' }),
    pinochoNino: P('person', { hair: '#3b2416', top: '#e63946', bottom: '#ffd166', hat: 'cap', capColor: '#ffd166', pose: 'up', mood: 'laugh' }),
    gepetto: P('person', { hair: '#eeeeee', hairStyle: 'bald', mustache: '#eeeeee', glasses: true, top: '#7d5a50', bottom: '#4e342e', vest: '#a1887f' }),
    grillo: P('insect', { color: '#6ab04c', hat: 'tophat', vest: '#e63946' }),
    hadaAzul: P('person', { hair: '#3a86ff', hairStyle: 'long', dress: '#8ecae6', wings: '#e0f2fe', item: 'wand', pose: 'wave', hat: 'tiara' }),
    // blancanieves
    blancanieves: P('person', { hair: '#1d1d1d', hairStyle: 'bob', dress: '#ffd54f', bodice: '#2f5aa8', hat: 'headband', bowColor: '#e63946', skin: '#fde2cf' }),
    reinaCelosa: P('person', { hair: '#222', hairStyle: 'long', dress: '#5b2a86', cape: '#2d1546', hat: 'crown', mood: 'grumpy', gown: true }),
    viejecita: P('person', { hair: '#bdbdbd', hat: 'hood', hood: '#4a3b5c', dress: '#4a3b5c', item: 'apple', pose: 'point' }),
    enano1: P('person', { beard: '#f5f5f5', top: '#e63946', bottom: '#6d4c41', hat: 'cap', capColor: '#ff9f1c' }),
    enano2: P('person', { beard: '#bcaaa4', top: '#2a9d8f', bottom: '#6d4c41', hat: 'cap', capColor: '#e76f51' }),
    enano3: P('person', { beard: '#8d6e63', top: '#ffb703', bottom: '#264653', hat: 'cap', capColor: '#3a86ff', glasses: true }),
    enano4: P('person', { beard: '#f5f5f5', top: '#8338ec', bottom: '#6d4c41', hat: 'cap', capColor: '#06d6a0' }),
    // ratita
    ratita: P('critter', { ...mouse, color: '#c7b8a8', dress: '#f78fb3', hat: 'bow', bowColor: '#e63946', item: 'broom' }),
    ratitaLazo: P('critter', { ...mouse, color: '#c7b8a8', dress: '#f78fb3', hat: 'bow', bowColor: '#e63946' }),
    ratonPerez: P('critter', { ...mouse, color: '#9e8e7e', bowtie: '#3a86ff', glasses: true }),
    gallo: P('bird', { color: '#e9844a', comb: '#e63946', tailFeathers: true, beak: '#ffb703' }),
    perro: P('critter', { color: '#c69c6d', ears: 'floppy', snout: 'muzzle', tail: 'thin', earColor: '#8d6e4a' }),
    cerdo: P('critter', { ...pig, vest: '#6a994e' }),
    gatoElegante: P('critter', { ...cat, color: '#7d7d85', bowtie: '#e63946', stripes: true, hat: 'tophat' }),
    // zorro y cuervo
    cuervo: P('bird', { color: '#33334a', beak: '#f4b942', item: 'cheese' }),
    cuervoSolo: P('bird', { color: '#33334a', beak: '#f4b942' }),
    // bella durmiente
    aurora: P('person', { hair: '#f6d365', hairStyle: 'long', dress: '#f8a5c2', gown: true, hat: 'tiara' }),
    hadaRosa: P('person', { hair: '#ffffff', hairStyle: 'bun', dress: '#ff8fab', wings: '#ffe5ec', item: 'wand', pose: 'wave' }),
    hadaVerde: P('person', { hair: '#ffffff', hairStyle: 'bun', dress: '#80ed99', wings: '#e9fbe9', item: 'wand', pose: 'wave' }),
    hadaCeleste: P('person', { hair: '#ffffff', hairStyle: 'bun', dress: '#90dbf4', wings: '#e7f8ff', item: 'wand', pose: 'wave' }),
    hadaGrunona: P('person', { hair: '#2d2d2d', hairStyle: 'long', dress: '#4b3869', cape: '#2d1b4e', hat: 'witch', mood: 'grumpy', skin: '#e9d5ff' }),
    // bremen
    burro: P('critter', { color: '#9e9e9e', ears: 'donkey', snout: 'muzzle', tail: 'thin', tuft: '#555', headW: 0.95, snoutC: '#d6d6d6' }),
    ladron: P('person', { top: '#5c5c5c', bottom: '#333', mask: true, hat: 'cap', capColor: '#444', hair: '#222' }),
    // sirenita
    sirenita: P('person', { hair: '#ff7aa2', hairStyle: 'long', top: '#9b5de5', tail: '#2ec4b6' }),
    sirenitaPiernas: P('person', { hair: '#ff7aa2', hairStyle: 'long', dress: '#9b5de5', item: 'shell' }),
    principeMar: P('person', { hair: '#3d2b1f', top: '#ffffff', bottom: '#1d3557', belt: '#e63946' }),
    pez: P('fish', { color: '#ffb703' }),
    pezAzul: P('fish', { color: '#4cc9f0' }),
    cangrejo: P('crab', {}),
    pulpina: P('octopus', { color: '#b185db', glasses: true }),
    // cabritillos
    mamaCabra: P('critter', { ...goat, dress: '#e5989b', apron: '#fff' }),
    cabritillo: P('critter', { ...goat, scarf: '#3a86ff' }),
    cabritillo2: P('critter', { ...goat, scarf: '#ff6b6b' }),
    cabritillo3: P('critter', { ...goat, scarf: '#ffd166' }),
    cabritillo4: P('critter', { ...goat, scarf: '#06d6a0' }),
    loboHarina: P('critter', { ...wolf, paw: '#ffffff', pose: 'out' }),
    // general
    nino: P('person', { hair: '#6b4226', top: '#4f86d9', bottom: '#34508f' }),
    nina: P('person', { hair: '#f2b134', hairStyle: 'long', dress: '#c77dff', hat: 'bow' }),
    rana: P('frog', {}),
  };

  // ---------- fondos ----------
  const SKY = { day: ['#7ec8f5', '#d8f1ff'], sunset: ['#f79d65', '#ffe0b5'], night: ['#121c4d', '#3d4b9a'], dusk: ['#6a5acd', '#f6b0c3'], storm: ['#3d4a63', '#7b8aa3'] };
  function sky(o, id) {
    const t = o.time || 'day', c = SKY[t];
    let s = `<defs><linearGradient id="${id}sky" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="${c[0]}"/><stop offset="1" stop-color="${c[1]}"/></linearGradient></defs><rect width="600" height="500" fill="url(#${id}sky)"/>`;
    if (t === 'night' || t === 'dusk') {
      const r = rng(3);
      s += '<g fill="#fff">';
      for (let i = 0; i < 40; i++) s += `<circle cx="${(r() * 600).toFixed(0)}" cy="${(r() * 260).toFixed(0)}" r="${(r() * 1.8 + 0.6).toFixed(1)}" class="${i % 3 ? '' : 'a-twinkle'}" style="animation-delay:${(r() * 3).toFixed(1)}s"/>`;
      s += '</g>';
      if (!o.noMoon) s += `<g transform="translate(490 118)">${moon()}</g>`;
    } else if (t === 'storm') {
      s += `<g transform="translate(140 80)">${cloud({ color: '#5a6478' })}</g><g transform="translate(420 60) scale(1.3)">${cloud({ color: '#4a5468' })}</g><path d="M300,90 l-20,50 h20 l-16,46 l40,-60 h-22 l18,-36z" fill="#ffe066"/>`;
    } else if (!o.noSun) {
      s += `<g transform="translate(500 122) scale(.75)">${sun()}</g>`;
      s += `<g class="a-drift"><g transform="translate(130 130) scale(.75)">${cloud({})}</g><g transform="translate(330 106) scale(.55)">${cloud({})}</g></g>`;
    }
    return s;
  }
  const nightify = (c, o) => (o.time === 'night' ? shade(c, -0.5) : o.time === 'sunset' || o.time === 'dusk' ? shade(c, -0.15) : c);
  function hills(o, far = '#a6d98a', near = '#7cc35a') {
    return `<path d="M0,320 Q150,270 300,310 T600,300 V500 H0Z" fill="${nightify(far, o)}"/><path d="M0,380 Q200,355 400,382 T600,372 V500 H0Z" fill="${nightify(near, o)}"/>`;
  }
  function scatterFlowers(o, seed = 5, n = 10) {
    if (o.time === 'night') return '';
    const r = rng(seed), cols = ['#ff6fa5', '#ffd166', '#a29bfe', '#ffffff', '#ff9f1c'];
    let s = '';
    for (let i = 0; i < n; i++) { const x = r() * 600, y = 400 + r() * 90; s += `<g transform="translate(${x.toFixed(0)} ${y.toFixed(0)}) scale(${(0.35 + r() * 0.25).toFixed(2)})">${flower({ color: cols[i % cols.length] })}</g>`; }
    return s;
  }
  function grassTufts(o, seed = 9) {
    const r = rng(seed), c = nightify('#5fa843', o);
    let s = `<g stroke="${c}" stroke-width="3" stroke-linecap="round" fill="none">`;
    for (let i = 0; i < 18; i++) { const x = r() * 600, y = 395 + r() * 100; s += `<path d="M${x.toFixed(0)},${y.toFixed(0)} l-4,-10 M${x.toFixed(0)},${y.toFixed(0)} l0,-13 M${x.toFixed(0)},${y.toFixed(0)} l4,-10"/>`; }
    return s + '</g>';
  }
  const BG = {
    meadow: (o, id) => sky(o, id) + hills(o) + grassTufts(o) + scatterFlowers(o),
    road: (o, id) => sky(o, id) + hills(o) + `<path d="M260,500 Q300,420 360,380 Q420,350 600,345 V372 Q440,380 400,420 Q360,460 380,500Z" fill="${nightify('#e8cf9c', o)}"/>` + grassTufts(o) + scatterFlowers(o, 8, 6),
    forest: (o, id) => {
      let s = sky(o, id) + `<path d="M0,330 Q150,280 300,320 T600,310 V500 H0Z" fill="${nightify('#7fb86a', o)}"/>`;
      const r = rng(21);
      s += `<g fill="${nightify('#3f8f4f', o)}">`;
      for (let i = 0; i < 11; i++) { const x = i * 60 + r() * 20, y = 330 + r() * 20, rr = 40 + r() * 20; s += `<rect x="${x - 5}" y="${y - 10}" width="10" height="60" fill="${nightify('#6b4a2f', o)}"/><circle cx="${x}" cy="${y - 30}" r="${rr}"/>`; }
      s += '</g>' + `<path d="M0,385 Q200,365 400,388 T600,378 V500 H0Z" fill="${nightify('#6fb34f', o)}"/>`;
      s += `<g transform="translate(40 410) scale(1.1)">${tree({ color: nightify('#4a9e4f', o) })}</g><g transform="translate(570 420) scale(1.2)">${tree({ color: nightify('#3d8b45', o) })}</g>`;
      return s + grassTufts(o, 4) + (o.flowers ? scatterFlowers(o, 13, 8) : '');
    },
    room: (o, id) => {
      const wall = o.wall || '#fbe3c6', floor = o.floor || '#c48a5a';
      let s = `<rect width="600" height="500" fill="${wall}"/><g fill="${shade(wall, -0.06)}">`;
      for (let y = 30; y < 380; y += 50) for (let x = (y / 50 % 2) * 25 + 10; x < 600; x += 50) s += `<circle cx="${x}" cy="${y}" r="4"/>`;
      s += `</g><rect y="372" width="600" height="128" fill="${floor}"/>`;
      for (let x = 0; x < 600; x += 75) s += `<path d="M${x},372 V500" stroke="${shade(floor, -0.12)}" stroke-width="2"/>`;
      s += `<rect y="362" width="600" height="12" fill="${shade(floor, -0.2)}"/>`;
      if (!o.noWindow) s += `<g transform="translate(${o.winX || 300} 250)">${window_({ night: o.time === 'night' })}</g>`;
      if (o.rug !== false) s += `<ellipse cx="300" cy="455" rx="190" ry="32" fill="${o.rug || '#e07a5f'}" opacity=".75"/><ellipse cx="300" cy="455" rx="160" ry="22" fill="none" stroke="#fff" stroke-width="3" stroke-dasharray="10 8" opacity=".6"/>`;
      if (o.time === 'night') s += `<rect width="600" height="500" fill="#1b2a6b" opacity=".18"/>`;
      return s;
    },
    castle: (o, id) => sky(o, id) + `<path d="M0,330 Q150,280 300,320 T600,310 V500 H0Z" fill="${nightify('#a6d98a', o)}"/><g transform="translate(300 345) scale(.75)">${castle({ lit: o.time === 'night' })}</g>` + `<path d="M0,385 Q200,365 400,388 T600,378 V500 H0Z" fill="${nightify('#7cc35a', o)}"/><path d="M270,500 Q290,420 300,345 Q310,420 330,500Z" fill="${nightify('#e8cf9c', o)}"/>` + grassTufts(o) + scatterFlowers(o, 17, 8),
    ballroom: (o, id) => {
      let s = `<defs><linearGradient id="${id}bw" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#5b3c88"/><stop offset="1" stop-color="#9d79d6"/></linearGradient></defs><rect width="600" height="500" fill="url(#${id}bw)"/>`;
      for (const x of [60, 220, 380, 540]) s += `<rect x="${x - 18}" y="40" width="36" height="340" fill="#e9dcff"/><rect x="${x - 26}" y="30" width="52" height="16" fill="#f5b700"/>`;
      for (const x of [140, 300, 460]) s += `<path d="M${x - 50},380 V170 Q${x},100 ${x + 50},170 V380Z" fill="#2d1b4e" opacity=".55"/>`;
      s += `<g transform="translate(300 200)">${chandelier()}</g>`;
      s += `<rect y="380" width="600" height="120" fill="#f3e9d2"/>`;
      for (let x = 0; x < 600; x += 50) for (let y = 380, j = 0; y < 500; y += 30, j++) if ((x / 50 + j) % 2) s += `<rect x="${x}" y="${y}" width="50" height="30" fill="#e2d3b0"/>`;
      return s;
    },
    village: (o, id) => {
      let s = sky(o, id) + `<path d="M0,320 Q150,280 300,310 T600,300 V500 H0Z" fill="${nightify('#a6d98a', o)}"/>`;
      const cols = [['#ffe8d6', '#e76f51'], ['#e0fbfc', '#3d5a80'], ['#fff3b0', '#e09f3e'], ['#f1e4ff', '#9d4edd']];
      [70, 220, 380, 530].forEach((x, i) => { s += `<g transform="translate(${x} 365) scale(.55)">${house({ kind: 'cottage', wall: cols[i][0], roof: cols[i][1], lit: o.time === 'night' })}</g>`; });
      s += `<rect y="380" width="600" height="120" fill="${nightify('#d8c3a5', o)}"/>`;
      const r = rng(31);
      s += `<g fill="${nightify('#c4ad8c', o)}">`;
      for (let i = 0; i < 40; i++) s += `<ellipse cx="${(r() * 600).toFixed(0)}" cy="${(390 + r() * 110).toFixed(0)}" rx="${(10 + r() * 8).toFixed(0)}" ry="5"/>`;
      return s + '</g>';
    },
    city: (o, id) => {
      let s = sky({ ...o, noSun: true }, id);
      const r = rng(41), cols = ['#8d99ae', '#adb5bd', '#6c757d', '#9a8c98', '#7f8fa6'];
      for (let i = 0; i < 9; i++) {
        const w = 60 + r() * 20, h = 150 + r() * 160, x = i * 68 - 10, y = 380 - h;
        s += `<rect x="${x.toFixed(0)}" y="${y.toFixed(0)}" width="${w.toFixed(0)}" height="${h.toFixed(0)}" fill="${nightify(cols[i % 5], o)}"/>`;
        for (let wy = y + 14; wy < 360; wy += 28) for (let wx = x + 10; wx < x + w - 14; wx += 20) s += `<rect x="${wx.toFixed(0)}" y="${wy.toFixed(0)}" width="10" height="14" fill="${r() > 0.4 ? '#ffe08a' : '#4a5568'}"/>`;
      }
      s += `<rect y="380" width="600" height="120" fill="#6c757d"/><path d="M0,440 h600" stroke="#f8f9fa" stroke-width="6" stroke-dasharray="40 30"/>`;
      s += `<g transform="translate(110 420)"><rect x="-50" y="-34" width="100" height="26" rx="10" fill="#e63946"/><rect x="-30" y="-54" width="60" height="24" rx="8" fill="#e63946"/><rect x="-24" y="-50" width="22" height="16" fill="#bde0fe"/><rect x="4" y="-50" width="22" height="16" fill="#bde0fe"/><circle cx="-28" cy="-8" r="10" fill="#333"/><circle cx="28" cy="-8" r="10" fill="#333"/></g>`;
      return s;
    },
    sea: (o, id) => {
      let s = sky(o, id) + `<defs><linearGradient id="${id}sea" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="${nightify('#4cc9f0', o)}"/><stop offset="1" stop-color="${nightify('#1d6fa3', o)}"/></linearGradient></defs>`;
      s += `<rect y="260" width="600" height="240" fill="url(#${id}sea)"/>`;
      s += `<g stroke="#fff" stroke-width="3" fill="none" opacity=".5" class="a-swim">${[300, 340, 380].map(y => `<path d="M-20,${y} q25,-12 50,0 t50,0 t50,0 t50,0 t50,0 t50,0 t50,0 t50,0 t50,0 t50,0 t50,0 t50,0 t50,0"/>`).join('')}</g>`;
      if (o.beach !== false) s += `<path d="M0,420 Q200,395 380,425 T600,410 V500 H0Z" fill="${nightify('#f6d79b', o)}"/><g fill="${nightify('#e9c47a', o)}"><circle cx="80" cy="460" r="3"/><circle cx="250" cy="480" r="3"/><circle cx="470" cy="455" r="3"/></g>`;
      return s;
    },
    underwater: (o, id) => {
      let s = `<defs><linearGradient id="${id}uw" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#48cae4"/><stop offset="1" stop-color="#0a4f8a"/></linearGradient></defs><rect width="600" height="500" fill="url(#${id}uw)"/>`;
      s += `<g fill="#fff" opacity=".08"><path d="M80,0 l60,0 l-120,500 h-40z"/><path d="M300,0 l50,0 l-60,500 h-60z"/><path d="M480,0 l40,0 l40,500 h-50z"/></g>`;
      s += `<path d="M0,420 Q150,400 300,425 T600,415 V500 H0Z" fill="#f2cc8f"/>`;
      s += `<g transform="translate(60 430)">${seaweed({})}</g><g transform="translate(540 428)">${seaweed({ color: '#52b788' })}</g><g transform="translate(470 440)">${rock({ color: '#6c757d' })}</g>`;
      s += `<g fill="#ff8fab"><circle cx="150" cy="445" r="8"/><circle cx="160" cy="430" r="6"/><circle cx="140" cy="432" r="6"/></g>`;
      s += `<g transform="translate(120 300)">${bubbles()}</g><g transform="translate(500 220)">${bubbles()}</g>`;
      return s;
    },
    pond: (o, id) => {
      let s = sky(o, id) + hills(o);
      s += `<ellipse cx="300" cy="440" rx="290" ry="62" fill="${nightify('#5bc0eb', o)}"/><ellipse cx="300" cy="440" rx="290" ry="62" fill="none" stroke="#fff" stroke-width="3" opacity=".4"/>`;
      s += `<g fill="#57cc99"><ellipse cx="120" cy="455" rx="22" ry="8"/><ellipse cx="470" cy="430" rx="20" ry="7"/></g>`;
      s += `<g stroke="#6a994e" stroke-width="4">${[30, 42, 54, 560, 572].map(x => `<path d="M${x},430 V360"/>`).join('')}</g><g fill="#8d5a2b">${[30, 54, 572].map(x => `<rect x="${x - 4}" y="356" width="8" height="22" rx="4"/>`).join('')}</g>`;
      return s;
    },
    clouds: (o, id) => {
      let s = sky({ ...o, noSun: true }, id);
      s += `<g transform="translate(470 300) scale(.55)">${castle({ color: '#e8e4ff', roof: '#7c6fd8' })}</g>`;
      s += `<g fill="#fff">`;
      for (let x = -40; x < 680; x += 70) s += `<circle cx="${x}" cy="${400 + (x % 140 ? 10 : -10)}" r="60"/>`;
      s += `<rect y="410" width="600" height="90"/></g><g fill="#eef3ff"><circle cx="100" cy="470" r="30"/><circle cx="400" cy="480" r="36"/></g>`;
      return s;
    },
    farm: (o, id) => {
      let s = sky(o, id) + hills(o);
      s += `<g transform="translate(110 380)"><rect x="-70" y="-120" width="140" height="120" fill="${nightify('#c0392b', o)}"/><path d="M-84,-116 L0,-180 L84,-116Z" fill="${nightify('#8e2b20', o)}"/><rect x="-30" y="-80" width="60" height="80" fill="#fff"/><path d="M-30,-80 l60,80 M30,-80 l-60,80" stroke="${nightify('#c0392b', o)}" stroke-width="6"/><rect x="-16" y="-150" width="32" height="24" fill="#fff"/></g>`;
      s += `<g transform="translate(420 395)">${fence({ w: 320 })}</g>` + grassTufts(o) + scatterFlowers(o, 23, 6);
      return s;
    },
    snow: (o, id) => {
      let s = `<defs><linearGradient id="${id}sn" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#9fb4c9"/><stop offset="1" stop-color="#e3edf7"/></linearGradient></defs><rect width="600" height="500" fill="url(#${id}sn)"/>`;
      s += `<path d="M0,330 Q150,290 300,320 T600,310 V500 H0Z" fill="#eef4fb"/><path d="M0,385 Q200,360 400,388 T600,378 V500 H0Z" fill="#ffffff"/>`;
      s += `<g transform="translate(70 395)">${tree({ kind: 'pine', snow: true })}</g><g transform="translate(540 390) scale(.9)">${tree({ kind: 'bare', snow: true })}</g>`;
      const r = rng(51);
      s += `<g fill="#fff" class="a-snow">`;
      for (let i = 0; i < 50; i++) s += `<circle cx="${(r() * 600).toFixed(0)}" cy="${(r() * 480).toFixed(0)}" r="${(1.5 + r() * 2.5).toFixed(1)}"/>`;
      return s + '</g>';
    },
    stage: (o, id) => `<rect width="600" height="500" fill="#2d1b4e"/><path d="M0,0 h600 v60 q-75,40 -150,0 q-75,40 -150,0 q-75,40 -150,0 q-75,40 -150,0z" fill="#c0392b"/><path d="M0,0 v400 q60,-160 40,-400z M600,0 v400 q-60,-160 -40,-400z" fill="#a52a2a"/><rect y="380" width="600" height="120" fill="#8d5a2b"/><ellipse cx="300" cy="380" rx="200" ry="30" fill="#ffe08a" opacity=".25"/>`,
  };

  // ---------- render ----------
  function drawItem(it, i) {
    const [name, x0 = 300, y = 420, s = 1, o = {}] = it;
    // los personajes nunca se salen del encuadre visible
    const x = CAST[name] || BUILDERS[name] ? Math.min(550 - 40 * s, Math.max(50 + 40 * s, x0)) : x0;
    let out;
    if (CAST[name]) { const [b, base] = CAST[name]; out = BUILDERS[b]({ ...base, ...o }); }
    else if (BUILDERS[name]) out = BUILDERS[name](o);
    else if (PROPS[name]) out = PROPS[name](o);
    else { console.warn('Ilustración desconocida:', name); return ''; }
    const sx = o.flip ? -s : s, sy = o.mirror ? -s : s;
    const rot = o.rot ? ` rotate(${o.rot})` : '';
    const isChar = !!(CAST[name] || BUILDERS[name]);
    const anim = o.anim || (isChar ? 'idle' : '');
    const op = o.op != null ? ` opacity="${o.op}"` : '';
    return `<g transform="translate(${x} ${y})${rot} scale(${sx} ${sy})"${op}><g class="${anim ? 'a-' + anim : ''}" style="animation-delay:${(i * 0.37) % 2}s">${out}</g></g>`;
  }

  // Encuadre según la forma de la página: en páginas anchas se recorta cielo, en las altas se muestra más.
  function frame(ratio = 1.2) {
    let w, h;
    if (ratio >= 1.2) { h = 430; w = Math.min(600, h * ratio); }
    else { w = 560; h = Math.min(500, w / ratio); }
    return `${(300 - w / 2).toFixed(1)} ${(500 - h).toFixed(1)} ${w.toFixed(1)} ${h.toFixed(1)}`;
  }

  function scene(sc, cls = '', ratio) {
    const id = 'i' + (uid++).toString(36);
    const bg = (BG[sc.bg] || BG.meadow)(sc, id);
    const items = (sc.it || []).map(drawItem).join('');
    const tint = sc.time === 'night' && sc.bg !== 'room' ? '<rect width="600" height="500" fill="#0b1440" opacity=".12"/>' : '';
    return `<svg class="ill ${cls}" viewBox="${frame(ratio)}" preserveAspectRatio="xMidYMax slice" xmlns="http://www.w3.org/2000/svg" role="img">${bg}${items}${tint}</svg>`;
  }

  return { scene, CAST, PROPS, BUILDERS, BG };
})();
