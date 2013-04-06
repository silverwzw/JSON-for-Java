package com.silverwzw.JSON;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

@SuppressWarnings("serial")
public abstract class JSON implements Cloneable,Serializable,Iterable<Entry<String,JSON>> {

	protected Object data;
	protected JSON() {}
	public static String version() {
		return "0.1";
	}
	public static JSON parse(String json_str){
		ArrayList<JsonToken> tokens;
		JSON root;
		tokens = JsonToken.getTokenStream(json_str);
		root = parseTokenStream(tokens,0,tokens.size());
		return root; 
	}
	public static JSON compileAs(Object obj, Class<?> objClass){
		if (obj == null) {
			return new JsonNull();
		}
		if (!objClass.isInstance(obj)) {
			throw new ClassCastException();
		}
		if (objClass.isInstance(JSON.class)) {
			JSON json = null;
			try {
				json = (JSON) objClass.newInstance();
			} catch (InstantiationException e) {
				assert false : "InstantiationException when compile a JSON Object to JSON";
			} catch (IllegalAccessException e) {
				assert false : "IllegalAccessException when compile a JSON Object to JSON";
			}
			json.data = ((JSON)obj).data;
			return json;
		} else if (objClass.isArray() || implementsInterface(objClass,List.class)){
			return new JsonArray(obj);
		} else if (implementsInterface(objClass, Map.class)) {
			return new JsonMap((Map<?,?>) obj);
		} else if (objClass == Integer.class || objClass == Short.class || objClass == Long.class || objClass == Double.class || objClass == Float.class) {
			return new JsonNumber(obj);
		} else if (objClass == Boolean.class) {
			return new JsonBoolean((Boolean)obj);
		} else if (objClass == String.class) {
			return new JsonString((String)obj);
		}
		Field[] fields = objClass.getFields();
		HashMap<String, JSON> hm = new HashMap<String, JSON>();
		for (Field field : fields) {
			try {
				hm.put(field.getName(),JSON.auto(field.get(obj)));
				if(field.getName().equals("list")) {
					System.out.println(field.get(obj).getClass());
					System.out.println(implementsInterface(field.get(obj).getClass(),List.class));
					System.out.println(implementsInterface(ArrayList.class,List.class));
				}
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				hm.put(field.getName(),new JsonNull());
			}
		}
		JsonMap jsmp = new JsonMap();
		jsmp.data = hm;
		return jsmp;
	}
	public static JSON auto(Object obj) {
		if (obj == null) {
			return new JsonNull();
		}
		return compileAs(obj, obj.getClass());
	}
	static String ReflectString(String d) {
		int index;
		d = d
			.replaceAll("\\\\", "\\\\\\\\")
			.replaceAll("\n", "\\\\n")
			.replaceAll("\t","\\\\t")
			.replaceAll("/","\\\\/")
			.replaceAll("\b","\\\\b")
			.replaceAll("\f", "\\\\f")
			.replaceAll("\r", "\\\\r");
		index = findUniChar(d);
		while (index > 0) {
			d = d.substring(0,index) + uniCharReflectString(d.charAt(index)) + d.substring(index + 1, d.length());
			index = findUniChar(d);
		}
		return "\"" + d + "\"";	
	}
	public static JSON JSFunction(String functionBody) {
		return new JsonFunction(functionBody);
	}
	protected static boolean implementsInterface(Class<?> objClass, Class<?> interf) {
		Class<?>[] inters = objClass.getInterfaces();
		for (int i=0; i < inters.length; i++) {
			if (inters[i] == interf) {
				return true;
			}
		}
		return false;
	}
	public abstract Object toObject();
	public abstract String toString();
	public abstract boolean equals(Object json);
	protected abstract String format(int level, String indentString);
	public boolean isDirectValue() {
		return false;
	}
	public boolean isContainer() {
		return false;
	}
	public boolean isFunction() {
		return false;
	}
	public boolean isNull() {
		return false;
	}
	public String format(String indentString) {
		if (!indentString.trim().equals("")) {
			throw new IndentStringException();
		}
		return format(0, indentString);
	}
	public String format() {
		return format(0,"\t");
	}
	public abstract Object clone();
	private static String uniCharReflectString(char c) {
		String ret = "\\u";
		ret += hex(c / 0x1000);
		ret += hex(c % 0x1000 / 0x0100);
		ret += hex(c % 0x0100 / 0x0010);
		ret += hex(c % 0x0010);
		return ret;
	}
	private static char hex(int i) {
		assert i >= 0 && i <= 15 : "Int to Hex Char Exception";
		if (i < 10) {
			return (char) ('0' + i);
		} else {
			return (char) ('a' - 10 + i);
		}
	}
	private static int findUniChar(String s) {
		for (int i = 0; i < s.length(); i++) {
			if(127 < s.charAt(i)) {
				return i;
			}
		}
		return -1;
	}
	private static JSON parseTokenStream(ArrayList<JsonToken> tks, int start, int end) {
		JsonToken first;
		int stack;
		if (start >= end){
			throw new ParsingException();
		}
		first = tks.get(start); 
		if (
				first.typeEquals(JsonToken.BOOLEAN)
				|| first.typeEquals(JsonToken.NUMBER)
				|| first.typeEquals(JsonToken.NULL)
				|| first.typeEquals(JsonToken.STRING)
		) {
			if (end == start +1) {
				return tks.get(start).jsonObj;
			} else {
				throw new ParsingException();
			}
		}
		if (first.typeEquals(JsonToken.LEFT_BRACKET)) {
			if (end < start +2) {
				throw new ParsingException();
			}
			if (tks.get(start +1).typeEquals(JsonToken.RIGHT_BRACKET)) {
				if (end == start + 2) {
					return new JsonArray(new ArrayList<Object>());
				} else {
					throw new ParsingException();
				}
			}
			int currentElementStart, curr;
			ArrayList<JSON> tmpList;
			tmpList = new ArrayList<JSON>();
			currentElementStart = curr = start + 1;
			stack = 0;
			while(curr < end) {
				if (tks.get(curr).typeEquals(JsonToken.LEFT_BRACKET) || tks.get(curr).typeEquals(JsonToken.LEFT_BRACE)) {
					stack++;
				} else if (tks.get(curr).typeEquals(JsonToken.RIGHT_BRACKET) || tks.get(curr).typeEquals(JsonToken.RIGHT_BRACE)) {
					stack--;
					if (stack < 0) {
						if (curr == end - 1 && tks.get(curr).typeEquals(JsonToken.RIGHT_BRACKET)) {
							tmpList.add(parseTokenStream(tks,currentElementStart,curr));
							JSON a[],ret = new JsonArray();
							a = new JSON[tmpList.size()];
							ret.data = tmpList.toArray(a);
							return ret;
						} else {
							throw new ParsingException();
						}
					}
				} else if (stack == 0 && tks.get(curr).typeEquals(JsonToken.COMMA)) {
					tmpList.add(parseTokenStream(tks,currentElementStart,curr));
					currentElementStart = curr + 1;
				}
				curr++;
			}
			throw new ParsingException();
		}
		if (first.typeEquals(JsonToken.LEFT_BRACE)) {
			if (end < start +2) {
				throw new ParsingException();
			}
			if (tks.get(start +1).typeEquals(JsonToken.RIGHT_BRACE)) {
				if (end == start + 2) {
					return new JsonMap(new HashMap<String,Object>());
				} else {
					throw new ParsingException();
				}
			}
			int currentFieldValueStart, curr;
			String currentFieldName;
			HashMap<String,JSON> tmpMap;
			tmpMap = new HashMap<String,JSON>();
			curr = start + 1;
			stack = 0;
			while(curr < end) {
				if ((!tks.get(curr).typeEquals(JsonToken.STRING)) || (!tks.get(curr + 1).typeEquals(JsonToken.COLON))) {
					throw new ParsingException();
				}
				currentFieldName = tks.get(curr).realStr;
				curr += 2;
				currentFieldValueStart = curr;
				
				while(curr < end) {
					if (tks.get(curr).typeEquals(JsonToken.LEFT_BRACKET) || tks.get(curr).typeEquals(JsonToken.LEFT_BRACE)) {
						stack++;
					} else if (tks.get(curr).typeEquals(JsonToken.RIGHT_BRACKET) || tks.get(curr).typeEquals(JsonToken.RIGHT_BRACE)) {
						stack--;
						if (stack < 0 && tks.get(curr).typeEquals(JsonToken.RIGHT_BRACE)) {
							if (curr == end - 1) {
								tmpMap.put(currentFieldName,parseTokenStream(tks,currentFieldValueStart,curr));
								JsonMap ret = new JsonMap();
								ret.data = tmpMap;
								return ret;
							} else {
								throw new ParsingException();
							}
						}
					} else if (stack == 0 && tks.get(curr).typeEquals(JsonToken.COMMA)) {
						tmpMap.put(currentFieldName,parseTokenStream(tks,currentFieldValueStart,curr));
						curr++;
						break;
					}
					curr++;
				}
			}
			throw new ParsingException();
		}
		throw new ParsingException();
	}
}

