using System;
using System.Text;
using System.Collections;
using System.Collections.Generic;

namespace MobileKit
{
	public class JSONException : Exception
	{
		public JSONException(string message) : base("JSON: " + message)
		{

		}
	}
	public class JSON : IEnumerable<string>
	{
		public enum Type
		{
			String,
			Number,
			Object,
			Array,
			Boolean,
			Null
		}
		public static JSON Clone(JSON copy)
		{
			if(copy == null)
			{
				return null;
			}
			switch(copy.type)
			{
				case Type.Array:
				{
					JSON clone = new JSON(Type.Array);
					for(int i = 0; i < copy.length; i++)
					{
						clone[i] = copy[i];
					}
					return clone;
				}
				case Type.Object:
				{
					JSON clone = new JSON(Type.Object);
					foreach(string key in copy)
					{
						clone[key] = copy[key];
					}
					return clone;
				}
			}
			return new JSON(copy.rawValue);
		}
		public static JSON Parse(string encoded)
		{
			if(string.IsNullOrEmpty(encoded))
			{
				return null;
			}
			string json = encoded.Trim();
			JSON item = null;
			if(json.StartsWith("{") && json.EndsWith("}"))
			{
				item = new JSON(Type.Object);
			}
			if(json.StartsWith("[") && json.EndsWith("]"))
			{
				item = new JSON(Type.Array);
			}
			if(item != null)
			{
				string inside = json.Substring(1, json.Length - 2);
				int level = 0;
				bool inQuotes = false;
				bool inKey = false;
				string key = "";
				string tail = "";
				int i = 0;
				while(i < inside.Length)
				{
					char c = inside[i];
					switch(c)
					{
						case '"':
						{
							inQuotes = !inQuotes;
							if(item.type == Type.Array)
							{
								tail += c;
								break;
							}
							if(string.IsNullOrEmpty(key))
							{
								inKey = !inKey;
							} else {
								tail += c;
							}
							break;
						}
						case ':':
						{
							if(inQuotes || level > 0)
							{
								tail += c;
								break;
							}
							if(item.type == Type.Array)
							{
								throw new JSONException("Parse error.");
							}
							if(string.IsNullOrEmpty(tail))
							{
								throw new JSONException("Parse error.");
							}
							key = tail;
							tail = "";
							break;
						}
						case ',':
						{
							if(inQuotes || level > 0)
							{
								tail += c;
								break;
							}
							if(string.IsNullOrEmpty(tail))
							{
								throw new JSONException("Parse error.");
							}
							JSON value = Parse(tail);
							tail = "";
							if(item.type == Type.Array)
							{
								item.Push(value);
								break;
							}
							item[key] = value;
							key = "";
							break;
						}
						case '\r':
						case '\n':
						{
							if(inQuotes)
							{
								throw new JSONException("Parse error. " + c + " at " + i + " and " + inside);
							}
							break;
						}
						case ' ':
						case '\t':
						{
							if(inQuotes)
							{
								tail += c;
							}
							break;
						}
						case '\\':
						{
							if(level != 0)
							{
								tail += c;
								break;
							}
							++i;
							if(!inQuotes)
							{
								throw new JSONException("Parse error.");
							}
							char n = inside[i];
							switch(n)
							{
								case 't':
								{
									tail += '\t';
									break;
								}
								case 'r':
								{
									tail += '\r';
									break;
								}
								case 'n':
								{
									tail += '\n';
									break;
								}
								case 'b':
								{
									tail += '\b';
									break;
								}
								case 'f':
								{
									tail += '\f';
									break;
								}
								case 'u':
								{
									string s = inside.Substring(i + 1, 4);
									tail += (char) int.Parse(s, System.Globalization.NumberStyles.AllowHexSpecifier);
									i += 4;
									break;
								}
								default:
								{
									tail += n;
									break;
								}
							}
							break;
						}
						default:
						{
							if(c == '{' || c == '[')
							{
								level++;
							}
							if(c == '}' || c == ']')
							{
								level--;
							}
							tail += c;
							break;
						}
					}
					i++;
				}
				if(level != 0)
				{
					throw new JSONException("Parse error.");
				}
				if(inQuotes)
				{
					throw new JSONException("Parse error.");
				}
				if(inKey)
				{
					throw new JSONException("Parse error.");
				}
				switch(item.type)
				{
					case Type.Array:
					{
						if(string.IsNullOrEmpty(tail))
						{
							if(item.length > 0)
							{
								throw new JSONException("Parse error.");
							}
							break;
						}
						item.Push(Parse(tail));
						break;
					}
					case Type.Object:
					{
						if(string.IsNullOrEmpty(key))
						{
							if(item.length > 0)
							{
								throw new JSONException("Parse error.");
							}
							break;
						}
						if(string.IsNullOrEmpty(tail))
						{
							throw new JSONException("Parse error.");
						}
						item[key] = Parse(tail);
						break;
					}
				}
				return item;
			}
			if(json.StartsWith("\"") && json.EndsWith("\""))
			{
				return new JSON(json.Substring(1, json.Length - 2));
			}
			if(json == "true")
			{
				return new JSON(true);
			}
			if(json == "false")
			{
				return new JSON(false);
			}
			if(json == "null")
			{
				return new JSON();
			}
			float numeric = 0f;
			if(float.TryParse(json, out numeric))
			{
				return new JSON(numeric);
			}
			throw new JSONException("Parse error." + json);
		}
		public static string Escape(string value)
		{
			StringBuilder builder = new StringBuilder();
			foreach(char c in value)
			{
				switch(c)
				{
					case '"':
					{
						builder.Append("\\\"");
						break;
					}
					case '\\':
					{
						builder.Append("\\\\");
						break;
					}
					case '/':
					{
						builder.Append("\\/");
						break;
					}
					case '\b':
					{
						builder.Append("\\b");
						break;
					}
					case '\f':
					{
						builder.Append("\\f");
						break;
					}
					case '\n':
					{
						builder.Append("\\n");
						break;
					}
					case '\r':
					{
						builder.Append("\\r");
						break;
					}
					case '\t':
					{
						builder.Append("\\t");
						break;
					}
					default:
					{
						int i = (int) c;
						if(i < 32 || i > 127)
						{
							builder.AppendFormat("\\u{0:X4}", i);
							break;
						}
						builder.Append(c);
						break;
					}
				}
			}
			return builder.ToString();
		}
		public static implicit operator JSON(string value)
		{
			return new JSON(value);
		}
		public static implicit operator JSON(int value)
		{
			return new JSON(value);
		}
		public static implicit operator JSON(float value)
		{
			return new JSON(value);
		}
		public static implicit operator JSON(bool value)
		{
			return new JSON(value);
		}
		public Type type = Type.Null;
		private string rawValue = "";
		private Dictionary<string, JSON> dictionary = null;
		private List<JSON> list = null;
		public JSON(Type type = Type.Null)
		{
			switch(type)
			{
				case Type.Object:
				{
					this.dictionary = new Dictionary<string, JSON>();
					break;
				}
				case Type.Array:
				{
					this.list = new List<JSON>();
					break;
				}
			}
			this.type = type;
		}
		public JSON(string value) : this(Type.String)
		{
			stringValue = value;
		}
		public JSON(float value) : this(Type.Number)
		{
			floatValue = value;
		}
		public JSON(int value) : this(Type.Number)
		{
			intValue = value;
		}
		public JSON(long value) : this(Type.Number)
		{
			longValue = value;
		}
		public JSON(bool value) : this(Type.Boolean)
		{
			booleanValue = value;
		}
		public bool isString
		{
			get
			{
				return type == Type.String;
			}
		}
		public bool isNumber
		{
			get
			{
				return type == Type.Number;
			}
		}
		public bool isObject
		{
			get
			{
				return type == Type.Object;
			}
		}
		public bool isArray
		{
			get
			{
				return type == Type.Array;
			}
		}
		public bool isBoolean
		{
			get
			{
				return type == Type.Boolean;
			}
		}
		public bool isNull
		{
			get
			{
				return type == Type.Null;
			}
		}
		public JSON this[string key]
		{
			get
			{
				if(isObject)
				{
					return dictionary[key];
				}
				throw new JSONException("String mapping is available just for objects.");
			}
			set
			{
				if(!isObject)
				{
					throw new JSONException("String mapping is available just for objects.");
				}
				if(dictionary.ContainsKey(key))
				{
					dictionary[key] = value;
				} else {
					dictionary.Add(key, value);
				}
			}
		}
		public JSON this[int index]
		{
			get
			{
				if(isObject)
				{
					return dictionary[index.ToString()];
				}
				if(isArray)
				{
					return list[index];
				}
				throw new JSONException("Index mapping is available just for arrays and objects.");
			}
			set
			{
				switch(type)
				{
					case Type.Object:
					{
						string key = index.ToString();
						if(dictionary.ContainsKey(key))
						{
							dictionary[key] = value;
						} else {
							dictionary.Add(key, value);
						}
						break;
					}
					case Type.Array:
					{
						if(index < 0)
						{
							throw new JSONException("Array cannot have negative indexes.");
						}
						if(index < list.Count)
						{
							list[index] = value;
						} else {
							for(int i = list.Count; i < index; i++)
							{
								list.Add(new JSON());
							}
							list.Add(value);
						}
						break;
					}
					default:
					{
						throw new JSONException("Index mapping is available just for arrays and objects.");
					}
				}
			}
		}
		public string stringValue
		{
			get
			{
				if(!isString)
				{
					throw new JSONException("Cannot get string value from non-string item.");
				}
				return rawValue;
			}
			set
			{
				if(!isString)
				{
					throw new JSONException("Cannot set string value to non-string item.");
				}
				rawValue = value;
			}
		}
		public float floatValue
		{
			get
			{
				if(!isNumber)
				{
					throw new JSONException("Cannot get float value from non-number item.");
				}
				float value = 0f;
				float.TryParse(rawValue, out value);
				return value;
			}
			set
			{
				if(!isNumber)
				{
					throw new JSONException("Cannot set float value to non-number item.");
				}
				rawValue = value.ToString();
			}
		}
		public int intValue
		{
			get
			{
				if(!isNumber)
				{
					throw new JSONException("Cannot get int value from non-number item.");
				}
				int value = 0;
				int.TryParse(rawValue, out value);
				return value;
			}
			set
			{
				if(!isNumber)
				{
					throw new JSONException("Cannot set int value to non-number item.");
				}
				rawValue = value.ToString();
			}
		}
		public long longValue
		{
			get
			{
				if(!isNumber)
				{
					throw new JSONException("Cannot get long value from non-number item.");
				}
				long value = 0;
				long.TryParse(rawValue, out value);
				return value;
			}
			set
			{
				if(!isNumber)
				{
					throw new JSONException("Cannot set long value to non-number item.");
				}
				rawValue = value.ToString();
			}
		}
		public bool booleanValue
		{
			get
			{
				if(!isBoolean)
				{
					throw new JSONException("Cannot get boolean value from non-boolean item.");
				}
				return rawValue == "1";
			}
			set
			{
				if(!isBoolean)
				{
					throw new JSONException("Cannot set boolean value to non-boolean item.");
				}
				rawValue = value ? "1" : "";
			}
		}
		public int length
		{
			get
			{
				switch(type)
				{
					case Type.Object:
					{
						return dictionary.Count;
					}
					case Type.Array:
					{
						return list.Count;
					}
					default:
					{
						throw new JSONException("Length can have just objects and arrays.");
					}
				}
			}
		}
		public string[] keys
		{
			get
			{
				switch(type)
				{
					case Type.Object:
					{
						string[] keys = new string[dictionary.Count];

						int i = 0;
						foreach(string key in dictionary.Keys)
						{
							keys[i] = key;
							i++;
						}

						return keys;
					}
					case Type.Array:
					{
						string[] keys = new string[list.Count];

						for(int i = 0; i < keys.Length; i++)
						{
							keys[i] = i + "";
						}

						return keys;
					}
					default:
					{
						throw new JSONException("Mapping can have just objects and arrays.");
					}
				}
			}
		}
		public JSON Clone()
		{
			return Clone(this);
		}
		public IEnumerator<string> GetEnumerator()
		{
			if(!isObject)
			{
				throw new JSONException("Mapping is available just for objects.");
			}
			foreach(string key in dictionary.Keys)
			{
				yield return key;
			}
		}
		IEnumerator IEnumerable.GetEnumerator()
		{
			return GetEnumerator();
		}
		public JSON Get(string key, JSON fallback = null)
		{
			if(!HasKey(key))
			{
				return fallback;
			}
			return this[key];
		}
		public JSON Get(int index, JSON fallback = null)
		{
			if(!HasKey(index))
			{
				return fallback;
			}
			return this[index];
		}
		public JSON GetObject(int index)
		{
			if(!HasKey(index))
			{
				return new JSON(Type.Object);
			}
			return this[index];
		}
		public JSON GetObject(string key)
		{
			if(!HasKey(key))
			{
				return new JSON(Type.Object);
			}
			return this[key];
		}
		public string GetString(string key, string fallback = "")
		{
			if(!HasKey(key))
			{
				return fallback;
			}
			return this[key].stringValue;
		}
		public string GetString(int index, string fallback = "")
		{
			if(!HasKey(index))
			{
				return fallback;
			}
			return this[index].stringValue;
		}
		public bool HasKey(int index)
		{
			switch(type)
			{
				case Type.Object:
				{
					return dictionary.ContainsKey(index.ToString());
				}
				case Type.Array:
				{
					return index >= 0 && index < list.Count;
				}
			}
			throw new JSONException("Index mapping is available just for arrays and objects.");
		}
		public bool HasKey(string key)
		{
			if(!isObject)
			{
				throw new JSONException("String mapping is available just for objects.");
			}
			return dictionary.ContainsKey(key);
		}
		public int IndexOf(JSON item, int start = 0)
		{
			if(!isArray)
			{
				throw new JSONException("IndexOf method is available just for arrays.");
			}
			start = Rotate(start);
			return list.IndexOf(item, start);
		}
		public string Join(string separator = "")
		{
			if(!isArray)
			{
				throw new JSONException("Join method is available just for arrays.");
			}
			bool first = false;
			StringBuilder join = new StringBuilder();
			foreach(JSON item in list)
			{
				if(first)
				{
					join.Append(separator);
				}
				join.Append(item.ToString());
				first = true;
			}
			return join.ToString();
		}
		public JSON Pop()
		{
			if(!isArray)
			{
				throw new JSONException("Pop method is available just for arrays.");
			}
			int index = list.Count - 1;
			JSON item = list[index];
			list.RemoveAt(index);
			return item;
		}
		public int Push(params JSON[] items)
		{
			if(!isArray)
			{
				throw new JSONException("Push method is available just for arrays.");
			}
			foreach(JSON item in items)
			{
				list.Add(item);
			}
			return list.Count;
		}
		public JSON[] Reverse()
		{
			if(!isArray)
			{
				throw new JSONException("Reverse method is available just for arrays.");
			}
			list.Reverse();
			return list.ToArray();
		}
		public JSON Shift()
		{
			if(!isArray)
			{
				throw new JSONException("Shift method is available just for arrays.");
			}
			JSON item = list[0];
			list.RemoveAt(0);
			return item;
		}
		public JSON[] Slice(int start, int end = 0)
		{
			if(!isArray)
			{
				throw new JSONException("Slice method is available just for arrays.");
			}
			start = Rotate(start);
			end = Rotate(end);
			if(end <= 0 || end > list.Count)
			{
				end = list.Count;
			}
			return list.GetRange(start, end - start).ToArray();
		}
		public JSON[] Splice(int index, int count, params JSON[] items)
		{
			if(!isArray)
			{
				throw new JSONException("Splice method is available just for arrays.");
			}
			index = Rotate(index);
			JSON[] slice = Slice(index, index + count);
			if(count > 0)
			{
				list.RemoveRange(index, count);
			}
			foreach(JSON item in items)
			{
				list.Insert(index, item);
				index++;
			}
			return slice;
		}
		public JSON[] ToArray()
		{
			if(!isArray)
			{
				throw new JSONException("ToArray method is available just for arrays.");
			}
			return list.ToArray();
		}
		public int Unshift(params JSON[] items)
		{
			if(!isArray)
			{
				throw new JSONException("Unshift method is available just for arrays.");
			}
			foreach(JSON item in items)
			{
				list.Insert(0, item);
			}
			return list.Count;
		}
		public override string ToString()
		{
			return Stringify(false);
		}
		public string ToString(bool pretty = false)
		{
			return Stringify(pretty);
		}
		private int Rotate(int index)
		{
			if(index < 0)
			{
				index += list.Count;
				if(index < 0)
				{
					index = 0;
				}
			}
			return index;
		}
		private string Stringify(bool pretty, int level = 0)
		{
			switch(type)
			{
				case Type.String:
				{
					return '"' + Escape(rawValue) + '"';
				}
				case Type.Number:
				{
					return rawValue;
				}
				case Type.Object:
				{
					StringBuilder encoded = new StringBuilder();
					encoded.Append('{');
					if(pretty)
					{
						encoded.Append('\n');
					}
					int i = 0;
					foreach(KeyValuePair<string, JSON> entry in dictionary)
					{
						if(i > 0)
						{
							encoded.Append(',');
							if(pretty)
							{
								encoded.Append('\n');
							}
						}
						if(pretty)
						{
							encoded.Append('\t', level + 1);
						}
						encoded.Append('"');
						encoded.Append(Escape(entry.Key));
						encoded.Append('"');
						encoded.Append(':');
						if(pretty)
						{
							encoded.Append(' ');
						}
						encoded.Append(entry.Value.Stringify(pretty, level + 1));
						i++;
					}
					if(pretty)
					{
						encoded.Append('\n');
						encoded.Append('\t', level);
					}
					encoded.Append('}');
					return encoded.ToString();
				}
				case Type.Array:
				{
					StringBuilder encoded = new StringBuilder();
					encoded.Append('[');
					int i = 0;
					foreach(JSON item in list)
					{
						if(i > 0)
						{
							encoded.Append(',');
							if(pretty)
							{
								encoded.Append(' ');
							}
						}
						encoded.Append(item.Stringify(pretty, level));
						i++;
					}
					encoded.Append(']');
					return encoded.ToString();
				}
				case Type.Boolean:
				{
					return rawValue == "1" ? "true" : "false";
				}
			}
			return "null";
		}
	}
}
