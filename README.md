<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="engine/src/main/resources/org/destinationsol/assets/textures/mainMenu/mainMenuLogo.png">
    <source media="(prefers-color-scheme: light)" srcset="readMeLogo.png">
    <img alt="Destination Sol" src="readMeLogo.png">
  </picture>
</p>

[![Gitpod Ready-to-Code](https://img.shields.io/badge/Gitpod-ready--to--code-blue?logo=gitpod)](https://gitpod.io/#https://github.com/MovingBlocks/DestinationSol)
[![Discord](https://img.shields.io/discord/270264625419911192.svg?label=discord)](http://discord.gg/Terasology)

**This is a personal development fork of Destination Sol (v2.1.0)**, forked from [MovingBlocks/DestinationSol](https://github.com/MovingBlocks/DestinationSol). It incorporates upstream bug fixes and open pull requests not yet merged there, along with additional crash-debugging improvements and gameplay fixes. See [todo.md](todo.md) for the full change log and known issues.

Destination Sol is an arcade space shooter originally started by Milosh Petrov and a small team on [Steam](http://store.steampowered.com/app/342980/). After releasing as an indie title, the team moved on to other projects and open-sourced the game. The open source group MovingBlocks behind [Terasology](http://terasology.org) stepped in to maintain it.

Destination Sol is licensed under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0.html) (except the soundtrack — see its section below).

You can download the official release on [Steam](http://store.steampowered.com/app/342980/) or the [Google Play Store](https://play.google.com/store/apps/details?id=com.miloshpetrov.sol2.android&hl=en). To build this fork from source see the section below.

Gameplay
--------

You start at the edge of a solar system as a pilot in a small ship. You are free to explore space, land on planets, fight with enemies, upgrade your ship and equipment, hire mercenaries, mine asteroids, and so on.

Enemy ships are orange icons, allies are blue. Enemies can be marked with a skull icon - beware! They are likely stronger than you. Improve your ship and equipment and fight them later!

Your ship has a certain number of hit points (your armor), which will recover if you have consumable repair kits in your inventory and stay idle for a short while. You may also have a shield that takes damage first. Each is vulnerable to different weapons, both on your ship and others.

Weapons and special abilities often need consumables to function (like Bullets or Slo Mo Charges) and take time to rearm.

You can destroy asteroids for easy money, even with the starting ship's ammo-less but weak gun.

Warnings get posted if you get close to dangerous ships or may soon collide with something on your current course. Blue dots along the edge of the screen indicate a planet is nearby.

Watch out about buying a new ship if you can only barely afford it - you might need to buy new compatible weaponry too!

Mercenaries will follow you around and start with a compatible weapon. They'll pick up items as well and keep them, greedy little buggers! But then they drop everything again on death, so ...

Controls
--------

Note: You can select either pure keyboard, keyboard + mouse, or controller (in the settings). Exact details may change over time. Below are the default key mappings (no mouse). You can change these in-game.

*Main screen*

* [Space] - Fire main gun
* [Ctrl] - Fire secondary gun (if equipped)
* [Shift] - Use ship ability
* [Left,Right] - Turn the ship
* [Up] - Thrust. There are no brakes! You have to turn and burn to change direction or slow down
* [Tab] - Show the map
* [I] - Show inventory
* [T] - Talk (interact with a station)
* [ESC] - Menu / close screens

*With map up*

* [Up, Down] - Zoom in and out on the map

*With inventory up*

* [Left, Right] - change page
* [Page Up, Page Down] - scroll up and down
* [Space] - equip / unequip item *OR* buy / sell if talking to a station
* [D] - discard selected item


Building and running from source
--------

You need **Java 11 or newer** (Java 17 recommended). All other dependencies are downloaded automatically by Gradle on first build.

Run all commands from the **project root directory** (where you cloned the project).

### Quick start (Windows)

Two convenience scripts are included in the project root.

**1. First-time setup** — checks for Java (installs Java 17 via `winget` if missing), then downloads all Gradle dependencies and compiles the project. Only needed once after a fresh clone:

```
setup.bat          (Command Prompt)
.\setup.ps1        (PowerShell)
```

> **PowerShell note:** if you see an execution-policy error, run this once and retry:
> ```
> Set-ExecutionPolicy RemoteSigned -Scope CurrentUser
> ```

**2. Launch the game from source:**

```
run.bat            (Command Prompt)
.\run.ps1          (PowerShell)
```

Both scripts print the log file path before launch and remind you to check it if the game exits with an error.

### Manual steps (Windows, Linux, macOS)

**Verify Java:**
```sh
java -version   # must print version 11 or higher
```

**Download dependencies and compile (first time only):**
```sh
# Windows
gradlew.bat :desktop:classes

# Linux / macOS — make the wrapper executable first if needed
chmod +x gradlew
./gradlew :desktop:classes
```

**Run from source:**
```sh
# Windows
gradlew.bat :desktop:run

# Linux / macOS
./gradlew :desktop:run
```

### Logs and crash reports

When running from source, the Gradle run task automatically enables debug-level file logging. Everything at INFO level and above is written to **`destinationsol.log`** in the project root (rolls over at 10 MB, keeps 3 backups). This is the first place to look after any crash.

Unhandled exceptions that reach the top-level crash reporter also write a timestamped `crash-<date>.log` to the same directory.

### Other Gradle tasks

| Task | Command |
|------|---------|
| Compile only | `gradlew :desktop:classes` |
| Build distributable zip (no bundled JRE) | `gradlew :desktop:distZipUnbundledJRE` |
| Build distributable zip (with bundled JREs) | `gradlew :desktop:distZipBundleJREs` |

### Troubleshooting

| Symptom | Fix |
|---------|-----|
| `'java' is not recognized` / `java: command not found` | Install Java 17 from [adoptium.net](https://adoptium.net/) and re-open your terminal |
| Build fails with `UnsupportedClassVersionError` | Your Java is too old — Java 11+ required, Java 17 recommended |
| `gradlew: Permission denied` (Linux / macOS) | Run `chmod +x gradlew` first |
| Game crashes immediately on launch | Open `destinationsol.log` in the project root — the full stack trace is there |
| Gradle hangs downloading dependencies | Check your internet connection; corporate proxies may need configuring in `~/.gradle/gradle.properties` |
| `.\setup.ps1` blocked by execution policy | Run `Set-ExecutionPolicy RemoteSigned -Scope CurrentUser` in an admin PowerShell, then retry |

IntelliJ IDEA imports the project automatically when you open the project directory — a pre-configured **Desktop** run configuration is included. Create an empty file named `devBuild` in the project root to tell the engine to load assets directly from source rather than from the compiled output.

For Android a little extra setup is needed. See instructions [here](https://github.com/MovingBlocks/DestSolAndroid).

[Steam Release Process](steam/SteamRelease.md)
------------

Contributors
------------
[GitHub contribution stats](https://github.com/MovingBlocks/DestinationSol/graphs/contributors)
[Contribution Leaderboard](http://destinationsol.org/contribute)

* Original creators: [Milosh Petrov](https://github.com/miloshpetrov), [Nika Burimenko](https://github.com/NoiseDoll), Kent C. Jensen, Julia Nikolaeva

* Contributors on GitHub: [Cervator](https://github.com/Cervator), [Rulasmur (PrivateAlpha)](https://github.com/Rulasmur), [theotherjay](https://github.com/theotherjay), [LinusVanElswijk](https://github.com/LinusVanElswijk), [SimonC4](https://github.com/SimonC4), [grauerkoala](https://github.com/grauerkoala), [rzats](https://github.com/rzats), [LadySerenaKitty](https://github.com/LadySerenaKitty), [askneller](https://github.com/askneller), [JGelfand](https://github.com/JGelfand), [AvaLanCS](https://github.com/Avalancs), [scirelli](https://github.com/scirelli), [Sigma-One](https://github.com/Sigma-One), [vampcat](https://github.com/vampcat), [Malanius](https://github.com/Malanius), [AonoZan](https://github.com/AonoZan), [ererbe](https://github.com/ererbe), [SurajDutta](https://github.com/SurajDuta), [jasyohuang](https://github.com/jasyohuang), [Steampunkery](https://github.com/Steampunkery), [Graviton48](https://github.com/Graviton48), [Adrijaned](https://github.com/Adrijaned), [MaxBorsch](https://github.com/MaxBorsch), [sohil123](https://github.com/sohil123), [FieryPheonix909](https://github.com/FieryPheonix909), [digitalripperynr](https://github.com/digitalripperynr), [NicholasBatesNZ](https://github.com/NicholasBatesNZ), [Pendi](https://github.com/ZPendi), [Torpedo99](https://github.com/Torpedo99), [AndyTechGuy](https://github.com/AndyTechGuy), [BenjaminAmos](https://github.com/BenjaminAmos), [dannykelemen](https://github.com/dannykelemen), [msteiger](https://github.com/msteiger), [oniatus](https://github.com/oniatus), [arpitkamboj](https://github.com/arpitkamboj), [manas96](https://github.com/manas96), [IsaacLic](https://github.com/IsaacLic), [Mpcs](https://github.com/Mpcs), [ZPendi](https://github.com/ZPendi), [nailorcngci](https://github.com/nailorcngci), [FearlessTobi](https://github.com/FearlessTobi), [ujjman](https://github.com/ujjman), [ThisIsPIRI](https://github.com/ThisIsPIRI), [Esnardo](https://github.com/Esnardo), [IsaiahBlanks](https://github.com/IsaiahBlanks), [DarkWeird](https://github.com/DarkWeird), [Mystic-Slice](https://github.com/Mystic-Slice), [superusercode](https://github.com/superusercode), [AdamJonsson](https://github.com/AdamJonsson), [sagarg22](https://github.com/sagarg22), [jankeromnes](https://github.com/jankeromnes)
... and your name here? :-) More coming!

* Soundtrack - Provided by [NeonInsect](https://github.com/NeonInsect) ([Soundcloud](https://soundcloud.com/neon-insect)) and copyrighted by him [CC-NC 4.0](https://creativecommons.org/licenses/by-nc/4.0/), free for our use with Destination Sol.
* All sprites from MillionthVector are licensed under a Creative Commons Attribution 4.0 International License - originally reachable at millionthvector.blogspot.id

Apologies in advance for any omissions, contact [Cervator](http://forum.terasology.org/members/cervator.2/) if you believe you've been missed :-)
