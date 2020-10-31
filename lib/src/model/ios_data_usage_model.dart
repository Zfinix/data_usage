class IOSDataUsageModel {
  var wifiCompelete;
  var wifiReceived;
  var wifiSent;
  var wwanCompelete;
  var wwanReceived;
  var wwanSent;

  IOSDataUsageModel({
    this.wifiCompelete,
    this.wifiReceived,
    this.wifiSent,
    this.wwanCompelete,
    this.wwanReceived,
    this.wwanSent,
  });

  IOSDataUsageModel.fromJson(Map<String, dynamic> json) {
    wifiCompelete = json['wifiCompelete'];
    wifiReceived = json['wifiReceived'];
    wifiSent = json['wifiSent'];
    wwanCompelete = json['wwanCompelete'];
    wwanReceived = json['wwanReceived'];
    wwanSent = json['wwanSent'];
  }

  Map<String, dynamic> toJson() {
    final Map<String, dynamic> data = new Map<String, dynamic>();
    data['wifiCompelete'] = this.wifiCompelete;
    data['wifiReceived'] = this.wifiReceived;
    data['wifiSent'] = this.wifiSent;
    data['wwanCompelete'] = this.wwanCompelete;
    data['wwanReceived'] = this.wwanReceived;
    data['wwanSent'] = this.wwanSent;
    return data;
  }
}
