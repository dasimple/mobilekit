package com.dasimple.mobilekit.billing;

import java.util.Date;

import com.amazon.device.iap.model.Receipt;

public class AmazonPurchase
{
	public String itemType = "";
	public String sku = "";
	public String payload = "";
	public String receiptId = "";
	public Date purchased = null;
	public boolean isCanceled = false;
	public Date canceled = null;

	public AmazonPurchase(Receipt receipt)
	{
		this.itemType = receipt.getProductType().toString();
		this.sku = receipt.getSku();
		this.receiptId = receipt.getReceiptId();
		this.purchased = receipt.getPurchaseDate();
		this.isCanceled = receipt.isCanceled();
		this.canceled = receipt.getCancelDate();
	}

	@Override
	public String toString()
	{
		return "SKU: " + sku + ", Item type: " + itemType + ", Receipt ID: " + receiptId + ", Purchased on: " + purchased + ", Is canceled: " + isCanceled + ", Canceled on: " + canceled;
	}
}
