# Instruções para o Claude no Windows

Fale com o usuário em português do Brasil.

## Contexto

Este repositório vai receber o "Basalto Client", um mod client-side em Fabric para Minecraft Java 26.1.2 (ClickGUI no Right Shift, HUD configurável, perfis de config e 34 módulos de utilidade/hack para singleplayer e servidores próprios).

O código ainda NÃO está aqui. Ele está num zip que o usuário baixa no PC: `basalto-client-26.1.2.zip`, com uma pasta `basalto` dentro, que já tem um commit git local na branch `main`. Neste repositório, por enquanto, só existe este arquivo.

Versões (do fabric-example-mod oficial, branch 26.1.2): minecraft 26.1.2, loader 0.20.1, loom 1.19-SNAPSHOT (plugin `net.fabricmc.fabric-loom`, sem mappings, Mojang names, jogo não ofuscado), fabric-api 0.155.3+26.1.2, Java 25, Gradle 9.8.2.

O código NUNCA foi compilado. Ele foi escrito conferindo os nomes contra a Fabric API 26.1.2 e o Wurst7 (branch 26.1.2), mas pode ter erros de assinatura.

## Tarefas, em ordem

1. Ache o zip (normalmente em `%USERPROFILE%\Downloads`; se não estiver lá, pergunte ao usuário). Descompacte em `%USERPROFILE%\Projetos\basalto-client` e entre na pasta `basalto`.

2. Confira o JDK 25 com `java -version`. Se não tiver, instale com `winget install EclipseAdoptium.Temurin.25.JDK`. Confira também o `git` e o `gh` (`winget install GitHub.cli`; rode `gh auth login` se não estiver logado na conta `rafaeldominguesdev`).

3. Publique no GitHub. O remote é `https://github.com/rafaeldominguesdev/basalto-client`. Ele já tem um commit (este arquivo), com histórico diferente do commit do zip. Então:
   ```
   git remote add origin https://github.com/rafaeldominguesdev/basalto-client.git
   git pull origin main --allow-unrelated-histories --no-rebase --no-edit
   git push -u origin main
   ```
   (se o remote já existir, use `git remote set-url origin ...`). Não use force push.

4. Compile com `.\gradlew.bat build`. Se der erro, corrija um por um até passar. Para descobrir os nomes certos de classes e métodos do Minecraft 26.1.2, consulte o código-fonte gerado pelo Loom (`.\gradlew.bat genSources`) ou clone como referência numa pasta temporária:
   ```
   git clone --depth 1 -b 26.1.2 https://github.com/Wurst-Imperium/Wurst7.git %TEMP%\wurst
   git clone --depth 1 -b 26.1.2 https://github.com/FabricMC/fabric.git %TEMP%\fabric
   ```
   Não copie código do Wurst (é GPL); use só para conferir nomes de API. Mixins com nomes incertos já usam `require = 0`.

5. Quando compilar, rode `.\gradlew.bat runClient` para testar: abra um mundo singleplayer, aperte Right Shift, ligue alguns módulos (Fly, ESP, Fullbright, XRay, KillAura) e veja se o HUD aparece. Corrija o que quebrar.

6. Faça commit das correções (um commit por correção ou assunto, não um commitzão) e `git push`. O .jar final fica em `build\libs\basalto-0.1.0.jar`.

7. Crie uma Release no GitHub:
   ```
   gh release create v0.1.0 build\libs\basalto-0.1.0.jar
   ```
   com notas em português explicando como instalar: Fabric Loader 26.1.2 + Fabric API + o .jar em `.minecraft\mods`. O mod é um .jar Java, então funciona igual no Windows, no macOS e no Linux.

Antes de qualquer coisa que apague arquivos ou mude configurações do GitHub além do push e da release, pergunte ao usuário. Este arquivo pode ser apagado depois que tudo estiver pronto, mas pergunte antes.
