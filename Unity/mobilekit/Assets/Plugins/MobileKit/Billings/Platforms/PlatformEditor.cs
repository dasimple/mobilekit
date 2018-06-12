#if UNITY_EDITOR
using UnityEngine;

namespace MobileKit.Billings.Platforms
{
	public class PlatformEditor : Platform
	{
		public override void Connect()
		{
			BillingEvents.Connect();
		}

		public override void Disconnect()
		{
			//Nothing to do..
		}

		public override void Refresh()
		{
			//Nothing to do..
		}

		public override bool CanMakePayments()
		{
			//Of course, YES!
			return true;
		}

		public override void QueryProductsDetails(string request)
		{
			//In the editor the request is returned as result, immediately!
			BillingEvents.ProductsDetailsLoad(request);
		}

		public override void QueryPurchases(string request)
		{
			//In the editor the request is returned as result, immediately!
			BillingEvents.PurchasesLoad(request);
		}

		public override void QueryPurchaseHistory(string request)
		{
			//In the editor the request is returned as result, immediately!
			BillingEvents.PurchaseHistoryLoad(request);
		}

		public override void Purchase(string request, string payload)
		{
			Debug.Log("WOW! Free purchase, you're lucky <3. Payment: " + request + ", payload: " + payload);
			//Purchases with editor are successfull every time!
			BillingEvents.PurchaseStart();
			BillingEvents.PurchaseComplete(payload);
		}
	}
}
#endif
