import 'dart:convert';

import 'dart:typed_data';

class DataUsageModel {
  String appName;
  String packageName;
  Uint8List appIconBytes;
  int received;
  int sent;

  DataUsageModel({
    this.appName,
    this.packageName,
    this.appIconBytes,
    this.received,
    this.sent,
  });

  DataUsageModel.fromJson(Map<String, dynamic> json) {
    appName = json['app_name'];
    packageName = json['package_name'];
    appIconBytes = json['app_icon'];
    received = json['received'];
    sent = json['sent'];
  }

  Map<String, dynamic> toJson() {
    final Map<String, dynamic> data = new Map<String, dynamic>();
    data['app_name'] = this.appName;
    data['package_name'] = this.packageName;
    data['app_icon'] = this.appIconBytes;
    data['received'] = this.received;
    data['sent'] = this.sent;
    return data;
  }
}
