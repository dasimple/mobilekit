using System.Collections.Generic;

namespace MobileKit.Storing
{
	public class StoreDictionary : Dictionary<string, string>
	{
		public StoreDictionary(params string[] entries)
		{
			for(int i = 0; i < entries.Length - entries.Length % 2; i += 2)
			{
				Add(entries[i], entries[i + 1]);
			}
		}
		public StoreDictionary(JSON json)
		{
			if(json != null)
			{
				foreach(string key in json)
				{
					Add(key, json[key].stringValue);
				}
			}
		}
		public string Get(string key, string fallback = null)
		{
			if(ContainsKey(key))
			{
				return this[key];
			}
			return fallback;
		}
		public int GetAsInt(string key, int fallback = 0)
		{
			int value = fallback;
			int.TryParse(Get(key), out value);
			return value;
		}
		public long GetAsLong(string key, long fallback = 0)
		{
			long value = fallback;
			long.TryParse(Get(key), out value);
			return value;
		}
		public float GetAsFloat(string key, float fallback = 0f)
		{
			float value = fallback;
			float.TryParse(Get(key), out value);
			return value;
		}
		public JSON ToJSON()
		{
			JSON json = new JSON(JSON.Type.Object);
			foreach(KeyValuePair<string, string> entry in this)
			{
				json[entry.Key] = entry.Value;
			}
			return json;
		}
	}
}
