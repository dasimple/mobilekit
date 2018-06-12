#import <Foundation/Foundation.h>

@interface Billing : NSObject

+ (void) connect;
+ (void) disconnect;
+ (void) refresh;
+ (BOOL) canMakePayments;
+ (void) queryProductsDetails: (NSString *) requestJSON;
+ (void) queryPurchases: (NSString *) requestJSON;
+ (void) queryPurchaseHistory: (NSString *) requestJSON;
+ (void) purchase: (NSString *) requestJSON withPayload: (NSString *) payload;

@end
