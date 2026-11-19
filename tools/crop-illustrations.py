"""Recorta las láminas ilustradas (cuadrícula de viñetas sobre fondo blanco) en una imagen por página.

Uso, desde la raíz del repositorio:  python tools/crop-illustrations.py
Las viñetas van en orden de lectura: la primera es la portada y las siguientes, las páginas.
Las imágenes resultantes quedan en app/src/main/assets/art/<id>/<n>.webp (0 = portada).
"""
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
LAMINAS = ROOT.parent / "BedStory-laminas"  # las láminas originales (.png), fuera del proyecto
OUT = ROOT / "app/src/main/assets/art"
SHEETS = {
    "tres-cerditos": "Los Tres Cerditos.png",
    "caperucita": "Caperucita Roja.png",
    "ricitos": "Ricitos de Oro y los Tres Osos.png",
    "liebre-tortuga": "La Liebre y la Tortuga.png",
    "patito-feo": "El Patito Feo.png",
    "cigarra-hormiga": "La Cigarra y la Hormiga.png",
    "leon-raton": "El León y el Ratón.png",
    "raton-campo-ciudad": "El Ratón de Campo y Ciudad.png",
    "juan-habichuelas": "Juan y las Habichuelas Mágicas.png",
    "pedro-lobo": "Pedro y el lobo.png",
    "cenicienta": "Cenicienta.png",
    "gato-botas": "El Gato con Botas.png",
    "pinocho": "Pinocho.png",
    "ratita-presumida": "La ratita presumida.png",
    "zorro-cuervo": "El zorro y el cuervo.png",
    "bella-durmiente": "La Bella Durmiente.png",
    "musicos-bremen": "Los músicos de Bremen.png",
    "sirenita": "La Sirenita.png",
    "siete-cabritillos": "El lobo y los siete cabritillos.png",
    "blancanieves": "Blancanieves y los siete enanitos.png",
    "mago-oz": "El mágico camino de Oz.png",
    "princesa-guisante": "La princesa y el guisante en viñetas.png",
    "soldadito-plomo": "Soldadito de plomo.png",
    "aladino": "Aladino y su genio.png",
    "ali-baba": "Alí Babá y el tesoro escondido.png",
    "espada-piedra": "Arturo y la espada legendaria.png",
    "atlantida": "Atlántida.png",
    "balto": "Balto.png",
    "bebe-pomposo": "Bebé y Don Pomposo.png",
    "emperador-llama": "El emperador y la llama mágica.png",
    "espejo-cantor": "El espejo canto.png",
    "hermanos-hechizo": "El hechizo mágico del jardín.png",
    "estrella-deseos": "La estrella de los deseos y la esperanza.png",
    "caja-musica": "La princesa de la caja musical.png",
    "yeti-nube": "Nube y Yuna.png",
    "veinte-mil-leguas": "Aventuras del Nautilus bajo el mar.png",
    "gnomo-detective": "El gnomo detective y el erizo.png",
    "rumpelstiltskin": "Rumpelstiltskin_ magia, oro y misterio.png",
    "scrooge": "Scrooge descubre la alegría navideña.png",
    "rey-midas": "El toque dorado del rey Midas.png",
    "estela-brilla": "Estela, la estrella de luz propia.png",
    "castora-nila": "Nila, la castora perseverante.png",
    "libro-errores": "Pipo y los errores mágicos.png",
    "viento-erik": "Erik y el viento rebelde.png",
    "luciernaga-parpadeo": "Lucio y Brisa, luces de amistad.png",
    "no-son-novios": "Mateo y Jimena, amigos de verdad.png",
    "mudanza-luna": "Ema y su hogar lunar.png",
    "pulpo-abrazos": "Otto aprende a respetar el espacio.png",
    "secreto-piedra": "El secreto que pesaba.png",
    "tambor-abuela": "Inti y el tambor del cóndor.png",
    "fabrica-suenos": "La fábrica de sueños de Algodón.png",
    "monstruo-despuesito": "Ximena y el monstruo Despuesito.png",
    "amigo-imaginario": "Pilar y Sombrerón, amigo imaginario.png",
    "leona-melena": "Melena y su gran transformación.png",
    "monstruo-miedoso": "Jazmín y el monstruo miedoso.png",
    "gigante-chiquito": "Lucho, el pequeño gigante.png",
    "fosforito": "Fosforito y la seguridad en casa.png",
    "faro-papa": "Tomasina y la luz del faro.png",
    "erizo-puas": "Púas aprende cuándo pinchar.png",
    "bisabuela-tiempo": "El viaje temporal de la bisabuela.png",
    "lobo-hermano": "Aullido, hermano mayor de la manada.png",
    "anillo-pastel": "Papá casi come el anillo.png",
    "lentes-lupe": "Lupe y sus lentes mágicos.png",
    "monstruo-basura": "El monstruo y la playa limpia.png",
    "alcancia-puerquito": "Andrea, el puerquito y la bicicleta.png",
    "palabras-magicas": "Benito y las verdaderas palabras mágicas.png",
    "cebra-paso": "Celia, la cebra del cruce seguro.png",
    "canguro-bolsa": "Coco aprende a saltar.png",
    "dado-tramposo": "El dado tramposo y la amistad.png",
    "capitana-sol": "La capitana Sol y la aventura inclusiva.png",
    "lalo-aburrido": "Lalo y el cohete de cartón.png",
    "loro-interrumpe": "Paco y la pluma dorada.png",
    "potrilla-campeona": "Pimienta aprende a ganar.png",
    "perdido-feria": "Ramiro y la feria luminosa.png",
    "nubes-hermanas": "Rayo y Llovizna, hermanas del arcoíris.png",
    "ardilla-remolino": "Remolino en la escuela del bosque.png",
}
# láminas sin separación blanca entre viñetas: se recortan como cuadrícula de 3 columnas
GRID = {"blancanieves"}
# láminas con una viñeta hecha de varias escenitas: se juntan los trozos que caen en la misma celda de la cuadrícula de 3x3
MERGE = {"mochila-oficios"}
INSET = 7  # px que se recortan de cada borde para quitar las esquinas redondeadas


