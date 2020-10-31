#import "DataUsagePlugin.h"
#if __has_include(<data_usage/data_usage-Swift.h>)
#import <data_usage/data_usage-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "data_usage-Swift.h"
#endif

@implementation DataUsagePlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftDataUsagePlugin registerWithRegistrar:registrar];
}
@end
