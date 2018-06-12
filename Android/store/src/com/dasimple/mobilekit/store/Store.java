package com.dasimple.mobilekit.store;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.dasimple.mobilekit.bridge.Bridge;
import com.dasimple.mobilekit.store.util.AESObfuscator;
import com.dasimple.mobilekit.store.util.DatabaseFunctions;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.text.TextUtils;

public final class Store
{
	private static final String DATABASE_NAME = "dasimple-store.db";
	private static final String SOOMLA_NAME = "store.kv.db";
	private static final String OBFUSCATION_PASSWORD = ""; //Set by default, for more secure...
	private static final String COLUMN_NAME = "name";

	private static Context context = null;
	private static AESObfuscator guardian = null;

	private static SQLiteDatabase database = null;

	public static void connect()
	{
		if(context != null && guardian != null)
		{
			Bridge.log("Store is already connected!");
			return;
		}
		Bridge.log("Connecting store...");
		context = Bridge.getApplicationContext();
		byte[] obfuscationSalt = { 29, 48, 54, 13, -69, -20, 11, 2, -46, -28, 19, 55, -98, -9, 10, -82, -13, -6, -13, 7 };
		String packageName = context.getPackageName();
		String deviceId = Bridge.getDeviceId(context);
		guardian = new AESObfuscator(obfuscationSalt, packageName + deviceId + OBFUSCATION_PASSWORD);
		sendConnect();
	}

	public static void disconnect()
	{
		if(context == null || guardian == null)
		{
			Bridge.log("Store is already disconnected!");
			return;
		}
		if(database != null)
		{
			close();
		}
		context = null;
		guardian = null;
		sendDisconnect();
	}

