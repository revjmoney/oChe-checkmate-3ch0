# HOWTO — Root & Liberate the Echo Show 5 (2nd gen, CRONOS)

A complete, reproducible, lab-grade walkthrough. Every phase, every gotcha, exact filenames to
search for, and the exact commands in order. Done on **Windows 11** with a **micro-USB** cable.

> **Author's note:** I (the user) did **all** downloads and web searches myself. Where you need a
> file I don't (and legally can't) ship, I tell you the **exact filename to Google** and the best
> place to look. Nothing Amazon owns is in this repo.

---

## 0. Device facts (confirm yours matches)

- **Device:** Echo Show 5, **2nd gen**, codename **CRONOS** (MediaTek SoC, micro-USB)
- **Fire/Echo OS:** `6.5.7.3` — build `44072a3-20240709_162755`
- **Bootloader:** LOCKED, `secure: yes`, `prod: 1`, **anti-rollback fuses SET**
- Serial / Bluetooth MAC: **[REDACTED — strip yours before publishing anything]**

Check `fastboot getvar all` (after entering fastboot, below). The build string decides which
firmware image the exploit uses — **this matters for anti-rollback (see §1).**

> ⚠️ **ANTI-ROLLBACK IS A ONE-WAY FUSE.** Flashing a preloader/lk/tee *older* than what's on the
> device = **permanent brick, no recovery.** The amonet toolkit auto-selects a version-matched
> image and refuses unknown versions — but never hand-flash old firmware. Boot images (`boot`)
> are NOT fused, so those are safe to re-flash.

---

## 1. Unlock bootloader + install TWRP (amonet-cronos)

**What to download yourself** (NOT in this repo — Amazon firmware blobs):
- Search: **`amonet-cronos-v1.0.0.zip`** — original Echo Show 5 (2nd gen) jailbreak bundle.
  Best sources: the **Droidwin** "Unlock Bootloader / TWRP / Root Echo Show 5 2nd Gen" guide, and
  **XDA**. The bundle contains: `fastbrick.bat/.ps1/.sh`, `backup.*`, `restore.*`,
  `boot-fastboot.*`, `boot-recovery.*`, `bin/` (with `cronos-kaeru.bin`, `microloader.bin`,
  `lk.bin`, `preloader.img`, `tz.img`, and **two** firmware images: `full-20210918.img` and
  `full-20240709.img`), `lk-payload/build/payload.bin`, and `modules/` (handshake scripts).
- **Kindle Fire ADB driver** + **Google USB driver** (for fastboot).
- **Android SDK Platform-Tools** (for `adb`/`fastboot`).

**The anti-rollback safety mechanism** (verify before you commit): open `fastbrick.ps1` and find
the version switch. It auto-picks the matching image:
```
"44072a3-20240709_162755" -> bin\full-20240709.img   ("Fire OS 6.5.7.0 or newer")   ← updated units
"5da77ac-20210918_073605" -> bin\full-20210918.img   ("Fire OS 6.5.5.0")
default                   -> ERROR + exit (flashes nothing)
```
If your device is on the 2024 build, it uses the 2024 image. **If `fastbrick` ever prints the
2021/"6.5.5.0" line or "Unsupported version" on a 2024 device — STOP.** It is structurally
incapable of downgrading, but verify the printed line says **"Fire OS 6.5.7.0 or newer"**.

**Steps:**
1. Plug the Echo into the **AC charger** (the button combo needs wall power).
2. Hold **all three buttons** together until `FASTBOOT mode...` appears on screen.
3. Connect the **micro-USB** cable to the PC. `fastboot devices` should list it.
   - On Windows, the fastboot interface wants a **WinUSB** driver. If `fastboot devices` is empty,
     use **Zadig** to install **WinUSB** on the fastboot interface. (BROM/preloader later uses a
     *different* driver — **UsbDk** — but for the all-in-one amonet path you may not need it.)
4. Run **`fastbrick.bat`**. It detects your version → confirm it printed the **2024 / "6.5.7.0 or
   newer"** image → type **`YES`**.
5. The script **intentionally corrupts a partition** ("fastbrick") to drop the chip into **BROM**,
   then runs the bootrom exploit. The screen may go black/garbled — **that's normal, don't unplug.**
6. ~5 minutes → it reboots into **TWRP**. On first TWRP boot, **swipe "Allow Modifications"**.
7. **Run `backup.bat` immediately.** This is REQUIRED — the exploit leaves the OS unbootable until
   you restore; the backup is the restore source. It saves `data.ab`, `system.ab`, `boot.ab`.