@SuppressWarnings("serial")
abstract class JsonDirectValue extends JSON {
	public Object toObject() {
		return data;
	}
	public boolean isDirectValue() {
		return true;
	}
	public String format(int level, String indentString) {
		String indent = "";
		for (int i = 0; i < level; i++) {
			indent += indentString;
		}
		return indent + this.toString();
	}
	public Iterator<Entry<String,JSON>> iterator() {
		return new JsonDummyIter();
	}
}

@SuppressWarnings("serial")
abstract class JsonContainer extends JSON {
	public boolean isContainer() {
		return true;
	}
	protected abstract String getJsonString(boolean doFormat, int level, String indentString);
	public String toString() {
		return getJsonString(false,0,"");
	}
	public String format(int level, String indentString) {
		return getJsonString(true,level,indentString);
	}
}

@SuppressWarnings("serial")
final class JsonNull extends JsonDirectValue {
	JsonNull() {
		data = null;
	}
	public String toString() {
		return "null";
	}
	public boolean equals(Object json) {
		return json.getClass() == JsonNull.class;
	}
	public boolean isNull() {
		return true;
	}
	public Object clone() {
		return new JsonNull();
	}
}


@SuppressWarnings("serial")
final class JsonFunction extends JsonDirectValue {
	JsonFunction(String functionBody) {
		data = new String(functionBody);
	}
	public Object toObject() {
		return this;
	}
	public boolean equals(Object obj) {
		if (obj.getClass() != JsonFunction.class) {
			return false;
		}
		return ((String)data).equals((String)((JsonFunction)obj).data);
	}
	public String toString() {
		return (String)data;
	}
	public boolean isFunction() {
		return true;
	}
	public Object clone() {
		return new JsonFunction((String) data);
	}
}