	public static void open(int version)
	{
		if(database != null)
		{
			Bridge.log("Database is already opened!");
			return;
		}
		SQLiteOpenHelper databaseHelper = new SQLiteOpenHelper(context, DATABASE_NAME, null, version) {
			@Override
			public void onCreate(SQLiteDatabase db)
			{
				Bridge.log("Database " + getDatabaseName() + " created!");
				sendDatabaseCreate();
			}
			@Override
			public void onOpen(SQLiteDatabase db)
			{
				Bridge.log("Database " + getDatabaseName() + " opened!");
				sendDatabaseOpen();
			}
			@Override
			public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
			{
				Bridge.log("Database " + getDatabaseName() + " upgraded from " + oldVersion + " to " + newVersion + "!");
				sendDatabaseUpgrade(oldVersion, newVersion);
			}
			@Override
			public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion)
			{
				Bridge.log("Database " + getDatabaseName() + " downgraded from " + oldVersion + " to " + newVersion + "!");
				sendDatabaseDowngrade(oldVersion, newVersion);
			}
		};
		database = databaseHelper.getWritableDatabase();
		Bridge.log("Database initiated successfully " + database + "! Context = " + context);
	}

	public static void close()
	{
		if(database == null)
		{
			Bridge.log("Database is already closed!");
			return;
		}
		database.close();
		database = null;
		sendDatabaseClose();
	}

	public static boolean reset()
	{
		if(database != null)
		{
			database.close();
		}
		database = null;
		return context.deleteDatabase(DATABASE_NAME);
	}

	public static boolean createCollection(String name, String columnsJSON)
	{
		if(database == null)
		{
			Bridge.log("Database not initialized.");
			return false;
		}
		if(!collectionExists(name))
		{
			Bridge.log("Creating collection " + name + " with columns: " + columnsJSON);
			try
			{
				JSONArray columns = new JSONArray(columnsJSON);
				String columnSQL = DatabaseUtils.sqlEscapeString(COLUMN_NAME) + " TEXT PRIMARY KEY NOT NULL";
				for(int i = 0; i < columns.length(); i++)
				{
					String column = columns.optString(i);
					if(!TextUtils.isEmpty(column) && !COLUMN_NAME.equals(column))
					{
						columnSQL += ", " + DatabaseUtils.sqlEscapeString(column) + " TEXT";
					}
				}
				try
				{
					String sql = "CREATE TABLE IF NOT EXISTS " + DatabaseUtils.sqlEscapeString(name) + " ( " + columnSQL + " )";
					Bridge.log("Executing SQL: " + sql);
					database.execSQL(sql);
					return true;
				}
				catch (SQLiteException e)
				{
					Bridge.log("Failed to execute SQL.");
				}
			}
			catch (JSONException e)
			{
				Bridge.log("Failed to parse columns JSON.");
			}
		}
		return false;
	}

	public static boolean destroyCollection(String name)
	{
		if(database == null)
		{
			Bridge.log("Database not initialized.");
			return false;
		}
		if(collectionExists(name))
		{
			Bridge.log("Destroying collection " + name);
			try
			{
				String sql = "DROP TABLE IF EXISTS " + DatabaseUtils.sqlEscapeString(name);
				Bridge.log("Executing SQL: " + sql);
				SQLiteStatement statement = database.compileStatement(sql);
				statement.execute();
				return true;
			}
			catch (SQLiteException e)
			{
				Bridge.log("Failed to execute SQL.");
			}
		}
		return false;
	}

	public static int add(String collection, String recordsJSON)
	{
		if(database == null)
		{
			Bridge.log("Database not initialized.");
			return 0;
		}
		Bridge.log("Adding to collection " + collection + " records: " + recordsJSON);
		int length = 0;
		try
		{
			JSONArray records = new JSONArray(recordsJSON);
			for(int i = 0; i < records.length(); i++)
			{
				JSONObject record = records.optJSONObject(i);
				ContentValues values = new ContentValues();
				Iterator<String> columns = record.keys();
				while(columns.hasNext())
				{
					String column = columns.next();
					String value = record.optString(column);
					String escapedColumn = DatabaseFunctions.sqlEscapeString(column);
					values.put(escapedColumn, value);
				}
				boolean success = add(collection, values);
				if(success)
				{
					Bridge.log("Added values: " + values);
					length++;
				}
			}
		}
		catch (JSONException e)
		{
			Bridge.log("Failed to parse records JSON.");
		}
		return length;
	}

	public static String get(String collection, String columnsJSON, String whereJSON, String limit)
	{
		if(database == null)
		{
			Bridge.log("Database not initialized.");
			return null;
		}
		//Columns
		String[] columns = DatabaseFunctions.parseColumns(columnsJSON);
		//Where
		AtomicReference<String> refWhere = new AtomicReference<String>(null);
		AtomicReference<String[]> refWhereArgs = new AtomicReference<String[]>(null);
		DatabaseFunctions.parseWhere(whereJSON, refWhere, refWhereArgs);
		String where = refWhere.get();
		String[] whereArgs = refWhereArgs.get();
		//Return
		return get(collection, columns, where, whereArgs, limit);
	}

	public static int set(String collection, String valuesJSON, String whereJSON)
	{
		if(database == null)
		{
			Bridge.log("Database not initialized.");
			return 0;
		}
		//Values
		ContentValues values = DatabaseFunctions.parseValues(valuesJSON);
		//Where
		AtomicReference<String> refWhere = new AtomicReference<String>(null);
		AtomicReference<String[]> refWhereArgs = new AtomicReference<String[]>(null);
		DatabaseFunctions.parseWhere(whereJSON, refWhere, refWhereArgs);
		String where = refWhere.get();
		String[] whereArgs = refWhereArgs.get();
		//Return
		return set(collection, values, where, whereArgs);
	}

	public static int remove(String collection, String whereJSON)
	{
		if(database == null)
		{
			Bridge.log("Database not initialized.");
			return 0;
		}
		//Where
		AtomicReference<String> refWhere = new AtomicReference<String>(null);
		AtomicReference<String[]> refWhereArgs = new AtomicReference<String[]>(null);
		DatabaseFunctions.parseWhere(whereJSON, refWhere, refWhereArgs);
		String where = refWhere.get();
		String[] whereArgs = refWhereArgs.get();
		//Return
		return remove(collection, where, whereArgs);
	}

	public static synchronized String exportSoomla(String secret, boolean delete)
	{
		File soomlaDatabase = context.getDatabasePath(SOOMLA_NAME);
		if(!soomlaDatabase.exists())
		{
			return null;
		}
		if(TextUtils.isEmpty(secret) && delete)
		{
			context.deleteDatabase(SOOMLA_NAME);
			return null;
		}
		SQLiteOpenHelper helper = new SQLiteOpenHelper(context, SOOMLA_NAME, null, 1) {
			@Override
			public void onCreate(SQLiteDatabase db)
			{
				db.execSQL("CREATE TABLE IF NOT EXISTS `kv_store` ( `key` TEXT PRIMARY KEY, `val` TEXT )");
			}
			@Override
			public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
			{
				//Do nothing
			}
		};
		SQLiteDatabase database = helper.getReadableDatabase();
		String[] columns = new String[] { "key", "val" };
		JSONObject json = new JSONObject();
		Cursor cursor = database.query("kv_store", columns, null, null, null, null, null);
		if(cursor != null)
		{
			byte[] obfuscationSalt = { 64, -54, -113, -47, 98, -52, 87, -102, -65, -127, 89, 51, -11, -35, 30, 77, -45, 75, -26, 3 };
			String packageName = context.getPackageName();
			String deviceId = Bridge.getDeviceId(context);
			AESObfuscator obfuscator = new AESObfuscator(obfuscationSalt, packageName + deviceId + secret);
			while(cursor.moveToNext())
			{
				int columnKey = cursor.getColumnIndex("key");
				int columnValue = cursor.getColumnIndex("val");
				try
				{
					String key = obfuscator.unobfuscate(cursor.getString(columnKey), "com.soomla.billing.util.AESObfuscator-1|");
					String value = obfuscator.unobfuscate(cursor.getString(columnValue), "com.soomla.billing.util.AESObfuscator-1|");
					json.put(key, value);
				}
				catch (AESObfuscator.ValidationException e)
				{
					
				}
				catch (JSONException e)
				{
					
				}
			}
			cursor.close();
		}
		database.close();
		if(delete)
		{
			context.deleteDatabase(SOOMLA_NAME);
		}
		return json.toString();
	}
	
	/* Methods */
	
	private static synchronized boolean collectionExists(String name)
	{
		String table = "sqlite_master";
		String[] columns = new String[] { COLUMN_NAME };
		String where = DatabaseFunctions.createWhereColumn("type") + " AND " + DatabaseFunctions.createWhereColumn(COLUMN_NAME);
		String[] whereArgs = new String[] { "table", name };
		Cursor cursor = database.query(table, columns, where, whereArgs, null, null, null);
		if(cursor != null)
		{
			if(cursor.moveToNext())
			{
				cursor.close();
				return true;
			}
		}
		return false;
	}
	
	private static boolean add(String collection, ContentValues values)
	{
		Bridge.log("Adding values " + values + " into collection " + collection);
		String nameColumn = DatabaseFunctions.sqlEscapeString(COLUMN_NAME);
		String nameValue = values.getAsString(nameColumn);
		if(!TextUtils.isEmpty(nameValue))
		{
			Bridge.log("Inserting into database.");
			String table = DatabaseFunctions.sqlEscapeString(collection);
			ContentValues safeValues = guardian.obfuscate(values);
			try
			{
				long insert = database.insertOrThrow(table, null, safeValues);
				Bridge.log("Inserted at row " + insert);
				return insert > 0;
			}
			catch (SQLiteException e)
			{
				Bridge.log("SQLiteException thrown! Why - " + e.getMessage());
			}
		}
		return false;
	}
	
	private static synchronized String get(String collection, String[] columns, String where, String[] whereArgs, String limit)
	{
		Bridge.log("Getting from collection " + collection + " columns: " + Arrays.toString(DatabaseFunctions.sqlEscapeColumns(columns)) + " where " + where + " - " + Arrays.toString(whereArgs));
		JSONArray result = new JSONArray();
		String table = DatabaseFunctions.sqlEscapeString(collection);
		String[] safeColumns = DatabaseFunctions.sqlEscapeColumns(columns);
		String[] safeWhereArgs = guardian.obfuscate(whereArgs);
		Cursor cursor = database.query(table, safeColumns, where, safeWhereArgs, null, null, null, limit);
		if(cursor != null)
		{
			while(cursor.moveToNext())
			{
				if(columns.length == 1)
				{
					String safeValue = cursor.getString(0);
					String value = guardian.tryUnobfuscate(safeValue);
					result.put(value);
					continue;
				}
				JSONObject record = new JSONObject();
				for(String column : columns)
				{
					int columnIndex = cursor.getColumnIndex(column);
					if(columnIndex != -1)
					{
						String safeValue = cursor.getString(columnIndex);
						String value = guardian.tryUnobfuscate(safeValue);
						try
						{
							record.put(column, value);
						}
						catch (JSONException e)
						{
							
						}
					}
				}
				result.put(record);
			}
			cursor.close();
		}
		return result.toString();
	}
	
	private static int set(String collection, ContentValues values, String where, String[] whereArgs)
	{
		Bridge.log("Updating collection " + collection + " values: " + values + " where " + where + " - " + Arrays.toString(whereArgs));
		String table = DatabaseFunctions.sqlEscapeString(collection);
		ContentValues safeValues = guardian.obfuscate(values);
		String[] safeWhereArgs = guardian.obfuscate(whereArgs);
		try
		{
			return database.update(table, safeValues, where, safeWhereArgs);
		}
		catch (Exception e)
		{
			Bridge.log("Cannot update: " + e.getMessage());
		}
		return 0;
	}
	
	private static int remove(String collection, String where, String[] whereArgs)
	{
		Bridge.log("Deleting from collection " + collection + " where " + where + " - " + Arrays.toString(whereArgs));
		String table = DatabaseFunctions.sqlEscapeString(collection);
		String[] safeWhereArgs = guardian.obfuscate(whereArgs);
		try
		{
			return database.delete(table, where, safeWhereArgs);
		}
		catch (Exception e)
		{
			Bridge.log("Cannot delete: " + e.getMessage());
		}
		return 0;
	}
	
	/* Events */
	
	private static void sendConnect()
	{
		Bridge.sendEvent("Store", "Connect", "");
	}
	
	private static void sendDisconnect()
	{
		Bridge.sendEvent("Store", "Disconnect", "");
	}
	
	private static void sendDatabaseCreate()
	{
		Bridge.sendEvent("Store", "DatabaseCreate", "");
	}
	
	private static void sendDatabaseDowngrade(int oldVersion, int newVersion)
	{
		Bridge.sendEvent("Store", "DatabaseDowngrade", oldVersion + ">" + newVersion);
	}
	
	private static void sendDatabaseUpgrade(int oldVersion, int newVersion)
	{
		Bridge.sendEvent("Store", "DatabaseUpgrade", oldVersion + ">" + newVersion);
	}
	
	private static void sendDatabaseOpen()
	{
		Bridge.sendEvent("Store", "DatabaseOpen", "");
	}
	
	private static void sendDatabaseClose()
	{
		Bridge.sendEvent("Store", "DatabaseClose", "");
	}
}
