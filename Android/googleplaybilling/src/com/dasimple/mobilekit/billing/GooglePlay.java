package com.dasimple.mobilekit.billing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.android.vending.billing.IInAppBillingService;
import com.dasimple.mobilekit.bridge.Bridge;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.IntentSender.SendIntentException;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;

public final class GooglePlay
{
	//Activity request code
    public static final int REQUEST_CODE = 327849; //Digits on phone keyboard (earthx -> 327849)
    
	//Billing response codes
	public static final int BILLING_RESPONSE_RESULT_OK = 0;
	public static final int BILLING_RESPONSE_RESULT_USER_CANCELED = 1;
	public static final int BILLING_RESPONSE_RESULT_SERVICE_UNAVAILABLE = 2;
	public static final int BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE = 3;
	public static final int BILLING_RESPONSE_RESULT_ITEM_UNAVAILABLE = 4;
	public static final int BILLING_RESPONSE_RESULT_DEVELOPER_ERROR = 5;
	public static final int BILLING_RESPONSE_RESULT_ERROR = 6;
	public static final int BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED = 7;
	public static final int BILLING_RESPONSE_RESULT_ITEM_NOT_OWNED = 8;
	
	//Keys for the responses from InAppBillingService
	public static final String RESPONSE_CODE = "RESPONSE_CODE";
	public static final String RESPONSE_GET_SKU_DETAILS_LIST = "DETAILS_LIST";
	public static final String RESPONSE_BUY_INTENT = "BUY_INTENT";
	public static final String RESPONSE_INAPP_PURCHASE_DATA = "INAPP_PURCHASE_DATA";
	public static final String RESPONSE_INAPP_SIGNATURE = "INAPP_DATA_SIGNATURE";
	public static final String RESPONSE_INAPP_ITEM_LIST = "INAPP_PURCHASE_ITEM_LIST";
	public static final String RESPONSE_INAPP_PURCHASE_DATA_LIST = "INAPP_PURCHASE_DATA_LIST";
	public static final String RESPONSE_INAPP_SIGNATURE_LIST = "INAPP_DATA_SIGNATURE_LIST";
	public static final String INAPP_CONTINUATION_TOKEN = "INAPP_CONTINUATION_TOKEN";
	
	//Item types
	public static final String ITEM_TYPE_INAPP = "inapp";
	public static final String ITEM_TYPE_SUBS = "subs";
	
	//Some fields on the getSkuDetails response bundle
	public static final String GET_SKU_DETAILS_ITEM_LIST = "ITEM_ID_LIST";
	public static final String GET_SKU_DETAILS_ITEM_TYPE_LIST = "ITEM_TYPE_LIST";
	
	//Connect fail codes
	public static final String CONNECT_NOT_SUPPORTED = "CONNECT_NOT_SUPPORTED";
	public static final String CONNECT_UNKNOWN = "CONNECT_UNKNOWN";
	
	//Purchase fail codes
	public static final String PURCHASE_NOT_SUPPORTED = "PURCHASE_NOT_SUPPORTED";
	public static final String PURCHASE_CANCELED = "PURCHASE_CANCELED";
	public static final String PURCHASE_ITEM_ALREADY_OWNED = "PURCHASE_ITEM_ALREADY_OWNED";
	public static final String PURCHASE_ITEM_UNAVAILABLE = "PURCHASE_ITEM_UNAVAILABLE";
	public static final String PURCHASE_INVALID = "PURCHASE_INVALID";
	public static final String PURCHASE_UNKNOWN = "PURCHASE_UNKNOWN";

	private static final String BASE64_PUBLIC_KEY = ""; //Set by default, for more secure...
	private static final boolean ASYNC = true;

	private static boolean connected = false;
	private static ServiceConnection serviceConnection = null;
	private static Context context = null;
	private static IInAppBillingService service = null;
	private static boolean subscriptionsSupported = false;
	private static boolean subscriptionUpdateSupported = false;
	private static Map<String, String> cachedItemTypes = null;
	private static Map<String, String> cachedPayloads = null;
	private static Map<String, Boolean> cachedConsumables = null;

