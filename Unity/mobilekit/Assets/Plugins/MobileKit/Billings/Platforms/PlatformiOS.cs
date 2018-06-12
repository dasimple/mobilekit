#if UNITY_IOS
using System.Runtime.InteropServices;

namespace DaSimple.MobileKit.Billings.Platforms
{
	public class PlatformiOS : Platform
	{
		[DllImport("__Internal")]
		private static extern void iosBilling_connect();
		[DllImport("__Internal")]
		private static extern void iosBilling_disconnect();
		[DllImport("__Internal")]
		private static extern void iosBilling_refresh();
		[DllImport("__Internal")]
		private static extern bool iosBilling_canMakePayments();
		[DllImport("__Internal")]
		private static extern void iosBilling_queryProductsDetails(string request);
		[DllImport("__Internal")]
		private static extern void iosBilling_queryPurchases(string request);
		[DllImport("__Internal")]
		private static extern void iosBilling_queryPurchaseHistory(string request);
		[DllImport("__Internal")]
		private static extern void iosBilling_purchase(string request, string payload);

		public override void Connect()
		{
			iosBilling_connect();
		}

		public override void Disconnect()
		{
			iosBilling_disconnect();
		}

		public override void Refresh()
		{
			iosBilling_refresh();
		}

		public override bool CanMakePayments()
		{
			return iosBilling_canMakePayments();
		}

		public override void QueryProductsDetails(string request)
		{
			iosBilling_queryProductsDetails(request);
		}

		public override void QueryPurchases(string request)
		{
			iosBilling_queryPurchases(request);
		}

		public override void QueryPurchaseHistory(string request)
		{
			iosBilling_queryPurchaseHistory(request);
		}

		public override void Purchase(string request, string payload)
		{
			iosBilling_purchase(request, payload);
		}
	}
}
#endif
