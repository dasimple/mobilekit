using System.Collections.Generic;

namespace DaSimple.MobileKit.Storing.Collections
{
	public abstract class Upgradables : Equippables
	{
		public const string levelColumn = "level";

		//group -> level
		private Dictionary<string, int> cachedLevel = new Dictionary<string, int>();

		public Upgradables(string table) : base(table)
		{
			if(cachedLevel.Count <= 0)
			{
				StoreList columns = new StoreList(nameColumn, groupColumn, equippedColumn, levelColumn);
				StoreDictionary[] records = GetMultiple(columns, null, -1);
				Cache(records);
			}
		}

		public override int Add(StoreDictionary[] records)
		{
			Cache(records); //do a cache
			return base.Add(records);
		}

		public virtual StoreDictionary Bake(string name, string group, int level, bool equipped = false)
		{
			StoreDictionary values = Bake(name, group, equipped);
			values.Add(levelColumn, level + "");
			return values;
		}

		public virtual int GetLevel(string name)
		{
			int level = GetAsInt(name, levelColumn, 0);
			if(level < 0)
			{
				level = 0;
			}
			return level;
		}

		public virtual int GetLevelOfGroup(string group)
		{
			if(group == noGroupValue)
			{
				return 0;
			}
			if(!cachedLevel.ContainsKey(group))
			{
				int level = 0;
				string equipped = GetEquipped(group); //equipped name
				if(!string.IsNullOrEmpty(equipped))
				{
					level = GetLevel(equipped);
				}
				cachedLevel.Add(group, level);
			}
			return cachedLevel[group];
		}

		public virtual bool SetLevelOfGroup(string group, int level)
		{
			if(group != noGroupValue)
			{
				if(level < 0)
				{
					level = 0;
				}
				string equipped = GetEquipped(group);
				if(!string.IsNullOrEmpty(equipped))
				{
					Unequip(equipped);
				}
				StoreDictionary columns = new StoreDictionary(equippedColumn, equippedValue);
				StoreDictionary where = new StoreDictionary(groupColumn, group, levelColumn, level + "");
				if(cachedLevel.ContainsKey(group))
				{
					cachedLevel[group] = level;
				} else {
					cachedLevel.Add(group, level);
				}
				return Set(columns, where) == 1;
            }
			return false;
        }

		public virtual bool IncreaseLevelOfGroup(string group)
		{
			int level = GetLevelOfGroup(group);
			return SetLevelOfGroup(group, ++level);
		}

		public virtual bool DecreaseLevelOfGroup(string group)
		{
			int level = GetLevelOfGroup(group);
			return SetLevelOfGroup(group, --level);
		}

		protected override void AllocateColumns()
		{
			base.AllocateColumns();
			this.columns.Add(levelColumn);
		}

		private void Cache(StoreDictionary[] records)
		{
			foreach(StoreDictionary record in records)
			{
				string group = record.Get(groupColumn);
				if(group != noGroupValue && !cachedLevel.ContainsKey(group))
				{
					string name = record.Get(nameColumn);
					if(!string.IsNullOrEmpty(name))
					{
						bool equipped = record.Get(equippedColumn) == equippedValue;
						if(equipped)
						{
							int level = record.GetAsInt(levelColumn, 0);
							cachedLevel.Add(group, level);
						}
					}
				}
			}
		}
    }
}
