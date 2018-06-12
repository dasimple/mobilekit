#import <Foundation/Foundation.h>
#import <sqlite3.h>
#import "Cursor.h"

@class Cursor;

@interface SQLiteDatabase : NSObject

@property (nonatomic, copy) void (^onCreate) (void);
@property (nonatomic, copy) void (^onDowngrade) (int, int);
@property (nonatomic, copy) void (^onOpen) (void);
@property (nonatomic, copy) void (^onUpgrade) (int, int);

@property (readonly) sqlite3 *native;
@property (readonly) BOOL opened;
@property (readonly) NSString *filename;
@property (readonly) int version;
@property (readonly) NSString *path;

- (instancetype) initWithFilename: (NSString *) filename andVersion: (int) version;
- (void) openOrCreate;
- (BOOL) open;
- (BOOL) close;
- (int) getVersion;
- (void) setVersion: (int) version;
- (BOOL) execSQL: (NSString *) sql;
- (long) insert: (NSString *) table withValues: (NSDictionary *) values;
- (Cursor *) rawQuery: (NSString *) sql;
- (Cursor *) query: (NSString *) table asColumns: (NSArray *) columns withWhere: (NSString *) where andWhereArgs: (NSArray *) whereArgs;
- (int) update: (NSString *) table withValues: (NSDictionary *) values withWhere: (NSString *) where andWhereArgs: (NSArray *) whereArgs;
- (int) delete: (NSString *) table withWhere: (NSString *) where andWhereArgs: (NSArray *) whereArgs;

@end
