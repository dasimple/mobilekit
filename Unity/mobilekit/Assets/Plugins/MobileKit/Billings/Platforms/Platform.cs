using System;

namespace MobileKit.Billings.Platforms
{
	public abstract class Platform
	{
		private static bool singleton = false;
		protected Platform()
		{
			if(singleton)
			{
				throw new Exception("The billings platform is a singleton.");
			}
			singleton = true;
		}
		public abstract void Connect();
		public abstract void Disconnect();
		public abstract void Refresh();
		public abstract bool CanMakePayments();
		public abstract void QueryProductsDetails(string request);
		public abstract void QueryPurchases(string request);
		public abstract void QueryPurchaseHistory(string request);
		public abstract void Purchase(string request, string payload);
	}
}
