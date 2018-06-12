#if UNITY_IOS
using System.Runtime.InteropServices;

namespace DaSimple.MobileKit.Storing.Platforms
{
	public class PlatformiOS : Platform
	{
		[DllImport("__Internal")]
		private static extern void iosStore_connect();
		[DllImport("__Internal")]
		private static extern void iosStore_disconnect();
		[DllImport("__Internal")]
		private static extern void iosStore_open(int version);
		[DllImport("__Internal")]
		private static extern void iosStore_close();
		[DllImport("__Internal")]
		private static extern bool iosStore_reset();
		[DllImport("__Internal")]
		private static extern bool iosStore_createCollection(string name, string columns);
		[DllImport("__Internal")]
		private static extern bool iosStore_destroyCollection(string name);
		[DllImport("__Internal")]
		private static extern int iosStore_add(string collection, string records);
		[DllImport("__Internal")]
		private static extern System.IntPtr iosStore_get(string collection, string columns, string where, string limit);
		[DllImport("__Internal")]
		private static extern int iosStore_set(string collection, string values, string where);
		[DllImport("__Internal")]
		private static extern int iosStore_remove(string collection, string where);

		public override void Connect()
		{
			iosStore_connect();
		}

		public override void Disconnect()
		{
			iosStore_disconnect();
		}

		public override void Open(int version)
		{
			iosStore_open(version);
		}

		public override void Close()
		{
			iosStore_close();
		}

		public override bool Reset()
		{
			return iosStore_reset();
		}

		public override bool CreateCollection(string name, StoreList columns)
		{
			string columnsJSON = columns.ToJSON().ToString();
			return iosStore_createCollection(name, columnsJSON);
		}

		public override bool DestroyCollection(string name)
		{
			return iosStore_destroyCollection(name);
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
			return iosStore_add(collection, recordsJSON);
		}

		public override StoreDictionary[] Get(string collection, StoreList columns, StoreDictionary where, int limit)
		{
			string columnsJSON = columns.ToJSON().ToString();
			string whereJSON = where != null ? where.ToJSON().ToString() : "";
			System.IntPtr pointer = iosStore_get(collection, columnsJSON, whereJSON, limit < 0 ? "" : limit + "");
			string recordsJSON = Marshal.PtrToStringAnsi(pointer);
			Marshal.FreeHGlobal(pointer);
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
			System.IntPtr pointer = iosStore_get(collection, columnsJSON, whereJSON, limit < 0 ? "" : limit + "");
			string recordsJSON = Marshal.PtrToStringAnsi(pointer);
			Marshal.FreeHGlobal(pointer);
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
			return iosStore_set(collection, valuesJSON, whereJSON);
		}

		public override int Remove(string collection, StoreDictionary where)
		{
			string whereJSON = where != null ? where.ToJSON().ToString() : "";
			return iosStore_remove(collection, whereJSON);
		}
	}
}
#endif
