"""Recorta las hojas de calcomanías pintadas (fondo transparente) en una imagen por calcomanía.

Uso, desde la raíz del repositorio:  python tools/crop-stickers.py
Cada calcomanía es del cuento indicado en SHEETS, en orden de lectura (filas de arriba abajo, de izquierda a derecha).
Se gana al terminar ese cuento. Resultado: app/src/main/assets/stickers/<id del cuento>.webp
Las calcomanías de objetos y animales (tools/content/stickers.json) van a stickers/obj/<id>.webp.
"""
import json
from pathlib import Path
import numpy as np
from PIL import Image, ImageDraw, ImageFont
from scipy import ndimage

ROOT = Path(__file__).resolve().parent.parent
LAMINAS = ROOT.parent / "BedStory-laminas"  # las láminas originales (.png), fuera del proyecto
OUT = ROOT / "app/src/main/assets/stickers"
SHEETS = {
    "25 stickers de cuentos fantásticos.png": [
        "pulgarcito", "flautista-hamelin", "aladino", "ali-baba", "peter-pan",
        "alicia", "mago-oz", "libro-selva", "principito", "isla-tesoro",
        "cascanueces", "mulan", "espada-piedra", "robin-hood", "scrooge",
        "jardin-dentro", "libro-errores", "hija-relojero", "pulpo-abrazos", "jarron-oro",
        "fabrica-suenos", "escama-dragon", "dos-conchas", "campeonato-caracoles", "semilla-cometas",
    ],
    "Colección de personajes de cuentos de hadas.png": [
        "caperucita", "tres-cerditos", "ricitos", "liebre-tortuga", "patito-feo",
        "cigarra-hormiga", "leon-raton", "juan-habichuelas", "pedro-lobo", "cenicienta",
        "gato-botas", "pinocho", "blancanieves", "ratita-presumida", "bella-durmiente",
        "musicos-bremen", "sirenita", "siete-cabritillos", "hansel-gretel", "rapunzel",
        "bella-bestia", "princesa-guisante", "principe-rana", "traje-emperador", "soldadito-plomo",
    ],
    "Diez personajes de fantasía en stickers.png": [
        "capitana-sol", "mochila-oficios", "capa-mama", "goma-unicornio", "semillas-abuelo",
        "unica-luna", "monstruo-miedoso", "fantasma-nuevo", "amigo-imaginario", None,  # Sir Teobaldo: cuento quitado
    ],
}
MAX_SIDE = 360

# Calcomanías de objetos, animales y personas (tools/content/stickers.json): van en láminas de 5 columnas, 25 por lámina,
# en el orden de la lista. «Calcomanías 1.png» trae las 25 primeras, «Calcomanías 2.png» las 25 siguientes...
OBJECTS = json.loads((ROOT / "tools/content/stickers.json").read_text(encoding="utf-8"))["stickers"]
OBJ_OUT = OUT / "obj"
PER_SHEET = 25


def object_sheets():
    ids = list(OBJECTS)
    return {f"Calcomanías {n + 1}.png": ids[i:i + PER_SHEET] for n, i in enumerate(range(0, len(ids), PER_SHEET))}


def placeholder(name: str) -> Image.Image:
    """Imagen provisional: una calcomanía redonda con el nombre, hasta que llegue la pintada."""
    size = 300
    im = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.ellipse((6, 6, size - 6, size - 6), fill=(255, 255, 255, 255))
    d.ellipse((20, 20, size - 20, size - 20), fill=(240, 226, 250, 255), outline=(150, 110, 190, 255), width=4)
    try:
        font = ImageFont.truetype("arialbd.ttf", 34)
        small = ImageFont.truetype("arial.ttf", 22)
    except OSError:
        font = small = ImageFont.load_default()
    words, lines = name.split(), [""]
    for w in words:  # partir el nombre en renglones que quepan
        test = (lines[-1] + " " + w).strip()
        if d.textlength(test, font=font) > size - 80 and lines[-1]:
            lines.append(w)
        else:
            lines[-1] = test
    y = size / 2 - len(lines) * 21
    for line in lines:
        d.text((size / 2, y), line, font=font, fill=(80, 40, 120, 255), anchor="ma")
        y += 42
    d.text((size / 2, size - 70), "provisional", font=small, fill=(150, 110, 190, 255), anchor="ma")
    return im


def stickers(img: Image.Image, cols: int, rows: int):
    """Las hojas son cuadrículas: cada trozo opaco va a la celda donde cae su centro, y una calcomanía son todos los de su celda."""
    a = np.asarray(img)
    h, w = a.shape[:2]
    solid = a[..., 3] > 200  # el borde blanco de cada calcomanía es opaco; el halo de alrededor, no
    solid = ndimage.binary_opening(solid, iterations=2)
    labels, n = ndimage.label(solid)
    cells = [[] for _ in range(cols * rows)]
    for i, box in enumerate(ndimage.find_objects(labels), start=1):
        ys, xs = box
        if (labels[box] == i).sum() < solid.size * 0.0004:
            continue  # motas sueltas
        cy = (ys.start + ys.stop) / 2; cx = (xs.start + xs.stop) / 2
        cells[int(cy // (h / rows)) * cols + int(cx // (w / cols))].append(i)
    for parts in cells:
        assert parts, "celda vacía en la hoja"
        mask = np.isin(labels, parts)
        ys, xs = np.nonzero(mask)
        box = (slice(ys.min(), ys.max() + 1), slice(xs.min(), xs.max() + 1))
        m = ndimage.binary_dilation(ndimage.binary_fill_holes(mask[box]), iterations=3)
        piece = a[box].copy()
        piece[..., 3] = np.where(m, piece[..., 3], 0)  # fuera de la silueta: transparente (sin restos del vecino)
        yield Image.fromarray(piece)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for sheet, ids in SHEETS.items():
        cols, rows = (5, len(ids) // 5)
        for story_id, sticker in zip(ids, stickers(Image.open(LAMINAS / sheet).convert("RGBA"), cols, rows)):
            if story_id is None:
                continue
            sticker.thumbnail((MAX_SIDE, MAX_SIDE), Image.LANCZOS)
            sticker.save(OUT / f"{story_id}.webp", quality=92, method=6)
        print(f"{sheet}: {sum(i is not None for i in ids)} calcomanías")
    OBJ_OUT.mkdir(parents=True, exist_ok=True)
    for n, (sheet, ids) in enumerate(object_sheets().items(), start=1):
        if not (LAMINAS / sheet).exists() and (LAMINAS / f"{n}.png").exists():
            sheet = f"{n}.png"  # también vale la lámina guardada solo con su número
        if (LAMINAS / sheet).exists():
            for sticker_id, sticker in zip(ids, stickers(Image.open(LAMINAS / sheet).convert("RGBA"), 5, (len(ids) + 4) // 5)):
                sticker.thumbnail((MAX_SIDE, MAX_SIDE), Image.LANCZOS)
                sticker.save(OBJ_OUT / f"{sticker_id}.webp", quality=92, method=6)
            print(f"{sheet}: {len(ids)} calcomanías")
        else:
            # mientras no llegue la lámina, cada calcomanía lleva una imagen provisional con su nombre
            for sticker_id in ids:
                placeholder(OBJECTS[sticker_id][1]).save(OBJ_OUT / f"{sticker_id}.webp", quality=90)
            print(f"{sheet}: falta, {len(ids)} provisionales")

if __name__ == "__main__":
    main()
