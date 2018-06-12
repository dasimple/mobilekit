package com.dasimple.mobilekit.store.util;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.dasimple.mobilekit.bridge.Bridge;

import android.content.ContentValues;

public final class DatabaseFunctions
{
	public static String sqlEscapeString(String value)
	{
		return String.format("\"%s\"", value);
	}
	
	public static String[] sqlEscapeColumns(String... columns)
	{
		String[] escapedColumns = new String[columns.length];
		for(int i = 0; i < escapedColumns.length; i++)
		{
			escapedColumns[i] = sqlEscapeString(columns[i]);
		}
		return escapedColumns;
	}
	
	public static String createWhereColumn(String name)
	{
		return sqlEscapeString(name) + " = ?";
	}
	
	public static ContentValues parseValues(String json)
	{
		try
		{
			JSONObject object = new JSONObject(json);
			ContentValues contentValues = new ContentValues();
			Iterator<String> keys = object.keys();
			while(keys.hasNext())
			{
				String column = keys.next();
				String value = object.optString(column);
				String escapedColumn = sqlEscapeString(column);
				contentValues.put(escapedColumn, value);
			}
			return contentValues;
		}
		catch (JSONException e)
		{
			Bridge.log("Cannot parse values: " + json);
		}
		return null;
	}
	
	public static String[] parseColumns(String json)
	{
		try
		{
			JSONArray array = new JSONArray(json);
			int length = array.length();
			String[] columns = new String[length];
			for(int i = 0; i < length; i++)
			{
				String column = array.optString(i);
				columns[i] = column;
			}
			return columns;
		}
		catch (JSONException e)
		{
			Bridge.log("Cannot parse *columns: " + json);
		}
		return null;
	}
	
	public static void parseWhere(String json, AtomicReference<String> refWhere, AtomicReference<String[]> refWhereArgs)
	{
		try
		{
			JSONObject object = new JSONObject(json);
			int length = object.length();
			String clause = "";
			String[] args = new String[length];
			int i = 0;
			Iterator<String> keys = object.keys();
			while(keys.hasNext())
			{
				if(i > 0)
				{
					clause += " AND ";
				}
				String column = keys.next();
				String value = object.optString(column);
				clause += createWhereColumn(column);
				args[i] = value;
				i++;
			}
			refWhere.set(clause);
			refWhereArgs.set(args);
		}
		catch (JSONException e)
		{
			Bridge.log("Cannot parse where: " + json);
		}
	}
}