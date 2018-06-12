#import <UIKit/UIDevice.h>
#import "Unity/UnitySendMessage.h"
#import "Bridge.h"

@implementation Bridge

+ (char *) convertToUnityString: (const char *) string
{
	if(string == NULL)
	{
		return NULL;
	}
	char* result = (char*) malloc(strlen(string) + 1);
	strcpy(result, string);
	return result;
}

+ (id) parseJSON: (NSString *) value
{
	NSError *error = nil;
	NSData* data = [value dataUsingEncoding: NSUTF8StringEncoding];
	return [NSJSONSerialization JSONObjectWithData: data options: kNilOptions error: &error];
}

+ (NSString *) stringifyJSON: (id) value
{
	NSError *error = nil;
	NSData *data = [NSJSONSerialization dataWithJSONObject: value options: kNilOptions error: &error];
	return [[NSString alloc] initWithData: data encoding: NSUTF8StringEncoding];
}

+ (NSString *) getDeviceId
{
	NSString *deviceId = [[[UIDevice currentDevice] identifierForVendor] UUIDString];
	if([deviceId length] == 0)
	{
		deviceId = @"RANDOM_DEVICE_ID";
	}
	return deviceId;
}

+ (void) log: (NSString *) message
{
	//Below row must be commented, just for debugging
	//NSLog(@"EXPlugin: %@", message);
}

+ (void) sendEvent: (NSString *) listener ofType: (NSString *) type withMessage: (NSString *) message
{
	NSString *unityListener = [listener stringByAppendingString: @"Events"];
	NSString *unityEvent = [@"Event" stringByAppendingString: type];
	UnitySendMessage([unityListener UTF8String], [unityEvent UTF8String], [message UTF8String]);
}

@end
