#import <Foundation/Foundation.h>

@interface DatabaseFunctions : NSObject

+ (NSString *) sqlEscapeString: (NSString *) value;
+ (NSArray *) sqlEscapeColumns: (NSArray *) columns;
+ (NSString *) createWhereColumn: (NSString *) name;
+ (NSDictionary *) parseValues: (NSString *) json;
+ (NSArray *) parseColumns: (NSString *) json;
+ (void) parseWhere: (NSString *) json refWhere: (NSString **) where refWhereArgs: (NSArray **) whereArgs;

@end
