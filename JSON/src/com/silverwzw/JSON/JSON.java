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
	public static JSON parse(String json_str){
		return null; 
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
		d = d
			.replaceAll("\\\\", "\\\\\\\\")
			.replaceAll("\n", "\\\\n")
			.replaceAll("\t","\\\\t")
			.replaceAll("/","\\\\/")
			.replaceAll("\b","\\\\b")
			.replaceAll("\f", "\\\\f")
			.replaceAll("\r", "\\\\r");
		
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
	private JsonArray(){};
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
@SuppressWarnings("serial")
final class IndentStringException extends RuntimeException {}