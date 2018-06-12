#import <Foundation/Foundation.h>
#import "Bridge.h"
#import "Store.h"

extern "C"
{
	void iosStore_connect()
	{
		[Store connect];
	}
	
	void iosStore_disconnect()
	{
		[Store disconnect];
	}
	
	void iosStore_open(int version)
	{
		[Store open: version];
	}
	
	void iosStore_close()
	{
		[Store close];
	}
	
	bool iosStore_reset()
	{
		return [Store reset];
	}
	
	bool iosStore_createCollection(const char* name, const char* columnsJSON)
	{
		NSString *nameNS = [NSString stringWithUTF8String: name];
		NSString *columnsJSONNS = [NSString stringWithUTF8String: columnsJSON];
		return [Store createCollection: nameNS withColumns: columnsJSONNS];
	}
	
	bool iosStore_destroyCollection(const char* name)
	{
		NSString *nameNS = [NSString stringWithUTF8String: name];
		return [Store destroyCollection: nameNS];
	}
	
	int iosStore_add(const char* collection, const char* recordsJSON)
	{
		NSString *collectionNS = [NSString stringWithUTF8String: collection];
		NSString *recordsJSONNS = [NSString stringWithUTF8String: recordsJSON];
		return [Store add: collectionNS theRecords: recordsJSONNS];
	}
	
	char* iosStore_get(const char* collection, const char* columnsJSON, const char* whereJSON, const char* limit)
	{
		NSString *collectionNS = [NSString stringWithUTF8String: collection];
		NSString *columnsJSONNS = [NSString stringWithUTF8String: columnsJSON];
		NSString *whereJSONNS = [NSString stringWithUTF8String: whereJSON];
		NSString *limitNS = [NSString stringWithUTF8String: limit];
		NSString *result = [Store get: collectionNS theColumns: columnsJSONNS withWhere: whereJSONNS withLimit: limitNS];
		return [Bridge convertToUnityString: [result UTF8String]];
	}
	
	int iosStore_set(const char* collection, const char* valuesJSON, const char* whereJSON)
	{
		NSString *collectionNS = [NSString stringWithUTF8String: collection];
		NSString *valuesJSONNS = [NSString stringWithUTF8String: valuesJSON];
		NSString *whereJSONNS = [NSString stringWithUTF8String: whereJSON];
		return [Store set: collectionNS theValues: valuesJSONNS withWhere: whereJSONNS];
	}
	
	int iosStore_remove(const char* collection, const char* whereJSON)
	{
		NSString *collectionNS = [NSString stringWithUTF8String: collection];
		NSString *whereJSONNS = [NSString stringWithUTF8String: whereJSON];
		return [Store remove: collectionNS withWhere: whereJSONNS];
	}
}
