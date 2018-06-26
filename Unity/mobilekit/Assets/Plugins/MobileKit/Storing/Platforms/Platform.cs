using System;
using System.Collections.Generic;

namespace MobileKit.Storing.Platforms
{
	public abstract class Platform
	{
		private static bool singleton = false;
		protected Platform()
		{
			if(singleton)
			{
				throw new Exception("The storing platform is a singleton.");
			}
			singleton = true;
		}
		public abstract void Connect();
		public abstract void Disconnect();
		public abstract void Open(int version);
		public abstract void Close();
		public abstract bool Reset();
		public abstract bool CreateCollection(string name, StoreList columns);
		public abstract bool DestroyCollection(string name);
		public abstract int Add(string collection, IEnumerable<StoreDictionary> records);
		public abstract IEnumerable<StoreDictionary> Get(string collection, StoreList columns, StoreDictionary where, int limit);
		public abstract IEnumerable<string> Get(string collection, string column, StoreDictionary where, int limit);
		public abstract int Set(string collection, StoreDictionary values, StoreDictionary where);
		public abstract int Remove(string collection, StoreDictionary where);
	}
}
