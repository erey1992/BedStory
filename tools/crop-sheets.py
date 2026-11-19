"""Recorta las hojas de 3x3 de dibujos para colorear y de fondos para el juego de calcomanías.

Uso, desde la raíz del repositorio:  python tools/crop-sheets.py
Las viñetas van en orden de lectura; una celda vacía de la hoja se salta (en la lista va None).
Dibujos para colorear: app/src/main/assets/coloring/<id del cuento>.webp (cada uno es de su cuento).
Fondos: app/src/main/assets/backgrounds/<nombre>.webp
"""
import importlib.util
import shutil
from pathlib import Path
import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
LAMINAS = ROOT.parent / "BedStory-laminas"  # las láminas originales (.png), fuera del proyecto
ASSETS = ROOT / "app/src/main/assets"
spec = importlib.util.spec_from_file_location("crop", ROOT / "tools/crop-illustrations.py")
crop = importlib.util.module_from_spec(spec); spec.loader.exec_module(crop)

COLORING = {
    "Nueve cuentos clásicos para colorear.png": [
        "tres-cerditos", "caperucita", "ricitos", "liebre-tortuga", "patito-feo", "cenicienta", "gato-botas", "pinocho", "blancanieves"],
    "Nueve cuentos clásicos para colorear (1).png": [
        "bella-durmiente", "sirenita", "hansel-gretel", "rapunzel", "bella-bestia", "pulgarcita", "aladino", "peter-pan", "alicia"],
    "Nueve aventuras de cuento para colorear.png": [
        "mago-oz", "libro-selva", "principito", "cascanueces", "lago-cisnes", "mulan", "hercules", "espada-piedra", "campanilla"],
    "Nueve cuentos fantásticos para colorear-1.png": [
        "robin-hood", "rey-midas", "tres-mosqueteros", "soldadito-plomo", "hermanas-invierno", "nino-dragon", "manada-hielo",
        "gigante-metal", "hotel-monstruos"],
    "Cinco cuentos fantásticos para colorear-2.png": [
        "panda-maestro", "emperador-llama", "cachorros-manchas", "dinosaurio-valle", "ogro-pantano", None, None, None, None],
}
# nombre del archivo y el título que ve el niño
BACKGROUNDS = {
    "Nueve paisajes infantiles llenos de color-1.png": [
        ("luna", "La Luna"), ("colonia-lunar", "Casitas en la Luna"), ("nave", "Nave espacial"), ("arrecife", "Fondo del mar"),
        ("barco-hundido", "Barco hundido"), ("playa", "Playa"), ("isla", "Isla de piratas"), ("bosque", "Casita del bosque"),
        ("selva", "Selva")],
    "Nueve escenarios infantiles llenos de color-2.png": [
        ("hielo", "Tierra del hielo"), ("sabana", "Sabana"), ("mercado", "Mercado"), ("salon", "Salón del palacio"),
        ("cabana", "Cabaña"), ("cuarto", "Mi cuarto"), ("banquete", "Banquete del rey"), ("granja", "Granja"), ("faro", "Faro")],
    "Nueve escenarios mágicos para cuentos.png": [
        ("piscina", "Piscina"), ("huerto", "Huerto"), ("cueva-cristales", "Cueva de cristales"), ("escuela-cueva", "Escuela de la cueva"),
        ("fabrica-suenos", "Fábrica de sueños"), ("jardin-gigante", "Jardín gigante"), ("navidad", "Navidad"), ("taller", "Taller de inventos"),
        ("feria", "Feria")],
}
COLORING_WIDTH = 1000  # se amplía para que el pincel y el balde tengan resolución


def cells(img, n):
    """Las hojas son cuadrículas regulares de 3x3: cada celda se recorta un poco hacia dentro, sin el marco ni el hueco blanco."""
    assert n == 9, n
    return [(x0 + 8, y0 + 8, x1 - 8, y1 - 8) for x0, y0, x1, y1 in crop.grid(img, 3, 3, gap=0)]


def trim_frame(page):
    """Quita el marco negro de la celda: desde cada borde, avanza hasta pasar la última línea casi toda oscura."""
    a = np.asarray(page) < 110
    h, w = a.shape
    def inner(lines):
        last = -1
        for i, dark in enumerate(lines[:40]):
            if dark > 0.6:
                last = i
        return last + 4
    top = inner([a[y].mean() for y in range(h)]); bottom = inner([a[h - 1 - y].mean() for y in range(h)])
    left = inner([a[:, x].mean() for x in range(w)]); right = inner([a[:, w - 1 - x].mean() for x in range(w)])
    return page.crop((left, top, w - right, h - bottom))


def main():
    out = ASSETS / "coloring"
    shutil.rmtree(out, ignore_errors=True); out.mkdir(parents=True)
    for sheet, ids in COLORING.items():
        img = Image.open(LAMINAS / sheet).convert("L")
        for story_id, (x0, y0, x1, y1) in zip(ids, cells(img.convert("RGB"), len(ids))):
            if story_id is None:
                continue
            page = trim_frame(img.crop((x0 - 6, y0 - 6, x1 + 6, y1 + 6)))
            page = page.resize((COLORING_WIDTH, round(page.height * COLORING_WIDTH / page.width)), Image.LANCZOS)
            a = np.asarray(page).astype(np.float32)
            a = np.clip((a - 90) * 255 / (215 - 90), 0, 255)  # líneas negras y fondo blanco limpios
            Image.fromarray(a.astype(np.uint8)).save(out / f"{story_id}.webp", lossless=True, method=6)
        print(f"{sheet}: {sum(i is not None for i in ids)} dibujos")
    out = ASSETS / "backgrounds"
    shutil.rmtree(out, ignore_errors=True); out.mkdir(parents=True)
    for sheet, names in BACKGROUNDS.items():
        img = Image.open(LAMINAS / sheet).convert("RGB")
        for (name, _), (x0, y0, x1, y1) in zip(names, cells(img, len(names))):
            img.crop((x0, y0, x1, y1)).save(out / f"{name}.webp", quality=90, method=6)
        print(f"{sheet}: {len(names)} fondos")
    # el título de cada fondo, para el catálogo de premios
    import json
    titles = {name: title for names in BACKGROUNDS.values() for name, title in names}
    (out / "titles.json").write_text(json.dumps(titles, ensure_ascii=False), encoding="utf-8")


if __name__ == "__main__":
    main()