8. **Run `restore.bat`**, select the backup folder → it restores data/system/boot (~6 MB/s, ~20 min).
9. TWRP → **Reboot → System**. First boot is slow; it should reach Fire OS.

**Root (initial):** flash the guide's **`boot-root.zip`** in TWRP (Install → select zip → swipe →
wipe cache/dalvik → reboot System). This gives a basic root: **adbd runs as root** in a permissive
SELinux `su` domain. `adb shell` is now `#`. (We upgrade this to Magisk in §2.)

**Re-enter TWRP anytime:** hold **Volume Up while connecting the AC charger**, or
`adb reboot recovery`.

---

## 2. Upgrade to Magisk (real `su` for apps & SSH)

The boot-root only makes *adbd* root — apps (and SSH sessions) still can't escalate, and SELinux is
**Enforcing**, so a dropped-in `su` won't transition. **Magisk** fixes that with its own policy.

**Download yourself:** **`Magisk-v30.7.apk`** (or current stable) — `github.com/topjohnwu/Magisk`
releases. It installs on Android 7.1 (app minSdk = Android 6) and is self-contained (patches offline).

> Boot images have **no anti-rollback**, so this is safe and fully reversible. Keep your raw boot
> backup; recovery is a 30-second `dd` (see §Recovery).

```bash
# 0) find + back up the boot partition (mmcblk0p9 on CRONOS; /dev/block/by-name/ does NOT exist here)
adb shell "su -c 'ls -l /dev/block/platform/bootdevice/by-name/boot'"     # -> mmcblk0p9
adb shell "su -c 'dd if=/dev/block/mmcblk0p9 of=/sdcard/boot-current.img bs=1048576'"
adb pull /sdcard/boot-current.img boot-current.img                         # keep a PC copy
adb shell "cp /sdcard/boot-current.img /sdcard/Download/boot-current.img"  # for Magisk's picker

# 1) install Magisk + patch (in the Magisk APP on the device):
adb install -r Magisk-v30.7.apk
#   Magisk app -> Install -> "Select and Patch a File" -> Download/boot-current.img
#   -> produces /sdcard/Download/magisk_patched-XXXXX.img

# 2) flash the patched boot + verify (md5 must match), then reboot
adb shell "su -c 'dd if=/sdcard/Download/magisk_patched-XXXXX.img of=/dev/block/mmcblk0p9 bs=1048576 && sync'"
adb shell "su -c 'md5sum /sdcard/Download/magisk_patched-XXXXX.img'"
adb shell "su -c 'dd if=/dev/block/mmcblk0p9 bs=1048576 count=16 2>/dev/null | md5sum'"   # must equal above
adb reboot

# 3) verify real root
adb shell "su -c id"     # -> uid=0(root) ... context=u:r:magisk:s0
adb shell "su -c 'magisk -c'"   # -> 30.7:MAGISK:R (30700)
```
> **First time any app (incl. your SSH session) calls `su`, a Magisk Grant prompt pops on the device
> screen — tap Grant once.**

---

## 3. De-Amazon: debloat, kill OTA, fix the app-killer

**Golden rules learned the hard way:**
- `pm disable-user --user 0 <pkg>` is reversible (`pm enable`), but Amazon **re-enables** protected
  apps. Two re-enablers must die first: the **local** `com.amazon.application.compatibility.enforcer`
  and the **cloud** `com.amazon.device.rdmapplication` (acts when WiFi is on — DCP pushes policy that
  resurrects disabled apps). **Keep WiFi off until both are disabled.**
- For apps that *still* won't stay disabled (the **launcher**), use
  **`pm uninstall --user 0 <pkg>`** — the framework re-*enables* but won't re-*install* (offline).
  Reverse with `pm install-existing <pkg>` or factory reset.
- **NEVER disable** the load-bearing set: framework/`fireos`, `systemui`, `settings` +
  `device.settings`, **webview/chromium/knightwebview**, `bluestone.keyboard`, wifi profile manager,
  `echoaudioservice` (also drives the **speaker**), all `knight.*` *except* the timeout one (below),
  `mediatek.*`, account `identity`/`dcp`, and anything `*.api` / `*.sdk` / `*.library`.

