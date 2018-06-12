using System.Collections.Generic;

namespace DaSimple.MobileKit.Storing.Collections
{
	public abstract class Lifetimes : Countables32
	{
		public Lifetimes(string table) : base(table)
		{

		}

		public override bool SetBalance(string name, int value)
		{
			if(value > 1)
			{
				value = 1;
			}
			return base.SetBalance(name, value);
		}

		public virtual bool IsOwned(string name)
		{
			return GetBalance(name) > 0;
		}
	}
}
