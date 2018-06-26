using System.Collections.Generic;

namespace MobileKit.Storing
{
	public class StoreList : List<string>
	{
		public StoreList(params string[] entries)
		{
			AddRange(entries);
		}
		public StoreList(JSON json)
		{
			if(json != null)
			{
				for(int i = 0; i < json.length; i++)
				{
					Add(json[i].stringValue);
				}
			}
		}
		public JSON ToJSON()
		{
			JSON json = new JSON(JSON.Type.Array);
			foreach(string item in this)
			{
				json.Push(item);
			}
			return json;
		}
	}
}
