using MobileKit.Storing.Collections;

namespace MobileKit.Storing
{
	public static class KeyValues
	{
		private static KeyValueCollection singleton = null;

		private static KeyValueCollection collection
		{
			get
			{
				if(singleton == null)
				{
					singleton = new KeyValueCollection();
				}
				return singleton;
			}
		}

		/* Get */

		public static string Get(string name, string fallback = null)
		{
			string value = collection.Get(name);
			if(value == null)
			{
				value = fallback;
			}
			return value;
		}

		public static int GetAsInt(string name, int fallback = 0)
		{
			int value = fallback;
			int.TryParse(Get(name), out value);
			return value;
		}

		public static long GetAsLong(string name, long fallback = 0)
		{
			long value = fallback;
			long.TryParse(Get(name), out value);
			return value;
		}

		public static float GetAsFloat(string name, float fallback = 0f)
		{
			float value = fallback;
			float.TryParse(Get(name), out value);
			return value;
		}

		public static bool GetAsBool(string name, bool fallback = false)
		{
			string value = Get(name);
			if(value == null)
			{
				return fallback;
			}
			return value == "1";
		}

		/* Set */

		public static bool Set(string name, string value)
		{
			return collection.Set(name, value);
		}

		public static bool Set(string name, int value)
		{
			return Set(name, value + "");
		}

		public static bool Set(string name, long value)
		{
			return Set(name, value + "");
		}

		public static bool Set(string name, float value)
		{
			return Set(name, value + "");
		}

		public static bool Set(string name, bool value)
		{
			return Set(name, value ? "1" : "0");
		}
	}
}
