"""Recorta las láminas del creador de personajes («Mi personaje») en sus partes.

Uso, desde la raíz del repositorio:  python tools/crop-avatar.py

Cada lámina es una cuadrícula con fondo transparente; cada casilla trae una parte, en orden de lectura
(filas de arriba abajo, de izquierda a derecha). Si una lámina todavía no está, se usan partes provisionales.

- Cuerpos (uno por cada ropa): el niño o la niña de cuerpo entero, de frente, calvo y sin cara. La piel se detecta
  sola y se guarda aparte en gris, para que la app la pinte del tono elegido. También se mide dónde está la cabeza.
- Caras: ojos, cejas, nariz y boca, sin la cabeza. El iris es gris: se guarda aparte para pintarlo del color elegido.
- Peinados y accesorios: tal cual, recortados.

Resultado: app/src/main/assets/avatar/<parte>.webp y avatar/avatar.json
"""
import colorsys
import json
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw
from scipy import ndimage

ROOT = Path(__file__).resolve().parent.parent
LAMINAS = ROOT.parent / "BedStory-laminas"  # las láminas originales (.png), fuera del proyecto
OUT = ROOT / "app/src/main/assets/avatar"

# lámina -> (columnas, filas, tipo, sexo, nombres de las partes)
SHEETS = {
    "niño-ropa.png": (3, 2, "body", "boy", ["Camiseta y jeans", "Sudadera deportiva", "Overol", "Camisa y bermudas", "Pijama", "Traje de superhéroe"]),
    "niña-ropa.png": (3, 2, "body", "girl", ["Vestido", "Camiseta y falda", "Overol", "Sudadera y leggings", "Pijama", "Traje de superheroína"]),
    "caras-niño.png": (3, 2, "face", "boy", ["Sonriente", "Risa grande", "Guiño", "Sorprendido", "Pícaro", "Tranquilo"]),
    "caras-niña.png": (3, 2, "face", "girl", ["Sonriente", "Risa grande", "Guiño", "Sorprendida", "Pícara", "Tranquila"]),
    "pelo-niño.png": (4, 2, "hair", "boy", ["Corto", "Rizado", "Despeinado", "Con copete", "Rapado", "Afro", "Liso con fleco", "Largo"]),
    "pelo-niña.png": (4, 2, "hair", "girl", ["Coletas", "Trenzas", "Cola de caballo", "Melena corta", "Rizos largos", "Moño", "Liso largo", "Afro con moño"]),
    "accesorios.png": (4, 2, "accessory", None, ["Gorra roja", "Gorra azul", "Lentes redondos", "Lentes de estrella",
                                                             "Lentes de sol", "Aretes de perla", "Aretes de corazón", "Aretes de estrella"]),
}
# de qué tipo es cada accesorio (en el orden de la lámina) y si es solo para niñas
ACCESSORY_KINDS = [("cap", False), ("cap", False), ("glasses", False), ("glasses", False),
                   ("glasses", False), ("earrings", True), ("earrings", True), ("earrings", True)]
# correcciones a mano después de mirar el resultado: escala, desplazamiento (en anchos de cabeza) y anclaje del peinado
ADJUST = {
    "hair-boy-3": {"scale": 1.08, "anchor": [0.5, 0.53]},
    "hair-boy-4": {"anchor": [0.53, 0.6]},
    "hair-boy-5": {"anchor": [0.5074, 0.47]},
    "hair-boy-7": {"scale": 0.92},
    "hair-boy-8": {"scale": 1.12, "anchor": [0.5051, 0.36]},
    "hair-girl-1": {"scale": 1.1},
    "hair-girl-3": {"scale": 1.05, "anchor": [0.4, 0.4]},
    "hair-girl-5": {"scale": 1.08, "anchor": [0.5, 0.29]},
    "hair-girl-7": {"scale": 1.12, "anchor": [0.4985, 0.2]},
    "hair-girl-8": {"anchor": [0.5095, 0.47]},
}
MAX_SIDE = {"body": 900, "face": 420, "hair": 560, "accessory": 420}


