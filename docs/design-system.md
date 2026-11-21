# Design system do Basalto

Tudo que o menu e o HUD desenham segue estes tokens. O código fica em
`src/main/java/com/basalto/ui` e as telas em `src/main/java/com/basalto/gui`.

## Unidade e densidade

A interface trabalha em **unidades**. Uma unidade vale `p` pixels de tela, e `p`
sai do tamanho da janela (`Ui.autoDensity`): perto de 2 em 1080p, 3 em 1440p e 4 em 4K.
A preferência "Tamanho da interface" multiplica por 0,8 (Pequeno) ou 1,25 (Grande).
Tudo é arredondado para o pixel na hora de desenhar, então contornos de 1 px e
cantos arredondados ficam nítidos em qualquer escala.

## Cor (`Theme`)

| Token | Escuro | Uso |
|---|---|---|
| window | #0C1214 (com a opacidade escolhida) | fundo da janela |
| surface | #101719 | cards |
| surface2 | #172124 | ícones, campos, chips |
| elevated | #1D2B2D | modais, menus, toasts |
| accent | #54F5C4 | preenchimento de ações e estados ativos |
| accent2 | #2ED8A3 | pressionado |
| accentSoft | #8CFFE1 | realces suaves |
| text / text2 / text3 | #E6EDEC / #91A4A3 / #627675 | principal, secundário, apagado |
| border / borderStrong | branco 8% / 15% | contornos de 1 px |
| danger / warning / info | #F2817A / #E9C46A / #8FB8D8 | erro, aviso, informação |

O tema claro usa os mesmos nomes; o destaque escurece para texto (`accentText`)
para manter o contraste. O verde-água só aparece em ação principal, estado ligado,
indicador ativo e foco. Superfícies grandes nunca são preenchidas com ele.

## Tipografia (`Type`, Manrope)

| Estilo | Peso | Tamanho | Uso |
|---|---|---|---|
| CAPTION | 500 | 6,5 | rótulos de seção, metadados |
| LABEL | 500 | 7 | descrições curtas |
| BODY / BODY_MEDIUM / BODY_SEMI | 400 / 500 / 600 | 7 | texto, itens, botões |
| TITLE | 600 | 8,5 | títulos de card e modal |
| HEADING | 700 | 11 | título da página |
| METRIC | 700 | 14 | números do Painel |

O Minecraft desenha TTF com filtro "nearest", então cada estilo tem uma fonte por
densidade (`assets/basalto/font/<estilo>_<densidade>.json`), gerada por
`tools/ui-assets/build_fonts.py`.

## Espaço, raio, sombra, movimento

- Espaçamento: 2, 4, 6, 8, 12, 16, 20, 24, 32.
- Raios: 4 (campos, chips), 6 (botões), 8 (cards, 16 px em 1080p), 10 (janela, modais).
- Sombras: difusas, só embaixo de cards, menus e modais (`Ui.shadow`). Brilho na cor
  de destaque (`Ui.glow`) só em toggle ligado, botão principal e item ativo.
- Movimento: 150 ms (hover), 200 ms (troca de página, toggles), 250 ms (janela).
  "Animações: Reduzidas" tira deslocamentos e mantém esmaecimentos; "Desligadas" tira tudo.

## Componentes (`Kit`)

Botão (primário, secundário, terciário, destrutivo, só ícone), toggle, checkbox,
slider, segmentado, dropdown, campo de texto, captura de tecla, seletor de cor,
badge, card e estado vazio. Todos têm hover, pressionado, desativado e anel de foco
para o teclado; o botão tem estado carregando.

## Estados cobertos

Carregando (avatar sem skin), vazio (busca sem resultado, sem favoritos, sem
atividade), ativo/inativo (cards e toggles), erro (validação de nome, falha ao
exportar/importar), sem mundo, salvo/salvando, alteração não salva no perfil,
modal de confirmação e toasts.

## Teclado

Tab e Shift+Tab percorrem os controles, Enter ou espaço ativam, setas mexem em
sliders e segmentados, Esc fecha o que estiver por cima (dropdown, modal, painel,
menu), Ctrl+F abre a busca de módulos.

## Regerar assets

```sh
npm i lucide-static @resvg/resvg-js   # numa pasta qualquer
NODE_MODULES=/essa/pasta/node_modules tools/ui-assets/gerar.sh
```

Ícones novos entram em `tools/ui-assets/icons.txt`.
