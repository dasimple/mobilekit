#import <Foundation/Foundation.h>

@interface AESObfuscator : NSObject

- (instancetype) initWithPassword: (NSString *) password;
- (NSString *) obfuscateString: (NSString *) unobfuscated;
- (NSArray *) obfuscateArray: (NSArray *) unobfuscated;
- (NSDictionary *) obfuscateDictionary: (NSDictionary *) unobfuscated;
- (NSString *) unobfuscate: (NSString *) obfuscated;

@end
