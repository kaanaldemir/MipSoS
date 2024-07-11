<h2 align="center"><u>MipSoS</u></h2>
<p align="center">
  <img src="https://raw.githubusercontent.com/kaanaldemir/MipSoS/master/app/src/main/res/mipmap-xxxhdpi/ic_launcher_foreground.webp" alt="MipSoS"/>
</p>


<p align="center">
<br>
</p>

### [+] Description
MipSoS is an Android application designed to provide users with the ability to view and select APNs (Access Point Names) based on signal strength, primarily for use during emergency situations. The app also includes additional features such as sending SMS messages with predefined text and location, and sound recording with playback.


Main Features

APN Management: View available APNs and select the one with the best signal strength. This is particularly useful in emergency situations where a primary network might be down, and users need to connect to an alternative network.

Signal Strength Sorting: APNs are sorted by their signal strength to help users choose the most reliable network available.



Additional Features

Emergency SMS: Send an SMS with a predefined message and the user's current location to a designated contact.

Sound Recording: Record and playback sound. This feature is currently standalone and not integrated with other app functionalities.



Permissions

The app requires high level permissions to function correctly



Installation

Since MipSoS is a system app intended to be shipped with specific phones, it should be placed in the system/priv-app folder on the device and the permissions must be also set inside privapp-permissions-platform.xml.
This provides the app with elevated permissions necessary for its functions.
A script pushes the apk and and privapp-permissions-platform.xml to their correct locations via Magisk Module.



Usage

Enter the predefined text for the emergency message.
The app will append the user's current location to the message.
Send the SMS to the designated contact.



APN Management

Press the APN Settings button to view a list of available APNs.
APNs are sorted by signal strength, with the strongest signal appearing first.
Select the desired APN to switch to that network.



Development Prerequisites

Android Studio
A device or emulator running Android with the necessary permissions set up, rooted and Magisk installed.



Clone The Repository

git clone https://github.com/yourusername/MipSoS.git
Open the project in Android Studio.
Build and run the app on your device or emulator.



Contributions
Contributions are welcome! Please fork the repository and submit pull requests.



License
This project is licensed under the MIT License. See the LICENSE file for details.
