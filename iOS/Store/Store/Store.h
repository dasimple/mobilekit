#import <Foundation/Foundation.h>

@interface Store : NSObject

+ (void) connect;
+ (void) disconnect;
+ (void) open: (int) version;
+ (void) close;
+ (BOOL) reset;
+ (BOOL) createCollection: (NSString *) name withColumns: (NSString *) columnsJSON;
+ (BOOL) destroyCollection: (NSString *) name;
+ (int) add: (NSString *) collection theRecords: (NSString *) recordsJSON;
+ (NSString *) get: (NSString *) collection theColumns: (NSString *) columnsJSON withWhere: (NSString *) whereJSON withLimit: (NSString *) limit;
+ (int) set: (NSString *) collection theValues: (NSString *) valuesJSON withWhere: (NSString *) whereJSON;
+ (int) remove: (NSString *) collection withWhere: (NSString *) whereJSON;

@end