@SuppressWarnings("serial")
final class JsonString extends JsonDirectValue {
	JsonString(String string) {
		data = new String(string);
	}
	public String toString() {
		return JSON.ReflectString((String)data);
	}
	public boolean equals(Object json) {
		if (json.getClass() != JsonString.class) {
			return false;
		}
		return data.equals(((JsonString)json).data);
	}
	public Object clone() {
		return new JsonString((String)data);
	}
}

@SuppressWarnings("serial")
final class JsonBoolean extends JsonDirectValue {
	JsonBoolean(boolean bool) {
		data = bool;
	}
	public String toString() {
		return data.toString();
	}
	public boolean equals(Object json) {
		if (json.getClass() != JsonBoolean.class) {
			return false;
		}
		return !(((Boolean)((JsonBoolean)json).data)^(Boolean)data);
	}
	public Object clone() {
		return new JsonBoolean((boolean)(Boolean)data);
	}
}

@SuppressWarnings("serial")
final class JsonNumber extends JsonDirectValue {
	JsonNumber(Object number) {
		assert (number.getClass() == Double.class)
			|| (number.getClass() == Long.class)
			|| (number.getClass() == Integer.class)
			|| (number.getClass() == Float.class)
			|| (number.getClass() == Short.class)
			: "Compiling a non-Number object to JsonNumber";
		if (number.getClass() == Short.class) {
			number = (Integer)(int)(short)(Short)number;
		}
		data = number;
	}
	public String toString() {
		return data.toString();
	}
	public boolean equals(Object json){
		Object o1,o2;
		int i=0, type=0;
		double d=0;
		float f=0;
		long l=0;
		if (json.getClass() != JsonNumber.class) {
			return false;
		}
		o1 = ((JSON)json).data; 
		o2 = data;
		if (o1.getClass() == o2.getClass()) {
			return o1.equals(o2);
		}
		if (o1.getClass() == Integer.class) {
			i = (int)(Integer)o1;
			type += 1;
		}
		if (o2.getClass() == Integer.class) {
			i = (int)(Integer)o2;
			type += 1;
		}
		if (o1.getClass() == Long.class) {
			l = (long)(Long)o1;
			type += 2;
		}
		if (o2.getClass() == Long.class) {
			l = (long)(Long)o2;
			type += 2;
		}
		if (o1.getClass() == Float.class) {
			f = (float)(Float)o1;
			type += 4;
		}
		if (o2.getClass() == Float.class) {
			f = (float)(Float)o2;
			type += 4;
		}
		if (o1.getClass() == Double.class) {
			d = (double)(Double)o1;
			type += 8;
		}
		if (o2.getClass() == Double.class) {
			d = (double)(Double)o2;
			type += 8;
		}
		if (type == 1 + 2) {
			return i == l;
		}
		if (type == 1 + 4) {
			return i == f;
		}
		if (type == 1 + 8) {
			return i == d;
		}
		if (type == 2 + 4) {
			return l == f;
		}
		if (type == 2 + 8) {
			return l == d;
		}
		if (type == 4 + 8) {
			return f == d;
		}
		assert false : "This default branch shouldn't get executed";
		return false;
	}
	public Object clone() {
		if (data.getClass() == Integer.class) {
			int i;
			i = (int)(Integer)data;
			return new JsonNumber(i);
		}
		if (data.getClass() == Float.class) {
			float f;
			f = (float)(Float)data;
			return new JsonNumber(f);
		}
		if (data.getClass() == Double.class) {
			double d;
			d = (double)(Double)data;
			return new JsonNumber(d);
		}
		if (data.getClass() == Long.class) {
			long l;
			l = (long)(Long)data;
			return new JsonNumber(l);
		}
		assert false : "This default branch shouldn't get executed";
		return new JsonNull();
	}
}

