# Commands in order (flat, copy-paste)

Assumes: bootloader already unlocked + TWRP + boot-root done via the amonet bundle (§1 of HOWTO —
that part is `.bat` scripts, not adb). `adb` is on PATH and `adb shell` returns `#` (root).
Replace placeholders in <angle brackets>. **Strip serial/MAC before sharing output.**

```bash
# ===== MAGISK (real su) =====
adb shell "su -c 'dd if=/dev/block/mmcblk0p9 of=/sdcard/boot-current.img bs=1048576'"
adb pull /sdcard/boot-current.img boot-current.img
adb shell "cp /sdcard/boot-current.img /sdcard/Download/boot-current.img"
adb install -r Magisk-v30.7.apk
#   >>> in Magisk app: Install -> Select and Patch a File -> Download/boot-current.img
adb shell "su -c 'dd if=/sdcard/Download/magisk_patched-<XXXXX>.img of=/dev/block/mmcblk0p9 bs=1048576 && sync'"
adb reboot
adb shell "su -c id"            # expect uid=0 ... context=u:r:magisk:s0

# ===== KILL OTA + RE-ENABLERS =====
for p in com.amazon.device.software.ota com.amazon.device.software.ota.override com.amazon.device.smarthome.ota \
 com.amazon.application.compatibility.enforcer com.amazon.device.rdmapplication; do adb shell "pm disable-user --user 0 $p"; done

# ===== DEBLOAT (batches) =====
for p in com.amazon.avod com.netflix.mediaclient.echo com.amazon.alexa.youtube.app com.amazon.spotify.mediabrowserservice \
 com.amazon.cloud9 com.amazon.venezia com.amazon.android.marketplace com.amazon.csapp \
 com.amazon.device.crashmanager com.amazon.device.logmanager com.amazon.wirelessmetrics.service \
 com.amazon.hybridadidservice com.amazon.device.metrics com.amazon.client.metrics \
 com.amazon.zordon com.android.camera2 \
 amazon.speech.davs.davcservice amazon.speech.sim com.amazon.alexa.awaservice com.amazon.alexa.beaconbroadcaster \
 com.amazon.alexa.datastore.app com.amazon.alexa.externalmediaplayer.fireos com.amazon.alexa.identity \
 com.amazon.alexa.timeoutmanagerapp com.amazon.alexa.visionapp com.amazon.alexa.webmediaplayer.fireos com.amazon.alexaviz \
 com.amazon.a4b.conferencing.chime com.amazon.a4b.mcc com.amazon.callexperiencecontroller.capabilityagent \
 com.amazon.comms.knightmessaging com.amazon.comms.messagingcontroller com.amazon.comms.multimodaltachyonarm \
 com.amazon.communication.discovery com.amazon.rtcsessioncontroller com.amazon.sharingservice.android.client.proxy \
 com.amazon.unifiedshare.actionchooser com.amazon.device.smarthome.adapters.ble com.amazon.device.smarthome.adapters.echo \
 com.amazon.device.smarthome.dshs.endpointdetectorCA com.amazon.device.smarthome.dshs.multimodalux \
 com.amazon.device.smarthome.dshs.services com.amazon.gloria.smarthome com.amazon.device.gadgetscontrolmanager \
 com.amazon.apl.awsanalyticsextension com.amazon.assetsync.service com.amazon.amasetup.service \
 com.amazon.kindle.otter.oobe com.amazon.ods.kindleconnect com.amazon.device.sync com.amazon.virtual.dash.knight.app \
 com.amazon.alarms com.amazon.clockfaceselector com.amazon.selector.clock.resources com.amazon.wha.mediabrowserservice \
 com.ring.halo.messaging com.amazon.tcomm com.amazon.tcomm.client com.amazon.tcomm.jackson \
 com.amazon.whisperjoin.middleware.v2.np com.amazon.whisperjoin.wss.wifiprovisioner com.amazon.wifi.sync \
 com.amazon.realtimeregistry.device com.amazon.spotlight com.amazon.recess com.amazon.audiohome com.amazon.obscura \
 com.amazon.knight.ecs; do adb shell "pm disable-user --user 0 $p"; done

# uninstall-for-user (sticks where disable doesn't): launcher + boot-crashers
for p in com.amazon.paladin com.amazon.bishop com.amazon.cardinal com.amazon.ambienthome; do adb shell "pm uninstall --user 0 $p"; done

# ===== LAUNCHER =====
adb install -r fr.neamar.kiss_222.apk
adb shell "cmd package set-home-activity fr.neamar.kiss/fr.neamar.kiss.MainActivity"

# ===== SSH (ECDSA key) =====
adb install -r SimpleSSHD-19.apk         # open once, tap Start, enable start-on-boot
adb forward tcp:2222 tcp:2222
ssh-keygen -t ecdsa -b 256 -f ~/.ssh/id_ecdsa -N ""
adb push ~/.ssh/id_ecdsa.pub /data/local/tmp/ak
adb shell "su -c 'cp /data/local/tmp/ak /data/data/org.galexander.sshd/files/authorized_keys; chmod 600 /data/data/org.galexander.sshd/files/authorized_keys; chown <APPUID>:<APPUID> /data/data/org.galexander.sshd/files/authorized_keys; chcon u:object_r:app_data_file:s0:c<X>,c<Y> /data/data/org.galexander.sshd/files/authorized_keys'"
ssh -p 2222 -i ~/.ssh/id_ecdsa root@127.0.0.1

# ===== ALWAYS-ON DISPLAY =====
adb shell "su -c 'settings put global stay_on_while_plugged_in 7'"
adb shell "su -c 'settings put system screen_off_timeout 2147483647'"

# ===== HOSTS BLOCK =====
adb push scripts/amz_hosts /sdcard/amz_hosts
adb shell "su -c 'cat /system/etc/hosts > /sdcard/hosts.orig; mount -o rw,remount /system; cp /sdcard/amz_hosts /system/etc/hosts; chmod 644 /system/etc/hosts; mount -o ro,remount /system'"

# ===== INSTALL YOUR APPS =====
adb install -r apps/displayoff/DisplayOff.apk
adb install -r apps/godmode/GodMode.apk
adb install -r apps/backyardpyro/BackyardPyro.apk
```
