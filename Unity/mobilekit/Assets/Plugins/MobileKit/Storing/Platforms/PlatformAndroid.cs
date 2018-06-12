#if UNITY_ANDROID
using UnityEngine;
using System.Collections.Generic;

namespace MobileKit.Storing.Platforms
{
	public class PlatformAndroid : Platform
	{
		private AndroidJavaClass native = null;

		public PlatformAndroid()
		{
			this.native = new AndroidJavaClass("com.dasimple.mobilekit.store.Store");
		}

		public override void Connect()
		{
			native.CallStatic("connect");
		}

		public override void Disconnect()
		{
			native.CallStatic("disconnect");
		}

		public override void Open(int version)
		{
			native.CallStatic("open", version);
		}

		public override void Close()
		{
			native.CallStatic("close");
		}

		public override bool Reset()
		{
			return native.CallStatic<bool>("reset");
        }

		public override bool CreateCollection(string name, StoreList columns)
		{
			string columnsJSON = columns.ToJSON().ToString();
			return native.CallStatic<bool>("createCollection", name, columnsJSON);
		}

		public override bool DestroyCollection(string name)
		{
			return native.CallStatic<bool>("destroyCollection", name);
		}

		public override int Add(string collection, StoreDictionary[] records)
		{
			JSON recordsArray = new JSON(JSON.Type.Array);
			foreach(StoreDictionary record in records)
			{
				JSON recordObject = record.ToJSON();
				recordsArray.Push(recordObject);
			}
			string recordsJSON = recordsArray.ToString();
			return native.CallStatic<int>("add", collection, recordsJSON);
		}

		public override StoreDictionary[] Get(string collection, StoreList columns, StoreDictionary where, int limit)
		{
			string columnsJSON = columns.ToJSON().ToString();
			string whereJSON = where != null ? where.ToJSON().ToString() : "";
			string recordsJSON = native.CallStatic<string>("get", collection, columnsJSON, whereJSON, limit < 0 ? null : limit + "");
			JSON recordsJSON = JSON.Parse(recordsJSON);
			StoreDictionary[] records = new StoreDictionary[recordsJSON.length];
			for(int i = 0; i < records.Length; i++)
			{
				records[i] = new StoreDictionary(recordsJSON[i]);
			}
			return records;
		}

		public override string[] Get(string collection, string column, StoreDictionary where, int limit)
		{
			StoreList columns = new StoreList(column);
			string columnsJSON = columns.ToJSON().ToString();
			string whereJSON = where != null ? where.ToJSON().ToString() : "";
			string recordsJSON = native.CallStatic<string>("get", collection, columnsJSON, whereJSON, limit < 0 ? null : limit + "");
			JSON recordsJSON = JSON.Parse(recordsJSON);
			string[] records = new string[recordsJSON.length];
			for(int i = 0; i < records.Length; i++)
			{
				records[i] = recordsJSON[i];
			}
			return records;
		}

		public override int Set(string collection, StoreDictionary values, StoreDictionary where)
		{
			string valuesJSON = values.ToJSON().ToString();
			string whereJSON = where != null ? where.ToJSON().ToString() : "";
			return native.CallStatic<int>("set", collection, valuesJSON, whereJSON);
		}

		public override int Remove(string collection, StoreDictionary where)
		{
			string whereJSON = where != null ? where.ToJSON().ToString() : "";
			return native.CallStatic<int>("remove", collection, whereJSON);
		}
	}
}
#endif