import 'dart:io';

import 'package:data_usage/data_usage.dart';
import 'package:flutter/material.dart';
import 'dart:async';

void main() {
  runApp(MyApp());
}

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Home(),
    );
  }
}

class Home extends StatefulWidget {
  @override
  _HomeState createState() => _HomeState();
}

class _HomeState extends State<Home> {
  List<DataUsageModel> _dataUsage = [];
  IOSDataUsageModel _dataiOSUsage;

  @override
  void initState() {
    initPlatformState();
    super.initState();
  }

  Future<void> initPlatformState() async {
    List<DataUsageModel> dataUsage;
    IOSDataUsageModel dataiOSUsage;
    try {
      print(await DataUsage.init());

      print('''dataUsage''');
      dataUsage = await DataUsage.dataUsageAndroid(
        withAppIcon: true,
        dataUsageType: DataUsageType.wifi,
      );

      dataiOSUsage = await DataUsage.dataUsageIOS();

      print(dataUsage);
    } catch (e) {
      print(e.toString());
    }

    if (!mounted) return;

    setState(() {
      _dataUsage = dataUsage;
      _dataiOSUsage = dataiOSUsage;
    });
  }

  @override
  Widget build(BuildContext context) {
    var size = MediaQuery.of(context).size;
    return Scaffold(
      appBar: AppBar(
        title: const Text('Data Usage Plugin Example'),
      ),
      body: Center(
        child: Platform.isAndroid
            ? Android(dataUsage: _dataUsage, size: size)
            : Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: _dataiOSUsage
                        ?.toJson()
                        ?.entries
                        ?.map((e) => Text(
                              '${e.key}: ${e.value}',
                              overflow: TextOverflow.ellipsis,
                            ))
                        ?.toList() ??
                    []),
      ),
    );
  }
}

class Android extends StatelessWidget {
  const Android({
    Key key,
    @required List<DataUsageModel> dataUsage,
    @required this.size,
  })  : _dataUsage = dataUsage,
        super(key: key);

  final List<DataUsageModel> _dataUsage;
  final Size size;

  @override
  Widget build(BuildContext context) {
    return ListView(
      children: [
        if (_dataUsage != null)
          for (var item in _dataUsage) ...[
            Padding(
              padding: const EdgeInsets.all(8.0),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.start,
                children: [
                  SizedBox(width: 10),
                  if (item.appIconBytes != null)
                    Container(
                      height: 60,
                      width: 60,
                      decoration: BoxDecoration(
                        image: DecorationImage(
                          image: MemoryImage(item.appIconBytes),
                        ),
                      ),
                    ),
                  SizedBox(width: 10),
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Container(
                        width: size.width * 0.7,
                        child: Text(
                          '${item.appName}',
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                      SizedBox(height: 10),
                      Container(
                        width: size.width * 0.7,
                        child: Text(
                          '${item.packageName}',
                          overflow: TextOverflow.ellipsis,
                          style: TextStyle(color: Colors.grey, fontSize: 11),
                        ),
                      ),
                      SizedBox(height: 6),
                      Row(
                        children: [
                          Text(
                            'Received: ${(item.received / 1048576).toStringAsFixed(4)}MB  ',
                            style: TextStyle(
                                color: Colors.grey[700], fontSize: 13),
                          ),
                          Text(
                            'Sent: ${(item.sent / 1048576).toStringAsFixed(4)}MB',
                            style: TextStyle(
                                color: Colors.grey[700], fontSize: 13),
                          ),
                        ],
                      ),
                    ],
                  ),
                ],
              ),
            ),
            Divider()
          ]
      ],
    );
  }
}
