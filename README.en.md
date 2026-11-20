# Gadomancy: Reforged

[Русский](README.md) · **English**

An unofficial port of **Gadomancy 1.7.10** to **Minecraft 1.21.1** on **NeoForge**. This add-on requires [Thaumcraft 4: Reforged](https://github.com/notlikethiss/thaumcraft-4-reforged) and adds its own tab to the Thaumonomicon.

## Features

- **Essentia storage.** Upgrade the Essentia Condenser with Elements of Darkness to store up to 3,000 units per aspect. Sticky jars mount on walls and ceilings, and linked remote jars distribute essentia between them.
- **Automation.** Arcane Dropper, Arcane Packager, and Infusion Claw.
- **Aura and nodes.** Aura Pylon, Node Manipulator, and foci for changing nodes or opening a gate.
- **Golems and familiar.** Break and Bodyguard cores, silverwood golems, and an ethereal familiar with elemental upgrades.

## Installation

1. Set up **Minecraft 1.21.1** with **NeoForge 21.1.256+**.
2. Use the supplied experimental [**Gadomancy 0.1.26-alpha+1.21.1**](https://github.com/Justevian/gadomancy-reforged/releases/tag/v0.1.26-alpha%2B1.21.1) JAR.
3. Put the JAR in your `mods` folder alongside **Thaumcraft 4: Reforged 1.1.3** for Minecraft 1.21.1, **TerraBlender 1.21.1-4.1.0.1** and **Curios 9.5.1+1.21.1** (NeoForge).

[Download Thaumcraft 4: Reforged](https://github.com/notlikethiss/thaumcraft-4-reforged/releases/tag/1.21.1)

## Building from source

Install **JDK 21** and put `thaumcraft-4-reforged-1.1.3-1.21.1.jar` in the repository's `libs` folder. Run `./gradlew build` or `gradlew.bat build` on Windows. The JAR is written to `build/libs`.

## Credits

- **makeo** and **HellFirePvP** — [original Gadomancy](https://github.com/makeo/Gadomancy).
- **Azanor** — original Thaumcraft.
- **overseas** — [Thaumcraft 4: Reforged](https://github.com/notlikethiss/thaumcraft-4-reforged).

Gadomancy's code is licensed under [LGPL-3.0-or-later](LICENSE).

## Experimental 1.21.1 backport

This branch targets notlikethiss's Thaumcraft 4: Reforged 1.1.3 for Minecraft 1.21.1 on NeoForge 21.1.256+, Java 21. It is an alpha backport of our 0.1.25 release for 26.3; full progression and visual validation are ongoing. Do not downgrade 26.3 worlds.

For development place these files in `libs`: `thaumcraft-4-reforged-1.1.3-1.21.1.jar`, `TerraBlender-neoforge-1.21.1-4.1.0.1.jar`, `curios-neoforge-9.5.1+1.21.1.jar`. Dependencies are not bundled. Thaumic Bases is optional; its JAR can be placed in `libs` for coexistence checks.

Run `./gradlew build`. Server checks: `./gradlew runServer -PportSmoke=true` after accepting the Minecraft EULA in `run/eula.txt`. For client checks copy the generated `run/world` to `run/saves/gadomancy-port-smoke` and run `./gradlew runClient -PclientSmoke=true`. Checks use isolated development data and automatically stop; smoke classes are excluded from the released JAR.
