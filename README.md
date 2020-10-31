# Data Usage

Data Usage gets Mobile/Wifi data usage values from mobile devices, on android it will fetch the data per app but due to current limitations on ios it can only give the total/complete values of overall data used.

## Screen Shots

<p float="left">
<img src="https://github.com/Zfinix/data_usage/blob/main/1.png?raw=true" width="200">
</p>

## Usage for Android

   - Initialize plugin and requests for permission
   - Request Data usage stats

   ```dart
     DataUsageModel.init() // Only Required for Android
     List<DataUsageModel> dataUsage = await DataUsage.dataUsageAndroid(
        withAppIcon: true, // if false `DataUsageModel.appIconBytes` will be null
        dataUsageType: DataUsageType.wifi, // DataUsageType.wifi | DataUsageType.mobile
        oldVersion: false // will be true for Android versions lower than 23 (MARSHMELLOW)
      );
   ```

  This would return:

   ```dart
      [   ...,
         DataUsageModel({
               String appName; //App's Name
               String packageName; // App's package name
               Uint8List appIconBytes; // Icon in bytes
               int received; // Amount of data Received
               int sent; // Amount of data sent/transferred
         })
      ]
   ```

[For more explanation](https://stackoverflow.com/questions/17674790/how-do-i-programmatically-show-data-usage-of-all-applications/29084035)



## Usage for iOS

 Request for Total data usage on iOS devices

   ```dart
     IOSDataUsageModel dataUsage = await DataUsage.dataUsageIOS();
   ```

 This would return:

   ```dart
     IOSDataUsageModel({
        int wifiCompelete, // Total Amount of wifi data (received + sent)
        int wifiReceived, // Amount of wifi data Received
        int wifiSent, // Amount of data sent/transferred
        int wwanCompelete, // Total Amount of mobile data (received + sent)
        int wwanReceived, // Amount of mobile data Received
        int wwanSent // Amount of data sent/transferred
     });
   ```

## Contribution

 Lots of PR's would be needed to make this plugin standard, as for iOS there's a permanent limitation for getting the exact data usage, there's only one way arount it and it's super complex.
