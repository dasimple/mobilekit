#import <StoreKit/StoreKit.h>
#import "Billing.h"
#import "Bridge.h"

@interface Billing()<SKProductsRequestDelegate, SKPaymentTransactionObserver>

@end

@implementation Billing
{
	SKProductsRequest *productsRequest;
	NSArray<SKProduct *> *products;
	NSMutableDictionary *cachedPayloads;
}

//Connect fail codes
NSString *const CONNECT_NOT_SUPPORTED = @"CONNECT_NOT_SUPPORTED";
NSString *const CONNECT_UNKNOWN = @"CONNECT_UNKNOWN";

//Purchase fail codes
NSString *const PURCHASE_NOT_SUPPORTED = @"PURCHASE_NOT_SUPPORTED";
NSString *const PURCHASE_CANCELED = @"PURCHASE_CANCELED";
NSString *const PURCHASE_ITEM_ALREADY_OWNED = @"PURCHASE_ITEM_ALREADY_OWNED";
NSString *const PURCHASE_ITEM_UNAVAILABLE = @"PURCHASE_ITEM_UNAVAILABLE";
NSString *const PURCHASE_INVALID = @"PURCHASE_INVALID";
NSString *const PURCHASE_UNKNOWN = @"PURCHASE_UNKNOWN";

static Billing *instance = nil;

+ (void) connect
{
	instance = [[Billing alloc] init];
}

+ (void) disconnect
{
	instance = nil;
}

+ (void) refresh
{
	
}

+ (BOOL) canMakePayments
{
	if(!instance)
	{
		[Bridge log: @"Seems that service is not connected yet."];
		return NO;
	}
	return [SKPaymentQueue canMakePayments];
}

+ (void) queryProductsDetails: (NSString *) requestJSON
{
	if(!instance)
	{
		[Bridge log: @"Seems that service is not connected yet."];
		return;
	}
	NSArray *productIdentifiers = [Bridge parseJSON: requestJSON];
	[instance queryProductsDetails: productIdentifiers];
}

+ (void) queryPurchases: (NSString *) requestJSON
{
	if(!instance)
	{
		[Bridge log: @"Seems that service is not connected yet."];
		return;
	}
}

+ (void) queryPurchaseHistory: (NSString *) requestJSON
{
	if(!instance)
	{
		[Bridge log: @"Seems that service is not connected yet."];
		return;
	}
}

+ (void) purchase: (NSString *) requestJSON withPayload: (NSString *) payload
{
	if(![self canMakePayments])
	{
		[self sendPurchaseFail: PURCHASE_NOT_SUPPORTED];
		return;
	}
	NSDictionary *request = [Bridge parseJSON: requestJSON];
	NSString *productIdentifier = [request valueForKey: @"sku"];
	[instance purchase: productIdentifier withPayload: payload];
}

/* Events */

+ (void) sendConnectFail: (NSString *) code
{
	[Bridge sendEvent: @"Billing" ofType: @"ConnectFail" withMessage: code];
}

+ (void) sendConnect
{
	[Bridge sendEvent: @"Billing" ofType: @"Connect" withMessage: @""];
}

+ (void) sendDisconnect
{
	[Bridge sendEvent: @"Billing" ofType: @"Disconnect" withMessage: @""];
}

+ (void) sendProductsDetailsLoad: (NSString *) productsDetails
{
	[Bridge sendEvent: @"Billing" ofType: @"ProductsDetailsLoad" withMessage: productsDetails];
}

+ (void) sendPurchasesLoad: (NSString *) purchases
{
	[Bridge sendEvent: @"Billing" ofType: @"PurchasesLoad" withMessage: purchases];
}

+ (void) sendPurchaseHistoryLoad: (NSString *) purchaseHistory
{
	[Bridge sendEvent: @"Billing" ofType: @"PurchaseHistoryLoad" withMessage: purchaseHistory];
}

+ (void) sendPurchaseStart
{
	[Bridge sendEvent: @"Billing" ofType: @"PurchaseStart" withMessage: @""];
}

+ (void) sendPurchaseFail: (NSString *) code
{
	[Bridge sendEvent: @"Billing" ofType: @"PurchaseFail" withMessage: code];
}

+ (void) sendPurchaseComplete: (NSString *) payload
{
	[Bridge sendEvent: @"Billing" ofType: @"PurchaseComplete" withMessage: payload];
}

