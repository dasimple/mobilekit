using System.Collections.Generic;

namespace MobileKit.Storing.Collections
{
	public abstract class Countables : VirtualCollection
	{
		public const string balanceColumn = "balance";
		protected int protectionOffset = 0; //as balances cannot be negative, this value will be negative, to keep working range positive and less than max value
		public Countables(string table) : base(table)
		{
			GenerateProtection();
		}
		protected void GenerateProtection()
		{
			if(protectionOffset == 0)
			{
				protectionOffset = UnityEngine.Random.Range(-9999, -4999);
			}
		}
		protected override void AllocateColumns()
		{
			base.AllocateColumns();
			columns.Add(balanceColumn);
		}
	}
}
