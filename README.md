# Winlator-Glibc with Box32 mod

It's a mod of Winlator Glibc with Box32 option.
Winlator-Glibc is a fork of the official Winlator. The Glibc version provides additional performance and stability improvements.
The goal is to provide more community friendly alternative. Collaboration are welcome.

This fork also represents the WinlatorXR version which runs the app in virtual headsets.

# Device requirement
* Android 8 or newer with ARM64 CPU
* Compatible GPU (Adreno GPUs have the best support)
* Legacy storage support (according to reports Coloros 15 and Oxygenos 15 are unsupported)

# Compiling

1. Clone the repository (requires `git` and `git-lfs` installed)
```
git clone git@github.com:longjunyu2/winlator.git
cd winlator
git submodule update --init --recursive
git lfs pull
```

2. Open the project in the Android Studio (we target latest stable version)

3. Install dependencies the Android Studio asks for

4. Connect your phone via USB with USB debugging enabled

5. Click run (green play icon)

# Links
- [Project downloads](https://github.com/longjunyu2/winlator/releases)
- [Project milestones](https://github.com/longjunyu2/winlator/milestones)
- [WinlatorXR on SideQuest](https://sidequestvr.com/app/37320/winlatorxr)

# Winlator

Winlator is an Android application that lets you to run Windows (x86_64) applications with Wine and Box86/Box64.

# Useful Tips

- If you are experiencing performance issues, try changing the Box64 preset to `Performance` in Container Settings -> Advanced Tab.
- For applications that use .NET Framework, try installing `Wine Mono` found in Start Menu -> System Tools.
- If some older games don't open, try adding the environment variable `MESA_EXTENSION_MAX_YEAR=2003` in Container Settings -> Environment Variables.
- Try running the games using the shortcut on the Winlator home screen, there you can define individual settings for each game.
- To display low resolution games correctly, try to enabling the `Force Fullscreen` option in the shortcut settings.
- To improve stability in games that uses Unity Engine, try changing the Box64 preset to `Stability` or in the shortcut settings add the exec argument `-force-gfx-direct`.

# Information

This project has been in constant development since version 1.0, the current app source code is up to version 7.1, I do not update this repository frequently precisely to avoid unofficial releases before the official releases of Winlator.

# Credits and Third-party apps
- GLIBC Patches by [Termux Pacman](https://github.com/termux-pacman/glibc-packages)
- Ubuntu RootFs ([Focal Fossa](https://releases.ubuntu.com/focal))
- Wine ([winehq.org](https://www.winehq.org/))
- Box86/Box64 by [ptitseb](https://github.com/ptitSeb)
- PRoot ([proot-me.github.io](https://proot-me.github.io))
- Mesa (Turnip/Zink/VirGL) ([mesa3d.org](https://www.mesa3d.org))
- DXVK ([github.com/doitsujin/dxvk](https://github.com/doitsujin/dxvk))
- VKD3D ([gitlab.winehq.org/wine/vkd3d](https://gitlab.winehq.org/wine/vkd3d))
- D8VK ([github.com/AlpyneDreams/d8vk](https://github.com/AlpyneDreams/d8vk))
- CNC DDraw ([github.com/FunkyFr3sh/cnc-ddraw](https://github.com/FunkyFr3sh/cnc-ddraw))

Many thanks to [brunodev85](https://github.com/brunodev85) (original Winlator) [ptitSeb](https://github.com/ptitSeb) (Box86/Box64), [Danylo](https://blogs.igalia.com/dpiliaiev/tags/mesa/) (Turnip) and others.<br>

---

<p align="center">
	<img src="logo.png" width="376" height="128" alt="Winlator Logo" />  
</p>
