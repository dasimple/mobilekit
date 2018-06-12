#import "Cursor.h"

@implementation Cursor

- (instancetype) initWithDatabase: (SQLiteDatabase *) database andSQL: (NSString *) sql andBindArgs: (NSArray *) bindArgs
{
	if(self = [super init])
	{
		_statement = [[SQLiteStatement alloc] initWithDatabase: database andSQL: sql andBindArgs: bindArgs];
	}
	return self;
}

- (BOOL) moveToNext
{
	return [self.statement execute];
}

- (void) close
{
	[self.statement close];
}

- (int) getInt: (int) columnIndex
{
	return sqlite3_column_int(self.statement.native, columnIndex);
}

- (NSString *) getString: (int) columnIndex
{
	const char *value = (const char *) sqlite3_column_text(self.statement.native, columnIndex);
	if(value)
	{
		return [NSString stringWithUTF8String: value];
	}
	return nil;
}

@end
