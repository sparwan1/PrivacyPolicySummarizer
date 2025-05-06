## Automated Privacy Risk Detection: A Scalable Method For Assessing Mobile Apps

We all have faced issues when it comes to data being collected by different apps. The PrivacyPolicySummarizer app aims to resolve the issue by summarizing privacy policies for users.

[![Watch the demo](https://img.shields.io/badge/Watch-Demo-blue?style=for-the-badge)](https://drive.google.com/file/d/1ut8o1DXr-lAJ8Fgmv1d28DjLdLWCGRT9/view?usp=sharing)

### To use the app:
- Follow the instructions mentioned on [this doc](https://docs.google.com/document/d/1KNhTVfGqgAhE0ms17-zGvL4-atIk7gLuSWWqDpf87ow/edit?usp=drivesdk)
- You need to install the app on your Android phone.
- You will be able to see the summarization of the privacy policies of the apps you have on your phone.
- The app will also give you notifications about the privacy policies of any new apps that you are installing.

### Steps to run the app on your local device:

- Clone the repo: `git clone https://github.com/unallami/PrivacyPolicySummarizer.git`
- Install Android Studio.
- Open the project in Android Studio.
- In the terminal, first run `./gradle clean build`
- Then run `./gradlew installDebug`
- You can now connect your Android phone with a USB to your laptop and run the app using the `run` option on the Android Studio.
- The app will automatically get installed on your phone.