```bash
# --- kill OTA (do this first; also covered by the hosts block in §8) ---
for p in com.amazon.device.software.ota com.amazon.device.software.ota.override com.amazon.device.smarthome.ota; do
  adb shell "pm disable-user --user 0 $p"; done

# --- kill the re-enablers ---
adb shell "pm disable-user --user 0 com.amazon.application.compatibility.enforcer"
adb shell "pm disable-user --user 0 com.amazon.device.rdmapplication"

# --- bloat batch 1: content/store leaf apps (100% safe) ---
for p in com.amazon.avod com.netflix.mediaclient.echo com.amazon.alexa.youtube.app \
 com.amazon.spotify.mediabrowserservice com.amazon.cloud9 com.amazon.venezia \
 com.amazon.android.marketplace com.amazon.csapp; do
  adb shell "pm disable-user --user 0 $p"; done

# --- bloat batch 2: telemetry / ads / diagnostics ---
for p in com.amazon.device.crashmanager com.amazon.device.logmanager \
 com.amazon.wirelessmetrics.service com.amazon.hybridadidservice \
 com.amazon.device.metrics com.amazon.client.metrics; do
  adb shell "pm disable-user --user 0 $p"; done

# --- the SILENT mic+cam package (no user-facing purpose) + camera app ---
adb shell "pm disable-user --user 0 com.amazon.zordon"
adb shell "pm disable-user --user 0 com.android.camera2"

# --- bloat batch 3: Alexa / voice / comms / smarthome / setup / remote-mgmt (~41 pkgs) ---
for p in amazon.speech.davs.davcservice amazon.speech.sim \
 com.amazon.alexa.awaservice com.amazon.alexa.beaconbroadcaster com.amazon.alexa.datastore.app \
 com.amazon.alexa.externalmediaplayer.fireos com.amazon.alexa.identity com.amazon.alexa.timeoutmanagerapp \
 com.amazon.alexa.visionapp com.amazon.alexa.webmediaplayer.fireos com.amazon.alexaviz \
 com.amazon.a4b.conferencing.chime com.amazon.a4b.mcc com.amazon.callexperiencecontroller.capabilityagent \
 com.amazon.comms.knightmessaging com.amazon.comms.messagingcontroller com.amazon.comms.multimodaltachyonarm \
 com.amazon.communication.discovery com.amazon.rtcsessioncontroller \
 com.amazon.sharingservice.android.client.proxy com.amazon.unifiedshare.actionchooser \
 com.amazon.device.smarthome.adapters.ble com.amazon.device.smarthome.adapters.echo \
 com.amazon.device.smarthome.dshs.endpointdetectorCA com.amazon.device.smarthome.dshs.multimodalux \
 com.amazon.device.smarthome.dshs.services com.amazon.gloria.smarthome com.amazon.device.gadgetscontrolmanager \
 com.amazon.apl.awsanalyticsextension com.amazon.assetsync.service \
 com.amazon.amasetup.service com.amazon.kindle.otter.oobe com.amazon.ods.kindleconnect \
 com.amazon.device.sync com.amazon.virtual.dash.knight.app \
 com.amazon.alarms com.amazon.clockfaceselector com.amazon.selector.clock.resources \
 com.amazon.wha.mediabrowserservice com.ring.halo.messaging; do
  adb shell "pm disable-user --user 0 $p"; done

# --- bloat batch 4 (tier 2): cloud transport / provisioning / leftover UI ---
for p in com.amazon.tcomm com.amazon.tcomm.client com.amazon.tcomm.jackson \
 com.amazon.whisperjoin.middleware.v2.np com.amazon.whisperjoin.wss.wifiprovisioner \
 com.amazon.wifi.sync com.amazon.realtimeregistry.device \
 com.amazon.spotlight com.amazon.recess com.amazon.audiohome com.amazon.obscura; do
  adb shell "pm disable-user --user 0 $p"; done

# --- boot-crashers after the home shell is gone (uninstall-for-user so they don't autostart+FC) ---
for p in com.amazon.bishop com.amazon.cardinal com.amazon.ambienthome; do
  adb shell "pm uninstall --user 0 $p"; done

# --- THE 30-SECOND APP KILLER: com.amazon.knight.ecs runs ECS_TimeoutManager (30s TimeoutPolicy)
#     that shoves any non-persistent task to the back. Disable it or your apps close every 30s. ---
adb shell "pm disable-user --user 0 com.amazon.knight.ecs"
adb shell "am force-stop com.amazon.knight.ecs"
```
> Find what's killing your foreground app: `adb shell "logcat -b crash -d | grep Process:"` and
> `adb shell "logcat -d | grep -i ECS_TimeoutManager"`.

---

## 4. Third-party launcher (KISS) + evict Amazon's launcher