def cells(img: Image.Image, cols: int, rows: int):
    """Cada parte de la cuadrícula. Un dibujo puede salirse un poco de su casilla (una coleta larga):
    cada trozo dibujado va a la casilla donde cae su centro, y la parte es todo lo de su casilla."""
    a = np.asarray(img)
    h, w = a.shape[:2]
    solid = a[..., 3] > 40
    labels, n = ndimage.label(ndimage.binary_dilation(solid, iterations=3))
    groups = [[] for _ in range(cols * rows)]
    for i, box in enumerate(ndimage.find_objects(labels), start=1):
        if (solid[box] & (labels[box] == i)).sum() < 60:
            continue  # motas
        ys, xs = box
        cy = (ys.start + ys.stop) / 2; cx = (xs.start + xs.stop) / 2
        groups[min(rows - 1, int(cy // (h / rows))) * cols + min(cols - 1, int(cx // (w / cols)))].append(i)
    for parts in groups:
        if not parts:
            raise SystemExit("casilla vacía en la lámina")
        keep = np.isin(labels, parts) & solid
        ys, xs = np.nonzero(keep)
        piece = a.copy()
        piece[..., 3] = np.where(keep, piece[..., 3], 0)
        yield Image.fromarray(piece[ys.min():ys.max() + 1, xs.min():xs.max() + 1])


def hair_anchor(img: Image.Image):
    """Dónde va la cara dentro del peinado, en fracciones de la imagen: el centro del hueco de la cara, la línea de la frente
    y el ancho del hueco. La cara está en las columnas donde, bajo el pelo de arriba, ya no hay más pelo hasta abajo
    (a los lados caen los mechones, la cola o las trenzas). Así un peinado de lado queda bien puesto."""
    al = np.asarray(img)[..., 3] > 60
    h, w = al.shape
    # se rellenan los huecos finos entre mechones, para que el pelo sea una sola mancha
    pad = 12
    al = ndimage.binary_closing(np.pad(al, pad), iterations=pad)[pad:-pad, pad:-pad]
    face = np.zeros(w, bool); edge = np.zeros(w, int)
    for x in range(w):
        col = np.nonzero(al[:, x])[0]
        if len(col) == 0:
            continue
        run_end = col[0]
        while run_end + 1 < h and al[run_end + 1, x]:
            run_end += 1
        if not al[run_end + 1:, x].any() and run_end < h * 0.85:
            face[x] = True; edge[x] = run_end + 1
    best, start = None, None
    for x in range(w + 1):
        if x < w and face[x]:
            start = x if start is None else start
        elif start is not None:
            if best is None or x - start > best[1] - best[0]:
                best = (start, x)
            start = None
    if best is None or best[1] - best[0] < w * 0.2:
        return None
    g0, g1 = best
    mid = edge[g0 + (g1 - g0) // 4: g1 - (g1 - g0) // 4]
    return [round((g0 + g1) / 2 / w, 4), round(float(np.median(mid)) / h, 4), round((g1 - g0) / w, 4)]


# colores de pelo: el primero es el de la lámina (castaño); los demás se pintan conservando luces y sombras
HAIR_COLORS = [None, (0.07, 0.35, 0.16), (0.08, 0.55, 0.62), (0.12, 0.6, 0.92), (0.04, 0.75, 0.78), (0.92, 0.45, 0.95)]


def recolor_hair(img: Image.Image, target):
    """Pinta el pelo de otro color: cambia el tono de lo que es pelo (no las ligas ni los moños de otro color)
    y escala la luz para que el pelo claro quede claro y el oscuro, oscuro."""
    a = np.asarray(img).astype(np.float32)
    hue, s, v = hsv(a.astype(np.uint8))
    opaque = a[..., 3] > 0
    hairish = opaque & ((np.abs(((hue - 0.07 + 0.5) % 1) - 0.5) < 0.08) | (s < 0.25))
    ref = float(np.median(v[hairish & (s > 0.25)])) if (hairish & (s > 0.25)).any() else 0.4
    th, ts, tv = target
    nv = np.clip(v / ref * tv, 0, 1)
    ns = np.where(s < 0.25, s * ts, ts) * np.clip(1.2 - nv * 0.4, 0, 1)
    import colorsys as cs
    rgb = np.zeros(a.shape[:2] + (3,), np.float32)
    # conversión HSV -> RGB vectorizada
    i = np.floor(th * 6) % 6; f = th * 6 - np.floor(th * 6)
    p_ = nv * (1 - ns); q = nv * (1 - f * ns); t = nv * (1 - (1 - f) * ns)
    r, g, b = {0: (nv, t, p_), 1: (q, nv, p_), 2: (p_, nv, t), 3: (p_, q, nv), 4: (t, p_, nv), 5: (nv, p_, q)}[int(i)]
    out = a.copy()
    for k, ch in enumerate((r, g, b)):
        out[..., k] = np.where(hairish, ch * 255, a[..., k])
    return Image.fromarray(out.clip(0, 255).astype(np.uint8))


def hsv(a):
    rgb = a[..., :3].astype(np.float32) / 255
    mx = rgb.max(-1); mn = rgb.min(-1)
    v = mx; s = np.where(mx > 0, (mx - mn) / np.maximum(mx, 1e-6), 0)
    r, g, b = rgb[..., 0], rgb[..., 1], rgb[..., 2]
    d = np.maximum(mx - mn, 1e-6)
    hue = np.where(mx == r, ((g - b) / d) % 6, np.where(mx == g, (b - r) / d + 2, (r - g) / d + 4)) / 6
    return hue, s, v


def shading(a, mask, ref_v):
    """Una capa gris con la luz y la sombra de [mask]: blanco donde el color es el de referencia, más oscuro en las sombras."""
    _, _, v = hsv(a)
    g = np.clip(v / max(ref_v, 1e-3), 0, 1) * 255
    out = np.zeros(a.shape[:2] + (4,), np.uint8)
    out[..., 0] = out[..., 1] = out[..., 2] = g.astype(np.uint8)
    out[..., 3] = np.where(mask, a[..., 3], 0)
    return Image.fromarray(out)


def strip_border(a):
    """Quita el borde blanco de calcomanía que trae el dibujo (la app le pone uno nuevo al personaje entero):
    lo blanco que toca el exterior, sin pasar por las líneas oscuras del contorno."""
    _, s, v = hsv(a)
    outside = a[..., 3] < 40
    whiteish = (s < 0.15) & (v > 0.8)
    region = outside | whiteish
    labels, _ = ndimage.label(np.pad(region, 1, constant_values=True))
    border = (labels == labels[0, 0])[1:-1, 1:-1] & ~outside
    out = a.copy()
    out[..., 3] = np.where(border, 0, out[..., 3])
    return out


def body_parts(img: Image.Image):
    """Separa la piel del cuerpo y mide la cabeza (la piel de la parte de arriba de la figura)."""
    a = strip_border(np.asarray(img))
    h, w = a.shape[:2]
    hue, s, v = hsv(a)
    opaque = a[..., 3] > 200
    # el color de la piel: el más común en la parte de arriba de la figura (la cabeza calva, sin cara)
    top = opaque.copy(); top[int(h * 0.28):] = False
    cand = top & (s > 0.08) & (s < 0.6) & (v > 0.45)
    hues = np.round(hue[cand] * 36).astype(int) % 36
    ref_h = (np.bincount(hues).argmax() + 0.5) / 36
    near = cand & (np.abs(((hue - ref_h + 0.5) % 1) - 0.5) < 0.05)
    ref_s, ref_v = float(np.median(s[near])), float(np.median(v[near]))
    dh = np.abs(((hue - ref_h + 0.5) % 1) - 0.5)
    skin = opaque & (dh < 0.05) & (np.abs(s - ref_s) < 0.25) & (v > ref_v * 0.35)
    skin = ndimage.binary_opening(skin, iterations=1)
    # los bordes suavizados de la piel (entre la piel y su contorno) también se pintan, menos el borde blanco
    not_white = (s > 0.08) | (v < 0.85)
    skin = skin | (ndimage.binary_dilation(skin, iterations=2) & opaque & not_white)
    # la cabeza, por la silueta: desde la coronilla hasta el cuello (la fila más angosta bajo la parte más ancha de la cabeza)
    rows = np.nonzero(opaque.any(1))[0]
    top_row = int(rows.min())
    widths = opaque.sum(1)
    upper = range(top_row, top_row + int(h * 0.35))
    widest = max(upper, key=lambda y: widths[y])
    neck = min(range(widest, top_row + int(h * 0.45)), key=lambda y: widths[y])
    xs = np.nonzero(opaque[widest])[0]
    head_box = [int(xs.min()), top_row, int(xs.max() - xs.min() + 1), int(neck - top_row)]
    return Image.fromarray(a), shading(a, skin, ref_v), head_box


def face_parts(img: Image.Image):
    """El iris (gris, sin color) se guarda aparte para pintarlo del color de ojos elegido."""
    a = np.asarray(img)
    _, s, v = hsv(a)
    iris = (a[..., 3] > 200) & (s < 0.14) & (v > 0.32) & (v < 0.82)
    iris = ndimage.binary_opening(iris, iterations=1)
    ref_v = float(np.percentile(v[iris], 80)) if iris.any() else 0.7
    return shading(a, iris, ref_v)


# ---------------- partes provisionales ----------------
SKIN = (249, 205, 170)
OUTLINE = (60, 40, 50)


def ph_body(i, sex):
    colors = [(70, 120, 200), (220, 80, 80), (90, 160, 90), (240, 180, 50), (150, 110, 200), (40, 170, 190)]
    w, h = 400, 800
    im = Image.new("RGBA", (w, h), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    c = colors[i % len(colors)]
    d.rectangle((140, 560, 190, 760), fill=(60, 70, 120), outline=OUTLINE, width=5)   # piernas
    d.rectangle((210, 560, 260, 760), fill=(60, 70, 120), outline=OUTLINE, width=5)
    d.ellipse((125, 740, 200, 790), fill=(90, 60, 40), outline=OUTLINE, width=5)
    d.ellipse((200, 740, 275, 790), fill=(90, 60, 40), outline=OUTLINE, width=5)
    d.rounded_rectangle((60, 330, 100, 560), 18, fill=SKIN, outline=OUTLINE, width=5)  # brazos
    d.rounded_rectangle((300, 330, 340, 560), 18, fill=SKIN, outline=OUTLINE, width=5)
    if sex == "girl":
        d.polygon([(110, 300), (290, 300), (330, 600), (70, 600)], fill=c, outline=OUTLINE)
    else:
        d.rounded_rectangle((100, 300, 300, 580), 30, fill=c, outline=OUTLINE, width=5)
    d.rectangle((180, 270, 220, 310), fill=SKIN)                                     # cuello
    d.ellipse((90, 30, 310, 290), fill=SKIN, outline=OUTLINE, width=6)               # cabeza
    d.text((150, 420), "provisional", fill=(255, 255, 255))
    return im


def ph_face(i, sex):
    im = Image.new("RGBA", (220, 160), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    for x in (55, 165):
        d.ellipse((x - 26, 30, x + 26, 82), fill=(255, 255, 255), outline=OUTLINE, width=4)
        d.ellipse((x - 15, 40, x + 15, 72), fill=(140, 140, 140))
        d.ellipse((x - 7, 49, x + 7, 63), fill=(20, 20, 20))
        d.line((x - 26, 14 - (i % 3) * 3, x + 26, 10), fill=OUTLINE, width=6)
        if sex == "girl":
            d.line((x - 30, 34, x - 40, 26), fill=OUTLINE, width=4)
    if i % 2 == 0:
        d.arc((70, 85, 150, 145), 10, 170, fill=(180, 40, 60), width=7)
    else:
        d.ellipse((90, 100, 130, 140), fill=(180, 40, 60), outline=OUTLINE, width=4)
    return im


def ph_hair(i, sex):
    colors = [(70, 40, 20), (20, 20, 20), (200, 150, 60), (150, 60, 30)]
    c = colors[i % 4]
    w, h = (300, 380) if sex == "girl" else (300, 200)
    im = Image.new("RGBA", (w, h), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    d.chord((0, 0, w, 220), 180, 360, fill=c, outline=OUTLINE, width=5)
    d.rectangle((0, 108, w, 125), fill=c)
    if sex == "girl":
        d.rounded_rectangle((0, 100, 50, h), 20, fill=c, outline=OUTLINE, width=5)
        d.rounded_rectangle((w - 50, 100, w, h), 20, fill=c, outline=OUTLINE, width=5)
    d.text((w / 2 - 30, 50), f"pelo {i + 1}", fill=(255, 255, 255))
    return im


def ph_accessory(i):
    kind = ACCESSORY_KINDS[i][0]
    if kind == "cap":
        im = Image.new("RGBA", (320, 170), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
        c = [(220, 50, 50), (50, 90, 210)][i % 2]
        d.chord((20, 0, 260, 240), 180, 360, fill=c, outline=OUTLINE, width=6)
        d.rounded_rectangle((150, 110, 320, 150), 15, fill=c, outline=OUTLINE, width=6)
    elif kind == "glasses":
        im = Image.new("RGBA", (300, 110), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
        c = [(40, 40, 40), (230, 90, 160), (20, 20, 20)][i - 2]
        fill = (30, 30, 30, 200) if i == 4 else None
        d.ellipse((10, 10, 130, 100), fill=fill, outline=c, width=9)
        d.ellipse((170, 10, 290, 100), fill=fill, outline=c, width=9)
        d.line((130, 50, 170, 50), fill=c, width=9)
    else:
        im = Image.new("RGBA", (300, 70), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
        c = [(250, 250, 245), (230, 50, 90), (250, 200, 40)][i - 5]
        for x in (25, 275):
            d.ellipse((x - 20, 15, x + 20, 55), fill=c, outline=OUTLINE, width=4)
    return im


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for f in OUT.glob("*.webp"):
        f.unlink()
    data = {"bodies": {"boy": [], "girl": []}, "faces": {"boy": [], "girl": []}, "hair": {"boy": [], "girl": []}, "accessories": [],
            "provisional": []}
    for sheet, (cols, rows, kind, sex, names) in SHEETS.items():
        path = LAMINAS / sheet
        if path.exists():
            parts = list(cells(Image.open(path).convert("RGBA"), cols, rows))
            print(f"{sheet}: {len(parts)} partes")
        else:
            make = {"body": lambda i: ph_body(i, sex), "face": lambda i: ph_face(i, sex), "hair": lambda i: ph_hair(i, sex),
                    "accessory": ph_accessory}[kind]
            parts = [make(i) for i in range(len(names))]
            data["provisional"].append(sheet)
            print(f"{sheet}: falta, {len(parts)} partes provisionales")
        for i, (part, name) in enumerate(zip(parts, names)):
            part.thumbnail((MAX_SIDE[kind], MAX_SIDE[kind]), Image.LANCZOS)
            pid = f"{kind}-{sex or 'all'}-{i + 1}"
            part.save(OUT / f"{pid}.webp", lossless=True)
            entry = {"id": pid, "name": name, "image": f"avatar/{pid}.webp"}
            fix = ADJUST.get(pid, {})
            if kind == "body":
                full, skin, head = body_parts(part)
                full.save(OUT / f"{pid}.webp", lossless=True)  # sin el borde blanco
                skin.save(OUT / f"{pid}-skin.webp", lossless=True)
                entry.update(skin=f"avatar/{pid}-skin.webp", head=head, size=[part.width, part.height])
                data["bodies"][sex].append(entry)
            elif kind == "face":
                face_parts(part).save(OUT / f"{pid}-iris.webp", lossless=True)
                entry.update(iris=f"avatar/{pid}-iris.webp")
                data["faces"][sex].append(entry)
            elif kind == "hair":
                anchor = fix.get("anchor") or hair_anchor(part)
                if anchor:
                    entry.update(anchor=anchor)
                part = Image.fromarray(strip_border(np.asarray(part)))  # sin el borde blanco, que se veía junto a la cara
                part.save(OUT / f"{pid}.webp", lossless=True)
                colors = [entry["image"]]
                for n, target in enumerate(HAIR_COLORS[1:], start=1):
                    recolor_hair(part, target).save(OUT / f"{pid}-c{n}.webp", lossless=True)
                    colors.append(f"avatar/{pid}-c{n}.webp")
                entry.update(colors=colors)
                data["hair"][sex].append(entry)
            else:
                Image.fromarray(strip_border(np.asarray(part))).save(OUT / f"{pid}.webp", lossless=True)
                k, girls = ACCESSORY_KINDS[i]
                entry.update(kind=k, girlsOnly=girls)
                data["accessories"].append(entry)
    for group in (data["bodies"], data["faces"], data["hair"]):
        for entries in group.values():
            for e in entries:
                if "scale" in ADJUST.get(e["id"], {}) or "dx" in ADJUST.get(e["id"], {}):
                    f = ADJUST[e["id"]]; e["adjust"] = [f.get("dx", 0), f.get("dy", 0), f.get("scale", 1)]
    (OUT / "avatar.json").write_text(json.dumps(data, ensure_ascii=False, indent=1), encoding="utf-8")
    print("listo:", OUT)


if __name__ == "__main__":
    main()
