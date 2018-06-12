#import <Foundation/Foundation.h>

@interface Bridge : NSObject

+ (char *) convertToUnityString: (const char*) string;
+ (id) parseJSON: (NSString *) value;
+ (NSString *) stringifyJSON: (id) value;
+ (NSString *) getDeviceId;
+ (void) log: (NSString *) message;
+ (void) sendEvent: (NSString *) listener ofType: (NSString *) type withMessage: (NSString *) message;

@end
