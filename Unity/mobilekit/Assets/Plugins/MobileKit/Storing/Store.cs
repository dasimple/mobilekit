using System.Collections.Generic;
using UnityEngine.Events;
using MobileKit.Storing.Platforms;

namespace MobileKit.Storing
{
	public static class Store
	{
		public static UnityAction OnConnect = delegate {};
		public static UnityAction OnDisconnect = delegate {};
		public static UnityAction OnDatabaseCreate = delegate {};
		public static UnityAction<int, int> OnDatabaseDowngrade = delegate {};
		public static UnityAction<int, int> OnDatabaseUpgrade = delegate {};
		public static UnityAction OnDatabaseOpen = delegate {};
		public static UnityAction OnDatabaseClose = delegate {};

		private static bool isInitialized = false;
		private static Platform cachedPlatform = null;

		public static Platform platform
		{
			get
			{
				if(!isInitialized)
				{
					StoreEvents.Create();
					isInitialized = true;
				}
				if(cachedPlatform == null)
				{
					#if UNITY_ANDROID && !UNITY_EDITOR
					cachedPlatform = new PlatformAndroid();
					#elif UNITY_IOS && !UNITY_EDITOR
					cachedPlatform = new PlatformiOS();
					#else
					cachedPlatform = new PlatformEditor();
					#endif
				}
				return cachedPlatform;
			}
		}

		public static void Connect()
		{
			platform.Connect();
		}

		public static void Disconnect()
		{
			platform.Disconnect();
		}

		public static void Open(int version)
		{
			platform.Open(version);
		}

		public static void Close()
		{
			platform.Close();
		}

		public static void Reset()
		{
			platform.Reset();
		}

		public static bool CreateCollection(string name, StoreList columns)
		{
			return platform.CreateCollection(name, columns);
		}

		public static bool DestroyCollection(string name)
		{
			return platform.DestroyCollection(name);
		}

		public static int Add(string collection, StoreDictionary[] records)
		{
			return platform.Add(collection, records);
		}

		public static StoreDictionary[] Get(string collection, StoreList columns, StoreDictionary where, int limit)
		{
			return platform.Get(collection, columns, where, limit);
		}

		public static string[] Get(string collection, string column, StoreDictionary where, int limit)
		{
			return platform.Get(collection, column, where, limit);
		}

		public static int Set(string collection, StoreDictionary columns, StoreDictionary where)
		{
			return platform.Set(collection, columns, where);
		}

		public static int Remove(string collection, StoreDictionary where)
		{
			return platform.Remove(collection, where);
		}
    }
}