**API-25 gotcha:** modern launchers need **Android 8+** and fail with `INSTALL_FAILED_OLDER_SDK`
(Lawnchair 15, Nova 8 both died this way). The device is **Android 7.1 / API 25**. Use a launcher
that still supports 7.1:
- **KISS Launcher** (`fr.neamar.kiss`) — supports API 21+. *Recommended.* (`fr.neamar.kiss_222.apk`)
- alternatives: Lawnchair **2.x** (old line), Nova **7.0.57** (last 5.0+ build).

```bash
adb install -r fr.neamar.kiss_222.apk
adb shell "cmd package set-home-activity fr.neamar.kiss/fr.neamar.kiss.MainActivity"

# The Echo home launcher is com.amazon.paladin (its .app.MMLauncherActivity / "MMLauncher").
# disable-user does NOT hold (protected app; framework flips it back within seconds, even offline,
# and the two launchers visibly fight). UNINSTALL-for-user is the fix that sticks:
adb shell "pm uninstall --user 0 com.amazon.paladin"   # reverse: cmd package install-existing com.amazon.paladin
```

---

## 5. SSH (SimpleSSHD, key-based, over USB tunnel)

**Download:** **`SimpleSSHD-19.apk`** (Dropbear, by Greg Alexander — F-Droid / its site).

```bash
adb install -r SimpleSSHD-19.apk
# open SimpleSSHD on device once, tap Start (it has no background service; enable "start on boot")
adb forward tcp:2222 tcp:2222          # USB tunnel - SSH with WiFi OFF
```
**Key gotchas:**
- SimpleSSHD-19 ships an **old Dropbear (2018) that does NOT support ed25519** — auth fails with
  `Pubkey auth attempt with unknown algo`. **Use an ECDSA key.**
- Its `authorized_keys` lives in `/data/data/org.galexander.sshd/files/authorized_keys` (per its
  prefs `home`/`path`). Owner must be the app uid, perms `600`, and on **Enforcing SELinux** the
  file needs the app's data context (`chcon u:object_r:app_data_file:s0:cXXX,cXXX`, match an existing
  file in that dir).

```bash
# generate ECDSA key on PC (no passphrase = passwordless)
ssh-keygen -t ecdsa -b 256 -f %USERPROFILE%\.ssh\id_ecdsa -N ""
# push + install authorized_keys (run via su; match context of an existing app file)
adb push %USERPROFILE%\.ssh\id_ecdsa.pub /data/local/tmp/ak
adb shell "su -c 'cp /data/local/tmp/ak /data/data/org.galexander.sshd/files/authorized_keys; chown <APPUID>:<APPUID> /data/data/org.galexander.sshd/files/authorized_keys; chmod 600 /data/data/org.galexander.sshd/files/authorized_keys; chcon u:object_r:app_data_file:s0:cXXX,cXXX /data/data/org.galexander.sshd/files/authorized_keys'"
# connect (passwordless); run `su` in-session for root (Magisk grants it)
ssh -p 2222 -i %USERPROFILE%\.ssh\id_ecdsa root@127.0.0.1
```
> `<APPUID>` = SimpleSSHD's uid (e.g. `u0_a62` = 10062). Get it from `ls -l` on the files dir.

---

## 6. Bluetooth speaker (A2DP sink)

The Echo's Bluetooth stack already includes **`A2dpSinkService`** (it's a stock feature Amazon hid
behind the Alexa UI). Keep `com.amazon.knight.btavrcp` + `AvrcpControllerService` enabled for
now-playing metadata on screen.

```bash
# make it discoverable, then pair from your phone and play media
adb shell "su -c 'settings put global bluetooth_on 1'"
adb shell "am start -a android.bluetooth.adapter.action.REQUEST_DISCOVERABLE --ei android.bluetooth.adapter.extra.DISCOVERABLE_DURATION 300"
# on the Echo: tap Allow. On phone: pair "Echo Show 5-xxxx", select as Media audio, play.
```
Audio comes out the Echo speaker; track title/artist appears on screen. Pairing persists.

---

## 7. Always-on display + a working on/off toggle

```bash
adb shell "su -c 'settings put global stay_on_while_plugged_in 7'"        # never sleep on AC
adb shell "su -c 'settings put system screen_off_timeout 2147483647'"     # timeout = never
```
**Hardware facts (CRONOS), discovered via `getevent -l`:**
- Volume keys → `/dev/input/event6`, values **`DOWN`/`UP`**, **no auto-repeat on hold**.
- Light sensor `event5` spams `ABS_X` constantly.
- Privacy/mute button → `event1` (`gpio-privacy-button`).
- **Volume keys do NOT wake the screen. The MUTE button DOES.**

