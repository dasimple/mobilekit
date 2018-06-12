#import "Store.h"
#import "Bridge.h"
#import "SQLite/SQLiteDatabase.h"
#import "Util/AESObfuscator.h"
#import "Util/DatabaseFunctions.h"

@implementation Store

static NSString *const DATABASE_NAME = @"dasimple-store.db";
static NSString *const OBFUSCATION_PASSWORD = @"<vasika>_cel-mai-(TARE)-cu_putulika=[MARE]";
static NSString *const COLUMN_NAME = @"name";

static AESObfuscator *guardian = nil;
static SQLiteDatabase *database = nil;

+ (void) connect
{
	if(guardian)
	{
		[Bridge log: @"Store is already connected!"];
		return;
	}
	[Bridge log: @"Connecting to store..."];
	NSString *bundleID = [[NSBundle mainBundle] bundleIdentifier];
	NSString* deviceId = [Bridge getDeviceId];
	guardian = [[AESObfuscator alloc] initWithPassword: [NSString stringWithFormat: @"%@%@%@", bundleID, deviceId, OBFUSCATION_PASSWORD]];
	[self sendConnect];
}

+ (void) disconnect
{
	if(!guardian)
	{
		[Bridge log: @"Store is already disconnected!"];
		return;
	}
	if(database)
	{
		[self close];
	}
	guardian = nil;
	[self sendDisconnect];
}

+ (void) open: (int) version
{
	if(database)
	{
		[Bridge log: @"Database is already opened!"];
		return;
	}
	database = [[SQLiteDatabase alloc] initWithFilename: DATABASE_NAME andVersion: version];
	database.onCreate = ^{
		[Bridge log: [NSString stringWithFormat: @"Database %@ created!", database.filename]];
		[self sendDatabaseCreate];
	};
	database.onOpen = ^{
		[Bridge log: [NSString stringWithFormat: @"Database %@ opened!", database.filename]];
		[self sendDatabaseOpen];
	};
	database.onUpgrade = ^(int oldVersion, int newVersion) {
		[Bridge log: [NSString stringWithFormat: @"Database %@ upgraded from %d to %d!", database.filename, oldVersion, newVersion]];
		[self sendDatabaseUpgrade: oldVersion toVersion: newVersion];
	};
	database.onDowngrade = ^(int oldVersion, int newVersion) {
		[Bridge log: [NSString stringWithFormat: @"Database %@ downgraded from %d to %d!", database.filename, oldVersion, newVersion]];
		[self sendDatabaseDowngrade: oldVersion toVersion: newVersion];
	};
	[database openOrCreate];
	[Bridge log: @"Database initiated successfully!"];
}

+ (void) close
{
	if(!database)
	{
		[Bridge log: @"Database is already closed!"];
		return;
	}
	[database close];
	database = nil;
	[self sendDatabaseClose];
}

+ (BOOL) reset
{
	return NO;
}

+ (BOOL) createCollection: (NSString *) name withColumns: (NSString *) columnsJSON
{
	if(!database)
	{
		[Bridge log: @"Database not initialized."];
		return NO;
	}
	if(![self collectionExists: name])
	{
		[Bridge log: [NSString stringWithFormat: @"Creating collection %@ with columns: %@", name, columnsJSON]];
		NSArray *columns = [Bridge parseJSON: columnsJSON];
		if(columns)
		{
			NSMutableString *columnSQL = [NSMutableString stringWithFormat: @"%@ TEXT PRIMARY KEY NOT NULL", [DatabaseFunctions sqlEscapeString: COLUMN_NAME]];
			for(int i = 0; i < [columns count]; i++)
			{
				NSString *column = [columns objectAtIndex: i];
				if(column && [column length] > 0 && ![COLUMN_NAME isEqualToString: column])
				{
					[columnSQL appendFormat: @", %@ TEXT", [DatabaseFunctions sqlEscapeString: column]];
				}
			}
			NSString *sql = [NSString stringWithFormat: @"CREATE TABLE IF NOT EXISTS %@ ( %@ )", [DatabaseFunctions sqlEscapeString: name], columnSQL];
			[Bridge log: [NSString stringWithFormat: @"Executing SQL: %@", sql]];
			if([database execSQL: sql])
			{
				return YES;
			} else {
				[Bridge log: @"Failed to execute SQL."];
			}
		} else {
			[Bridge log: @"Failed to parse columns JSON."];
		}
	}
	return NO;
}

