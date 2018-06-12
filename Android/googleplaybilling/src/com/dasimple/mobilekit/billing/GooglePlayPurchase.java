package com.dasimple.mobilekit.billing;

import org.json.JSONException;
import org.json.JSONObject;

public final class GooglePlayPurchase
{
	public String itemType = "";
	public String sku = "";
	public String payload = "";
	public String orderId = "";
	public String packageName = "";
	public long time = -1;
	public int state = -1;
	public String token = "";
	public String signature = "";
	public boolean isAutoRenewing = false;

	public GooglePlayPurchase(String json, String signature) throws JSONException
	{
		JSONObject data = new JSONObject(json);
		this.sku = data.optString("productId");
		this.orderId = data.optString("orderId");
		this.packageName = data.optString("packageName");
		this.time = data.optLong("purchaseTime");
		this.state = data.optInt("purchaseState");
		this.payload = data.optString("developerPayload");
		this.token = data.optString("token", data.optString("purchaseToken"));
		this.isAutoRenewing = data.optBoolean("autoRenewing");
		this.signature = signature;
	}

	@Override
	public String toString()
	{
		return "SKU: " + sku + ", Item type: " + itemType + ", Order ID: " + orderId + ", Package name: " + packageName + ", Time: " + time + ", State: " + state + ", Payload: " + payload + ", Token: " + token + ", Autorenewing: " + isAutoRenewing + ", Signature: " + signature;
	}
}