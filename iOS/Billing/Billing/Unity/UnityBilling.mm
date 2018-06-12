#import <Foundation/Foundation.h>
#import "Bridge.h"
#import "Billing.h"

extern "C"
{
	void iosBilling_connect()
	{
		[Billing connect];
	}
	
	void iosBilling_disconnect()
	{
		[Billing disconnect];
	}
	
	void iosBilling_refresh()
	{
		[Billing refresh];
	}
	
	bool iosBilling_canMakePayments()
	{
		return [Billing canMakePayments];
	}
	
	void iosBilling_queryProductsDetails(const char* requestJSON)
	{
		NSString *requestJSONNS = [NSString stringWithUTF8String: requestJSON];
		[Billing queryProductsDetails: requestJSONNS];
	}
	
	void iosBilling_queryPurchases(const char* requestJSON)
	{
		NSString *requestJSONNS = [NSString stringWithUTF8String: requestJSON];
		[Billing queryPurchases: requestJSONNS];
	}
	
	void iosBilling_queryPurchaseHistory(const char* requestJSON)
	{
		NSString *requestJSONNS = [NSString stringWithUTF8String: requestJSON];
		[Billing queryPurchaseHistory: requestJSONNS];
	}
	
	void iosBilling_purchase(const char* requestJSON, const char* payload)
	{
		NSString *requestJSONNS = [NSString stringWithUTF8String: requestJSON];
		NSString *payloadNS = [NSString stringWithUTF8String: payload];
		[Billing purchase: requestJSONNS withPayload: payloadNS];
	}
}