@SuppressWarnings("serial")
final class JsonArray extends JsonContainer {
	JsonArray(){};
	JsonArray(Object arr) {
		assert arr.getClass().isArray()||JSON.implementsInterface(arr.getClass(),List.class) : "Compiling a non-Array object to JsonArray";
		if (arr.getClass().isArray()) {
			JSON[] JSONArr;
			JSONArr = new JSON[Array.getLength(arr)];
			for (int i = 0; i < Array.getLength(arr); i++) {
				JSONArr[i] = JSON.auto(Array.get(arr, i));
			}
			data = JSONArr;
		} else {
			List<?> list;
			JSON[] JSONArr;
			list = (List<?>) arr;
			JSONArr = new JSON[list.size()];
			for (int i = 0; i < list.size(); i++) {
				JSONArr[i] = JSON.auto(list.get(i));
			}
			data = JSONArr;
		}
	}
	protected String getJsonString(boolean doFormat, int level, String indentString) {
		String str = "", indent = "";
		if (doFormat) {
			for (int i = 0; i < level; i++) {
				indent += indentString;
			}
		}
		for (int i = 0; i < Array.getLength(data); i++) {
			if (doFormat) {
				str += ((JSON[])data)[i].format(level+1, indentString) + (i == Array.getLength(data)-1?"\n":",\n");
			}
			else {
				str += ((JSON[])data)[i].toString() + (i == Array.getLength(data)-1?"":",");;
			}
		}
		if (str.trim().equals("")) {
			return indent + "[]";
		} else {
			return indent + (doFormat?"[\n":"[") + str +indent + "]";
		}
	}
	public boolean equals(Object obj) {
		if (obj.getClass() != JsonArray.class) {
			return false;
		}
		if (Array.getLength(data) != Array.getLength(((JsonArray)obj).data)) {
			return false;
		}
		for (int i = 0; i < Array.getLength(data); i++) {
			if (!((JSON)Array.get(data, i)).equals((JSON)Array.get(((JsonArray)obj).data,i))) {
				return false;
			}
		}
		return true;
	}
	public Object toObject() {
		Object[] obj;
		obj = new Object[Array.getLength(data)];
		for (int i = 0; i < obj.length; i++) {
			obj[i] = ((JSON)Array.get(data, i)).toObject();
		}
		return obj;
	}
	public Object clone() {
		JSON cloneList[];
		cloneList = new JSON[Array.getLength(data)];
		for(int i = 0; i < cloneList.length; i++) {
			cloneList[i] = (JSON)((JSON)Array.get(data, i)).clone();
		}
		JSON jsarray = new JsonArray();
		jsarray.data = cloneList;
		return jsarray;
	}
	public Iterator<Entry<String,JSON>> iterator() {
		return new JsonArrayIter(this);
	}
}

