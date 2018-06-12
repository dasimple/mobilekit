using System;

namespace MobileKit.Storing.Collections
{
	public sealed class InsufficientBalanceException : Exception
	{
		public InsufficientBalanceException(string collection, int amount) : base("Insufficient balance to take " + amount + " from " + collection + ".")
		{

		}

		public InsufficientBalanceException(string collection, long amount) : base("Insufficient balance to take " + amount + " from " + collection + ".")
		{

		}
	}
}
