﻿using UnityEngine;
using UnityEngine.Events;

namespace MobileKit.Billings
{
	internal class BillingEvents : MonoBehaviour
	{
		private const string gameObjectName = "BillingEvents";
		private static BillingEvents instance = null;
		public static void Create()
		{
			if(instance != null)
			{
				return;
			}
			instance = GameObject.FindObjectOfType<BillingEvents>();
			if(!instance)
			{
				GameObject gameObject = GameObject.Find(gameObjectName);
				if(!gameObject)
				{
					gameObject = new GameObject(gameObjectName);
				}
				instance = gameObject.GetComponent<BillingEvents>();
				if(!instance)
				{
					instance = gameObject.AddComponent<BillingEvents>();
				}
			}
		}
		/* Events handling */
		public static void ConnectFail(string code)
		{
			Billing.OnConnectFail.Invoke(code);
		}
		public static void Connect()
		{
			Billing.OnConnect.Invoke();
		}
		public static void Disconnect()
		{
			Billing.OnDisconnect.Invoke();
		}
		public static void ProductsDetailsLoad(string response)
		{
			Billing.OnProductsDetailsLoad.Invoke(response);
		}
		public static void PurchasesLoad(string response)
		{
			Billing.OnPurchasesLoad.Invoke(response);
		}
		public static void PurchaseHistoryLoad(string response)
		{
			Billing.OnPurchaseHistoryLoad.Invoke(response);
		}
		public static void PurchaseStart()
		{
			Billing.OnPurchaseStart.Invoke();
		}
		public static void PurchaseFail(string code)
		{
			Billing.OnPurchaseFail.Invoke(code);
		}
		public static void PurchaseComplete(string payload)
		{
			Billing.OnPurchaseComplete.Invoke(payload);
		}
		/* Raw events from native (they have just one parameter, so we must redirect them to another methods) */
		private void EventConnectFail(string message)
		{
			ConnectFail(message);
		}
		private void EventConnect()
		{
			Connect();
		}
		private void EventDisconnect()
		{
			Disconnect();
		}
		private void EventProductsDetailsLoad(string message)
		{
			ProductsDetailsLoad(message);
		}
		private void EventPurchasesLoad(string message)
		{
			PurchasesLoad(message);
		}
		private void EventPurchaseHistoryLoad(string message)
		{
			PurchaseHistoryLoad(message);
		}
		private void EventPurchaseStart()
		{
			PurchaseStart();
		}
		private void EventPurchaseFail(string message)
		{
			PurchaseFail(message);
		}
		private void EventPurchaseComplete(string message)
		{
			PurchaseComplete(message);
		}
		/* Unity */
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