	public static void connect()
	{
		if(connected)
		{
			Bridge.log("IAB already setted up!");
			return;
		}
		serviceConnection = new ServiceConnection() {
			@Override
			public void onServiceDisconnected(ComponentName name)
			{
				boolean wasConnected = connected;
				service = null;
				connected = false;
				Bridge.log("Billing service disconnected.");
				if(wasConnected)
				{
					sendDisconnect();
					return;
				}
				sendConnectFail(CONNECT_NOT_SUPPORTED);
			}
			@Override
			public void onServiceConnected(ComponentName name, IBinder binder)
			{
				Bridge.log("Billing service connected.");
				service = IInAppBillingService.Stub.asInterface(binder);
                String packageName = Bridge.getPackageName();
                int response = 0;
				try
				{
					Bridge.log("Checking for in-app billing 3 support.");
					//Check for in-app billing v3 support.
					response = service.isBillingSupported(3, packageName, ITEM_TYPE_INAPP);
					Bridge.log("In-app billing version 3 responsed " + response);
					if(response != BILLING_RESPONSE_RESULT_OK)
					{
						//If in-app purchases aren't supported, neither are subscriptions.
                        subscriptionsSupported = false;
                        subscriptionUpdateSupported = false;
                        sendConnectFail(CONNECT_NOT_SUPPORTED);
						return;
					}
					Bridge.log("Checking for in-app billing 5 support for subscriptions.");
					//Check for v5 subscriptions support. This is needed for getBuyIntentToReplaceSku which allows for subscription update.
					response = service.isBillingSupported(5, packageName, ITEM_TYPE_SUBS);
					Bridge.log("In-app billing version 5 for subscriptions responsed " + response);
                    subscriptionUpdateSupported = response == BILLING_RESPONSE_RESULT_OK;
                    Bridge.log("Checking for in-app billing 3 support for subscriptions.");
                    //Check for v3 subscriptions support.
                    response = service.isBillingSupported(3, packageName, ITEM_TYPE_SUBS);
					Bridge.log("In-app billing version 3 for subscriptions responsed " + response);
                    subscriptionsSupported = subscriptionUpdateSupported ? true : response == BILLING_RESPONSE_RESULT_OK;
					connected = true;
					cachedItemTypes = new HashMap<String, String>();
					cachedPayloads = new HashMap<String, String>();
					cachedConsumables = new HashMap<String, Boolean>();
					Bridge.log("Connecting succeded.");
					sendConnect();
				}
				catch (RemoteException e)
				{
					Bridge.log("RemoteException happened :(");
					sendConnectFail(CONNECT_UNKNOWN);
				}
			}
		};
		context = Bridge.getApplicationContext();
		Intent serviceIntent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
		serviceIntent.setPackage("com.android.vending");
		List<ResolveInfo> intentServices = context.getPackageManager().queryIntentServices(serviceIntent, 0);
		Bridge.log("Trying to bind IAB service.");
		if(intentServices != null && !intentServices.isEmpty())
		{
			context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
			return;
		}
		Bridge.log("Binding service failed.");
		sendConnectFail(CONNECT_NOT_SUPPORTED);
	}

	public static void disconnect()
	{
		if(service != null)
		{
			if(serviceConnection != null)
			{
				context.unbindService(serviceConnection);
				serviceConnection = null;
			}
			service = null;
	    }
		connected = false;
		context = null;
	}

	public static void refresh()
	{
		//Nothing to do...
	}

	public static boolean canMakePayments()
	{
		if(!connected)
		{
			Bridge.log("Seems that service is not connected yet.");
			return false;
		}
		return subscriptionsSupported || true;
	}

	public static void queryProductsDetails(String requestJSON)
	{
		if(!connected)
		{
			Bridge.log("Seems that service is not connected yet.");
			return;
		}
		try
		{
			final JSONObject request = new JSONObject(requestJSON);
			if(ASYNC)
			{
				Thread thread = new Thread(new Runnable() {
		            public void run()
		            {
		            	queryProductsDetails(request);
		            }
		        });
				thread.start();
			}
			queryProductsDetails(request);
		}
		catch (JSONException e)
		{
			Bridge.log("Cannot parse data in request. Request - " + requestJSON);
		}
	}

