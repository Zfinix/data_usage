import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:data_usage/data_usage.dart';

void main() {
  const MethodChannel channel = MethodChannel('data_usage');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('getPlatformVersion', () async {
    // expect(await DataUsage.platformVersion, '42');
  });
}
