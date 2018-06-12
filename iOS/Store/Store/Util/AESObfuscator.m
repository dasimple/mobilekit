#import "AESObfuscator.h"
#import "../Encryptor/FBEncryptorAES.h"

@implementation AESObfuscator
{
	NSString *_password;
}

- (instancetype) initWithPassword: (NSString *) password
{
	if(self = [super init])
	{
		_password = password;
	}
	return self;
}

- (NSString *) obfuscateString: (NSString *) unobfuscated
{
	@synchronized (self)
	{
		return [FBEncryptorAES encryptBase64String: unobfuscated keyString: _password separateLines: NO];
	}
}

- (NSArray *) obfuscateArray: (NSArray *) unobfuscated
{
	if(!unobfuscated)
	{
		return nil;
	}
	NSMutableArray *obfuscated = [NSMutableArray new];
	for(NSString *value in unobfuscated)
	{
		NSString *obfuscatedValue = [self obfuscateString: value];
		[obfuscated addObject: obfuscatedValue];
	}
	return obfuscated;
}

- (NSDictionary *) obfuscateDictionary: (NSDictionary *) unobfuscated
{
	if(!unobfuscated)
	{
		return nil;
	}
	NSMutableDictionary *obfuscated = [NSMutableDictionary new];
	for(NSString *key in unobfuscated)
	{
		NSString *value = [unobfuscated valueForKey: key];
		NSString *obfuscatedValue = [self obfuscateString: value];
		[obfuscated setValue: obfuscatedValue forKey: key];
	}
	return obfuscated;
}

- (NSString *) unobfuscate: (NSString *) obfuscated
{
	@synchronized (self)
	{
		return [FBEncryptorAES decryptBase64String: obfuscated keyString: _password];
	}
}

@end
