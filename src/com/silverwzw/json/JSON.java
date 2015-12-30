package com.silverwzw.json;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.silverwzw.json.CustomCompiler.Identifier;

import java.util.NoSuchElementException;

/**
 * the JSON object, compiles an Java Object to JSON Object, parse JSON String to JSON object or reverse.
 * @author Silverwzw
 */
public abstract class JSON implements Cloneable,Serializable,Iterable<Entry<String,JSON>> {

	private static final long serialVersionUID = 1L;

	protected Object data;
	
	protected JSON() {}
	
	/**
	 * get the content of the JSON object, 
	 * @return
	 * * HashMap<String,JSON> => if the JSON object is a representation of an object<br />
	 * * JSON[] => if the JSON object is a representation of an array<br />
	 * * Short,Integer,Long,Float,Double => if the JSON object is a representation of a Short,Integer,Long,Float,Double<br />
	 * * String => if the JSON object is a representation of String<br />
	 * * Boolean => if the JSON object is a representation of boolean<br />
	 * * null => if the JSON object is a representation of null
	 */
	public abstract Object toObject();
	
	/**
	 * get a child JSON of a JsonMap by its name
	 * @param name the name of the child JSON entry
	 * @return the child JSON, or null if there's no corresponding child element.
	 * @throws JsonUnsupportedOperationException if the Object is not an instance of JsonMap
	 */
	public JSON get(String name) {
		throw new UnsupportedOperationException("get() is only defined in Json map.");
	}
	
	/**
	 * get the size of JSON object
	 * @return if this is a direct value => return 1<br>otherwise, return the size of the container.
	 */
	public abstract int size();
	
	/**
	 * get a child JSON of a JsonMap by its index
	 * @param index the index of the child JSON entry
	 * @return the child JSON
	 * @throws JsonUnsupportedOperationException if the Object is not an instance of JsonArray
	 */
	public JSON at(int index) {
		throw new UnsupportedOperationException("at() is only defined in Json array.");
	}
	
	/**
	 * return the JSON String(one line, with no indent) representation of the JSON object
	 * @return
	 * the JSON String
	 */
	public abstract String toString();
	public abstract boolean equals(Object json);
	protected abstract String format(int level, String indentString);
	/**
	 * check whether the JSON object represents a direct value. 
	 * @return
	 * * true => if content is null, boolean, number, String<br />
	 * * false => otherwise
	 */
	public boolean isDirectValue() {
		return false;
	}
	/**
	 * check whether the JSON object represents a container. 
	 * @return
	 * * true => if content is HashMap, Object, Array, ArrayList<br />
	 * * false => otherwise
	 */
	public boolean isContainer() {
		return false;
	}
	/**
	 * check whether the JSON object represents a null object. 
	 * @return
	 * * true => if content is null<br />
	 * * false => otherwise
	 */
	public boolean isNull() {
		return false;
	}
	/**
	 * return the JSON String (multiple line, with indent) representation of the JSON object
	 * @param indentString
	 * the string used as indent
	 * @return
	 * the JSON String
	 * @throws
	 * com.silverwzw.JSON.IndentStringException
	 */
	public String format(String indentString) {
		if (!indentString.trim().equals("")) {
			throw new IllegalArgumentException("Indent String '" + indentString + "' contains non-blank charactor.");
		}
		return format(0, indentString);
	}
	/**
	 * return the JSON String (multiple line, with indent '\t') representation of the JSON object
	 * @return
	 * the JSON String
	 */
	public String format() {
		return format(0,"\t");
	}
	public abstract Object clone();
	
}

abstract class JsonDirectValue extends JSON {
	private static final long serialVersionUID = 1L;
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
	public final int size() {
		return 1;
	}
}

abstract class JsonContainer extends JSON {
	private static final long serialVersionUID = 1L;
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

final class JsonNull extends JsonDirectValue {
	private static final long serialVersionUID = 1L;
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

final class JsonString extends JsonDirectValue {
	private static final long serialVersionUID = 1L;
	JsonString(String string) {
		data = new String(string);
	}
	public String toString() {
		return reflectString((String)data);
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

	static String reflectString(String d) {
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
	
	static String uniCharReflectString(char c) {
		String ret = "\\u";
		ret += int2hexCh(c / 0x1000);
		ret += int2hexCh(c % 0x1000 / 0x0100);
		ret += int2hexCh(c % 0x0100 / 0x0010);
		ret += int2hexCh(c % 0x0010);
		return ret;
	}
	
	static char int2hexCh(int i) {
		assert i >= 0 && i <= 15 : "Int to Hex Char Exception";
		if (i < 10) {
			return (char) ('0' + i);
		} else {
			return (char) ('a' - 10 + i);
		}
	}
	
	static int findUniChar(String s) {
		for (int i = 0; i < s.length(); i++) {
			if(127 < s.charAt(i)) {
				return i;
			}
		}
		return -1;
	}
}

final class JsonBoolean extends JsonDirectValue {
	private static final long serialVersionUID = 1L;
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

final class JsonNumber extends JsonDirectValue {
	private static final long serialVersionUID = 1L;
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

final class JsonArray extends JsonContainer {
	private static final long serialVersionUID = 1L;
	
	JsonArray(){
		data = new JSON[0];
	};
	
	JsonArray(Object arr, List<Identifier> location, Iterable<? extends CustomCompiler> customCompilers) {
		
		assert arr.getClass().isArray() || Collection.class.isInstance(arr) : "Compiling a non-Array-non-Collection object to JsonArray";
		
		if (arr.getClass().isArray()) {
			JSON[] JSONArr;
			JSONArr = new JSON[Array.getLength(arr)];
			for (int i = 0; i < Array.getLength(arr); i++) {
				location.add(new CustomCompiler.Identifier(i));
				JSONArr[i] = CompileUtil.compile(Array.get(arr, i), location, customCompilers);
			}
			data = JSONArr;
		} else {
			Collection<?> coll;
			JSON[] JSONArr;
			coll = (Collection<?>) arr;
			JSONArr = new JSON[coll.size()];
			int i = 0;
			for (Object o : coll) {
				i++;
				location.add(new CustomCompiler.Identifier(i));
				JSONArr[i] = CompileUtil.compile(o, location, customCompilers);
			}
			data = JSONArr;
		}
	}
	
	@Override
	public JSON at(int index) {
		return ((JSON[]) data)[index];
	}
	
	@Override
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
	public final int size() {
		return Array.getLength(data);
	}
}

final class JsonMap extends JsonContainer {
	private static final long serialVersionUID = 1L;
	
	JsonMap() {
		data = new HashMap<String, JSON>();
	}
	
	JsonMap(Map<?,?> map, List<Identifier> location, Iterable<? extends CustomCompiler> ccs) {
		HashMap<String,JSON> d;
		d = new HashMap<String,JSON>();
		Iterator<?> it = map.keySet().iterator();
		while(it.hasNext()) {
			String k;
			k = it.next().toString();
			location.add(new Identifier(k));
			d.put(k, CompileUtil.compile(map.get(k), location, ccs));
		}
		data = d;
	}
	
	@SuppressWarnings("unchecked")
	public JSON get(String name) {
		return ((Map<String,JSON>) data).get(name);
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
				s += indent + indentString + JsonString.reflectString(k) + ":\n" + d.get(k).format(level + 2, indentString) + (it.hasNext()?",\n":"\n");
			} else {
				s += JsonString.reflectString(k) + ":" + d.get(k).toString() + (it.hasNext()?",":"");
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
	
	@SuppressWarnings("unchecked")
	public final int size() {
		return ((HashMap<String,JSON>) data).size();
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
