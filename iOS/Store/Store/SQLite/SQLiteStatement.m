#import "SQLiteStatement.h"

@implementation SQLiteStatement

- (instancetype) initWithDatabase: (SQLiteDatabase *) database andSQL: (NSString *) sql andBindArgs: (NSArray *) bindArgs
{
	if(self = [super init])
	{
		if(sqlite3_prepare_v2([database native], [sql UTF8String], -1, &_native, NULL) != SQLITE_OK)
		{
			//throw sql exception
		}
		if(bindArgs && [bindArgs count] > 0)
		{
			int i = 1;
			for(NSString *bindArg in bindArgs)
			{
				sqlite3_bind_text(self.native, i, [bindArg UTF8String], -1, NULL);
				i++;
			}
		}
		_database = database;
	}
	return self;
}

- (int) executeUpdateDelete
{
	if(sqlite3_step(self.native) != SQLITE_DONE)
	{
		//throw sql exception
	}
	return sqlite3_changes([self.database native]);
}

- (long) executeInsert
{
	if(sqlite3_step(self.native) != SQLITE_DONE)
	{
		//throw sql exception
	}
	return sqlite3_last_insert_rowid([self.database native]);
}

- (BOOL) execute
{
	return sqlite3_step(self.native) == SQLITE_ROW;
}

- (void) close
{
	sqlite3_finalize(self.native);
}

@end
