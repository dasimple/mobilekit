#import "DatabaseFunctions.h"
#import "Bridge.h"

@implementation DatabaseFunctions

+ (NSString *) sqlEscapeString: (NSString *) value
{
	return [NSString stringWithFormat: @"\"%@\"", value];
}

+ (NSArray *) sqlEscapeColumns: (NSArray *) columns
{
	NSMutableArray *escapedColumns = [NSMutableArray new];
	for(int i = 0; i < [columns count]; i++)
	{
		NSString *column = [columns objectAtIndex: i];
		NSString *escapedColumn = [self sqlEscapeString: column];
		[escapedColumns insertObject: escapedColumn atIndex: i];
	}
	return escapedColumns;
}

+ (NSString *) createWhereColumn: (NSString *) name
{
	return [NSString stringWithFormat: @"%@ = ?", [self sqlEscapeString: name]];
}

+ (NSDictionary *) parseValues: (NSString *) json
{
	NSDictionary *object = [Bridge parseJSON: json];
	if(object)
	{
		NSMutableDictionary *values = [NSMutableDictionary new];
		for(NSString *column in object)
		{
			NSString *value = object[column];
			NSString *escapedColumn = [DatabaseFunctions sqlEscapeString: column];
			[values setValue: value forKey: escapedColumn];
		}
		return values;
	} else {
		[Bridge log: [NSString stringWithFormat: @"Cannot parse values: %@", json]];
	}
	return nil;
}

+ (NSArray *) parseColumns: (NSString *) json
{
	NSArray *array = [Bridge parseJSON: json];
	if(array)
	{
		NSMutableArray *columns = [NSMutableArray new];
		for(NSString* column in array)
		{
			[columns addObject: column];
		}
		return columns;
	} else {
		[Bridge log: [NSString stringWithFormat: @"Cannot parse *columns: %@", json]];
	}
	return nil;
}

+ (void) parseWhere: (NSString *) json refWhere: (NSString **) where refWhereArgs: (NSArray **) whereArgs
{
	NSDictionary *object = [Bridge parseJSON: json];
	if(object)
	{
		NSMutableString *clause = [NSMutableString new];
		NSMutableArray *args = [NSMutableArray new];
		int i = 0;
		for(NSString *column in object)
		{
			if(i > 0)
			{
				[clause appendString: @" AND "];
			}
			NSString *value = object[column];
			[clause appendString: [self createWhereColumn: column]];
			[args addObject: value];
			i++;
		}
		*where = clause;
		*whereArgs = args;
	} else {
		[Bridge log: [NSString stringWithFormat: @"Cannot parse where: %@", json]];
	}
}

@end
