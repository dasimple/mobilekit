using System.Collections.Generic;

namespace MobileKit.Storing.Collections
{
	public abstract class Countables32 : Countables
	{
		private Dictionary<string, int> cachedBalance = new Dictionary<string, int>(); //name -> balance
		public Countables32(string table) : base(table)
		{
			if(cachedBalance.Count <= 0)
			{
				StoreList columns = new StoreList(nameColumn, balanceColumn);
				IEnumerable<StoreDictionary> records = GetMultiple(columns, null, -1);
				Cache(records);
			}
		}
		public StoreDictionary Bake(string name, int balance)
		{
			StoreDictionary values = Bake(name);
			values.Add(balanceColumn, balance + "");
			return values;
		}
		public virtual int GetBalance(string name)
		{
			if(!cachedBalance.ContainsKey(name))
			{
				int value = GetAsInt(name, balanceColumn, 0);
				cachedBalance.Add(name, value + protectionOffset);
			}
			return cachedBalance[name] - protectionOffset;
		}
		public virtual bool SetBalance(string name, int value)
		{
			if(value < 0)
			{
				value = 0;
			}
			int secureValue = value + protectionOffset;
			if(cachedBalance.ContainsKey(name))
			{
				cachedBalance[name] = secureValue;
			} else {
				cachedBalance.Add(name, secureValue);
			}
			return Set(name, balanceColumn, value + "") == 1;
		}
		public virtual bool CanGive(string name, int amount)
		{
			return true;
		}
		public virtual bool Give(string name, int amount)
		{
			if(amount >= 0)
			{
				int balance = GetBalance(name);
				balance += amount;
				return SetBalance(name, balance);
			}
			return false;
		}
		public virtual bool CanTake(string name, int amount)
		{
			return GetBalance(name) >= amount;
		}
		public virtual bool Take(string name, int amount)
		{
			if(amount >= 0)
			{
				int balance = GetBalance(name);
				if(balance < amount)
				{
					throw new InsufficientBalanceException(table, amount);
				}
				balance -= amount;
				return SetBalance(name, balance);
			}
			return false;
		}
		protected override void Cache(IEnumerable<StoreDictionary> records)
		{
			foreach(StoreDictionary record in records)
			{
				string name = record.Get(nameColumn);
				if(!string.IsNullOrEmpty(name))
				{
					int balance = record.GetAsInt(balanceColumn, 0);
					cachedBalance.Add(name, balance + protectionOffset);
				}
			}
		}
	}
}
