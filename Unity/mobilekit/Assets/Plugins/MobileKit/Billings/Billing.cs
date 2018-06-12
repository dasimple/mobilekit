using UnityEngine.Events;
using MobileKit.Billings.Platforms;

namespace MobileKit.Billings
{
	public static class Billing
	{
		#if UNITY_ANDROID && ANDROID_IAB_GOOGLEPLAY
		//Here goes Android settings for Google Play Store
		#elif UNITY_ANDROID && ANDROID_IAB_AMAZON
		//Here goes Android settings for Amazon Store
		#elif UNITY_IOS
		//Here goes iOS settings
		#endif

		public static UnityAction OnConnect = delegate {};
		public static UnityAction<string> OnConnectFail = delegate {};
		public static UnityAction OnDisconnect = delegate {};
		public static UnityAction<string> OnProductsDetailsLoad = delegate {};
		public static UnityAction<string> OnPurchasesLoad = delegate {};
		public static UnityAction<string> OnPurchaseHistoryLoad = delegate {};
		public static UnityAction OnPurchaseStart = delegate {};
		public static UnityAction<string> OnPurchaseFail = delegate {};
		public static UnityAction<string> OnPurchaseComplete = delegate {};

		private static bool isInitialized = false;
		private static Platform cachedPlatform = null;

		public static Platform platform
		{
			get
			{
				if(!isInitialized)
				{
					BillingEvents.Create();
					isInitialized = true;
				}
				if(cachedPlatform == null)
				{
					#if UNITY_ANDROID && !UNITY_EDITOR
					cachedPlatform = new PlatformAndroid();
					#elif UNITY_IOS && !UNITY_EDITOR
					cachedPlatform = new PlatformiOS();
					#else
					cachedPlatform = new PlatformEditor();
					#endif
				}
				return cachedPlatform;
			}
		}

		public static void Connect()
		{
			platform.Connect();
		}

		public static void Disconnect()
		{
			platform.Disconnect();
		}

		public static void Refresh()
		{
			platform.Refresh();
		}

		public static bool CanMakePayments()
		{
			return platform.CanMakePayments();
		}

		public static void QueryProductsDetails(string request)
		{
			platform.QueryProductsDetails(request);
		}

		public static void QueryPurchases(string request)
		{
			platform.QueryPurchases(request);
		}

		public static void QueryPurchaseHistory(string request)
		{
			platform.QueryPurchaseHistory(request);
		}

		public static void Purchase(string request, string payload)
		{
			platform.Purchase(request, payload);
		}
	}
}
