# Gotchas — the traps that ate hours

A consolidated list of every non-obvious thing that bit during this project. Read before you start.

### Bootloader / exploit
- **Anti-rollback is a permanent fuse.** Never flash preloader/lk/tee older than installed. The
  amonet `fastbrick.ps1` auto-matches the firmware image and refuses unknown versions — confirm it
  prints the **"Fire OS 6.5.7.0 or newer"** (2024) line on an updated unit before typing `YES`.
- **"fastbrick" looks like a brick on purpose** — it corrupts a partition to force BROM, screen goes
  black/garbled. Don't unplug.
- **The `backup.bat` step is mandatory** — the OS won't boot until `restore.bat` writes it back.

### Root / Magisk
- Boot-root only makes **adbd** root (SELinux `su` domain, adbd-only). Apps/SSH can't escalate.
  **Magisk** is required for real `su` everywhere. Context becomes `u:r:magisk:s0`.
- Boot partition path here is **`/dev/block/mmcblk0p9`** — **`/dev/block/by-name/` does not exist**;
  use `/dev/block/platform/bootdevice/by-name/boot` to resolve it.
- Boot images have **no anti-rollback** — safe to dd back if a patch bootloops.
- **First `su` from any app (incl. SSH) → Magisk Grant prompt on the device.** Tap Grant once.

### Debloat
- `pm disable-user` is **reverted by Amazon**: locally by
  `com.amazon.application.compatibility.enforcer`, remotely by `com.amazon.device.rdmapplication`
  (DCP policy push when WiFi is on). Disable **both** first; keep WiFi off until then.
- The **launcher** (`com.amazon.paladin`) is a *protected app* — `disable-user` flips back within
  seconds (the two launchers visibly fight). Use **`pm uninstall --user 0`**; it sticks (offline).
- After removing the home shell, `com.amazon.bishop` / `cardinal` / `ambienthome` **crash-loop on
  boot** ("arrangement/music/alexa vid stopped" popups). `pm uninstall --user 0` them.
- **Apps closing after ~30 s** = `com.amazon.knight.ecs` running `ECS_TimeoutManager` (30s
  `TimeoutPolicy` shoves non-persistent tasks to back). Disable `com.amazon.knight.ecs`.
- **Never** disable `*.api`/`*.sdk`/`*.library` (other apps link them), `echoaudioservice` (also the
  speaker), webview, keyboard, `knight.*` (except ecs), `mediatek.*`, identity/dcp.

### `install-existing` is MISSING on this Fire OS 7.1 build
Both `cmd package install-existing <pkg>` and `pm install-existing <pkg>` return "Unknown command"
/ dump the `pm` help. So you **cannot** undo a `pm uninstall --user 0` the usual way. To restore a
**system** app you uninstalled-for-user, reinstall its on-disk APK directly:
```
find /system -iname '*.apk' 2>/dev/null | grep -i <name>          # locate it (in /system/priv-app or /system/app)
pm install -r --user 0 /system/priv-app/<pkg>/<pkg>.apk           # re-register for user 0  -> "Success"
pm enable <pkg>                                                    # if it was also disabled
```
This is THE reversal method on this device. (Disabled-but-not-uninstalled apps still just need
`pm enable <pkg>`.)

### Load-bearing Amazon "glue" — DO NOT disable (Fire OS system apps crash)
These look like bloat but Fire OS **system apps hard-depend on them** and crash-loop if removed:
- **`amazon.speech.sim`** — `com.android.systemui` binds to it (`SimConnectionListener` / "SimClient");
  without it SystemUI throws `RuntimeException: SimClient was never got created` every ~10 min
  (`com.amazon.systemui.common.SimConnectionListerner.onFinish`). Re-enable: `pm enable amazon.speech.sim`.
- **`com.amazon.kindle.otter.oobe`** — `com.android.settings` reads a "launched countries" resource
  out of it on startup (`com.amazon.oobe.commons.accounts.CountryHelper.getLaunchedCountries`);
  without it Settings (and `SettingsProxyContentProvider`) crash with
  `NameNotFoundException: com.amazon.kindle.otter.oobe`. Re-add (see "install-existing missing" below):
  `pm install -r --user 0 /system/priv-app/com.amazon.kindle.otter.oobe/com.amazon.kindle.otter.oobe.apk`
  then `pm enable com.amazon.kindle.otter.oobe`.

  These are dependency glue, NOT surveillance — keep them enabled; everything user-facing (Alexa,
  ads, telemetry, OTA) stays gutted. `amazon.speech.sim` holds a **system-fixed** `RECORD_AUDIO`
  perm that `pm revoke` can't remove — fine, the always-listening services are disabled + mute
  button cuts the mic in hardware.
  Diagnose any "X has stopped": `adb shell "logcat -b crash -d | grep -A40 'Process: <pkg>'"` and
  read the `Caused by:` / `NameNotFoundException` line → re-enable that package.
- **Known cosmetic casualty:** stock **Settings → Sound → Equalizer (EQ) page crashes** — it binds to
  a disabled Amazon audio component. Not worth re-enabling Alexa-media junk for one EQ screen; use
  **VLC's built-in equalizer** or your **BT-source phone's EQ** instead. The rest of Settings is fine.

### Launcher / apps
- **`INSTALL_FAILED_OLDER_SDK`** = the APK's minSdk > device API 25 (Android 7.1). Lawnchair 15 and
  Nova 8 need Android 8. Use **KISS** (API 21+) / Lawnchair 2 / Nova 7.0.57.

### SSH
- SimpleSSHD-19's **Dropbear (2018) has no ed25519** → `unknown algo`. **Use an ECDSA key.**
- `authorized_keys` perms `600`, owned by the app uid, and **SELinux context must match** the app's
  data dir (`chcon u:object_r:app_data_file:s0:cXXX,cXXX`).
- No background service — enable "start on boot" in the app, or it won't survive reboot.

### Display / input
- **Volume keys don't wake the screen; the mute button does.** Volume = `event6` (DOWN/UP, no
  autorepeat); mute = `event1`; light sensor `event5` spams `ABS_X`.
- **`getevent` full-buffers to a pipe** → shell `getevent | while read` monitors don't see presses
  until ~4 KB. Interactive PTY (`adb shell` → `getevent -l`) is unbuffered. This killed the
  volume-combo toggle idea → shipped a tile + mute-button instead.

### Building APKs
- **PowerShell `@$array` splat on a *single* file** explodes the path into characters
  (`D`, `:`, `\`…). Force array: `$classes = @(Get-ChildItem ... | % { $_.FullName })`.
- No `android-25` platform needed: compile vs android-35 jar, manifest minSdk21/target25.
- JDK is new (25) → use `javac --release 11`; d8 (build-tools 35) accepts that bytecode.

### Windows / adb
- Git-bash mangles `/data/...` paths passed to native exes → use `MSYS_NO_PATHCONV=1` or PowerShell.
- Two adb client versions (e.g. platform-tools vs a local copy) ping-pong "killing server" — pick one.
