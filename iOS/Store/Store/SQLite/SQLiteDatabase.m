#import "SQLiteDatabase.h"
#import "SQLiteStatement.h"

@implementation SQLiteDatabase

+ (NSString *) applicationDocumentsDirectory
{
	NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
	NSString *basePath = ([paths count] > 0) ? paths[0] : nil;
	return basePath;
}

+ (NSString *) applicationDirectory
{
	static NSString* appDir = nil;
	if(appDir == nil)
	{
		NSArray *paths = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, YES);
		if([paths count] == 0)
		{
			return nil;
		}
		NSFileManager *fileManager = [NSFileManager defaultManager];
		NSString *basePath = paths[0];
		NSError *error = nil;
		if(![fileManager fileExistsAtPath: basePath])
		{
			if(![fileManager createDirectoryAtPath: basePath withIntermediateDirectories: YES attributes: nil error: &error])
			{
				return nil;
			}
		}
		appDir = [basePath copy];
	}
	return appDir;
}

- (void) create: (BOOL) close;
{
	if([self open])
	{
		if(close)
		{
			[self close];
		}
	}
}

- (instancetype) initWithFilename: (NSString *) filename andVersion: (int) version
{
	if(self = [super init])
	{
		_filename = filename;
		_version = version;
		_path = [[SQLiteDatabase applicationDirectory] stringByAppendingPathComponent: filename];
	}
	return self;
}

- (void) openOrCreate
{
	@synchronized (self)
	{
		NSString* oldPath = [[SQLiteDatabase applicationDocumentsDirectory] stringByAppendingPathComponent: self.filename];
		NSFileManager *filemgr = [NSFileManager defaultManager];
		NSError *error = nil;
		if([filemgr fileExistsAtPath: oldPath] == YES)
		{
			[[NSFileManager defaultManager] copyItemAtPath: oldPath toPath: self.path error: &error];
			if(!error)
			{
				[filemgr removeItemAtPath: oldPath error: nil];
			}
		}
		if([filemgr fileExistsAtPath: self.path] == NO)
		{
			[self create: false];
			//NSURL* url = [NSURL fileURLWithPath: databasebPath];
			//[KeevaUtils addSkipBackupAttributeToItemAtURL: url];
		}
		if([self open])
		{
			int version = [self getVersion];
			if(version != self.version)
			{
				//check if it's readonly
				if(version == 0)
				{
					self.onCreate();
				} else {
					if(version > self.version)
					{
						self.onDowngrade(version, self.version);
					} else {
						self.onUpgrade(version, self.version);
					}
				}
				[self setVersion: self.version];
			}
			self.onOpen();
		}
	}
}

- (BOOL) open
{
	if(!self.opened)
	{
		_opened = sqlite3_open([self.path UTF8String], &_native) == SQLITE_OK;
	}
	return self.opened;
}

- (BOOL) close
{
	if(self.opened)
	{
		_opened = sqlite3_close(self.native) != SQLITE_OK;
	}
	return self.opened;
}

- (int) getVersion
{
	int version = 0;
	Cursor *cursor = [self rawQuery: @"PRAGMA user_version;"];
	if(cursor)
	{
		if([cursor moveToNext])
		{
			version = [cursor getInt: 0];
		}
		[cursor close];
	}
	return version;
}

- (void) setVersion: (int) version
{
	NSString *sql = [[NSString alloc] initWithFormat: @"PRAGMA user_version = %d;", version];
	[self execSQL: sql];
}

- (BOOL) execSQL: (NSString *) sql
{
	char *error = nil;
	BOOL success = sqlite3_exec(self.native, [sql UTF8String], NULL, NULL, &error) == SQLITE_OK;
	return success;
}

- (long) insert: (NSString *) table withValues: (NSDictionary *) values
{
	long inserted = -1;
	NSMutableString *columnSQL = [NSMutableString new];
	NSMutableString *valueSQL = [NSMutableString new];
	NSMutableArray *bindArgs = [NSMutableArray new];
	int i = 0;
	for(NSString *key in values)
	{
		NSString *value = values[key];
		if(i > 0)
		{
			[columnSQL appendString: @", "];
		}
		[columnSQL appendString: key];
		if(i > 0)
		{
			[valueSQL appendString: @", "];
		}
		[valueSQL appendString: @"?"];
		[bindArgs addObject: value];
		i++;
	}
	NSMutableString *sql = [NSMutableString stringWithFormat: @"INSERT INTO %@ ( %@ ) VALUES ( %@ )", table, columnSQL, valueSQL];
	SQLiteStatement *statement = [[SQLiteStatement alloc] initWithDatabase: self andSQL: sql andBindArgs: bindArgs];
	inserted = [statement executeInsert];
	[statement close];
	return inserted;
}

- (Cursor *) rawQuery: (NSString *) sql withBindArgs: (NSArray *) bindArgs
{
	return [[Cursor alloc] initWithDatabase: self andSQL: sql andBindArgs: bindArgs];
}

- (Cursor *) rawQuery: (NSString *) sql
{
	return [self rawQuery: sql withBindArgs: nil];
}

- (Cursor *) query: (NSString *) table asColumns: (NSArray *) columns withWhere: (NSString *) where andWhereArgs: (NSArray *) whereArgs
{
	NSMutableString *sql = [NSMutableString stringWithString: @"SELECT "];
	if(columns && [columns count] > 0)
	{
		int i = 0;
		for(NSString *column in columns)
		{
			if(i > 0)
			{
				[sql appendString: @", "];
			}
			[sql appendString: column];
			i++;
		}
	} else {
		[sql appendString: @"*"];
	}
	[sql appendFormat: @" FROM %@", table];
	NSMutableArray *bindArgs = [NSMutableArray new];
	if(where && [where length] > 0)
	{
		[sql appendFormat: @" WHERE %@", where];
		[bindArgs addObjectsFromArray: whereArgs];
	}
	return [self rawQuery: sql withBindArgs: bindArgs];
}

- (int) update: (NSString *) table withValues: (NSDictionary *) values withWhere: (NSString *) where andWhereArgs: (NSArray *) whereArgs
{
	int affected = 0;
	NSMutableString *sql = [NSMutableString stringWithFormat: @"UPDATE %@ SET ", table];
	NSMutableArray *bindArgs = [NSMutableArray new];
	int i = 0;
	for(NSString *key in values)
	{
		NSString *value = values[key];
		if(i > 0)
		{
			[sql appendString: @", "];
		}
		[sql appendFormat: @"%@ = ?", key];
		[bindArgs addObject: value];
		i++;
	}
	if(where && [where length] > 0)
	{
		[sql appendFormat: @" WHERE %@", where];
		[bindArgs addObjectsFromArray: whereArgs];
	}
	SQLiteStatement *statement = [[SQLiteStatement alloc] initWithDatabase: self andSQL: sql andBindArgs: bindArgs];
	affected = [statement executeUpdateDelete];
	[statement close];
	return affected;
}

- (int) delete: (NSString *) table withWhere: (NSString *) where andWhereArgs: (NSArray *) whereArgs
{
	int affected = 0;
	NSMutableString *sql = [NSMutableString stringWithFormat: @"DELETE FROM %@", table];
	NSMutableArray *bindArgs = [NSMutableArray new];
	if(where && [where length] > 0)
	{
		[sql appendFormat: @" WHERE %@", where];
		[bindArgs addObjectsFromArray: whereArgs];
	}
	SQLiteStatement *statement = [[SQLiteStatement alloc] initWithDatabase: self andSQL: sql andBindArgs: bindArgs];
	affected = [statement executeUpdateDelete];
	[statement close];
	return affected;
}

@end