def runs(flags, min_len):
    """Tramos consecutivos de valores False (contenido) con longitud mínima."""
    out, start = [], None
    for i, gutter in enumerate(list(flags) + [True]):
        if not gutter and start is None:
            start = i
        elif gutter and start is not None:
            if i - start >= min_len:
                out.append((start, i))
            start = None
    return out


def panels(img):
    w, h = img.size
    px = img.load()
    white = [[min(px[x, y]) > 232 for x in range(w)] for y in range(h)]
    row_white = [sum(r) / w for r in white]
    result = []
    for y0, y1 in runs([f > 0.95 for f in row_white], 100):
        col_white = [sum(white[y][x] for y in range(y0, y1)) / (y1 - y0) for x in range(w)]
        for x0, x1 in runs([f > 0.9 for f in col_white], 100):
            # ajusta arriba/abajo con las columnas de esta viñeta (las filas no siempre están alineadas)
            inner = [sum(white[y][x] for x in range(x0, x1)) / (x1 - x0) for y in range(h)]
            ys = runs([f > 0.9 for f in inner], 100)
            ys = [r for r in ys if r[0] < y1 and r[1] > y0]
            if ys:
                result.append((x0, ys[0][0], x1, ys[0][1]))
    return result


def key(text):
    """Nombre comparable: sin acentos, mayúsculas ni signos («El Flautista de Hamelín» == «el flautista de hamelin»)."""
    from unicodedata import normalize
    return "".join(c for c in normalize("NFD", text.lower()) if c.isalnum())


def merge_cells(img, boxes, cols=3, rows=3):
    w, h = img.size
    cells = {}
    for x0, y0, x1, y1 in boxes:
        key = (int((y0 + y1) / 2 // (h / rows)), int((x0 + x1) / 2 // (w / cols)))
        a = cells.get(key)
        cells[key] = (x0, y0, x1, y1) if a is None else (min(a[0], x0), min(a[1], y0), max(a[2], x1), max(a[3], y1))
    # cada celda ocupa todo el alto de su fila y todo el ancho de su columna (los trozos sueltos pueden quedarse cortos)
    rows_y = {r: (min(c[1] for k, c in cells.items() if k[0] == r), max(c[3] for k, c in cells.items() if k[0] == r)) for r, _ in cells}
    cols_x = {q: (min(c[0] for k, c in cells.items() if k[1] == q), max(c[2] for k, c in cells.items() if k[1] == q)) for _, q in cells}
    return [(cols_x[q][0], rows_y[r][0], cols_x[q][1], rows_y[r][1]) for r, q in sorted(cells)]


def grid(img, cols=3, rows=3, gap=4):
    w, h = img.size
    cw, rh = w / cols, h / rows
    return [(round(c * cw + gap), round(r * rh + gap), round((c + 1) * cw - gap), round((r + 1) * rh - gap))
            for r in range(rows) for c in range(cols)]


def main():
    from json import loads
    catalog = {s["id"]: s for s in loads((ROOT / "app/src/main/assets/stories.json").read_text("utf-8"))}
    # además de SHEETS, cualquier lámina en BedStory-laminas cuyo nombre coincida con el título de un cuento
    by_title = {key(s["title"]): s["id"] for s in catalog.values()}
    sheets = dict(SHEETS)
    for f in LAMINAS.glob("*.png"):
        story_id = by_title.get(key(f.stem))
        if story_id and story_id not in sheets:
            sheets[story_id] = f.name
    errors = []
    for story_id, name in sheets.items():
        img = Image.open(LAMINAS / name).convert("RGB")
        boxes = grid(img) if story_id in GRID else panels(img)
        if story_id in MERGE:
            boxes = merge_cells(img, boxes)
        expected = 1 + len(catalog[story_id]["pages"])
        if not 0 < len(boxes) <= expected:
            errors.append(f"{name} ({story_id}): {len(boxes)} viñetas, se esperaban {expected}")
            continue
        if len(boxes) < expected:  # las páginas sin viñeta conservan la escena vectorial
            print(f"AVISO {name}: faltan {expected - len(boxes)} viñetas (páginas {len(boxes)}–{expected - 1})")
        dest = OUT / story_id
        dest.mkdir(parents=True, exist_ok=True)
        for n, (x0, y0, x1, y1) in enumerate(boxes):
            img.crop((x0 + INSET, y0 + INSET, x1 - INSET, y1 - INSET)).save(dest / f"{n}.webp", quality=90, method=6)
        print(f"{story_id}: {len(boxes)} ilustraciones {[(b[2] - b[0], b[3] - b[1]) for b in boxes]}")
    assert not errors, "\n".join(errors)


if __name__ == "__main__":
    main()
