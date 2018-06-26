using System.Collections.Generic;

namespace MobileKit.Storing.Collections
{
	public abstract class VirtualCollection
	{
		public const string nameColumn = "name";
		public string table = null;
		public StoreList columns = new StoreList();
		public VirtualCollection(string table)
		{
			this.table = table;
			AllocateColumns();
			CreateCollection();
		}
		public virtual int Add(IEnumerable<StoreDictionary> records)
		{
			return Store.Add(table, records);
		}
		public virtual int Add(params StoreDictionary[] records)
		{
			return Add(records);
		}
		public virtual StoreDictionary Bake(string name)
		{
			return new StoreDictionary(nameColumn, name);
		}
		/* Get multiple - 2+ columns */
		public virtual IEnumerable<StoreDictionary> GetMultiple(StoreList columns, StoreDictionary where = null, int limit = 1)
		{
			return Store.Get(table, columns, where, limit);
		}
		public virtual IEnumerable<StoreDictionary> GetMultiple(StoreList columns, string whereColumn, string whereValue, int limit = 1)
		{
			StoreDictionary where = new StoreDictionary(whereColumn, whereValue);
			return GetMultiple(columns, where, limit);
		}
		/* Get multiple - 1 column */
		public virtual IEnumerable<string> GetMultiple(string column, StoreDictionary where = null, int limit = 1)
		{
			return Store.Get(table, column, where, limit);
		}
		public virtual IEnumerable<string> GetMultiple(string column, string whereColumn, string whereValue, int limit = 1)
		{
			StoreDictionary where = new StoreDictionary(whereColumn, whereValue);
			return GetMultiple(column, where, limit);
		}
		/* Get single (stop at first record) - 2+ columns */
		public virtual StoreDictionary Get(StoreList columns, StoreDictionary where = null)
		{
			List<StoreDictionary> records = new List<StoreDictionary>(GetMultiple(columns, where));
			return records.Count > 0 ? records[0] : null;
		}
		public virtual StoreDictionary Get(StoreList columns, string whereColumn, string whereValue)
		{
			StoreDictionary where = new StoreDictionary(whereColumn, whereValue);
			return Get(columns, where);
		}
		public virtual StoreDictionary Get(string name, StoreList columns)
		{
			return Get(columns, nameColumn, name);
		}
		/* Get single (stop at first record) - 1 column */
		public virtual string Get(string column, StoreDictionary where = null)
		{
			List<string> records = new List<string>(GetMultiple(column, where));
			return records.Count > 0 ? records[0] : null;
		}
		public virtual string Get(string column, string whereColumn, string whereValue)
		{
			StoreDictionary where = new StoreDictionary(whereColumn, whereValue);
			return Get(column, where);
		}
		public virtual string Get(string name, string column)
		{
			return Get(column, nameColumn, name);
		}
		/* Get single (stop at first record) - 1 column - cast type shortcuts, with fallback */
		public virtual int GetAsInt(string name, string column, int fallback = 0)
		{
			int value = fallback;
			int.TryParse(Get(name, column), out value);
			return value;
		}
		public virtual long GetAsLong(string name, string column, long fallback = 0)
		{
			long value = fallback;
			long.TryParse(Get(name, column), out value);
			return value;
		}
		public virtual float GetAsFloat(string name, string column, float fallback = 0f)
		{
			float value = fallback;
			float.TryParse(Get(name, column), out value);
			return value;
		}
		/* Set */
		public virtual int Set(StoreDictionary values, StoreDictionary where)
		{
			return Store.Set(table, values, where);
		}
		public virtual int Set(string column, string value, string whereColumn, string whereValue)
		{
			StoreDictionary values = new StoreDictionary(column, value);
			StoreDictionary where = new StoreDictionary(whereColumn, whereValue);
			return Set(values, where);
		}
		public virtual int Set(string name, string column, string value)
		{
			return Set(column, value, nameColumn, name);
		}
		/* Remove */
		public virtual int Remove(StoreDictionary where)
		{
			return Store.Remove(table, where);
		}
		public virtual int Remove(string whereColumn, string whereValue)
		{
			StoreDictionary where = new StoreDictionary(whereColumn, whereValue);
			return Remove(where);
		}
		public virtual int Remove(string name)
		{
			return Remove(nameColumn, name);
		}
		protected virtual void OnCreate()
		{

		}
		protected virtual void AllocateColumns()
		{
			columns.Add(nameColumn);
		}
		private void CreateCollection()
		{
			if(Store.CreateCollection(table, columns))
			{
				OnCreate();
			}
		}
	}
}
