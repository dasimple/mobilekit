#import <Foundation/Foundation.h>
#import <sqlite3.h>
#import "SQLiteDatabase.h"
#import "SQLiteStatement.h"

@class SQLiteDatabase;
@class SQLiteStatement;

@interface Cursor : NSObject

@property (readonly) SQLiteStatement *statement;

- (instancetype) initWithDatabase: (SQLiteDatabase *) database andSQL: (NSString *) sql andBindArgs: (NSArray *) bindArgs;
- (BOOL) moveToNext;
- (void) close;
- (int) getInt: (int) columnIndex;
- (NSString *) getString: (int) columnIndex;

@end
