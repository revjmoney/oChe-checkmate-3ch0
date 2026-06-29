# Backyard Pyro

`com.simian.pyro` — the author's own application, included here as a prebuilt **signed** APK
(`BackyardPyro.apk`).

- Built for **API 25**, so it installs cleanly on the Echo Show 5 (2nd gen) / Android 7.1.
- Install: `adb install -r apps/backyardpyro/BackyardPyro.apk`
- Launch activity: `com.simian.pyro/.SplashActivity` → `.MainActivity`

This is original work by Rev. J. Money, part of the Simian Tactical Toolbox. If it talks to external
firing hardware (USB/serial/BLE), grant it whatever permissions it requests on first run; with the
device rooted it can reach hardware as needed.
