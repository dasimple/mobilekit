#if UNITY_EDITOR
using UnityEngine;
using System.Collections.Generic;

namespace DaSimple.MobileKit.Storing.Platforms
{
	public class PlatformEditor : Platform
	{
		public override void Connect()
		{
			StoreEvents.Connect();
		}

		public override void Disconnect()
		{
			StoreEvents.Disconnect();
		}

		public override void Open(int version)
		{
			int currentVersion = 0;
			int.TryParse(Get("meta.version"), out currentVersion);
			if(currentVersion != version)
			{
				if(currentVersion == 0)
				{
					StoreEvents.DatabaseCreate();
				} else {
					if(currentVersion > version)
					{
						StoreEvents.DatabaseDowngrade(currentVersion, version);
					} else {
						StoreEvents.DatabaseUpgrade(currentVersion, version);
					}
				}
			}
			Set("meta.version", version + "");
			StoreEvents.DatabaseOpen();
		}

		public override void Close()
		{

		}

		public override bool Reset()
		{
			JSON collections = GetCollections();
			for(int i = 0; i < collections.length; i++)
			{
				string collection = collections[i].stringValue;
				DestroyCollection(collection);
			}
			Delete("collections");
			return true;
		}

		public override bool CreateCollection(string name, StoreList columns)
		{
			if(CollectionExists(name))
			{
				return false;
			}
			AddCollection(name);
			string columnsJSON = columns.ToJSON().ToString();
			Set("collections." + name, "[]");
			Set("meta.collections." + name, columnsJSON);
			return true;
		}

		public override bool DestroyCollection(string name)
		{
			if(!CollectionExists(name))
			{
				return false;
			}
			JSON records = GetRecords(name);
			for(int i = 0; i < records.length; i++)
			{
				string record = records[i].stringValue;
				RemoveRecord(name, record);
			}
			RemoveCollection(name);
			Delete("collections." + name);
			Delete("meta.collections." + name);
			return true;
		}

		public override int Add(string collection, StoreDictionary[] records)
		{
			int length = 0;
			foreach(StoreDictionary record in records)
			{
				string name = record["name"];
				if(string.IsNullOrEmpty(name) || RecordExists(collection, name))
				{
					continue;
				}
				AddRecord(collection, name);
				foreach(KeyValuePair<string, string> item in record)
				{
					string column = item.Key;
					string value = item.Value;
					Set("collections." + collection + "." + name + "." + column, value);
				}
				length++;
			}
			return length;
		}

		public override StoreDictionary[] Get(string collection, StoreList columns, StoreDictionary where, int limit)
		{
			List<StoreDictionary> records = new List<StoreDictionary>();
			JSON names = GetRecords(collection);
			for(int i = 0; i < names.length; i++)
			{
				if(limit == 0)
				{
					break;
				}
				string name = names[i].stringValue;
				if(IsRecordIncluded(collection, name, where))
				{
					StoreDictionary record = new StoreDictionary();
					foreach(string column in columns)
					{
						string value = Get("collections." + collection + "." + name + "." + column);
						record.Add(column, value);
					}
					records.Add(record);
					limit--;
				}
			}
			return records.ToArray();
		}

		public override string[] Get(string collection, string column, StoreDictionary where, int limit)
		{
			StoreList records = new StoreList();
			JSON names = GetRecords(collection);
			for(int i = 0; i < names.length; i++)
			{
				if(limit == 0)
				{
					break;
				}
				string name = names[i].stringValue;
				if(IsRecordIncluded(collection, name, where))
				{
					string value = Get("collections." + collection + "." + name + "." + column);
					records.Add(value);
					limit--;
				}
			}
			return records.ToArray();
		}

		public override int Set(string collection, StoreDictionary values, StoreDictionary where)
		{
			int affected = 0;
			JSON names = GetRecords(collection);
			for(int i = 0; i < names.length; i++)
			{
				string name = names[i].stringValue;
				if(IsRecordIncluded(collection, name, where))
				{
					foreach(KeyValuePair<string, string> entry in values)
					{
						string column = entry.Key;
						string value = entry.Value;
						Set("collections." + collection + "." + name + "." + column, value);
					}
					affected++;
				}
			}
			return affected;
		}

		public override int Remove(string collection, StoreDictionary where)
		{
			int affected = 0;
			StoreList columns = new StoreList(JSON.Parse(Get("meta.collections." + collection)));
			JSON names = GetRecords(collection);
			for(int i = 0; i < names.length; i++)
			{
				string name = names[i].stringValue;
				if(IsRecordIncluded(collection, name, where))
				{
					foreach(string column in columns)
					{
						Delete("collections." + collection + "." + name + "." + column);
					}
					RemoveRecord(collection, name);
					affected++;
				}
			}
			return affected;
		}

		private bool IsRecordIncluded(string collection, string name, StoreDictionary where)
		{
			if(where != null)
			{
				foreach(KeyValuePair<string, string> entry in where)
				{
					string column = entry.Key;
					string value = entry.Value;
					if(Get("collections." + collection + "." + name + "." + column) != value)
					{
						return false;
					}
				}
			}
			return true;
		}

		private bool Has(string key)
		{
			return PlayerPrefs.HasKey("store." + key);
		}

		private void Set(string key, string value)
		{
			PlayerPrefs.SetString("store." + key, value);
		}

		private string Get(string key, string defaultValue = "")
		{
			return PlayerPrefs.GetString("store." + key, defaultValue);
		}

		private void Delete(string key)
		{
			PlayerPrefs.DeleteKey("store." + key);
		}

		private JSON GetCollections()
		{
			string collections = Get("collections", "[]");
			return JSON.Parse(collections);
		}

		private bool CollectionExists(string name)
		{
			JSON collections = GetCollections();
			for(int i = 0; i < collections.length; i++)
			{
				string collection = collections[i].stringValue;
				if(collection == name)
				{
					return true;
				}
			}
			return false;
		}

		private void SetCollections(JSON collections)
		{
			Set("collections", collections + "");
		}

		private void AddCollection(string name)
		{
			JSON collections = GetCollections();
			collections.Push(name);
			SetCollections(collections);
		}

		private void RemoveCollection(string name)
		{
			JSON collections = GetCollections();
			for(int i = 0; i < collections.length; i++)
			{
				string collection = collections[i].stringValue;
				if(collection == name)
				{
					collections.Splice(i, 1);
					break;
				}
			}
			SetCollections(collections);
		}

		private JSON GetRecords(string collection)
		{
			string records = Get("collections." + collection, "[]");
			return JSON.Parse(records);
		}

		private bool RecordExists(string collection, string name)
		{
			JSON records = GetRecords(collection);
			for(int i = 0; i < records.length; i++)
			{
				string record = records[i].stringValue;
				if(record == name)
				{
					return true;
				}
			}
			return false;
		}

		private void SetRecords(string collection, JSON records)
		{
			Set("collections." + collection, records + "");
		}

		private void AddRecord(string collection, string name)
		{
			JSON records = GetRecords(collection);
			records.Push(name);
			SetRecords(collection, records);
		}

		private void RemoveRecord(string collection, string name)
		{
			JSON records = GetRecords(collection);
			for(int i = 0; i < records.length; i++)
			{
				string record = records[i].stringValue;
				if(record == name)
				{
					records.Splice(i, 1);
					break;
				}
			}
			SetRecords(collection, records);
		}
	}
}
#endif
