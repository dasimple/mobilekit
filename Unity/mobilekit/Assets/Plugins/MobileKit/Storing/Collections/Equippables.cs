using System.Collections.Generic;

namespace DaSimple.MobileKit.Storing.Collections
{
	public abstract class Equippables : Lifetimes
	{
		public const string groupColumn = "group";
		public const string equippedColumn = "equipped";
		public const string singleGroupValue = ".";
		public const string noGroupValue = "-";
		public const string unequippedValue = "";
		public const string equippedValue = "fck";

		private Dictionary<string, string> cachedGroup = new Dictionary<string, string>(); //name -> group
		private Dictionary<string, bool> cachedEquipped = new Dictionary<string, bool>(); //name -> equipped

		public Equippables(string table) : base(table)
		{
			if(cachedGroup.Count <= 0 && cachedEquipped.Count <= 0)
			{
				StoreList columns = new StoreList(nameColumn, groupColumn, equippedColumn);
				StoreDictionary[] records = GetMultiple(columns, null, -1);
				Cache(records);
			}
		}

		public override int Add(StoreDictionary[] records)
		{
			Cache(records); //do a cache
			return base.Add(records);
		}

		public virtual StoreDictionary Bake(string name, string group, bool equipped = false)
		{
			StoreDictionary values = Bake(name);
			values.Add(groupColumn, group);
			if(equipped)
			{
				values.Add(balanceColumn, "1");
				values.Add(equippedColumn, equippedValue);
			}
			return values;
		}

		public virtual string GetGroup(string name)
		{
			if(!cachedGroup.ContainsKey(name))
			{
				string group = Get(name, groupColumn);
				cachedGroup.Add(name, group);
			}
			return cachedGroup[name];
		}

		public virtual bool IsEquipped(string name)
		{
			if(!cachedEquipped.ContainsKey(name))
			{
				bool equipped = Get(name, equippedColumn) == equippedValue;
				cachedEquipped.Add(name, equipped);
			}
			return cachedEquipped[name];
		}

		public virtual bool Equip(string name)
		{
			if(!IsOwned(name))
			{
				return false;
			}
			string group = GetGroup(name);
			if(group != noGroupValue)
			{
				string equipped = GetEquipped(group);
				if(!string.IsNullOrEmpty(equipped))
				{
					Unequip(equipped);
				}
			}
			if(cachedEquipped.ContainsKey(name))
			{
				cachedEquipped[name] = true;
			} else {
				cachedEquipped.Add(name, true);
			}
			return Set(name, equippedColumn, equippedValue) == 1;
		}

		public virtual bool Unequip(string name)
		{
			if(cachedEquipped.ContainsKey(name))
			{
				cachedEquipped[name] = false;
			} else {
				cachedEquipped.Add(name, false);
			}
			return Set(name, equippedColumn, unequippedValue) == 1;
        }

		public virtual bool ToggleEquip(string name)
		{
			bool equipped = IsEquipped(name);
			return ToggleEquip(name, equipped);
		}

		public virtual bool ToggleEquip(string name, bool equipped)
		{
			if(equipped)
			{
				return Equip(name);
			}
			return Unequip(name);
		}

		public virtual string GetEquipped(string group = singleGroupValue)
		{
			if(group != noGroupValue)
			{
				foreach(KeyValuePair<string, string> entry in cachedGroup)
				{
					if(entry.Value == group && cachedEquipped.ContainsKey(entry.Key) && cachedEquipped[entry.Key])
					{
						return entry.Key;
					}
				}
				StoreDictionary where = new StoreDictionary(groupColumn, group, equippedColumn, equippedValue);
				string name = Get(nameColumn, where);
				if(!string.IsNullOrEmpty(name))
				{
					if(cachedGroup.ContainsKey(name))
					{
						cachedGroup[name] = group;
					} else {
						cachedGroup.Add(name, group);
					}
					if(cachedEquipped.ContainsKey(name))
					{
						cachedEquipped[name] = true;
					} else {
						cachedEquipped.Add(name, true);
					}
				}
				return name;
			}
			return null;
		}

		protected override void AllocateColumns()
		{
			base.AllocateColumns();
			columns.Add(groupColumn);
			columns.Add(equippedColumn);
		}

		private void Cache(StoreDictionary[] records)
		{
			foreach(StoreDictionary record in records)
			{
				string name = record.Get(nameColumn);
				if(!string.IsNullOrEmpty(name))
				{
					string group = record.Get(groupColumn);
					bool equipped = record.Get(equippedColumn) == equippedValue;
					cachedGroup.Add(name, group);
					cachedEquipped.Add(name, equipped);
				}
			}
		}
	}
}
