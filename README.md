# Basalto Client — Minecraft Java 26.1.2 (Fabric)

Client de mods com ClickGUI (Right Shift), HUD configurável, teclas por módulo e configs salvas.

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
4. O mod fica em `build\libs\basalto-0.1.0.jar`.

Se der erro na compilação, copie o texto do erro e mande pro Claude que ele corrige.

## 2. Instalar no jogo

1. Instale o **Fabric Loader** para **26.1.2**: https://fabricmc.net/use/installer/
2. Baixe a **Fabric API** para 26.1.2 (versão 0.155.3+26.1.2 ou mais nova): https://modrinth.com/mod/fabric-api
3. Coloque os dois `.jar` (Fabric API + `basalto-0.1.0.jar`) em:
   `%appdata%\.minecraft\mods`
4. No launcher, escolha o perfil **fabric-loader-26.1.2** e jogue.

## 3. Como usar

| Ação | Como |
|---|---|
| Abrir/fechar o menu | **Right Shift** (dá pra trocar no painel Client) |
| Ligar/desligar módulo | Clique esquerdo |
| Abrir opções do módulo | Clique direito |
| Mudar a tecla do módulo | Clique do meio, ou "Tecla:" nas opções. Backspace remove |
| Buscar módulo | Só digitar com o menu aberto |
| Mover painel | Arraste pelo título |

**Painel Client:** cor de destaque, opacidade dos painéis, desfoque do fundo, tecla do menu, o que aparece no HUD e as configs (Padrão, PvP, Survival). Clique numa config para carregar e em "Salvar em ..." para guardar o estado atual.

Tudo fica salvo em `%appdata%\.minecraft\config\basalto.json`.

## 4. Módulos (34)

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
  gui/ClickGuiScreen.java o menu
  hud/                    HUD e notificações
  modules/<categoria>/    cada módulo
  mixin/                  ganchos no código do Minecraft
```

Para criar um módulo novo: crie uma classe em `modules/<categoria>/` estendendo `Module`, sobrescreva `onUpdate()` e registre em `ModuleManager`.
