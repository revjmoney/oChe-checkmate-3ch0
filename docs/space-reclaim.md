# Reclaim space

`/data` on this device is small (~2 GB free). `disable-user` keeps an app's data + odex;
**`uninstall --user 0`** removes them (frees space) and is reversible by reinstalling the on-disk
system APK (this 7.1 build has no `install-existing` — see below). The APK itself lives on read-only
`/system` and can't be freed without modifying system.

## 1. Trim all app caches (safe, instant)
`pm trim-caches` wants a size **with a K/M/G suffix** (a bare huge number errors with
"Invalid suffix"). Ask for more free space than exists so it trims everything:
```
adb shell "su -c 'pm trim-caches 4G'"
```

## 2. Uninstall-for-user the already-disabled Amazon bloat (frees data + odex)
Targets only **disabled** packages whose name is Amazon/Ring/Netflix/Ivona — i.e. stuff already
turned off and unused. Safe + reversible.

> ⚠️ **WARNING — this pass uninstalled load-bearing glue in testing.** It grabs anything *currently
> disabled* matching the name filter. If `amazon.speech.sim` or `com.amazon.kindle.otter.oobe` are
> disabled when you run it, they get **uninstalled-for-user**, which breaks **SystemUI** and
> **Settings** (and `pm enable` won't fix it — you must `pm install -r` the system APK; see gotchas).
> **Make sure those two are ENABLED before running this**, or just skip the deep pass — it only frees
> a couple hundred MB anyway.
```
adb shell "su -c 'for p in \$(pm list packages -d | sed s/package://); do echo \$p | grep -qiE \"amazon|com.ring|netflix|ivona\" && pm uninstall --user 0 \$p; done'"
```

## 3. Check before/after
```
adb shell "df -h /data"
```

## Restore anything later
`install-existing` is missing on this Fire OS 7.1 build — reinstall the on-disk system APK instead:
```
# undo a uninstall-for-user (find the APK first): find /system -iname '*.apk' | grep <name>
adb shell "su -c 'pm install -r --user 0 /system/priv-app/<pkg>/<pkg>.apk'"   # -> Success
adb shell "pm enable <pkg>"                                                    # re-enable a disabled app
```

> Don't uninstall AOSP/system components blindly (`pm list packages -d` also lists system
> default-disabled packages) — the filter above keeps it to Amazon-family apps only.