+ (BOOL) destroyCollection: (NSString *) name
{
	if(!database)
	{
		[Bridge log: @"Database not initialized."];
		return NO;
	}
	if([self collectionExists: name])
	{
		[Bridge log: [NSString stringWithFormat: @"Destroying collection %@", name]];
		NSString *sql = [NSString stringWithFormat: @"DROP TABLE IF EXISTS %@", [DatabaseFunctions sqlEscapeString: name]];
		[Bridge log: [NSString stringWithFormat: @"Executing SQL: %@", sql]];
		if([database execSQL: sql])
		{
			return YES;
		} else {
			[Bridge log: @"Failed to execute SQL."];
		}
	}
	return NO;
}

+ (int) add: (NSString *) collection theRecords: (NSString *) recordsJSON
{
	if(!database)
	{
		[Bridge log: @"Database not initialized."];
		return NO;
	}
	[Bridge log: [NSString stringWithFormat: @"Adding to collection %@ records: %@", collection, recordsJSON]];
	int length = 0;
	NSArray *records = [Bridge parseJSON: recordsJSON];
	if(records)
	{
		for(NSDictionary *record in records)
		{
			NSMutableDictionary *values = [NSMutableDictionary new];
			for(NSString *column in record)
			{
				NSString *value = [record valueForKey: column];
				NSString *escapedColumn = [DatabaseFunctions sqlEscapeString: column];
				[values setValue: value forKey: escapedColumn];
			}
			BOOL success = [self add: collection theValues: values];
			if(success)
			{
				[Bridge log: [NSString stringWithFormat: @"Added values: %@", values]];
				length++;
			}
		}
	} else {
		[Bridge log: @"Failed to parse records JSON."];
	}
	return length;
}

+ (NSString *) get: (NSString *) collection theColumns: (NSString *) columnsJSON withWhere: (NSString *) whereJSON withLimit: (NSString *) limit
{
	if(!database)
	{
		[Bridge log: @"Database not initialized."];
		return nil;
	}
	//Columns
	NSArray *columns = [DatabaseFunctions parseColumns: columnsJSON];
	//Where
	NSString *where = @"";
	NSArray *whereArgs = @[];
	[DatabaseFunctions parseWhere: whereJSON refWhere: &where refWhereArgs: &whereArgs];
	//Return
	return [self get: collection theColumns: columns withWhere: where andWhereArgs: whereArgs withLimit: limit];
}

+ (int) set: (NSString *) collection theValues: (NSString *) valuesJSON withWhere: (NSString *) whereJSON
{
	if(!database)
	{
		[Bridge log: @"Database not initialized."];
		return 0;
	}
	//Values
	NSDictionary *values = [DatabaseFunctions parseValues: valuesJSON];
	//Where
	NSString *where = @"";
	NSArray *whereArgs = @[];
	[DatabaseFunctions parseWhere: whereJSON refWhere: &where refWhereArgs: &whereArgs];
	//Return
	return [self set: collection theValues: values withWhere: where andWhereArgs: whereArgs];
}

+ (int) remove: (NSString *) collection withWhere: (NSString *) whereJSON
{
	if(!database)
	{
		[Bridge log: @"Database not initialized."];
		return 0;
	}
	//Where
	NSString *where = @"";
	NSArray *whereArgs = @[];
	[DatabaseFunctions parseWhere: whereJSON refWhere: &where refWhereArgs: &whereArgs];
	//Return
	return [self remove: collection withWhere: where andWhereArgs: whereArgs];
}

+ (BOOL) collectionExists: (NSString *) name
{
	@synchronized (self)
	{
		NSString *table = @"sqlite_master";
		NSArray *columns = @[COLUMN_NAME];
		NSString *where = [NSString stringWithFormat: @"%@ AND %@", [DatabaseFunctions createWhereColumn: @"type"], [DatabaseFunctions createWhereColumn: COLUMN_NAME]];
		NSArray *whereArgs = @[@"table", name];
		Cursor *cursor = [database query: table asColumns: columns withWhere: where andWhereArgs: whereArgs];
		if(cursor)
		{
			if([cursor moveToNext])
			{
				[cursor close];
				return YES;
			}
		}
		return NO;
	}
}

+ (BOOL) add: (NSString *) collection theValues: (NSDictionary *) values
{
	[Bridge log: [NSString stringWithFormat: @"Adding values %@ into collection %@", values, collection]];
	NSString *nameColumn = [DatabaseFunctions sqlEscapeString: COLUMN_NAME];
	NSString *nameValue = [values objectForKey: nameColumn];
	if(nameValue && [nameValue length] > 0)
	{
		[Bridge log: @"Inserting into database."];
		NSString *table = [DatabaseFunctions sqlEscapeString: collection];
		NSDictionary *safeValues = [guardian obfuscateDictionary: values];
		long insert = [database insert: table withValues: safeValues];
		[Bridge log: [NSString stringWithFormat: @"Inserted at row %ld", insert]];
		return insert > 0;
	}
	return NO;
}