@SuppressWarnings("serial")
final class JsonMap extends JsonContainer {
	JsonMap() {}
	JsonMap(Map<?,?> map) {
		HashMap<String,JSON> d;
		d = new HashMap<String,JSON>();
		Iterator<?> it = map.keySet().iterator();
		while(it.hasNext()) {
			String k;
			k = it.next().toString();
			d.put(k, JSON.auto(map.get(k)));
		}
		data = d;
	}
	@SuppressWarnings("unchecked")
	protected String getJsonString(boolean doFormat, int level, String indentString) {
		HashMap<String,JSON> d;
		String s = "", indent = "";
		Iterator<String> it;
		
		if (doFormat) {
			for (int i = 0; i < level; i++) {
				indent += indentString; 
			}
		}
		
		d = (HashMap<String,JSON>)data;
		it = d.keySet().iterator();
				
		while(it.hasNext()) {
			String k;
			k = it.next();
			if (doFormat) {
				s += indent + indentString + JSON.ReflectString(k) + ":\n" + d.get(k).format(level + 2, indentString) + (it.hasNext()?",\n":"\n");
			} else {
				s += JSON.ReflectString(k) + ":" + d.get(k).toString() + (it.hasNext()?",":"");
			}
		}
		if (s.trim().equals("")) {
			return indent + "{}";
		} else {
			return indent + (doFormat?"{\n":"{") + s + indent + "}";
		}
	}
	@SuppressWarnings("unchecked")
	public boolean equals(Object obj) {
		if (obj.getClass() != JsonMap.class) {
			return false;
		}
		HashMap<String,JSON> hm1,hm2;
		hm1 = (HashMap<String,JSON>)data;
		hm2 = (HashMap<String,JSON>)((JsonMap)obj).data;
		if (hm1.keySet().size() != hm2.keySet().size()) {
			return false;
		}
		Iterator<String> it;
		it = hm1.keySet().iterator();
		while(it.hasNext()) {
			String k;
			k = it.next();
			if (!hm2.containsKey(k)){
				return false;
			}
			if(!hm1.get(k).equals(hm2.get(k))) {
				return false;
			}
		}
		return true;
	}
	@SuppressWarnings("unchecked")
	public Object toObject() {
		HashMap<String,JSON> hm;
		HashMap<String,Object> hmr;
		hm = (HashMap<String,JSON>)data;
		hmr = new HashMap<String,Object>();
		Iterator<String> it;
		it = hm.keySet().iterator();
		while(it.hasNext()) {
			String k;
			k = it.next();
			hmr.put(k, hm.get(k).toObject());
		}
		return hmr;
	}
	@SuppressWarnings("unchecked")
	public Object clone() {
		HashMap<String, JSON> hm1,hm2;
		hm1 = (HashMap<String,JSON>) data;
		hm2 = new HashMap<String,JSON>();
		for (Entry<String,JSON> e : hm1.entrySet()) {
			hm2.put(e.getKey(), (JSON) e.getValue().clone());
		}
		JsonMap jsmap = new JsonMap();
		jsmap.data = hm2;
		return jsmap;
	}
	@SuppressWarnings("unchecked")
	public Iterator<Entry<String,JSON>> iterator() {
		return ((HashMap<String,JSON>) data).entrySet().iterator();
	}
}

