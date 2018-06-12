package com.dasimple.mobilekit.billing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.amazon.device.iap.PurchasingListener;
import com.amazon.device.iap.PurchasingService;
import com.amazon.device.iap.model.FulfillmentResult;
import com.amazon.device.iap.model.Product;
import com.amazon.device.iap.model.ProductDataResponse;
import com.amazon.device.iap.model.PurchaseResponse;
import com.amazon.device.iap.model.PurchaseUpdatesResponse;
import com.amazon.device.iap.model.Receipt;
import com.amazon.device.iap.model.UserData;
import com.amazon.device.iap.model.UserDataResponse;
import com.dasimple.mobilekit.bridge.Bridge;

public class Amazon
{
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
		
	private static boolean connected = false;
	private static boolean canRefresh = false;
	private static UserData userData = null;
	private static Map<String, String> cachedPayloads = new HashMap<String, String>();
	private static JSONArray receivedPurchases = new JSONArray();
	
	public static void connect()
	{
		if(connected)
		{
			Bridge.log("IAB already setted up!");
			return;
		}
		Bridge.log("Registering new listener for purchasing service.");
        PurchasingService.registerListener(Bridge.getApplicationContext(), new PurchasingListener() {
			@Override
			public void onUserDataResponse(UserDataResponse response)
			{
				UserDataResponse.RequestStatus status = response.getRequestStatus();
				Bridge.log("onGetUserDataResponse: requestId (" + response.getRequestId() + ") userIdRequestStatus: " + status + ")");
		        switch(status)
		        {
		        	case SUCCESSFUL:
		        	{
						if(!connected)
						{
							connected = true;
					        sendConnect();
						}
		        		userData = response.getUserData();
		        		Bridge.log("onUserDataResponse: get user id (" + userData.getUserId() + ", marketplace (" + userData.getMarketplace() + ")");
			            break;
		        	}
			        case FAILED:
			        case NOT_SUPPORTED:
			        {
			        	Bridge.log("onUserDataResponse failed, status code is " + status);
						boolean wasConnected = connected;
						if(wasConnected)
						{
				        	disconnect();
							return;
						}
				        sendConnectFail(CONNECT_NOT_SUPPORTED);
			            break;
			        }
		        }
			}
			@Override
			public void onPurchaseUpdatesResponse(PurchaseUpdatesResponse response)
			{
				if(userData != null && userData.getUserId() != null && !userData.getUserId().equals(response.getUserData().getUserId()))
				{
					Bridge.log("The purchase updates is not for the current user id. Maybe you have to iab.refresh()?");
	                return;
	            }
				PurchaseUpdatesResponse.RequestStatus status = response.getRequestStatus();
				Bridge.log("onPurchaseUpdatesResponse: requestId (" + response.getRequestId() + ") purchaseUpdatesResponseStatus (" + status + ") userId (" + response.getUserData().getUserId() + ")");
				switch(status)
				{
					case SUCCESSFUL:
					{
					    for(Receipt receipt : response.getReceipts())
					    {
						    receivedPurchases.put(receipt.toJSON());
						    if(receipt.isCanceled())
						    {
	                            continue;
	                        }
				            String receiptId = receipt.getReceiptId();
				            PurchasingService.notifyFulfillment(receiptId, FulfillmentResult.FULFILLED);
					    }
					    if(response.hasMore())
					    {
					    	queryPurchases(null);
					    	break;
					    }
					    if(receivedPurchases.length() > 0)
					    {
					    	String purchases = receivedPurchases.toString();
							receivedPurchases = new JSONArray();
					    	sendPurchasesLoad(purchases);
					    }
					    break;
					}
					case FAILED:
					case NOT_SUPPORTED:
					{
						Bridge.log("onProductDataResponse: failed, should retry request");
						break;
					}
				}
			}
			@Override
			public void onPurchaseResponse(PurchaseResponse response)
			{
				if(userData != null && userData.getUserId() != null && !userData.getUserId().equals(response.getUserData().getUserId()))
				{
					Bridge.log("The purchase is not for the current user id. Maybe you have to iab.refresh()?");
	            	sendPurchaseFail(PURCHASE_INVALID);
	                return;
	            }
				String requestId = response.getRequestId().toString();
		        String userId = response.getUserData().getUserId();
		        PurchaseResponse.RequestStatus status = response.getRequestStatus();
		        Bridge.log("onPurchaseResponse: requestId (" + requestId + ") userId (" + userId + ") purchaseRequestStatus (" + status + ")");
		        switch(status)
		        {
			        case SUCCESSFUL:
			        {
			            Receipt receipt = response.getReceipt();
			            if(receipt.isCanceled())
			            {
			            	sendPurchaseFail(PURCHASE_CANCELED);
			                break;
			            }
			            String receiptId = receipt.getReceiptId();
			            /*if(!verifyReceipt(receiptId))
			            {
			                //If the purchase cannot be verified, show relevant error message to the customer.
			            	PlayLog.Message("Purchase cannot be verified, please retry later.");
			            	onPurchaseFailure(IabFailure.PURCHASE_SECURITY);
			            	return;
			            }*/
			            Bridge.log("onPurchaseResponse: receipt json:" + receipt.toJSON());
			            PurchasingService.notifyFulfillment(receiptId, FulfillmentResult.FULFILLED);
			            AmazonPurchase purchase = new AmazonPurchase(receipt);
			            purchase.payload = cachedPayloads.get(purchase.sku);
			            sendPurchaseComplete(purchase.payload);
			            break;
			        }
			        case ALREADY_PURCHASED:
			        {
			            //This is not applicable for consumable item. It is only application for entitlement and subscription. Check related samples for more details.
			        	Bridge.log("onPurchaseResponse: already purchased, should never get here for a consumable.");
			        	sendPurchaseFail(PURCHASE_ITEM_ALREADY_OWNED);
			            break;
			        }
			        case INVALID_SKU:
			        {
			        	Bridge.log("onPurchaseResponse: invalid SKU! onProductDataResponse should have disabled buy button already.");
			            Set<String> unavailableSkus = new HashSet<String>();
			            unavailableSkus.add(response.getReceipt().getSku());
			            sendPurchaseFail(PURCHASE_ITEM_UNAVAILABLE);
			            break;
			        }
			        case FAILED:
			        {
			        	Bridge.log("onPurchaseResponse: failed so remove purchase request from local storage");
			        	sendPurchaseFail(PURCHASE_UNKNOWN);
			            break;
			        }
			        case NOT_SUPPORTED:
			        {
			        	Bridge.log("onPurchaseResponse: failed so remove purchase request from local storage");
			        	sendPurchaseFail(PURCHASE_NOT_SUPPORTED);
			            break;
			        }
		        }
			}
			@Override
			public void onProductDataResponse(ProductDataResponse response)
			{
				ProductDataResponse.RequestStatus status = response.getRequestStatus();
				Bridge.log("onProductDataResponse: RequestStatus (" + status + ")");
		        switch(status)
		        {
			        case SUCCESSFUL:
			        {
			        	Bridge.log("onProductDataResponse: successful. The item data map in this response includes the valid SKUs");
			            Set<String> unavailableSkus = response.getUnavailableSkus();
			            Bridge.log("onProductDataResponse: " + unavailableSkus.size() + " unavailable skus");
			            try
			            {
				            Map<String, Product> products = response.getProductData();
				            Set<String> productsKeys = products.keySet();
							JSONArray skuDetailsArray = new JSONArray();
				            for(String sku : productsKeys)
				            {
		                        Product product = products.get(sku);
		                        Bridge.log("Product: " + product.toString());
								skuDetailsArray.put(product.toJSON());
		                    }
							String productsDetails = skuDetailsArray.toString();
							sendProductsDetailsLoad(productsDetails);
			            }
			            catch (JSONException e)
			            {
				        	Bridge.log("Cannot parse product data on onProductDataResponse!");
			            }
			            break;
			        }
			        case FAILED:
			        case NOT_SUPPORTED:
			        {
			        	Bridge.log("onProductDataResponse: failed, should retry request. Status " + status);
			            break;
			        }
		        }
			}
		});
        canRefresh = true;
        refresh();
	}
	
