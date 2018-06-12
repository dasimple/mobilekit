using System.Collections.Generic;

namespace DaSimple.MobileKit.Storing.Collections
{
	public abstract class Countables64 : Countables
	{
		private Dictionary<string, long> cachedBalance = new Dictionary<string, long>(); //name -> balance

		public Countables64(string table) : base(table)
		{
			if(cachedBalance.Count <= 0)
			{
				StoreList columns = new StoreList(nameColumn, balanceColumn);
				StoreDictionary[] records = GetMultiple(columns, null, -1);
				Cache(records);
			}
		}

		public override int Add(StoreDictionary[] records)
		{
			GenerateProtection(); //this is becuase Add is called before constructor
			Cache(records); //do a cache
			return base.Add(records);
		}

		public StoreDictionary Bake(string name, long balance)
		{
			StoreDictionary values = Bake(name);
			values.Add(balanceColumn, balance + "");
			return values;
		}

		public virtual long GetBalance(string name)
		{
			if(!cachedBalance.ContainsKey(name))
			{
				long value = GetAsLong(name, balanceColumn, 0);
				cachedBalance.Add(name, value + protectionOffset);
			}
			return cachedBalance[name] - protectionOffset;
		}

		public virtual bool SetBalance(string name, long value)
		{
			if(value < 0)
			{
				value = 0;
			}
			long secureValue = value + protectionOffset;
			if(cachedBalance.ContainsKey(name))
			{
				cachedBalance[name] = secureValue;
			} else {
				cachedBalance.Add(name, secureValue);
			}
			return Set(name, balanceColumn, value + "") == 1;
		}

		public virtual bool CanGive(string name, long amount)
		{
			return true;
		}

		public virtual bool Give(string name, long amount)
		{
			if(amount >= 0)
			{
				long balance = GetBalance(name);
				balance += amount;
				return SetBalance(name, balance);
			}
			return false;
		}

		public virtual bool CanTake(string name, long amount)
		{
			return GetBalance(name) >= amount;
		}

		public virtual bool Take(string name, long amount)
		{
			if(amount >= 0)
			{
				long balance = GetBalance(name);
				if(balance < amount)
				{
					throw new InsufficientBalanceException(table, amount);
				}
				balance -= amount;
				return SetBalance(name, balance);
			}
			return false;
		}

		private void Cache(StoreDictionary[] records)
		{
			foreach(StoreDictionary record in records)
			{
				string name = record.Get(nameColumn);
				if(!string.IsNullOrEmpty(name))
				{
					long balance = record.GetAsLong(balanceColumn, 0);
					cachedBalance.Add(name, balance + protectionOffset);
				}
			}
		}
	}
}
