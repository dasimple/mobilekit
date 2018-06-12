using UnityEngine;
using UnityEngine.Events;

namespace DaSimple.MobileKit.Storing
{
	internal class StoreEvents : MonoBehaviour
	{
		private const string gameObjectName = "StoreEvents";

		private static StoreEvents instance = null;

		public static void Create()
		{
			if(instance != null)
			{
				return;
			}
			instance = GameObject.FindObjectOfType<StoreEvents>();
			if(!instance)
			{
				GameObject gameObject = GameObject.Find(gameObjectName);
				if(!gameObject)
				{
					gameObject = new GameObject(gameObjectName);
				}
				instance = gameObject.GetComponent<StoreEvents>();
				if(!instance)
				{
					instance = gameObject.AddComponent<StoreEvents>();
				}
			}
		}

		/* Events handling */

		public static void Connect()
		{
			Store.OnConnect.Invoke();
		}

		public static void Disconnect()
		{
			Store.OnDisconnect.Invoke();
		}

		public static void DatabaseCreate()
		{
			Store.OnDatabaseCreate.Invoke();
		}

		public static void DatabaseDowngrade(int oldVersion, int newVersion)
		{
			Store.OnDatabaseDowngrade.Invoke(oldVersion, newVersion);
		}

		public static void DatabaseUpgrade(int oldVersion, int newVersion)
		{
			Store.OnDatabaseUpgrade.Invoke(oldVersion, newVersion);
		}

		public static void DatabaseOpen()
		{
			Store.OnDatabaseOpen.Invoke();
		}

		public static void DatabaseClose()
		{
			Store.OnDatabaseClose.Invoke();
		}

		/* Raw events from native (they have just one parameter, so we must redirect them to another methods) */

		private void EventConnect()
		{
			Connect();
		}

		private void EventDisconnect()
		{
			Disconnect();
		}

		private void EventDatabaseCreate()
		{
			DatabaseCreate();
		}

		private void EventDatabaseDowngrade(string message)
		{
			int point = message.IndexOf('>');
			if(point > 0)
			{
				int oldVersion = 0;
				int.TryParse(message.Substring(0, point), out oldVersion);
				int newVersion = 0;
				int.TryParse(message.Substring(point + 1), out newVersion);
				DatabaseDowngrade(oldVersion, newVersion);
			}
		}

		private void EventDatabaseUpgrade(string message)
		{
			int point = message.IndexOf('>');
			if(point > 0)
			{
				int oldVersion = 0;
				int.TryParse(message.Substring(0, point), out oldVersion);
				int newVersion = 0;
				int.TryParse(message.Substring(point + 1), out newVersion);
				DatabaseUpgrade(oldVersion, newVersion);
			}
		}

		private void EventDatabaseOpen()
		{
			DatabaseOpen();
		}

		private void EventDatabaseClose()
		{
			DatabaseClose();
		}

		/* Methods */

		private void Awake()
		{
			if(instance != null)
			{
				GameObject.Destroy(gameObject);
				return;
			}
			instance = this;
			gameObject.name = gameObjectName;
			GameObject.DontDestroyOnLoad(gameObject);
		}
	}
}
