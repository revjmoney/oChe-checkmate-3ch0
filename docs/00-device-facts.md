# Device facts — Echo Show 5 (2nd gen), CRONOS

> Strip your own serial / Bluetooth MAC before publishing anything.

## Identity
- Model: Echo Show 5, **2nd generation**
- Codename: **CRONOS**
- SoC: MediaTek (MTK), micro-USB port
- Fire/Echo OS tested: **6.5.7.3**, bootloader build `44072a3-20240709_162755`
- `unlock_status: false` (LOCKED), `secure: yes`, `prod: 1`, `rpmb_state: 1`
- Anti-rollback fuses: `antirback_tee_version 0x0104`, `antirback_lk_version 0x0103`,
  `antirback_pl_version 0x0103` — **SET** (one-way; never downgrade these chains)

## Partitions / blocks
- boot = `/dev/block/mmcblk0p9`
  (symlink: `/dev/block/platform/bootdevice/by-name/boot`; **`/dev/block/by-name/` absent**)

## Input devices (`getevent -l`)
| Device | Name | Notes |
|---|---|---|
| `/dev/input/event6` | volume keys | `KEY_VOLUMEUP` / `KEY_VOLUMEDOWN`, values `DOWN`/`UP`, **no autorepeat** |
| `/dev/input/event1` | `gpio-privacy-button` | the **mute** button — **this one wakes the screen** |
| `/dev/input/event5` | `m_alsps_input` | ambient-light/proximity — **spams `ABS_X` constantly** |
| `/dev/input/event3` | `amazon_touch` | touchscreen |

## Wake behavior
- **Volume keys do NOT wake the display.**
- **The mute button DOES wake the display.**

## Bluetooth
- Adapter name pattern: `Echo Show 5-XXXX`
- Stack includes **`A2dpSinkService`** → can act as a Bluetooth **speaker** (A2DP sink) +
  `AvrcpControllerService` for now-playing metadata.

## SELinux
- **Enforcing.** Boot-root grants root only to adbd (`u:r:su:s0`); Magisk provides app/SSH `su`
  (`u:r:magisk:s0`).
