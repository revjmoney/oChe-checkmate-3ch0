# F-Droid Privileged Extension (silent installs)

Fire OS sets a UserManager restriction **`no_install_unknown_sources`** that blocks the on-device
GUI installer (F-Droid → "problem parsing the package", even with unknown-sources enabled). Root
`pm install` / `adb install` bypass it, but to make **F-Droid itself install/update silently**,
install its Privileged Extension as a **Magisk module**.

> Check the restriction: `adb shell "su -c 'dumpsys user | grep -i unknown'"` →
> shows `no_install_unknown_sources` if Fire OS is blocking GUI installs.

## Steps

1. **Download** the official extension (F-Droid-signed) flashable zip:
   - `gitlab.com/fdroid/privileged-extension` → **Releases** → `FDroidPrivilegedExtension-XX.zip`
     (the same zip installs as a Magisk module). Search: *"F-Droid Privileged Extension Magisk"*.

2. **Push it to the device:**
   ```
   adb push FDroidPrivilegedExtension-XX.zip /sdcard/Download/
   ```

3. **Install as a Magisk module** (pick one):
   - **Magisk app:** Modules → Install from storage → select the zip → **Reboot**.
   - **Shell:**
     ```
     adb shell "su -c 'magisk --install-module /sdcard/Download/FDroidPrivilegedExtension-XX.zip'"
     adb reboot
     ```

4. **Verify + enable** after reboot:
   ```
   adb shell "pm list packages | grep privileged"     # -> org.fdroid.fdroid.privileged
   ```
   Open **F-Droid → Settings** — it auto-detects the extension (Installer = Privileged). Installs
   and updates now happen with no dialog and no parse error.

## Notes
- Must be the **official F-Droid-signed** zip — F-Droid won't trust a random build.
- minSdk is still a separate wall: use F-Droid's **Versions** tab for Android-7.1-compatible builds.
- Alternative (less durable): clear the restriction by editing `/data/system/users/0.xml` with root,
  but Fire OS may re-apply it on boot — the Privileged Extension is the robust fix.
