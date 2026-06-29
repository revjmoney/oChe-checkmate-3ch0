# The clean break: LineageOS 18.1 (Android 11) on cronos

After fully de-Amazoning Fire OS, we hit the wall the device is *designed* to have: Fire OS routes
basic hardware (mic, audio, SystemUI, Settings) **through its Alexa plumbing**. Remove the spyware
and the UI itself crash-loops:
- `com.android.systemui` → needs `amazon.speech.sim` (Speech Interaction Manager) → which crash-loops
  for an Alexa media-DRM native lib (`libreggae_widevine.so`).
- `com.android.settings` → needs `com.amazon.kindle.otter.oobe` resources (country list).
- **The mic** capture path is wired through the Alexa audio stack we removed.

Conclusion: **Fire OS can't be both de-Amazoned AND functional.** The clean fix is to replace it.
Good news — a community port exists, and the bootloader's already unlocked (amonet).

## What you flash
- **LineageOS 18.1 (Android 11), UNOFFICIAL, for `cronos`** by bengris32 / R0rt1z2 / FieryFlames.
- Release: <https://github.com/amazon-oss/releases/releases/tag/lineage-18.1-cronos-v0.3>
- File: `lineage-18.1-20260624-UNOFFICIAL-cronos.zip`
- **SHA256:** `a7ae5375798ac00d6c1c30b4036a291feaf2e54fc941efa79a3772f281f8b114` (verify before flashing!)
- Trick behind it: they repurposed the **MT8163 Android 9 kernel + blobs** from Amazon's Fire tablets,
  so it runs newer Android than stock. (Kernel/device-tree sources are linked on the XDA thread.)

## Verify the device THREE ways first (never flash `checkers`/`crown`)
```
adb shell getprop ro.product.device      # cronos
adb shell getprop ro.product.name        # cronos
adb shell getprop ro.boot.hardware       # mt8163
```
This device: `cronos` / model `AEOCN` / `mt8163` / build `NS6570`. `verifiedbootstate=yellow`,
`flash.locked=1` are normal for an amonet device (flash via TWRP, not fastboot).

## Back up EVERYTHING first (two physical drives)
No `vbmeta`/`dtbo`/`vendor`/`nvram` partitions exist on this pre-Treble MT8163. Dump the criticals
raw to the PC (read-only, safe) — irreplaceable ones are **lk, tee1/tee2, persist, kb/dkb, MISC**:
```
# partition map: ls -l /dev/block/platform/bootdevice/by-name/
for pair in boot:9 recovery:10 lk:3 tee1:4 tee2:6 persist:14 logo:5 MISC:8 kb:1 dkb:2 metadata:15 expdb:7 swdl:11; do
  name=${pair%:*}; num=${pair#*:}
  adb exec-out "su -c 'dd if=/dev/block/mmcblk0p$num 2>/dev/null'" > "$name.img"
done
sha256sum *.img > SHA256SUMS.txt
```
Keep these + the amonet stock `.ab` backup on **two** drives. The flash only wipes system/data/cache —
it does NOT touch lk/tee/persist, so anti-rollback is never at risk.

## Install (verbatim from the XDA thread — do NOT improvise wipes)
1. Reboot to **TWRP** (Vol-Up + AC charger).
2. **Wipe → Format Data →** type `yes` → ✓
3. **Wipe → Advanced Wipe →** Data + System + Cache → swipe
4. **Advanced → ADB Sideload → swipe**, then on PC:
   `adb sideload lineage-18.1-20260624-UNOFFICIAL-cronos.zip`
   *(sideload because Format Data wiped internal storage)*
5. **Wipe → Format Data →** type `yes` → ✓  *(again, per instructions)*
6. **Reboot → System** — first boot 5–10 min. **No GApps/Magisk/mods until one clean boot.**

## What works / doesn't (cronos LineageOS 18.1)
- ✅ touch, **WiFi**, **Bluetooth**, **speaker/audio**, brightness, **mic** (just "quieter than expected")
- ❌ **Camera** (known broken)
- ⚠️ SELinux **Permissive** (don't store secrets), **deep sleep disabled**, battery always reports 100%,
  **the MUTE button is now the POWER button** (no more mic-mute/display-wake conflict)
- EXPERIMENTAL — expect bugs. First-boot health check showed **0 FATAL crashes** vs Fire OS's endless spew.

## After first boot
- Your custom APKs reinstall and run: **BackyardPyro** (no root), GOD MODE / Display Off / Web Control
  (their root features need Magisk).
- **F-Droid/Aurora installs "just work"** — LineageOS has **no Fire OS `no_install_unknown_sources`
  restriction** (the thing that made GUI installs fail on stock).
- **Root:** dump the Lineage boot, patch in the Magisk app, flash it back (same method as on Fire OS).

## Rollback to Fire OS
TWRP → restore the amonet backup (`restore.bat`), or `dd` the raw partition backups back. Boot has no
anti-rollback, so it's always safe to revert.

> Sources: [LineageOS cronos v0.3 release](https://github.com/amazon-oss/releases/releases/tag/lineage-18.1-cronos-v0.3) ·
> [XDA cronos thread](https://xdaforums.com/t/rom-unofficial-11-cronos-lineageos-18-1-for-the-amazon-echo-show-5-2021.4772598/) ·
> [Derek Seaman guide](https://www.derekseaman.com/2025/11/home-assistant-hacking-your-echo-show-5-and-8.html)
