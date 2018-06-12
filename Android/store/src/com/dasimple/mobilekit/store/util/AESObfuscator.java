/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dasimple.mobilekit.store.util;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.KeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import android.content.ContentValues;
import android.text.TextUtils;

public class AESObfuscator
{
	private static final String UTF8 = "UTF-8";
	private static final String KEYGEN_ALGORITHM = "PBEWITHSHAAND256BITAES-CBC-BC";
	private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";
	private static final byte[] IV = { 16, 74, 71, -80, 32, 101, -47, 72, 117, -14, 0, -29, 70, 65, -12, 74 };
	private static final String header = "com.dasimple.store.util.AESObfuscator-1|";
	
	private Cipher encryptor = null;
	private Cipher decryptor = null;

	public AESObfuscator(byte[] salt, String password)
	{
		byte[] passwordData = null;
		try
		{
			SecretKeyFactory factory = SecretKeyFactory.getInstance(KEYGEN_ALGORITHM);
			KeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, 1024, 256);
			passwordData = factory.generateSecret(keySpec).getEncoded();
		}
		catch (GeneralSecurityException e1)
		{
			try
			{
				MessageDigest digester = MessageDigest.getInstance("MD5");
				char[] passwordChars = password.toCharArray();
				for(int i = 0; i < passwordChars.length; i++)
				{
					byte passwordChar = (byte) passwordChars[i];
					digester.update(passwordChar);
				}
				passwordData = digester.digest();
			}
			catch (NoSuchAlgorithmException e2)
			{
				throw new RuntimeException("Invalid environment", e2);
			}
		}
		SecretKey secret = new SecretKeySpec(passwordData, "AES");
		try
		{
			encryptor = Cipher.getInstance(CIPHER_ALGORITHM);
			encryptor.init(1, secret, new IvParameterSpec(IV));
			decryptor = Cipher.getInstance(CIPHER_ALGORITHM);
			decryptor.init(2, secret, new IvParameterSpec(IV));
		}
		catch (GeneralSecurityException e)
		{
			throw new RuntimeException("Invalid environment", e);
		}
	}

	public String obfuscate(String unobfuscated, String header)
	{
		if(TextUtils.isEmpty(unobfuscated))
		{
			return "";
		}
		try
		{
			// Header is appended as an integrity check
			return Base64.encode(encryptor.doFinal((header + unobfuscated).getBytes(UTF8)));
		}
		catch (UnsupportedEncodingException e)
		{
			throw new RuntimeException("Invalid environment", e);
		}
		catch (GeneralSecurityException e)
		{
			throw new RuntimeException("Invalid environment", e);
		}
	}

	public String obfuscate(String unobfuscated)
	{
		return obfuscate(unobfuscated, header);
	}

	public String[] obfuscate(String[] unobfuscated)
	{
		if(unobfuscated == null)
		{
			return null;
		}
		String[] obfuscated = new String[unobfuscated.length];
		for(int i = 0; i < unobfuscated.length; i++)
		{
			String value = unobfuscated[i];
			String obfuscatedValue = obfuscate(value);
			obfuscated[i] = obfuscatedValue;
		}
		return obfuscated;
	}

	public ContentValues obfuscate(ContentValues unobfuscated)
	{
		if(unobfuscated == null)
		{
			return null;
		}
		ContentValues obfuscated = new ContentValues();
		for(String key : unobfuscated.keySet())
		{
			String value = unobfuscated.getAsString(key);
			String obfuscatedValue = obfuscate(value);
			obfuscated.put(key, obfuscatedValue);
		}
		return obfuscated;
	}

	public String unobfuscate(String obfuscated, String header) throws ValidationException
	{
		if(TextUtils.isEmpty(obfuscated))
		{
			return "";
		}
		try
		{
			String result = new String(decryptor.doFinal(Base64.decode(obfuscated)), UTF8);
			// Check for presence of header. This serves as a final integrity check, for cases
			// where the block size is correct during decryption.
			int headerIndex = result.indexOf(header);
			if(headerIndex != 0)
			{
				throw new ValidationException("Header not found (invalid data or key)" + ":" + obfuscated);
			}
			return result.substring(header.length(), result.length());
		}
		catch (Base64DecoderException e)
		{
			throw new ValidationException(e.getMessage() + ":" + obfuscated);
		}
		catch (IllegalBlockSizeException e)
		{
			throw new ValidationException(e.getMessage() + ":" + obfuscated);
		}
		catch (BadPaddingException e)
		{
			throw new ValidationException(e.getMessage() + ":" + obfuscated);
		}
		catch (UnsupportedEncodingException e)
		{
			throw new RuntimeException("Invalid environment", e);
		}
	}

	public String unobfuscate(String obfuscated) throws ValidationException
	{
		return unobfuscate(obfuscated, header);
	}
	
	public String tryUnobfuscate(String obfuscated)
	{
		String unobfuscated = null;
		try
		{
			unobfuscated = unobfuscate(obfuscated);
		}
		catch (ValidationException e)
		{
			//Fuck it!!!!!
		}
		return unobfuscated;
	}

	public class ValidationException extends Exception
	{
		private static final long serialVersionUID = 1L;
		
		public ValidationException()
		{
			super();
		}

		public ValidationException(String s)
		{
			super(s);
		}
	}
}