	public static void queryPurchases(String requestJSON)
	{
		if(!connected)
		{
			Bridge.log("Seems that service is not connected yet.");
			return;
		}
		try
		{
			final JSONObject request = new JSONObject(requestJSON);
			if(ASYNC)
			{
				Thread thread = new Thread(new Runnable() {
		            public void run()
		            {
		            	queryPurchases(request);
		            }
		        });
				thread.start();
			}
			queryPurchases(request);
		}
		catch (JSONException e)
		{
			Bridge.log("Cannot parse data in request. Request - " + requestJSON);
		}
	}

	public static void queryPurchaseHistory(String requestJSON)
	{
		if(!connected)
		{
			Bridge.log("Seems that service is not connected yet.");
			return;
		}
		try
		{
			final JSONObject request = new JSONObject(requestJSON);
			if(ASYNC)
			{
				Thread thread = new Thread(new Runnable() {
		            public void run()
		            {
		            	queryPurchaseHistory(request);
		            }
		        });
				thread.start();
			}
			queryPurchaseHistory(request);
		}
		catch (JSONException e)
		{
			Bridge.log("Cannot parse data in request. Request - " + requestJSON);
		}
	}

	public static void purchase(String requestJSON, String payload)
	{
		if(!canMakePayments())
		{
			sendPurchaseFail(PURCHASE_NOT_SUPPORTED);
			return;
		}
		Bridge.log("Purchasing...");
		try
		{
			JSONObject request = new JSONObject(requestJSON);
			String itemType = request.has("itemType") ? request.getString("itemType") : ITEM_TYPE_INAPP;
			String sku = request.getString("sku");
			boolean consumable = request.has("consumable") ? request.getBoolean("consumable") : true;
			Activity activity = Bridge.getCurrentActivity();
			Intent intent = new Intent(activity, GooglePlayPurchaseActivity.class);
            intent.putExtra("itemType", itemType);
			intent.putExtra("sku", sku);
            intent.putExtra("consumable", consumable);
            intent.putExtra("payload", payload);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			Bridge.log("Purchase started.");
			sendPurchaseStart();
			Bridge.log("Start a new activity for purchasing.");
			activity.startActivity(intent);
		}
		catch (JSONException e)
		{
			Bridge.log("Cannot find required data in request. SKU or/and item type not found. Request - " + requestJSON);
		}
	}

