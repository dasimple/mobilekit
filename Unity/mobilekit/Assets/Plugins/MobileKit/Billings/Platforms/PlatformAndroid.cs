#if UNITY_ANDROID
using UnityEngine;

namespace MobileKit.Billings.Platforms
{
	public class PlatformAndroid : Platform
	{
		private AndroidJavaClass native = null;

		public PlatformAndroid()
		{
			#if ANDROID_IAB_GOOGLEPLAY
			this.native = new AndroidJavaClass("com.dasimple.mobilekit.billing.GooglePlay");
			#elif ANDROID_IAB_AMAZON
			this.native = new AndroidJavaClass("com.dasimple.mobilekit.billing.Amazon");
			#endif
		}

		public override void Connect()
		{
			native.CallStatic("connect");
		}

		public override void Disconnect()
		{
			native.CallStatic("disconnect");
		}

		public override void Refresh()
		{
			native.CallStatic("refresh");
		}

		public override bool CanMakePayments()
		{
			return native.CallStatic<bool>("canMakePayments");
		}

		public override void QueryProductsDetails(string request)
		{
			native.CallStatic("queryProductsDetails", request);
		}

		public override void QueryPurchases(string request)
		{
			native.CallStatic("queryPurchases", request);
		}

		public override void QueryPurchaseHistory(string request)
		{
			native.CallStatic("queryPurchaseHistory", request);
		}

		public override void Purchase(string request, string payload)
		{
			native.CallStatic("purchase", request, payload);
		}
	}
}
#endif