/* Instance */

- (instancetype) init
{
	if(self = [super init])
	{
		[[SKPaymentQueue defaultQueue] addTransactionObserver: self];
		cachedPayloads = [NSMutableDictionary new];
		[Billing sendConnect];
	}
	return self;
}

- (void) queryProductsDetails: (NSArray *) productIdentifiers
{
	productsRequest = [[SKProductsRequest alloc] initWithProductIdentifiers: [NSSet setWithArray: productIdentifiers]];
	productsRequest.delegate = self;
	[productsRequest start];
}

- (SKProduct *) getProductByIdentifier: (NSString *) productIdentifier
{
	if(!products)
	{
		return nil;
	}
	for(SKProduct *product in products)
	{
		if([product.productIdentifier isEqualToString: productIdentifier])
		{
			return product;
		}
	}
	return nil;
}

- (void) purchase: (NSString *) productIdentifier withPayload: (NSString *) payload
{
	SKProduct *product = [self getProductByIdentifier: productIdentifier];
	if(!product)
	{
		[Billing sendPurchaseFail: PURCHASE_ITEM_UNAVAILABLE];
		return;
	}
	[cachedPayloads setValue: payload forKey: productIdentifier];
	SKMutablePayment *payment = [SKMutablePayment paymentWithProduct: product];
	[[SKPaymentQueue defaultQueue] addPayment: payment];
	[Billing sendPurchaseStart];
}

- (void) productsRequest: (SKProductsRequest *) request didReceiveResponse: (SKProductsResponse *) response
{
	NSMutableArray *productsDetails = [NSMutableArray new];
	for(SKProduct *product in response.products)
	{
		NSDictionary *productDetails = @{
			@"productId": product.productIdentifier,
			@"price": product.price,
			@"currency": [product.priceLocale objectForKey: NSLocaleCurrencyCode]
		};
		[productsDetails addObject: productDetails];
	}
	NSString *productsDetailsJSON = [Bridge stringifyJSON: productsDetails];
	[Billing sendProductsDetailsLoad: productsDetailsJSON];
	products = response.products;
}

- (void) paymentQueue: (SKPaymentQueue *) queue updatedTransactions: (NSArray *) transactions
{
	for(SKPaymentTransaction *transaction in transactions)
	{
		switch(transaction.transactionState)
		{
			case SKPaymentTransactionStatePurchasing:
			{
				//show a progress
				break;
			}
			case SKPaymentTransactionStateDeferred:
			{
				//show a progress
				break;
			}
			case SKPaymentTransactionStateFailed:
			{
				[queue finishTransaction: transaction];
				switch(transaction.error.code)
				{
					case SKErrorClientInvalid:
					case SKErrorPaymentInvalid:
					{
						[Billing sendPurchaseFail: PURCHASE_INVALID];
						break;
					}
					case SKErrorPaymentCancelled:
					{
						[Billing sendPurchaseFail: PURCHASE_CANCELED];
						break;
					}
					case SKErrorPaymentNotAllowed:
					{
						[Billing sendPurchaseFail: PURCHASE_NOT_SUPPORTED];
						break;
					}
					case SKErrorStoreProductNotAvailable:
					{
						[Billing sendPurchaseFail: PURCHASE_ITEM_UNAVAILABLE];
						break;
					}
					default:
					{
						[Billing sendPurchaseFail: PURCHASE_UNKNOWN];
						break;
					}
				}
				break;
			}
			case SKPaymentTransactionStatePurchased:
			{
				NSString *productIdentifier = transaction.payment.productIdentifier;
				NSString *payload = productIdentifier;
				if([cachedPayloads objectForKey: productIdentifier])
				{
					payload = [cachedPayloads valueForKey: productIdentifier];
				}
				[queue finishTransaction: transaction];
				[Billing sendPurchaseComplete: payload];
				break;
			}
			case SKPaymentTransactionStateRestored:
			{
				//restore products, just for subscription and non-consumable
				break;
			}
			default:
			{
				break;
			}
		}
	}
}

- (BOOL) paymentQueue: (SKPaymentQueue *) queue shouldAddStorePayment: (SKPayment *) payment forProduct: (SKProduct *) product
{
	return YES;
}

@end