final class JsonEntryForArray implements Entry<String, JSON> {
	private JSON _value, _refer;
	private int _index;
	JsonEntryForArray(JSON value, JsonArray refer, int index) {
		_index = index;
		_value = value;
		_refer = refer;
	}
	public String getKey() {
		return "";
	}
	public JSON getValue() {
		return _value;
	}
	public JSON setValue(JSON value) {
		JSON v,old;
		v = (JSON)value.clone();
		_value = v;
		old = (JSON)Array.get(_refer.data, _index);
		Array.set(_refer.data,_index,v);
		return old;
	}
}

final class JsonDummyIter implements Iterator<Entry<String,JSON>> {
	public boolean hasNext() {
		return false;
	}
	public Entry<String,JSON> next() {
		throw new NoSuchElementException();
	}
	public void remove() {
		throw new UnsupportedOperationException();
	}
}

final class JsonArrayIter implements Iterator<Entry<String,JSON>> {
	private int index;
	private JsonArray _refer;
	private boolean removeAllowed;
	JsonArrayIter(JsonArray refer) {
		index = -1;
		_refer = refer;
		removeAllowed = false;
	}
	public boolean hasNext() {
		return index + 1 < Array.getLength(_refer.data);
	}
	public Entry<String,JSON> next() {
		if (index < Array.getLength(_refer.data)) {
			index++;
			removeAllowed = true;
			return new JsonEntryForArray((JSON)Array.get(_refer.data, index),_refer,index);
		} else {
			throw new NoSuchElementException();
		}
	}
	public void remove() {
		JSON jsarr[];
		
		if (!removeAllowed) {
			throw new IllegalStateException();
		} else {
			removeAllowed = false;
		}
		
		jsarr = new JSON[Array.getLength(_refer.data) - 1];
		
		for (int i = 0, j = 0; j < jsarr.length ;i++,j++) {
			if (i == index) {
				j--;
			} else {
				jsarr[j] = (JSON) Array.get(_refer.data, i);
			}
		}
		
		index--;
		_refer.data = jsarr;
	}
}

