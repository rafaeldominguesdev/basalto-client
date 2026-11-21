"""Monta o atlas da interface: formas arredondadas, anéis, sombra e ícones.

Tudo é branco com alfa; o jogo tinge com a cor do tema na hora de desenhar.
Densidade: 4 texels por unidade da interface.
Uso: python3 build_atlas.py <pasta-dos-icones> <saida-png> <saida-json>
"""
import json
import math
import os
import sys

from PIL import Image, ImageDraw, ImageFilter

D = 4          # texels por unidade
SS = 4         # supersampling das formas
PAD = 2

icons_dir, out_png, out_json = sys.argv[1:4]
sprites = {}   # nome -> imagem


def rounded(size_px, radius_px, stroke_px=None):
    """Retângulo arredondado (ou só o contorno) com antisserrilhado."""
    s = size_px * SS
    img = Image.new('L', (s, s), 0)
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([0, 0, s, s], radius=radius_px * SS, fill=255)
    if stroke_px:
        k = stroke_px * SS
        d.rounded_rectangle([k, k, s - k, s - k], radius=max(0, (radius_px - stroke_px) * SS), fill=0)
    img = img.resize((size_px, size_px), Image.LANCZOS)
    out = Image.new('RGBA', (size_px, size_px), (255, 255, 255, 0))
    out.putalpha(img)
    return out


# Preenchimentos e contornos 1:1 em pixels de tela, um por raio (1 a 48 px).
# Desenhados sem escala, os cantos saem nítidos em qualquer tamanho de interface.
MAX_R = 48
for r in range(1, MAX_R + 1):
    sprites[f'fill/{r}'] = rounded(r * 2 + 2, r)
    sprites[f'ring/{r}'] = rounded(r * 2 + 2, r, stroke_px=1)

# Sombra difusa: canto de 8 unidades desfocado. Margem = 16 unidades.
SH_R, SH_BLUR = 8, 16
size = (SH_R + SH_BLUR) * D * 2 + 8
base = Image.new('L', (size, size), 0)
m = SH_BLUR * D
ImageDraw.Draw(base).rounded_rectangle([m, m, size - 1 - m, size - 1 - m], radius=SH_R * D, fill=255)
base = base.filter(ImageFilter.GaussianBlur(SH_BLUR * D / 2.6))
shadow = Image.new('RGBA', (size, size), (255, 255, 255, 0))
shadow.putalpha(base)
sprites['shadow'] = shadow

# Ícones
for f in sorted(os.listdir(icons_dir)):
    if f.endswith('.png'):
        im = Image.open(os.path.join(icons_dir, f)).convert('RGBA')
        a = im.getchannel('A')
        white = Image.new('RGBA', im.size, (255, 255, 255, 0))
        white.putalpha(a)
        sprites['icon/' + f[:-4]] = white

# Empacotamento em prateleiras, maiores primeiro.
W = 2048
order = sorted(sprites.items(), key=lambda kv: (-kv[1].size[1], kv[0]))
x = y = shelf = 0
placed = {}
for name, im in order:
    w, h = im.size
    if x + w + PAD > W:
        x, y, shelf = 0, y + shelf + PAD, 0
    placed[name] = (x + PAD, y + PAD, w, h)
    x += w + PAD
    shelf = max(shelf, h + PAD)
H = 1 << math.ceil(math.log2(y + shelf + PAD))
atlas = Image.new('RGBA', (W, H), (255, 255, 255, 0))
for name, (px, py, w, h) in placed.items():
    atlas.paste(sprites[name], (px, py))
atlas.save(out_png, optimize=True)

meta = {
    'size': [W, H],
    'density': D,
    'maxRadius': MAX_R,
    'shadow': {'radius': SH_R, 'blur': SH_BLUR},
    'sprites': {k: list(v) for k, v in sorted(placed.items())},
}
with open(out_json, 'w') as fh:
    json.dump(meta, fh, separators=(',', ':'))
print(f'atlas {W}x{H}, {len(placed)} sprites')
