"""Gera os JSON de fonte da interface (Manrope) para cada estilo e densidade.

O Minecraft desenha TTF com filtro "nearest", então cada estilo tem uma versão
por densidade (1 a 4 pixels por unidade, em passos) para a letra sair nítida.
Precisa casar com com.basalto.ui.Type.
"""
import json
import os
import sys

STYLES = {
    'caption': (500, 6.5),
    'label': (500, 7),
    'body': (400, 7),
    'body_medium': (500, 7),
    'body_semi': (600, 7),
    'title': (600, 8.5),
    'heading': (700, 11),
    'metric': (700, 14),
    'brand': (800, 8),
}

# Pixels de tela por unidade da interface que o jogo pode usar.
DENSITIES = [1, 1.25, 1.5, 1.75, 2, 2.5, 3, 3.5, 4]

out = sys.argv[1]
os.makedirs(out, exist_ok=True)
for name, (weight, size) in STYLES.items():
    for density in DENSITIES:
        data = {'providers': [{
            'type': 'ttf',
            'file': f'basalto:manrope-{weight}.ttf',
            'size': size,
            'oversample': density,
        }]}
        with open(os.path.join(out, f'{name}_{int(density * 100)}.json'), 'w') as fh:
            json.dump(data, fh, indent=2)
print(f'{len(STYLES) * len(DENSITIES)} fontes')