	public static void createBuyIntent(GooglePlayPurchaseActivity activity)
	{
		if(Bridge.getCurrentActivity() == null)
		{
			return;
		}
		Bridge.log("Create buy intent.");
		Intent intent = activity.getIntent();
		String itemType = intent.getStringExtra("itemType");
		if(!subscriptionsSupported && itemType.equals(ITEM_TYPE_SUBS))
		{
			Bridge.log("Subscriptions not supported.");
			sendPurchaseFail(PURCHASE_NOT_SUPPORTED);
            activity.finish();
            return;
        }
		String sku = intent.getStringExtra("sku");
		boolean consumable = intent.getBooleanExtra("consumable", true);
		String payload = intent.getStringExtra("payload");
        String packageName = Bridge.getPackageName();
        cachedItemTypes.put(sku, itemType);
        cachedPayloads.put(sku, payload);
        cachedConsumables.put(sku, consumable);
		try
		{
			String oldSkus = null;
			if(intent.hasExtra("oldSkus"))
			{
				oldSkus = intent.getStringExtra("oldSkus");
			}
			Bundle buyIntentBundle = null;
            if(TextUtils.isEmpty(oldSkus))
            {
                //Purchasing a new item or subscription re-signup.
                buyIntentBundle = service.getBuyIntent(3, packageName, sku, itemType, payload);
            } else {
                //Subscription upgrade/downgrade
                if(subscriptionUpdateSupported)
                {
                	List<String> oldSkusList = new ArrayList<String>(Arrays.asList(oldSkus.split(",")));
                    buyIntentBundle = service.getBuyIntentToReplaceSkus(5, packageName, oldSkusList, sku, itemType, payload);
                }
            }
            int response = getResponseCodeFromBundle(buyIntentBundle);
            if(response != BILLING_RESPONSE_RESULT_OK)
            {
    			Bridge.log("getBuyIntent response - " + response + ".");
    			sendPurchaseFail(getPurchaseFailCode(response));
                activity.finish();
                return;
            }
            PendingIntent pendingIntent = buyIntentBundle.getParcelable(RESPONSE_BUY_INTENT);
			Bridge.log("Activity startIntentSenderForResult.");
            activity.startIntentSenderForResult(pendingIntent.getIntentSender(), REQUEST_CODE, new Intent(), Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0));
            return;
		}
		catch (SendIntentException e)
		{
			Bridge.log("SendIntentException exception.");
			e.printStackTrace();
			sendPurchaseFail(PURCHASE_UNKNOWN);
			activity.finish();
		}
		catch (RemoteException e)
		{
			Bridge.log("RemoteException exception.");
			e.printStackTrace();
			sendPurchaseFail(PURCHASE_UNKNOWN);
			activity.finish();
		}
	}

	public static void handleActivityResult(int resultCode, Intent data)
	{
		Bridge.log("Handle activity result. resultCode - " + resultCode + ", intent data: " + data);
		//Cancellation moved here b/c there can be a cancellation with null data.
        if(resultCode == Activity.RESULT_CANCELED)
        {
    		Bridge.log("Activity cancelled.");
    		sendPurchaseFail(PURCHASE_CANCELED);
            return;
        }
		if(data == null)
		{
    		Bridge.log("Data is null.");
    		sendPurchaseFail(PURCHASE_UNKNOWN);
			return;
		}
		Bundle purchaseBundle = data.getExtras();
		int response = getResponseCodeFromBundle(purchaseBundle);
		String purchaseData = data.getStringExtra(RESPONSE_INAPP_PURCHASE_DATA);
        String signature = data.getStringExtra(RESPONSE_INAPP_SIGNATURE);
    	if(TextUtils.isEmpty(purchaseData) || TextUtils.isEmpty(signature))
    	{
    		Bridge.log("purchaseData and/or signature are null.");
    		sendPurchaseFail(PURCHASE_UNKNOWN);
    		return;
    	}
    	if(resultCode == Activity.RESULT_OK)
    	{
    		if(response == BILLING_RESPONSE_RESULT_OK)
    		{
            	try
            	{
    				if(!GooglePlaySecurity.verifyPurchase(BASE64_PUBLIC_KEY, purchaseData, signature))
    				{
    		    		Bridge.log("Purchase is NOT valid.");
    		    		sendPurchaseFail(PURCHASE_INVALID);
    					return;
    				}
		    		Bridge.log("Purchase is valid.");
    				final GooglePlayPurchase purchase = new GooglePlayPurchase(purchaseData, signature);
    				String sku = purchase.sku;
    				purchase.itemType = cachedItemTypes.get(sku);
    				purchase.payload = TextUtils.isEmpty(purchase.payload) ? cachedPayloads.get(sku) : purchase.payload;
		    		boolean consumable = cachedConsumables.get(sku);
		    		if(!consumable)
		    		{
		    			sendPurchaseComplete(purchase.payload);
	    				return;
		    		}
		    		if(ASYNC)
		    		{
		    			consumeAsync(purchase);
		    			return;
		    		}
		    		consume(purchase);
    			}
            	catch (JSONException e)
            	{
		    		Bridge.log("Cannot parse purchase data.");
		    		sendPurchaseFail(PURCHASE_INVALID);
    			}
    		} else {
        		Bridge.log("Response code is not as expected - " + response + ".");
        		sendPurchaseFail(getPurchaseFailCode(response));
    		}
    	} else {
    		Bridge.log("Activity result code is not as expected - " + resultCode + ".");
    		sendPurchaseFail(PURCHASE_UNKNOWN);
    	}
	}

	public static boolean consume(String token)
	{
		try
		{
			String packageName = Bridge.getPackageName();
			int response = service.consumePurchase(3, packageName, token);
			if(response == BILLING_RESPONSE_RESULT_OK)
			{
				Bridge.log("Successfully consumed token: " + token);
				return true;
			}
			Bridge.log("Error consuming token " + token + ". Response - " + response);
		}
		catch (RemoteException e)
		{
			Bridge.log("Remote exception while consuming. token: " + token);
		}
		return false;
	}
	
	/* Methods */
	
	private static void consume(GooglePlayPurchase purchase)
	{
		if(!purchase.itemType.equals(ITEM_TYPE_INAPP))
		{
			Bridge.log("Items of type '" + ITEM_TYPE_INAPP + "' can't be consumed.");
		}
		String sku = purchase.sku;
		String token = purchase.token;
		if(TextUtils.isEmpty(token))
		{
			Bridge.log("Can't consume " + sku + ". No token provided.");
			return;
		}
		Bridge.log("Consuming sku: " + sku + ", token: " + token);
		boolean consumed = consume(token);
		if(consumed)
		{
			sendPurchaseComplete(purchase.payload);
		}
	}
	
	private static void consumeAsync(final List<GooglePlayPurchase> purchases)
	{
		Thread thread = new Thread(new Runnable() {
            public void run()
            {
                for(GooglePlayPurchase purchase : purchases)
                {
                	consume(purchase);
                }
            }
        });
		thread.start();
	}
	
	private static void consumeAsync(GooglePlayPurchase purchase)
	{
		List<GooglePlayPurchase> purchases = new ArrayList<GooglePlayPurchase>();
		purchases.add(purchase);
		consumeAsync(purchases);
	}
	
	private static void queryProductsDetails(JSONObject request)
	{
		JSONArray skuDetailsArray = new JSONArray();
		queryProductsDetails(request, ITEM_TYPE_INAPP, skuDetailsArray);
		queryProductsDetails(request, ITEM_TYPE_SUBS, skuDetailsArray);
		String productsDetails = skuDetailsArray.toString();
		sendProductsDetailsLoad(productsDetails);
	}
	
	private static void queryProductsDetails(JSONObject request, String itemType, JSONArray skuDetailsArray)
	{
        if(!request.has(itemType))
        {
        	return;
        }
		ArrayList<String> skusList = new ArrayList<String>();
		JSONArray skusArray = request.optJSONArray(itemType);
        String packageName = Bridge.getPackageName();
		for(int i = 0; i < skusArray.length(); i++)
		{
			String sku = skusArray.optString(i);
			skusList.add(sku);
		}
		if(skusList.size() == 0)
		{
			return;
		}
		Bundle skusBundle = new Bundle();
		skusBundle.putStringArrayList(GET_SKU_DETAILS_ITEM_LIST, skusList);
		try
		{
			Bundle skuDetailsBundle = service.getSkuDetails(3, packageName, itemType, skusBundle);
			int response = getResponseCodeFromBundle(skuDetailsBundle);
			if(response == BILLING_RESPONSE_RESULT_OK)
			{
				if(skuDetailsBundle.containsKey(RESPONSE_GET_SKU_DETAILS_LIST))
				{
					ArrayList<String> responseList = skuDetailsBundle.getStringArrayList(RESPONSE_GET_SKU_DETAILS_LIST);
					Bridge.log("Got sku details response list with size: " + responseList.size());
					for(String responseItem : responseList)
					{
						try
						{
							JSONObject skuDetails = new JSONObject(responseItem);
				        	Bridge.log("Got sku details: " + skuDetails);
							skuDetailsArray.put(skuDetails);
						}
						catch (JSONException e)
						{
							Bridge.log("Response item - " + responseItem);
							e.printStackTrace();
						}
			        }
				} else {
	            	Bridge.log("getSkuDetails() returned a bundle with neither an error nor a detail list.");
	            }
			} else {
            	Bridge.log("getSkuDetails() failed with response: " + response);
            }
		}
		catch (RemoteException e)
		{
			Bridge.log("RemoteException on getSkuDetails(). See stack trace.");
			e.printStackTrace();
		}
	}
	
	private static void queryPurchases(JSONObject request)
	{
		JSONArray purchasesArray = null;
        String packageName = Bridge.getPackageName();
		try
		{
			String itemType = request.getString("itemType");
			String continueToken = null;
			do
			{
				Bundle ownedItems = service.getPurchases(3, packageName, itemType, continueToken);
				int response = getResponseCodeFromBundle(ownedItems);
		        Bridge.log("Owned items response: " + response);
		        if(response == BILLING_RESPONSE_RESULT_OK)
		        {
		        	if(ownedItems.containsKey(RESPONSE_INAPP_ITEM_LIST) && ownedItems.containsKey(RESPONSE_INAPP_PURCHASE_DATA_LIST) && ownedItems.containsKey(RESPONSE_INAPP_SIGNATURE_LIST))
		        	{
		        		if(purchasesArray == null)
		        		{
		        			purchasesArray = new JSONArray();
		        		}
		        		ArrayList<String> ownedSkus = ownedItems.getStringArrayList(RESPONSE_INAPP_ITEM_LIST);
		                ArrayList<String> purchaseDataList = ownedItems.getStringArrayList(RESPONSE_INAPP_PURCHASE_DATA_LIST);
		                ArrayList<String> signatureList = ownedItems.getStringArrayList(RESPONSE_INAPP_SIGNATURE_LIST);
                    	Bridge.log("Got purchaseDataList with size: " + purchaseDataList.size());
		                for(int i = 0; i < purchaseDataList.size(); i++)
		                {
		                    String purchaseData = purchaseDataList.get(i);
		                    String signature = signatureList.get(i);
		                    String sku = ownedSkus.get(i);
		                    if(GooglePlaySecurity.verifyPurchase(BASE64_PUBLIC_KEY, purchaseData, signature))
		                    {
		                    	Bridge.log("Sku is owned: " + sku);
		        				JSONObject purchase = new JSONObject(purchaseData);
		                        if(TextUtils.isEmpty(purchase.optString("token", purchase.optString("purchaseToken"))))
		                        {
		                        	Bridge.log("BUG: empty/null token! Purchase data: " + purchaseData);
		                        }
		                        purchasesArray.put(purchase);
		                    } else {
		                    	Bridge.log("Purchase signature verification **FAILED**. Purchase data: " + purchaseData + ". Signature: " + signature);
		                    }
		                }
		                continueToken = ownedItems.getString(INAPP_CONTINUATION_TOKEN);
		                Bridge.log("Continuation token: " + continueToken);
		            } else {
		            	Bridge.log("Bundle returned from getPurchases() doesn't contain required fields.");
		            }
		        } else {
		        	Bridge.log("getPurchases() failed with response: " + response);
		        }
			} while (!TextUtils.isEmpty(continueToken));
		}
		catch (JSONException e)
		{
			Bridge.log("Cannot find required data in request. Item type not found. Request - " + request);
			e.printStackTrace();
		}
		catch (RemoteException e)
		{
			Bridge.log("RemoteException on getSkuDetails(). See stack trace.");
			e.printStackTrace();
		}
		if(purchasesArray != null)
		{
			String purchases = purchasesArray.toString();
			sendPurchasesLoad(purchases);
		}
	}
	
	private static void queryPurchaseHistory(JSONObject request)
	{
		JSONArray history = null;
        String packageName = Bridge.getPackageName();
		try
		{
			String itemType = request.getString("itemType");
			String continueToken = null;
			Bundle extraParams = null;
			do
			{
				Bundle ownedItems = service.getPurchaseHistory(6, packageName, itemType, continueToken, extraParams);
				int response = getResponseCodeFromBundle(ownedItems);
		        Bridge.log("Purchase history response: " + response);
		        if(response == BILLING_RESPONSE_RESULT_OK)
		        {
		        	if(ownedItems.containsKey(RESPONSE_INAPP_ITEM_LIST) && ownedItems.containsKey(RESPONSE_INAPP_PURCHASE_DATA_LIST) && ownedItems.containsKey(RESPONSE_INAPP_SIGNATURE_LIST))
		        	{
		        		if(history == null)
		        		{
		        			history = new JSONArray();
		        		}
		        		ArrayList<String> ownedSkus = ownedItems.getStringArrayList(RESPONSE_INAPP_ITEM_LIST);
		                ArrayList<String> purchaseDataList = ownedItems.getStringArrayList(RESPONSE_INAPP_PURCHASE_DATA_LIST);
		                ArrayList<String> signatureList = ownedItems.getStringArrayList(RESPONSE_INAPP_SIGNATURE_LIST);
                    	Bridge.log("Got purchaseDataList with size: " + purchaseDataList.size());
		                for(int i = 0; i < purchaseDataList.size(); i++)
		                {
		                    String purchaseData = purchaseDataList.get(i);
		                    String signature = signatureList.get(i);
		                    String sku = ownedSkus.get(i);
		                    if(GooglePlaySecurity.verifyPurchase(BASE64_PUBLIC_KEY, purchaseData, signature))
		                    {
		                    	Bridge.log("Sku purchased: " + sku);
		        				JSONObject purchase = new JSONObject(purchaseData);
		                        if(TextUtils.isEmpty(purchase.optString("token", purchase.optString("purchaseToken"))))
		                        {
		                        	Bridge.log("BUG: empty/null token! Purchase data: " + purchaseData);
		                        }
		                        history.put(purchase);
		                    } else {
		                    	Bridge.log("Purchase signature verification **FAILED**. Purchase data: " + purchaseData + ". Signature: " + signature);
		                    }
		                }
		                continueToken = ownedItems.getString(INAPP_CONTINUATION_TOKEN);
		                Bridge.log("Continuation token: " + continueToken);
		            } else {
		            	Bridge.log("Bundle returned from getPurchaseHistory() doesn't contain required fields.");
		            }
		        } else {
		        	Bridge.log("getPurchaseHistory() failed with response: " + response);
		        }
			} while (!TextUtils.isEmpty(continueToken));
		}
		catch (JSONException e)
		{
			Bridge.log("Cannot find required data in request. Item type not found. Request - " + request);
			e.printStackTrace();
		}
		catch (RemoteException e)
		{
			Bridge.log("RemoteException on getSkuDetails(). See stack trace.");
			e.printStackTrace();
		}
		if(history != null)
		{
			String purchaseHistory = history.toString();
			sendPurchaseHistoryLoad(purchaseHistory);
		}
	}

	private static int getResponseCodeFromBundle(Bundle bundle)
	{
		if(bundle == null)
		{
			return -1;
		}
		Object code = bundle.get(RESPONSE_CODE);
		if(code == null)
		{
			return -1;
		}
		if(code instanceof Integer)
		{
			return ((Integer) code).intValue();
		}
		if(code instanceof Long)
		{
			return (int) ((Long) code).longValue();
		}
		throw new RuntimeException("Unexpected type for bundle response code: " + code.getClass().getName());
	}
	
	private static String getPurchaseFailCode(int response)
	{
		switch(response)
		{
			case BILLING_RESPONSE_RESULT_USER_CANCELED:
			{
				return PURCHASE_CANCELED;
			}
			case BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED:
			{
				return PURCHASE_ITEM_ALREADY_OWNED;
			}
			case BILLING_RESPONSE_RESULT_ITEM_UNAVAILABLE:
			{
				return PURCHASE_ITEM_UNAVAILABLE;
			}
		}
		return PURCHASE_UNKNOWN;
	}
	
	/* Events */
	
	private static void sendConnectFail(String code)
	{
		Bridge.sendEvent("Billing", "ConnectFail", code);
	}
	
	private static void sendConnect()
	{
		Bridge.sendEvent("Billing", "Connect", "");
	}
	
	private static void sendDisconnect()
	{
		Bridge.sendEvent("Billing", "Disconnect", "");
	}
	
	private static void sendProductsDetailsLoad(String productsDetails)
	{
		Bridge.sendEvent("Billing", "ProductsDetailsLoad", productsDetails);
	}
	
	private static void sendPurchasesLoad(String purchases)
	{
		Bridge.sendEvent("Billing", "PurchasesLoad", purchases);
	}
	
	private static void sendPurchaseHistoryLoad(String purchaseHistory)
	{
		Bridge.sendEvent("Billing", "PurchaseHistoryLoad", purchaseHistory);
	}
	
	private static void sendPurchaseStart()
	{
		Bridge.sendEvent("Billing", "PurchaseStart", "");
	}
	
	private static void sendPurchaseFail(String code)
	{
		Bridge.sendEvent("Billing", "PurchaseFail", code);
	}
	
	private static void sendPurchaseComplete(String payload)
	{
		Bridge.sendEvent("Billing", "PurchaseComplete", payload);
	}
}