#!/bin/sh
# Regera o atlas e as fontes da interface. Precisa de node e python3 com Pillow.
# NODE_MODULES: pasta com lucide-static e @resvg/resvg-js instalados (npm i lucide-static @resvg/resvg-js).
set -e
cd "$(dirname "$0")"
TMP=$(mktemp -d)
RES=../../src/main/resources/assets/basalto
node render-icons.mjs "${NODE_MODULES:?defina NODE_MODULES}" "$TMP/icons"
mkdir -p "$RES/textures/ui"
python3 build_atlas.py "$TMP/icons" "$RES/textures/ui/atlas.png" "$RES/textures/ui/atlas.json"
python3 build_fonts.py "$RES/font"
