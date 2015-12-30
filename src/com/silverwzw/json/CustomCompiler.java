package com.silverwzw.json;

import java.util.List;

public interface CustomCompiler {
	
	public abstract JSON compile(Object o, List<Identifier> location, Iterable<? extends CustomCompiler> customCompilers);
	public abstract boolean accepts(Object o, List<Identifier> location);

	public static class Identifier {
		enum Type {
			ARRAY, MAP
		}
		
		Type type;
		long index;
		String key;
		
		Identifier(long index) {
			key = null;
			this.index = index;
			type = Type.ARRAY;
		}
		
		Identifier(String key) {
			this.key = key;
			index = -1;
			type = Type.MAP;
		}
	}
}