final class JsonToken {
	int tokenType;
	int s;
	int e;
	JSON jsonObj;
	String realStr;
	static JsonToken STRING = JsonToken.string(0, 0, null, null);
	static JsonToken NUMBER = JsonToken.number(0, 0, null);
	static JsonToken BOOLEAN = JsonToken.bool(0, 0, null);
	static JsonToken NULL = JsonToken.empty(0, 0);
	static JsonToken LEFT_BRACE = JsonToken.leftBrace(0);
	static JsonToken LEFT_BRACKET = JsonToken.leftBracket(0);
	static JsonToken RIGHT_BRACKET = JsonToken.rightBracket(0);
	static JsonToken RIGHT_BRACE = JsonToken.rightBrace(0);
	static JsonToken COLON = JsonToken.colon(0);
	static JsonToken COMMA = JsonToken.comma(0);
	private static class Carrier {
		public JSON json;
		public int end;
		public String rs;
		Carrier(JSON js, int i, String s) {
			json = js;
			end = i;
			rs = s;
		}
		Carrier(JSON js, int i) {
			json = js;
			end = i;
			rs = null;
		}
	};
	private JsonToken(int i, int start, int end) {
		tokenType = i;
		s = start;
		e = end;
		jsonObj = null;
		realStr = null;
	}
	private JsonToken(int i, int start, int end, JSON js) {
		tokenType = i;
		s = start;
		e = end;
		jsonObj = js;
		realStr = null;
	}
	private JsonToken(int i, int start, int end, JSON js, String rs) {
		tokenType = i;
		s = start;
		e = end;
		jsonObj = js;
		realStr = rs;
	}
	public boolean typeEquals(Object obj) {
		if (obj.getClass() == Integer.class) {
			return tokenType == (int)(Integer)obj;
		} else if (obj.getClass() == JsonToken.class) {
			return tokenType == ((JsonToken)obj).tokenType;
		}
		return false;
	}
	static JsonToken string(int start, int end, JsonString js, String rs) {
		return new JsonToken(0,start,end,js,rs);
	}
	static JsonToken number(int start, int end, JsonNumber js) {
		return new JsonToken(1,start,end,js);
	}
	static JsonToken empty(int start, int end) {
		return new JsonToken(3,start,end,new JsonNull());
	}
	static JsonToken bool(int start, int end, JsonBoolean js) {
		return new JsonToken(5,start,end,js);
	}
	static JsonToken leftBrace(int start) {
		return new JsonToken(6,start, start + 1);
	}
	static JsonToken rightBrace(int start) {
		return new JsonToken(7,start,start + 1);
	}
	static JsonToken leftBracket(int start) {
		return new JsonToken(8,start,start + 1);
	}
	static JsonToken rightBracket(int start) {
		return new JsonToken(9,start,start + 1);
	}
	static JsonToken colon(int start) {
		return new JsonToken(10,start,start + 1);
	}
	static JsonToken comma(int start) {
		return new JsonToken(11,start,start + 1);
	}
	public static ArrayList<JsonToken> getTokenStream(String json_str) {
		ArrayList<JsonToken> tokenStream = new ArrayList<JsonToken>();
		int curr;
		Carrier carrier;
		curr = eatSpaces(0, json_str);
		while(curr < json_str.length()) {
			switch (json_str.charAt(curr)) {
				case '[':
					tokenStream.add(JsonToken.leftBracket(curr));
					curr++;
					break;
				case '{':
					tokenStream.add(JsonToken.leftBrace(curr));
					curr++;
					break;
				case '}':
					tokenStream.add(JsonToken.rightBrace(curr));
					curr++;
					break;
				case ']':
					tokenStream.add(JsonToken.rightBracket(curr));
					curr++;
					break;
				case ':':
					tokenStream.add(JsonToken.colon(curr));
					curr++;
					break;
				case ',':
					tokenStream.add(JsonToken.comma(curr));
					curr++;
					break;
				case '"':
				case '\'':
					carrier = eatString(curr, json_str);
					tokenStream.add(JsonToken.string(curr,carrier.end, (JsonString)carrier.json, carrier.rs));
					curr = carrier.end;
					break;
				case '+':
				case '-':
				case '.':
				case '0':
				case '1':
				case '2':
				case '3':
				case '4':
				case '5':
				case '6':
				case '7':
				case '8':
				case '9':
					carrier = eatNumber(curr, json_str);
					tokenStream.add(JsonToken.number(curr,carrier.end, (JsonNumber)carrier.json));
					curr = carrier.end;
					break;
				case 'n':
					if (!json_str.substring(curr, curr + 4).equals("null")) {
						throw new LexicalException();
					}
					tokenStream.add(JsonToken.empty(curr, curr+4));
					curr += 4;
					break;
				case 't':
					if (!json_str.substring(curr, curr + 4).equals("true")) {
						throw new LexicalException();
					}
					tokenStream.add(JsonToken.bool(curr, curr + 4, new JsonBoolean(true)));
					curr += 4;
					break;
				case 'f':
					if (!json_str.substring(curr, curr + 5).equals("false")) {
						throw new LexicalException();
					}
					tokenStream.add(JsonToken.bool(curr, curr + 5, new JsonBoolean(false)));
					curr += 5;
					break;
				default:
					throw new LexicalException();
			}
			curr = eatSpaces(curr, json_str);
			if (curr == -1) {
				break;
			}
		}
		return tokenStream;
	}
	private static int eatSpaces(int start, String json_str) {
		if (start >= json_str.length()) {
			return -1;
		}
		while (isSpace(json_str.charAt(start))) {
			start++;
			if (start >= json_str.length()) {
				return -1;
			}
		}
		return start;
	}
	private static Carrier eatNumber(int start, String json_str) {
		double frac = 0, esp = 0.1;
		int i = 0, exp = 0;
		boolean positive = true, epositive = true;
		char tmpc;
		//eat sign
		switch (json_str.charAt(start)) {
			case '-':
				positive = false;
			case '+':
				start++;
			default:
				;
		}
		//eat int
		tmpc = json_str.charAt(start);
		if (tmpc == '0' && json_str.charAt(start + 1) <= '9' && json_str.charAt(start + 1) >= '0' ) {
			throw new LexicalException();
		}
		while (tmpc <= '9' && tmpc >= '0'){
			i *= 10;
			i += tmpc - '0';
			start++;
			tmpc = json_str.charAt(start);
		}
		//eat frac
		if (json_str.charAt(start) == '.') {
			start++;
			tmpc = json_str.charAt(start);
			while(tmpc <= '9' && tmpc >= '0') {
				frac += esp * (tmpc - '0');
				esp /= 10;
				start++;
				tmpc = json_str.charAt(start);
			}
		}
		//eat exp
		tmpc = json_str.charAt(start);
		if (tmpc == 'e' || tmpc == 'E') {
			start++;
			switch (json_str.charAt(start)) {
				case '-':
					epositive = false;
				case '+':
					start++;
				default:
					;
			}
			tmpc = json_str.charAt(start);
			if (tmpc == '0' && json_str.charAt(start + 1) <= '9' && json_str.charAt(start + 1) >= '0') {
				throw new LexicalException();
			}
			while (tmpc >= '0' && tmpc <= '9') {
				exp *= 10;
				exp += tmpc - '0';
			}
		}
		return new Carrier(new JsonNumber((positive?1:-1)*(i+frac)*Math.pow(10, (epositive?1:-1)*exp)),start);
	}
	private static Carrier eatString(int start, String json_str) {
		char strQuote = json_str.charAt(start);
		String realString="";
		int end, charCode;
		end = start + 1;
		while (json_str.charAt(end) != strQuote) {
			if (json_str.charAt(end) == '\\') {
				end++;
				switch (json_str.charAt(end)) {
					case 'u':
						charCode = getUnicode(json_str,end + 1);
						if (charCode < 0) {
							throw new LexicalException();
						}
						end += 4;
						realString += (char) charCode;
						break;
					case '"':
					case '\'':
					case '/':
					case '\\':
						realString += json_str.charAt(end);
						break;
					case 'n':
						realString += '\n';
						break;
					case 'r':
						realString += '\r';
						break;
					case 'b':
						realString += '\b';
						break;
					case 't':
						realString += '\t';
						break;
					default:
						throw new LexicalException();
				}
			} else {
				realString += json_str.charAt(end);
			}
			end++;
		}
		return new Carrier(new JsonString(realString), end +  1, realString);
	}
	private static boolean isSpace(char c) {
		return c ==' ' || c == '\t' || c == '\n' || c == '\r';
	}
	private static int hex(char c) {
		if (c >= '0' && c <= '9') {
			return c - '0';
		} else if (c >= 'a' && c <= 'f') {
			return c - 'a' + 10;
		} else if (c >= 'A' && c <= 'F') {
			return c - 'A' + 10;
		} else {
			return -1;
		}
	}
	private static int getUnicode(String jsstr, int start) {
		if (hex(jsstr.charAt(start)) >= 0 && hex(jsstr.charAt(start + 1)) >= 0 && hex(jsstr.charAt(start+2)) >= 0 && hex(jsstr.charAt(start+3)) >= 0) {
			int charCode = 0;
			charCode += hex(jsstr.charAt(start)) * 0x1000;
			charCode += hex(jsstr.charAt(start + 1)) * 0x0100;
			charCode += hex(jsstr.charAt(start + 2)) * 0x0010;
			charCode += hex(jsstr.charAt(start + 3)) * 0x0001;
			return charCode;
		} else {
			return -1;
		}
	}
}

@SuppressWarnings("serial")
final class LexicalException extends RuntimeException {}
@SuppressWarnings("serial")
final class ParsingException extends RuntimeException {}
@SuppressWarnings("serial")
final class IndentStringException extends RuntimeException {}