+ (NSString *) get: (NSString *) collection theColumns: (NSArray *) columns withWhere: (NSString *) where andWhereArgs: (NSArray *) whereArgs withLimit: (NSString *) limit
{
	@synchronized (self)
	{
		[Bridge log: [NSString stringWithFormat: @"Getting from collection %@ columns: %@ where %@ - %@", collection, [columns componentsJoinedByString: @","], where, [whereArgs componentsJoinedByString: @","]]];
		NSMutableArray *records = [NSMutableArray new];
		NSString *table = [DatabaseFunctions sqlEscapeString: collection];
		NSArray *safeColumns = [DatabaseFunctions sqlEscapeColumns: columns];
		NSArray *safeWhereArgs = [guardian obfuscateArray: whereArgs];
		Cursor *cursor = [database query: table asColumns: safeColumns withWhere: where andWhereArgs: safeWhereArgs];
		if(cursor)
		{
			while([cursor moveToNext])
			{
				if([columns count] == 1)
				{
					NSString *safeValue = [cursor getString: 0];
					NSString *value = [guardian unobfuscate: safeValue];
					if(value)
					{
						[records addObject: value];
					}
					continue;
				}
				NSMutableDictionary *record = [NSMutableDictionary new];
				for(NSString *column in columns)
				{
					NSUInteger columnIndex = [columns indexOfObject: column];
					if(columnIndex != -1)
					{
						NSString *safeValue = [cursor getString: (int) columnIndex];
						NSString *value = [guardian unobfuscate: safeValue];
						if(value)
						{
							[record setValue: value forKey: column];
						}
					}
				}
				[records addObject: record];
			}
			[cursor close];
		}
		return [Bridge stringifyJSON: records];
	}
}

+ (int) set: (NSString *) collection theValues: (NSDictionary *) values withWhere: (NSString *) where andWhereArgs: (NSArray *) whereArgs
{
	[Bridge log: [NSString stringWithFormat: @"Updating collection %@ values: %@ where %@ - %@", collection, values, where, [whereArgs componentsJoinedByString: @","]]];
	NSString *table = [DatabaseFunctions sqlEscapeString: collection];
	NSDictionary *safeValues = [guardian obfuscateDictionary: values];
	NSArray *safeWhereArgs = [guardian obfuscateArray: whereArgs];
	@try
	{
		return [database update: table withValues: safeValues withWhere: where andWhereArgs: safeWhereArgs];
	}
	@catch (NSException *e)
	{
		[Bridge log: [NSString stringWithFormat: @"Cannot update: %@", [e reason]]];
	}
	return 0;
}

+ (int) remove: (NSString *) collection withWhere: (NSString *) where andWhereArgs: (NSArray *) whereArgs
{
	[Bridge log: [NSString stringWithFormat: @"Deleting from collection %@ where %@ - %@", collection, where, [whereArgs componentsJoinedByString: @","]]];
	NSString *table = [DatabaseFunctions sqlEscapeString: collection];
	NSArray *safeWhereArgs = [guardian obfuscateArray: whereArgs];
	@try
	{
		return [database delete: table withWhere: where andWhereArgs: safeWhereArgs];
	}
	@catch (NSException *e)
	{
		[Bridge log: [NSString stringWithFormat: @"Cannot delete: %@", [e reason]]];
	}
	return 0;
}

/* Events */

+ (void) sendConnect
{
	[Bridge sendEvent: @"Store" ofType: @"Connect" withMessage: @""];
}

+ (void) sendDisconnect
{
	[Bridge sendEvent: @"Store" ofType: @"Disconnect" withMessage: @""];
}

+ (void) sendDatabaseCreate
{
	[Bridge sendEvent: @"Store" ofType: @"DatabaseCreate" withMessage: @""];
}

+ (void) sendDatabaseDowngrade: (int) oldVersion toVersion: (int) newVersion
{
	[Bridge sendEvent: @"Store" ofType: @"DatabaseDowngrade" withMessage: [NSString stringWithFormat: @"%@>%@", [NSNumber numberWithInt: oldVersion], [NSNumber numberWithInt: newVersion]]];
}

+ (void) sendDatabaseUpgrade: (int) oldVersion toVersion: (int) newVersion
{
	[Bridge sendEvent: @"Store" ofType: @"DatabaseUpgrade" withMessage: [NSString stringWithFormat: @"%@>%@", [NSNumber numberWithInt: oldVersion], [NSNumber numberWithInt: newVersion]]];
}

+ (void) sendDatabaseOpen
{
	[Bridge sendEvent: @"Store" ofType: @"DatabaseOpen" withMessage: @""];
}

+ (void) sendDatabaseClose
{
	[Bridge sendEvent: @"Store" ofType: @"DatabaseClose" withMessage: @""];
}

@end
