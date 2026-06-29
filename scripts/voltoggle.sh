#!/system/bin/sh
# DIAGNOSTIC poll mode - one event per getevent call (no pipe buffering)
LOG=/data/local/tmp/vt.log
echo "=== started poll ===" >> $LOG
while true; do
  ev=$(getevent -lqc 1 2>/dev/null)
  case "$ev" in
    *KEY_VOLUME*) echo "$ev" >> $LOG ;;
  esac
done
