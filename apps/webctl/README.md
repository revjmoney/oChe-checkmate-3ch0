# Simian Web Control

`com.rev.webctl` — a tiny **HTTP control panel served by the Echo itself**. Browse to it from any
phone/PC on the same WiFi and control the device: kill/start apps, display/volume/reboot, change
settings, and a root console. Green monkey icon.

## How it works
- A self-contained Java HTTP server (`ServerSocket`, no external deps) runs in a **foreground
  Service**, bound to `0.0.0.0:8080` → reachable on the device's LAN IP.
- Endpoints execute commands as **root via Magisk** (`su -c ...`). First call → tap **Grant** on
  the on-device Magisk prompt.
- The served HTML/JS runs in *your* browser, so the UI uses modern JS; only the on-device Java is
  API-25-constrained.

## Install & run
```
adb install -r WebControl.apk        # (or: build.ps1 then install out\WebControl.apk)
```
On the Echo: open **Web Control** (it shows `http://<ip>:8080` and starts the server) → turn WiFi on
→ open that URL from any device on the LAN. Get the IP: `adb shell ip -4 addr show wlan0`.

## Endpoints
| Path | Action |
|---|---|
| `/` | the control-panel HTML |
| `/api/apps` | JSON list of launchable apps |
| `/api/start?pkg=&cls=` | `am start -n pkg/cls` |
| `/api/kill?pkg=` | `am force-stop pkg` |
| `/api/killall` | force-stop all 3rd-party apps |
| `/api/key?code=` | `input keyevent <code>` (223 off, 224 wake, 24/25 vol, 26 power, 3 home, 4 back) |
| `/api/setting?ns=&k=&v=` | `settings put <ns> <k> <v>` |
| `/api/cmd?c=` | run an arbitrary root shell command |

## ⚠️ Security
This is **unauthenticated LAN control with a root console + reboot** — anyone on the same WiFi can
drive the device. Intended for a **trusted home network only**. For untrusted networks, add a
token/PIN check (gate every `/api/*` on a secret query param) and/or bind to a specific interface.
Do **not** port-forward this to the internet.

> Part of the Simian Tactical Toolbox. Build with `build.ps1` (edit `$SDK`/`$KS` for your machine).
