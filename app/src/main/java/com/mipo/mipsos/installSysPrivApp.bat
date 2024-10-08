@echo off
:: Set necessary variables for the app package, directory name, and main activity
set app_package=com.mipo.mipsos
set dir_app_name=MipSoS
set MAIN_ACTIVITY=com.mipo.mipsos.MainActivity

:: Set ADB command
set ADB="adb"

:: Set path for the APK location and its name
set apk_host=app\build\outputs\apk\debug\app-debug.apk
set apk_name=%dir_app_name%.apk

:: Delete previous APK if it exists
if exist %apk_host% del %apk_host%

:: Compile the APK using Gradle, adapting for production build, flavors, etc.
call gradlew assembleDebug

:: Verify if the APK was successfully built
if not exist %apk_host% (
    echo APK build failed.
    pause
    exit /b 1
)

:: Get the device model and set it to the variable device_model
for /f "tokens=*" %%i in ('%ADB% shell getprop ro.product.model') do set device_model=%%i

:: Define paths based on the device model
if "%device_model%"=="SM-M315F" (
    :: Set paths for Samsung Galaxy M31
    set permissions_xml_host=app\src\main\java\com\mipo\mipsos\Samsung\privapp-permissions-platform.xml
    set permissions_xml_name=privapp-permissions-platform.xml
    set apk_path=/data/adb/modules/%dir_app_name%/system/priv-app/%dir_app_name%
    set permissions_path=/data/adb/modules/%dir_app_name%/system/etc/permissions
) else if "%device_model%"=="mipo_M59" (
    :: Set paths for mipo_M59 device
    set permissions_xml_host=app\src\main\java\com\mipo\mipsos\privapp-permissions-platform.xml
    set permissions_xml_name=privapp-permissions-platform.xml
    set apk_path=/data/adb/modules/%dir_app_name%/system/priv-app/%dir_app_name%
    set permissions_path=/data/adb/modules/%dir_app_name%/system/etc/permissions
) else if "%device_model%"=="Android SDK built for x86_64" (
     :: Set paths for Android SDK built for x86_64 device
     set permissions_xml_host=app\src\main\java\com\mipo\mipsos\emulator\privapp-permissions-platform.xml
     set permissions_xml_name=privapp-permissions-platform.xml
     set apk_path=/data/adb/modules/%dir_app_name%/system/priv-app/%dir_app_name%
     set permissions_path=/data/adb/modules/%dir_app_name%/system/etc/permissions
) else (
     :: Stop script for unsupported devices to prevent potential issues
     echo Unsupported device model: %device_model%
     echo Script will stop to prevent potential issues.
     pause
     exit /b 1
)

:: Set ADB shell command with superuser permissions
set ADB_SH=%ADB% shell su -c

:: Create necessary directories on the device for Magisk module
echo Creating module directories on device...
%ADB_SH% "mkdir -p %apk_path%"
%ADB_SH% "mkdir -p %permissions_path%"
%ADB_SH% "mkdir -p /sdcard/tmp"

:: Push the APK to the temporary directory on the device
echo Pushing APK to device...
%ADB% push %apk_host% /sdcard/tmp/%apk_name%

:: Move the APK from the temporary directory to the Magisk module directory
echo Moving APK to module directory...
%ADB_SH% "mv /sdcard/tmp/%apk_name% %apk_path%/"

:: Push the permissions XML to the temporary directory on the device
echo Pushing permissions XML to device...
%ADB% push %permissions_xml_host% /sdcard/tmp/%permissions_xml_name%

:: Move the permissions XML from the temporary directory to the Magisk module directory
echo Moving permissions XML to Magisk module directory...
%ADB_SH% "mv /sdcard/tmp/%permissions_xml_name% %permissions_path%/"

:: Create the module.prop file with module details
echo Creating module.prop file...
(
echo id=%dir_app_name%
echo name=%dir_app_name%
echo version=1.1.0
echo versionCode=1
echo author=Kaan
echo description=Installs the app inside system/priv-app and includes permissions inside privapp-permissions-platform.xml
) > module.prop

:: Push the module.prop file to the temporary directory on the device
echo Pushing module.prop to device...
%ADB% push module.prop /sdcard/tmp/module.prop
%ADB_SH% "mv /sdcard/tmp/module.prop /data/adb/modules/%dir_app_name%/module.prop"

:: Set the correct permissions for the module files and directories
echo Setting permissions...
%ADB_SH% "chown -R root:root /data/adb/modules/%dir_app_name%"
%ADB_SH% "chmod 755 /data/adb/modules/%dir_app_name%"
%ADB_SH% "chmod 755 %apk_path%"
%ADB_SH% "chmod 644 %apk_path%/%apk_name%"
%ADB_SH% "chmod 755 %permissions_path%"
%ADB_SH% "chmod 644 %permissions_path%/%permissions_xml_name%"
%ADB_SH% "chmod 644 /data/adb/modules/%dir_app_name%/module.prop"

:: Conditional block to change permissions of tty files only for mipo_M59
if "%device_model%"=="mipo_M59" (
    echo Changing permissions of tty files...
    %ADB_SH% "chmod 666 /dev/ttyC0"
)

:: Enable the Magisk module
echo Enabling module...
%ADB_SH% "touch /data/adb/modules/%dir_app_name%/update"

:: Clean up temporary files
echo Cleaning up temporary files...
%ADB_SH% "rm -r /sdcard/tmp"
del module.prop

:: Reboot the device to apply the changes
echo Rebooting device...
%ADB_SH% "reboot"

:: Wait for the device to reboot
echo Waiting for device to reboot...
:wait_for_device
%ADB% wait-for-device
echo Device found, continuing...

:: Wait an additional 60 seconds to ensure the system is fully booted
echo Waiting for system to fully boot...

if "%device_model%"=="SM-M315F" (
    echo Waiting for SM-M315F to fully boot...
    set wait_time=10
) else if "%device_model%"=="mipo_M59" (
    echo Waiting for mipo_M59 to fully boot...
    set wait_time=10
) else (
    echo Waiting for unknown to fully boot...
    set wait_time=20
)

:wait_loop
set /a wait_time=wait_time-1
ping 127.0.0.1 -n 2 >nul
if %wait_time% gtr 0 goto wait_loop

:: Install the APK as a user app to ensure all permissions are granted
echo Installing APK as user app to ensure permissions...
%ADB% install -r %apk_host%

echo APK installation completed.
exit
