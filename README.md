# Gadomancy: Reforged

**Русский** · [English](README.en.md)

Неофициальный порт **Gadomancy 1.7.10** для **Minecraft 1.21.1** на **NeoForge**. Аддон работает с [Thaumcraft 4: Reforged](https://github.com/notlikethiss/thaumcraft-4-reforged) и добавляет собственную вкладку в Таумономиконе.

## Что добавляет мод

- **Работа с эссенцией.** Конденсатор вмещает до 3000 единиц на аспект после улучшений Элементами тьмы. Липкие банки крепятся к стенам и потолкам, а связанные удалённые банки распределяют эссенцию между собой.
- **Автоматизация.** Магический выбрасыватель, упаковщик и коготь инфузии.
- **Аура и узлы.** Пилон ауры, манипулятор узлов и фокусы для изменения узла или открытия врат.
- **Големы и фамильяр.** Сердца разрушителя и телохранителя, големы из серебряного дерева и эфирный фамильяр с эффектами стихийных улучшений.

## Установка

1. Создайте профиль **Minecraft 1.21.1** с **NeoForge 21.1.256+**.
2. Скачайте [**Gadomancy 0.1.26-alpha+1.21.1**](https://github.com/Justevian/gadomancy-reforged/releases/tag/v0.1.26-alpha%2B1.21.1) из раздела [Releases](../../releases).
3. Положите JAR в папку `mods` вместе с **Thaumcraft 4: Reforged 1.1.3** для Minecraft 1.21.1 и **TerraBlender 1.21.1-4.1.0.1** и **Curios 9.5.1+1.21.1** (NeoForge).

[Скачать Thaumcraft 4: Reforged](https://github.com/notlikethiss/thaumcraft-4-reforged/releases/tag/1.21.1)

## Сборка из исходников

Установите **JDK 21** и поместите `thaumcraft-4-reforged-1.1.3-1.21.1.jar` в папку `libs` в корне репозитория. Выполните `./gradlew build` или `gradlew.bat build` на Windows. Готовый JAR появится в `build/libs`.

## Авторы и благодарности

- **makeo** и **HellFirePvP** — авторы [оригинального Gadomancy](https://github.com/makeo/Gadomancy).
- **Azanor** — автор оригинального Thaumcraft.
- **overseas** — автор порта [Thaumcraft 4: Reforged](https://github.com/notlikethiss/thaumcraft-4-reforged).

Лицензия кода Gadomancy — [LGPL-3.0-or-later](LICENSE).

## Автоматические проверки

JDK 21, отдельный каталог `run`; тестовые классы из `src/smoke` не входят в выпускаемый JAR.

- `./gradlew runServer -PportSmoke=true`: рецепты, блочные сущности, сохранение предметов и топлива, кодек посылок, создание големов, загрузка общих mixin-целей, симуляция и выполнение передачи предметов, выгрузка и разрушение упаковщика. Для первого запуска примите EULA в `run/eula.txt`.
- Для клиентской проверки скопируйте созданный тестовым сервером `run/world` в `run/saves/gadomancy-port-smoke`, затем выполните `./gradlew runClient -PclientSmoke=true`: модели и текстуры предметов, клиентские mixin-цели, загрузка одиночного мира и вызовы рендереров. Проверка автоматически закрывает игру.
- Для проверки совместимости можно дополнительно положить `thaumicbases-0.1.54-alpha+1.21.1.jar` в `libs`. Это необязательная зависимость только среды разработки.

На Windows используйте `gradlew.bat`. Без флагов `portSmoke`/`clientSmoke` проверки не запускаются.
