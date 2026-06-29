# Apps (Simian Tactical Toolbox)

All built **without Gradle/Android Studio** — just the Android SDK command-line tools + a JDK.
Target **API 25 (Android 7.1)** so they run on the Echo Show 5 (2nd gen).

## Build prerequisites
- Android SDK with **build-tools 35.0.0** and a **platform jar** (android-34 or -35).
- A JDK with `javac`, `keytool`, `jar` (JDK 25 works; the scripts use `javac --release 11`).

## Build any app
Each app has a `build.ps1` (PowerShell). **Edit the top of the script** to match your machine:
- `$SDK` — your Android SDK path (default `D:\android\Sdk`)
- `$BT` / `$AJAR` — build-tools + android.jar
- `$KS` — keystore path. None is shipped (signing keys are gitignored). `godmode/build.ps1`
  auto-generates a debug keystore if missing; point the others at the same one, or generate:
  ```
  keytool -genkeypair -keystore debug.keystore -storepass android -keypass android -alias dbg -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Debug"
  ```
Then:
```
powershell -ExecutionPolicy Bypass -File apps\<app>\build.ps1
adb install -r apps\<app>\out\<App>.apk
```

## Pipeline (what build.ps1 does)
`aapt2 compile` → `aapt2 link` (manifest + resources) → `javac --release 11` → `d8`
(`java -cp lib/d8.jar com.android.tools.r8.D8 --min-api 21`) → `jar uf` dex into apk →
`zipalign` → `apksigner`.

### Gotcha
PowerShell `@$array` splat on a **single** class file explodes the path into characters.
Always: `$classes = @(Get-ChildItem ... | % { $_.FullName })`.

## Apps
- **godmode/** — `com.rev.godmode`. Scrollable menu: Quick Settings, Hidden/Dev menus
  (`*#*#4636#*#*` testing, MediaTek EngineerMode), an app launcher with per-app **KILL**
  (root `am force-stop`), and "kill all background apps". Blue monkey icon.
- **displayoff/** — `com.rev.displayoff`. One-tap, no-UI screen-off tile (root `input keyevent 223`).
  Red monkey icon.
- **backyardpyro/** — `com.simian.pyro`. Author's own app, included as a prebuilt signed APK.

> Icons are recolors of the author's Simian Tactical Unit artwork via System.Drawing `ColorMatrix`
> (R↔B swap = blue; zero green = red). First root call inside any app → Magisk Grant prompt.
