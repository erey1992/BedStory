"""Recorta la lámina de insignias (cuadrícula 5×5, en el orden de ACHIEVEMENTS) en assets/badges/<id>.webp.

Uso, desde la raíz del repositorio:  python tools/crop-badges.py
Cada insignia lleva una máscara circular, así el fondo queda transparente y no asoman las vecinas.
"""
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
LAMINAS = ROOT.parent / "BedStory-laminas"  # las láminas originales (.png), fuera del proyecto
SHEET = LAMINAS / "25 insignias mágicas de lectura.png"
OUT = ROOT / "app/src/main/assets/badges"
# en el mismo orden que ACHIEVEMENTS (Achievements.kt) y que la lámina, de izquierda a derecha y de arriba abajo
IDS = [
    "primer-cuento", "lector-curioso", "raton-biblioteca", "devoralibros", "gran-lector",
    "maestro-mama-oca", "amigo-animales", "invitado-corte", "aprendiz-mago", "domador-dragones",
    "lobo-de-mar", "astronauta", "viajero-tiempo", "corazon-valiente", "amigo-marti",
    "pequeno-poeta", "racha-3", "racha-5", "racha-10", "buho-nocturno",
    "gallo-madrugador", "oidos-atentos", "maraton", "cuento-del-dia", "otra-vez",
]
SIZE = 320


def main():
    img = Image.open(SHEET).convert("RGBA")
    w, h = img.size
    cw, ch = w / 5, h / 5
    OUT.mkdir(parents=True, exist_ok=True)
    mask = Image.new("L", (SIZE * 4, SIZE * 4), 0)
    ImageDraw.Draw(mask).ellipse((6, 6, SIZE * 4 - 6, SIZE * 4 - 6), fill=255)
    mask = mask.resize((SIZE, SIZE), Image.LANCZOS)  # borde suavizado
    for n, badge_id in enumerate(IDS):
        r, c = divmod(n, 5)
        cell = img.crop((round(c * cw), round(r * ch), round((c + 1) * cw), round((r + 1) * ch))).resize((SIZE, SIZE), Image.LANCZOS)
        alpha = Image.composite(cell.getchannel("A"), Image.new("L", cell.size, 0), mask)
        cell.putalpha(alpha)
        cell.save(OUT / f"{badge_id}.webp", quality=92, method=6)
    print(f"{len(IDS)} insignias en {OUT}")


if __name__ == "__main__":
    main()
