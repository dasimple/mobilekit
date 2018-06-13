using System.Collections.Generic;

namespace MobileKit.Storing.Collections
{
	public sealed class KeyValueCollection : VirtualCollection
	{
		public KeyValueCollection() : base("keys")
		{

		}

		public string Get(string name)
		{
			return Get(name, "value");
		}

		public bool Set(string name, string value)
		{
			if(Set(name, "value", value) == 0)
			{
				StoreDictionary values = Bake(name);
				values.Add("value", value);
				return Add(values) == 1;
			}
			return true;
		}

		protected override void AllocateColumns()
		{
			base.AllocateColumns();
			columns.Add("value");
		}
	}
}
