[![Downloads](http://cf.way2muchnoise.eu/1454405.svg)](https://www.curseforge.com/minecraft/mc-mods/ae2-fluid-terminals-rework)
[![MCVersion](http://cf.way2muchnoise.eu/versions/1454405.svg)](https://www.curseforge.com/minecraft/mc-mods/ae2-fluid-terminals-rework)
[![GitHub issues](https://img.shields.io/github/issues/NuanKi/AE2FluidTerminals-Rework.svg)](https://github.com/NuanKi/AE2FluidTerminals-Rework/issues)
[![GitHub pull requests](https://img.shields.io/github/issues-pr/NuanKi/AE2FluidTerminals-Rework.svg)](https://github.com/NuanKi/AE2FluidTerminals-Rework/pulls)

# AE2 Fluid Terminals Rework

Quality of life improvements for AE2 fluid terminals that reduce clicks and make common actions smoother.

## CurseForge
https://www.curseforge.com/minecraft/mc-mods/ae2-fluid-terminals-rework

## What it does
When using **FILL** in the AE2 Fluid Terminal, you normally need to pick up a fluid container onto your cursor first. This mod adds a shortcut:

- **FILL works even with an empty cursor**
  - Click a fluid in the terminal and the mod automatically finds a compatible container in your inventory and fills it.
  - Search order: hotbar left to right, then main inventory top-left to bottom-right.
- **If you are already holding a container**, behavior is unchanged (vanilla AE2 cursor-based filling works the same).
- **EMPTY** behavior is unchanged.

## Requirements
- Minecraft **1.12.2**
- Forge
- Applied Energistics 2 (AE2)

### Compatibility notes
- Compatible with **AE2UEL Wireless Universal Terminal**.

## Usage
1. Open an **AE2 Fluid Terminal** (or compatible wireless terminal).
2. Make sure you have an empty compatible fluid container somewhere in your inventory.
3. With an empty cursor, click a fluid in the terminal using **FILL**.
4. The first compatible container found (hotbar first) will be used automatically.

If you want to use a specific container, pick it up onto your cursor first. The mod will not change that flow.

## Building
Typical workflow:
1. Clone the repository
2. Import into your IDE (IntelliJ IDEA or Eclipse)
3. Run the ForgeGradle setup tasks for your environment
4. Build using Gradle

## Contributing
Issues and pull requests are welcome. If you submit a PR, keep changes focused and include a short description of what changed and why.

## License
MIT (see `LICENSE`).
