:: WIN BATCH SCRIPT
:: Setup emulator https://stackoverflow.com/a/64397712/13361987

:: CHANGE THESE
set app_package=com.example.mipsos
set dir_app_name=MipSoS
set MAIN_ACTIVITY=com.example.mipsos.MainActivity

set ADB="adb"

:: Adjust the path to match your APK location
:: Example path, change it to your actual APK path
set apk_host=app\build\outputs\apk\debug\app-debug.apk
set apk_name=%dir_app_name%.apk

:: Adjust the path to match your privapp-permissions-platform.xml location
set privapp_permissions_xml_host=app\src\main\java\com\example\mipsos\privapp-permissions-platform.xml
set privapp_permissions_xml_name=privapp-permissions-platform.xml

:: Delete previous APK if it exists
if exist %apk_host% del %apk_host%

:: Compile the APK: you can adapt this for production build, flavors, etc.
call gradlew assembleDebug

:: Verify if the APK was successfully built
if not exist %apk_host% (
    echo APK build failed.
    pause
    exit /b 1
)

set ADB_SH=%ADB% shell su -c

:: Create Magisk module directory structure on the device
echo Creating module directories on device...
%ADB_SH% "mkdir -p /data/adb/modules/%dir_app_name%/system/priv-app/%dir_app_name%"
%ADB_SH% "mkdir -p /data/adb/modules/%dir_app_name%/system/etc/permissions"
%ADB_SH% "mkdir -p /sdcard/tmp"

:: Push the APK to the temporary directory
echo Pushing APK to device...
%ADB% push %apk_host% /sdcard/tmp/%apk_name%

:: Move the APK to the Magisk module directory
echo Moving APK to module directory...
%ADB_SH% "mv /sdcard/tmp/%apk_name% /data/adb/modules/%dir_app_name%/system/priv-app/%dir_app_name%/"

:: Push privapp-permissions-platform.xml to the temporary directory
echo Pushing privapp-permissions-platform.xml to device...
%ADB% push %privapp_permissions_xml_host% /sdcard/tmp/%privapp_permissions_xml_name%

:: Move privapp-permissions-platform.xml to the Magisk module directory
echo Moving privapp-permissions-platform.xml to Magisk module directory...
%ADB_SH% "mv /sdcard/tmp/%privapp_permissions_xml_name% /data/adb/modules/%dir_app_name%/system/etc/permissions/"

:: Create module.prop
echo Creating module.prop file...
(
echo id=%dir_app_name%
echo name=%dir_app_name%
echo version=1.0
echo versionCode=1
echo author=Kaan
echo description=MipSoS
) > module.prop

:: Push module.prop to the Magisk module directory
echo Pushing module.prop to device...
%ADB% push module.prop /sdcard/tmp/module.prop
%ADB_SH% "mv /sdcard/tmp/module.prop /data/adb/modules/%dir_app_name%/module.prop"

:: Set the correct permissions
echo Setting permissions...
%ADB_SH% "chown -R root:root /data/adb/modules/%dir_app_name%"
%ADB_SH% "chmod 755 /data/adb/modules/%dir_app_name%"
%ADB_SH% "chmod 755 /data/adb/modules/%dir_app_name%/system"
%ADB_SH% "chmod 755 /data/adb/modules/%dir_app_name%/system/priv-app"
%ADB_SH% "chmod 755 /data/adb/modules/%dir_app_name%/system/priv-app/%dir_app_name%"
%ADB_SH% "chmod 644 /data/adb/modules/%dir_app_name%/system/priv-app/%dir_app_name%/%apk_name%"
%ADB_SH% "chmod 644 /data/adb/modules/%dir_app_name%/system/etc/permissions/%privapp_permissions_xml_name%"
%ADB_SH% "chmod 644 /data/adb/modules/%dir_app_name%/module.prop"

:: Enable the module
echo Enabling module...
%ADB_SH% "touch /data/adb/modules/%dir_app_name%/update"

:: Clean up temporary files
echo Cleaning up temporary files...
%ADB_SH% "rm -r /sdcard/tmp"
del module.prop

:: Reboot the device to apply changes
echo Rebooting device...
%ADB_SH% "reboot"

echo APK installation completed.
