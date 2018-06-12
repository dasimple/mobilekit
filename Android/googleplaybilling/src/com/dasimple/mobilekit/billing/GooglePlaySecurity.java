package com.dasimple.mobilekit.billing;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import com.dasimple.mobilekit.bridge.Bridge;

import android.text.TextUtils;
import android.util.Base64;

public final class GooglePlaySecurity
{
	private static final String KEY_FACTORY_ALGORITHM = "RSA";
	private static final String SIGNATURE_ALGORITHM = "SHA1withRSA";

	public static boolean verifyPurchase(PublicKey publicKey, String signedData, String signature)
	{
		Bridge.log("Google Play Verify Purchase.");
		try
		{
			byte[] signatureBytes = Base64.decode(signature, Base64.DEFAULT);
			Signature sig = Signature.getInstance(SIGNATURE_ALGORITHM);
			sig.initVerify(publicKey);
			sig.update(signedData.getBytes());
			return sig.verify(signatureBytes);
		}
		catch (IllegalArgumentException e)
		{
			Bridge.log("Verify Purchase > IllegalArgumentException.");
		}
		catch (NoSuchAlgorithmException e)
		{
			Bridge.log("Verify Purchase > NoSuchAlgorithmException.");
		}
		catch (InvalidKeyException e)
		{
			Bridge.log("Verify Purchase > InvalidKeyException.");
		}
		catch (SignatureException e)
		{
			Bridge.log("Verify Purchase > SignatureException.");
		}
		return false;
    }

	public static boolean verifyPurchase(String base64PublicKey, String signedData, String signature)
	{
		Bridge.log("Verifying purchase. base64EncodedPublicKey: " + base64PublicKey + ", signedData: " + signedData + ", signature: " + signature);
		if(TextUtils.isEmpty(signedData) || TextUtils.isEmpty(base64PublicKey) || TextUtils.isEmpty(signature))
		{
			Bridge.log("Null or empty base64PublicKey and/or signedData and/or signature.");
			return false;
		}
		PublicKey key = generatePublicKey(base64PublicKey);
		return verifyPurchase(key, signedData, signature);
    }

	public static PublicKey generatePublicKey(String base64PublicKey)
	{
		try
		{
			byte[] decodedKey = Base64.decode(base64PublicKey, Base64.DEFAULT);
			KeyFactory keyFactory = KeyFactory.getInstance(KEY_FACTORY_ALGORITHM);
			return keyFactory.generatePublic(new X509EncodedKeySpec(decodedKey));
		}
		catch (NoSuchAlgorithmException e)
		{
			throw new RuntimeException(e);
		}
		catch (InvalidKeySpecException e)
		{
			throw new IllegalArgumentException(e);
		}
    }
}