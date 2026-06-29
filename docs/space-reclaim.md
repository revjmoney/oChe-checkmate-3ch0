# Reclaim space

`/data` on this device is small (~2 GB free). `disable-user` keeps an app's data + odex;
**`uninstall --user 0`** removes them (frees space) and is still reversible via
`cmd package install-existing <pkg>`. The APK itself lives on read-only `/system` and can't be
freed without modifying system.

## 1. Trim all app caches (safe, instant)
`pm trim-caches` wants a size **with a K/M/G suffix** (a bare huge number errors with
"Invalid suffix"). Ask for more free space than exists so it trims everything:
```
adb shell "su -c 'pm trim-caches 4G'"
```

## 2. Uninstall-for-user the already-disabled Amazon bloat (frees data + odex)
Targets only **disabled** packages whose name is Amazon/Ring/Netflix/Ivona — i.e. stuff already
turned off and unused. Safe + reversible.
```
adb shell "su -c 'for p in \$(pm list packages -d | sed s/package://); do echo \$p | grep -qiE \"amazon|com.ring|netflix|ivona\" && pm uninstall --user 0 \$p; done'"
```

## 3. Check before/after
```
adb shell "df -h /data"
```

## Restore anything later
```
adb shell "cmd package install-existing <pkg>"     # brings a uninstalled-for-user app back
adb shell "pm enable <pkg>"                          # re-enable a disabled app
```

> Don't uninstall AOSP/system components blindly (`pm list packages -d` also lists system
> default-disabled packages) — the filter above keeps it to Amazon-family apps only.
