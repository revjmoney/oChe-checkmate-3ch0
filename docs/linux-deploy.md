# Linux on top (Debian/Ubuntu chroot)

You can run a full Linux userland **alongside** Android using the existing kernel — no risky
re-partitioning. Two routes; both need root (you have Magisk). **Watch space:** `/data` only has
~2 GB free on this device, so use a small image / minimal distro.

## Route A — Linux Deploy (full chroot, best performance)
1. Install **Linux Deploy** (`ru.meefik.linuxdeploy`) — F-Droid (use the Versions tab for a 7.1
   build), or `adb install -r` the APK from PC.
2. Open it → grant root (Magisk Grant prompt).
3. Settings (the ⚙ / properties screen):
   - **Distribution:** Debian (or Ubuntu/Alpine — Alpine is tiny if space is tight)
   - **Installation type:** File (creates a disk image) · **Installation path:** e.g.
     `/data/local/linux.img` · **Image size:** keep it small (e.g. 1024–1500 MB given ~2 GB free)
   - **Architecture:** match your CPU (`getprop ro.product.cpu.abi` → armhf for armeabi-v7a,
     arm64 for arm64-v8a)
   - Enable **SSH** (and/or **GUI/VNC** if you want a desktop)
4. Tap **Install** (downloads the rootfs — needs WiFi). When done, **Start**.
5. Connect: `ssh <user>@<echo-ip> -p 22` (or the port shown), or VNC to `:5900` for a desktop.

## Route B — Termux + proot-distro (no image, simplest)
1. Install **Termux** from F-Droid.
2. In Termux:
   ```
   pkg update && pkg install proot-distro
   proot-distro install debian
   proot-distro login debian
   ```
3. You're in Debian (proot — no kernel modules, but `apt` works). Add a desktop + `tigervnc` if you
   want GUI over VNC.

## Tips
- **Alpine** (Linux Deploy) or a minimal Debian keeps the image small for this ~2 GB-free device.
- Route A (chroot) is faster and more "real"; Route B (proot) is easiest and needs no image file.
- Both pull packages from the internet → WiFi required (safe now: OTA dead + hosts block).
