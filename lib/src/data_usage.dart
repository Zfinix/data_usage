import 'dart:async';
import 'dart:io';
import 'package:data_usage/src/model/data_usage_model.dart';
import 'package:flutter/services.dart';
import 'package:permissions_plugin/permissions_plugin.dart';

import 'model/ios_data_usage_model.dart';

/// Type of DataUsage.
enum DataUsageType { mobile, wifi }

class DataUsage {
  static const MethodChannel _channel = const MethodChannel('data_usage');

  /// Initializes plugin and requests for permission
  static Future<bool> init() async {
    try {
      final permission = await PermissionsPlugin.checkPermissions(
          [Permission.READ_PHONE_STATE]);

      if (permission[Permission.READ_PHONE_STATE] != PermissionState.GRANTED) {
        await PermissionsPlugin.requestPermissions(
            [Permission.READ_PHONE_STATE]);
        return await init();
      } else {
        return await _channel.invokeMethod('init');
      }
    } catch (e) {
      return false;
    }
  }

  ///Gets Data Usage From Android Device as `Future<List<DataUsageModel>>`
  ///
  /// Params:
  /// ```dart
  /// bool withAppIcon // if false `DataUsageModel.appIconBytes` will be null.
  /// bool oldVersion // will be true for Android versions lower than 23 (MARSHMELLOW)
  /// DataUsageType dataUsageType // Toggle between Wifi and Mobile Data Usage
  ///
  /// ```
  ///
  /// [WARNING]
  ///
  /// This method only supports Android versions greater than `21 (LOLLIPOP)`
  ///
  /// For android versions greater than `23 (MARSHMELLOW)`
  ///
  /// - Call `DataUsageType.init()` first before this to get necesarry permissions
  ///
  /// For android versions greater than `21 (LOLLIPOP)` less than `23 (MARSHMELLOW)` :
  ///
  /// - Data resets after every reboot
  ///
  /// - It may also be unsupported on some devices.

  static Future<List<DataUsageModel>> dataUsageAndroid({
    bool withAppIcon = false,
    bool oldVersion = false,
    DataUsageType dataUsageType = DataUsageType.mobile,
  }) async {
    if (Platform.isAndroid) {
      final List<dynamic> dataUsage = await _channel.invokeMethod(
        oldVersion ? 'getDataUsageOld' : 'getDataUsage',
        <String, dynamic>{
          "withAppIcon": withAppIcon,
          "isWifi": dataUsageType == DataUsageType.wifi,
        },
      );
      return dataUsage
          .map((e) => DataUsageModel.fromJson(Map<String, dynamic>.from(e)))
          .toList();
    } else {
      //Limit API to Android Platform
      print(
        PlatformException(
          code: 'DATA_USAGE',
          message:
              'This method can only be called on an android device use .dataUsageIOS() instead',
        ),
      );
      return [];
    }
  }

  /// Gets Data Usage From iOS Device as `Future<IOSDataUsageModel>`

  /// [WARNING]
  ///
  /// - This method will only get the total amounts of data transfered and received
  ///
  /// - Data resets after every reboot

  static Future<IOSDataUsageModel> dataUsageIOS() async {
    if (Platform.isIOS) {
      final data = await _channel.invokeMethod(
        'getDataUsage',
      );
      return IOSDataUsageModel.fromJson(Map<String, dynamic>.from(data));
    } else {
      //Limit API to iOS Platform
      throw PlatformException(
          code: 'DATA_USAGE',
          message:
              'This method can only be called on an ios device use .dataUsageAndroid() instead');
    }
  }
}
