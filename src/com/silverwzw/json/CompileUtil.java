package com.silverwzw.json;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.silverwzw.json.CustomCompiler.Identifier;

class CompileUtil {
	
	private final static ArrayList<CustomCompiler> defaultCompilers;
	
	static {
		defaultCompilers = new ArrayList<CustomCompiler>(8);
		defaultCompilers.add(new NullCompiler());
		defaultCompilers.add(new JSONCompiler());
		defaultCompilers.add(new MapCompiler());
		defaultCompilers.add(new CollectionCompiler());
		defaultCompilers.add(new BooleanCompiler());
		defaultCompilers.add(new StringCompiler());
		defaultCompilers.add(new NumberCompiler());
		defaultCompilers.add(new ReflectionClassCompiler());
	}
	
	final static JSON compile(Object obj, List<Identifier> location, Iterable<? extends CustomCompiler> customCompilers) {
		
		JSON ret;
		
		ret = compileByCompilerList(obj, location, customCompilers);
		
		if (ret != null) {
			return ret;
		}
		
		return compileByCompilerList(obj, location, defaultCompilers);
		
	}
	
	final private static JSON compileByCompilerList(Object obj, List<Identifier> location, Iterable<? extends CustomCompiler> customCompilers) {

		if (customCompilers != null) {
			for (CustomCompiler cc : customCompilers) {
				if (cc.accepts(obj, location)) {
					return cc.compile(obj, location, customCompilers);
				}
			}
		}
		
		return null;
	}
	
}

final class NullCompiler implements CustomCompiler {

	@Override
	final public JSON compile(Object o, List<Identifier> location, Iterable<? extends CustomCompiler> customCompilers) {
		return JSONFactory.jsonNull;
	}

	@Override
	final public boolean accepts(Object o, List<Identifier> location) {
		return o == null;
	}
	
}

final class JSONCompiler implements CustomCompiler {

	@Override
	final public JSON compile(Object o, List<Identifier> location, Iterable<? extends CustomCompiler> customCompilers) {

		Class<?> objClass = o.getClass();
		
		JSON json = null;
		try {
			json = (JSON) objClass.newInstance();
		} catch (InstantiationException e) {
			assert false : "InstantiationException when compile a JSON Object to JSON";
		} catch (IllegalAccessException e) {
			assert false : "IllegalAccessException when compile a JSON Object to JSON";
		}
		json.data = ((JSON)o).data;
		return json;
		
	}

	@Override
	final public boolean accepts(Object o, List<Identifier> location) {
		return JSON.class.isInstance(o);
	}
	
}

final class MapCompiler implements CustomCompiler {

	@Override
	final public JSON compile(Object o, List<Identifier> location, Iterable<? extends CustomCompiler> customCompilers) {
		return new JsonMap((Map<?,?>) o, location, customCompilers);
	}

	@Override
	final public boolean accepts(Object o, List<Identifier> location) {
		return Map.class.isInstance(o);
	}
	
}

final class CollectionCompiler implements CustomCompiler {

	@Override
	final public JSON compile(Object o, List<Identifier> location, Iterable<? extends CustomCompiler> customCompilers) {
		return new JsonArray(o, location, customCompilers);
	}

	@Override
	final public boolean accepts(Object o, List<Identifier> location) {
		return o.getClass().isArray() || Collection.class.isInstance(o);
	}
	
}

final class BooleanCompiler implements CustomCompiler {

	@Override
	final public JSON compile(Object o, List<Identifier> location, Iterable<? extends CustomCompiler> customCompilers) {
		if ((Boolean) o) {
			return JSONFactory.jsonTrue;
		} else {
			return JSONFactory.jsonFalse;
		}
	}

	@Override
	final public boolean accepts(Object o, List<Identifier> location) {
		return o.getClass() == Boolean.class;
	}
	
}

final class StringCompiler implements CustomCompiler {

	@Override
	final public JSON compile(Object o, List<Identifier> location, Iterable<? extends CustomCompiler> customCompilers) {
		return new JsonString((String) o);
	}

	@Override
	final public boolean accepts(Object o, List<Identifier> location) {
		return o.getClass() == String.class;
	}
	
}

final class NumberCompiler implements CustomCompiler {

	@Override
	final public JSON compile(Object o, List<Identifier> location, Iterable<? extends CustomCompiler> customCompilers) {
		if (o.getClass() == Integer.class) {
			Integer i = (Integer) o;
			if (i == 1) {
				return JSONFactory.jsonI1;
			} else if (i == 0) {
				return JSONFactory.jsonI0;
			}
		} else if (o.getClass() == Short.class) {
			Short i = (Short) o;
			if (i == 1) {
				return JSONFactory.jsonI1;
			} else if (i == 0) {
				return JSONFactory.jsonI0;
			}
		} else if (o.getClass() == Long.class) {
			Long i = (Long) o;
			if (i == 1) {
				return JSONFactory.jsonL1;
			} else if (i == 0) {
				return JSONFactory.jsonL0;
			}
		}
		return new JsonNumber(o);
	}

	@Override
	final public boolean accepts(Object o, List<Identifier> location) {
		Class<?> objClass = o.getClass();
		return objClass == Short.class || objClass == Integer.class || objClass == Long.class || objClass == Float.class || objClass == Double.class;
	}
	
}


final class ReflectionClassCompiler implements CustomCompiler {

	@Override
	final public JSON compile(Object o, List<Identifier> location, Iterable<? extends CustomCompiler> customCompilers) {
		
		Class<?> objClass = o.getClass();
		Field[] fields = objClass.getDeclaredFields();
		
		HashMap<String, JSON> hm = new HashMap<String, JSON>();
		for (Field field : fields) {
			
			boolean accessible;
			String name;
			Object member;
			
			accessible = field.isAccessible();
			field.setAccessible(true);
			
			name = field.getName();
			member = null;
			
			try {
				member = field.get(o);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				assert false : "Accessibility Exception";
			}
			
			Identifier i = new Identifier(name);
			location.add(i);
			hm.put(name, compile(member, location, customCompilers));
			location.remove(location.size() - 1);
			
			field.setAccessible(accessible);;
		}
		JsonMap jsmp = new JsonMap();
		jsmp.data = hm;
		return jsmp;
	}

	@Override
	final public boolean accepts(Object o, List<Identifier> location) {
		return true;
	}
	
}