	public static void disconnect()
	{
		userData = null;
		connected = false;
		sendDisconnect();
	}
	
	public static void refresh()
	{
		if(!canRefresh)
		{
			return;
		}
		Bridge.log("Refreshing...");
		PurchasingService.getUserData();
	}

	public static boolean canMakePayments()
	{
		if(!connected)
		{
			Bridge.log("Seems that service is not connected yet.");
			return false;
		}
		return true;
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
			JSONArray request = new JSONArray(requestJSON);
			Set<String> skus = new HashSet<String>();
			for(int i = 0; i < request.length(); i++)
			{
				String sku = request.optString(i);
				skus.add(sku);
			}
			PurchasingService.getProductData(skus);
		}
		catch(JSONException e)
		{
			Bridge.log("Cannot find required data in request. Item type not found. Request - " + requestJSON);
		}
	}

	public static void queryPurchases(String requestJSON)
	{
		if(!connected)
		{
			Bridge.log("Seems that service is not connected yet.");
			return;
		}
		PurchasingService.getPurchaseUpdates(false);
	}

	public static void queryPurchaseHistory(String requestJSON)
	{
		Bridge.log("Amazon doesn't allow to query purchase history.");
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
			String sku = request.getString("sku");
	        cachedPayloads.put(sku, payload);
			Bridge.log("Purchase started.");
			sendPurchaseStart();
			Bridge.log("Start a new activity for purchasing.");
			PurchasingService.purchase(sku);
		}
		catch (JSONException e)
		{
			Bridge.log("SKU not found in request data. Request - " + requestJSON);
		}
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