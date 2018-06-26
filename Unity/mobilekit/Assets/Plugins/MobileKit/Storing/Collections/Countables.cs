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
		public override int Add(IEnumerable<StoreDictionary> records)
		{
			GenerateProtection(); //this is becuase Add is called before constructor
			Cache(records); //do a cache
			return base.Add(records);
		}
		protected virtual void Cache(IEnumerable<StoreDictionary> records)
		{
			
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