**Why the slick "vol+ & vol- = toggle" script was abandoned:** `getevent` **full-buffers** its
output to a pipe, so a shell `getevent -l | while read` monitor never sees individual presses until
~4 KB accumulates. (`getevent -lqc 1` polling busy-loops on the light-sensor spam.) The combo *is*
detectable in an interactive PTY, but an unbuffered reader in a plain script on this toybox is a
pain. `scripts/voltoggle.sh` is included with this explanation. Use a PTY if you want to watch:
`adb shell` → `su` → `getevent -l`.

**The reliable toggle that shipped:**
- **OFF** = a one-tap **"Display Off"** tile (`apps/displayoff/`) → root `input keyevent 223`.
- **ON** = press the **mute button** (stock wake).

---

## 8. Hardening: hosts block + F-Droid

**Hosts sinkhole** (domain-based; not as airtight as a firewall but simple and effective with the
apps already gutted). Only Amazon **device** endpoints — not generic AWS/CloudFront/Akamai (which
would break half the internet). File: `scripts/amz_hosts`.
```bash
adb push scripts/amz_hosts /sdcard/amz_hosts
adb shell "su -c 'cat /system/etc/hosts > /sdcard/hosts.orig; mount -o rw,remount /system; cp /sdcard/amz_hosts /system/etc/hosts; chmod 644 /system/etc/hosts; mount -o ro,remount /system'"
```
**F-Droid** (Google-free store; needs WiFi to sync): install **`F-Droid.apk`** from f-droid.org.
Good picks: VLC, Termux, NewPipe, Aurora Store.

---

## 9. Build your own apps (no Gradle, against the SDK)

**Environment:** Android SDK (build-tools 34/35, platforms 34/35) + a JDK with `javac`/`keytool`.
No `android-25` platform needed — **compile against android-35's `android.jar`** and set the
manifest **minSdk 21 / target 25** so it installs and runs on 7.1.

**Pipeline (see `apps/godmode/build.ps1`):**
```
aapt2 compile --dir res -o out\res-compiled.zip
aapt2 link --manifest AndroidManifest.xml -I android.jar --min-sdk-version 21 --target-sdk-version 25 -o out\base.apk out\res-compiled.zip
javac --release 11 -classpath android.jar -d obj src\...\*.java
java -cp build-tools\35.0.0\lib\d8.jar com.android.tools.r8.D8 --min-api 21 --lib android.jar --output out <classes>
jar uf out\base.apk classes.dex
zipalign -f 4 out\base.apk out\aligned.apk
apksigner sign --ks debug.keystore --ks-pass pass:android --out out\App.apk out\aligned.apk
```
**Gotchas:**
- **PowerShell `@splat` on a single class file** breaks the path into characters. Force an array:
  `$classes = @(Get-ChildItem ... | % { $_.FullName })`.
- Use a recolored launcher icon to tell same-art apps apart (System.Drawing `ColorMatrix`:
  R↔B swap = blue; zero the green = red).
- First root call inside any app → Magisk Grant prompt on device.

**Apps in this repo:**
- **GOD MODE** (`apps/godmode/`, `com.rev.godmode`) — scrollable menu of hidden settings + dev
  menus (incl. `*#*#4636#*#*` testing + MediaTek EngineerMode) + an app launcher with per-app
  **KILL** (root `am force-stop`) + "kill all background apps".
- **Display Off** (`apps/displayoff/`, `com.rev.displayoff`) — one-tap screen-off tile.
- **Backyard Pyro** (`apps/backyardpyro/`, `com.simian.pyro`) — author's own app; included as-is.

---

## Recovery (if the patched boot or anything bootloops)

- Re-enter **TWRP**: Volume-Up + connect AC charger (or `adb reboot recovery`).
- Restore boot: `adb shell "su -c 'dd if=/sdcard/boot-current.img of=/dev/block/mmcblk0p9'"`
  (boot has **no anti-rollback** — always safe to reflash).
- Worst case: `restore.bat` from your amonet backup (§1) brings the OS back.
- Re-enable anything over-disabled: `adb shell "pm enable <pkg>"` or
  `adb shell "cmd package install-existing <pkg>"`.

See **docs/gotchas.md** for the consolidated trap list and **docs/commands-in-order.md** for a flat,
copy-paste command sequence.
