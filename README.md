# Basalto Client — Minecraft Java 26.1.2 (Fabric)

Client de mods com menu próprio (Right Shift), HUD configurável, teclas por módulo e perfis.

> Use no **singleplayer** ou em **servidores seus**. Em servidores públicos, módulos como KillAura, Fly, Speed, Reach e XRay são detectados por anticheat e dão ban.

---

## 1. Compilar no Windows

**Uma vez só:** instale o **JDK 25** (o Minecraft 26.1 exige Java 25).
- Baixe em https://adoptium.net/temurin/releases/?version=25 → Windows x64 → `.msi`
- Na instalação, marque **"Set JAVA_HOME variable"**.

**Para gerar o mod:**
1. Extraia esta pasta (ex.: `C:\basalto`).
2. Abra a pasta, clique na barra de endereço, digite `cmd` e aperte Enter.
3. Rode:
   ```
   gradlew.bat build
   ```
   A primeira vez demora alguns minutos (baixa o Gradle e o Minecraft).
4. O mod fica em `build\libs\basalto-0.2.0.jar`.

Se der erro na compilação, copie o texto do erro e mande pro Claude que ele corrige.

## 2. Instalar no jogo

1. Instale o **Fabric Loader** para **26.1.2**: https://fabricmc.net/use/installer/
2. Baixe a **Fabric API** para 26.1.2 (versão 0.155.3+26.1.2 ou mais nova): https://modrinth.com/mod/fabric-api
3. Coloque os dois `.jar` (Fabric API + `basalto-0.2.0.jar`) em:
   `%appdata%\.minecraft\mods`
4. No launcher, escolha o perfil **fabric-loader-26.1.2** e jogue.

## 3. Como usar

| Ação | Como |
|---|---|
| Abrir/fechar o menu | **Right Shift** (troca em Configurações › Controles e atalhos) |
| Ligar/desligar módulo | Toggle no card, ou clique direito no card |
| Opções do módulo | Clique no card (abre o painel lateral) |
| Tecla do módulo | No painel lateral, ou em Configurações › Controles e atalhos. Backspace remove |
| Tecla que só liga enquanto apertada | Modo da tecla: **Segurar** |
| Buscar módulo | **Ctrl+F** ou o campo de busca em Módulos |
| Favoritos | Estrela no card; aparecem nos atalhos rápidos do Painel |
| Fechar painel, modal ou o menu | **Esc** |
| Navegar pelo teclado | **Tab** / Shift+Tab, Enter ou espaço para ativar |

O menu tem cinco telas:

- **Painel:** ambiente (Local, LAN ou Servidor), módulos ativos, FPS/TPS, perfil ativo, atividade recente, favoritos, sessão e avisos.
- **Módulos:** grade com busca, filtros por categoria e estado, e o painel de opções de cada um.
- **Perfis:** criar, carregar, salvar, duplicar, renomear, exportar, importar, definir o perfil carregado ao abrir o jogo e excluir. Avisa quando há alterações não salvas.
- **HUD:** liga e desliga cada elemento da tela do jogo.
- **Configurações:** tema claro/escuro, cor de destaque, opacidade, tamanho da interface, animações, desfoque, sons, teclas e modo streamer.

Tudo fica salvo em `%appdata%\.minecraft\config\basalto.json`.

## 4. Módulos (35)

- **Combat:** KillAura, TriggerBot, AutoClicker, AimAssist, Reach, Velocity, AutoTotem
- **Movement:** Fly, Speed, Sprint, NoFall, Step, Spider, Glide, SafeWalk, Parkour, AutoWalk, BoatFly, NoSlow
- **Render:** ESP (contorno colorido através das paredes), Fullbright, XRay, Zoom, NoHurtCam, NoWeather
- **Player:** AutoEat, AutoTool, FastPlace, FastBreak, ChestStealer, AutoRespawn, AntiAFK
- **World:** Timer, Nuker, Scaffold

**HUD:** logo + FPS, lista de módulos ativos, coordenadas (com conversão do Nether), poções, durabilidade da armadura, keystrokes (WASD), info do alvo e notificações.

## 5. Estrutura do código

```
src/main/java/com/basalto/
  Basalto.java            entrada do mod, ticks e teclas
  module/                 Module, Category, ModuleManager
  setting/                opções (liga/desliga, número, modo)
  config/                 salvar/carregar e perfis
  ui/                     motor da interface: tema, tipografia, componentes
  gui/                    o menu (App) e as páginas em gui/pages
  hud/                    HUD, notificações e histórico da sessão
  modules/<categoria>/    cada módulo
  mixin/                  ganchos no código do Minecraft
```

Para criar um módulo novo: crie uma classe em `modules/<categoria>/` estendendo `Module`, sobrescreva `onUpdate()` e registre em `ModuleManager`.

O visual segue o design system descrito em [docs/design-system.md](docs/design-system.md).
