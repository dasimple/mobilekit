#import <Foundation/Foundation.h>
#import <sqlite3.h>
#import "SQLiteDatabase.h"

@class SQLiteDatabase;

@interface SQLiteStatement : NSObject

@property (readonly) sqlite3_stmt *native;
@property (readonly) SQLiteDatabase *database;

- (instancetype) initWithDatabase: (SQLiteDatabase *) database andSQL: (NSString *) sql andBindArgs: (NSArray *) bindArgs;
- (int) executeUpdateDelete;
- (long) executeInsert;
- (BOOL) execute;
- (void) close;

@end
