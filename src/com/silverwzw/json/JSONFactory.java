package com.silverwzw.json;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.silverwzw.json.exception.JsonStringFormatException;

public class JSONFactory {

	final static JSON jsonNull;
	final static JSON jsonTrue;
	final static JSON jsonFalse;
	final static JSON jsonI1;
	final static JSON jsonL1;
	final static JSON jsonI0;
	final static JSON jsonL0;
	
	static {
		jsonNull = new JsonNull();
		jsonTrue = new JsonBoolean(true);
		jsonFalse = new JsonBoolean(false);
		jsonI1 = new JsonNumber((Integer)1);
		jsonL1 = new JsonNumber((Long)1L);
		jsonI0 = new JsonNumber((Integer)0);
		jsonL0 = new JsonNumber((Long)0L);
	}

	/**
	 * read a ASCII file contains one standard JSON string, objects in json will be parsed as HashMap<String,JSON>
	 * @param file
	 * the JSON file
	 * @return
	 * the JSON object represented by the given JSON string
	 * @throws JsonStringFormatException
	 * @throws IOException 
	 */
	public final static JSON parse(File file) throws JsonStringFormatException, IOException {
		Reader fr = null;
		JSON json = null;
		
		fr = new FileReader(file);
		json = parse(fr);
		fr.close();
		
		return json;
	}
	
	/**
	 * read a a standard JSON string from InputStream is, objects in json will be parsed as HashMap<String,JSON>
	 * @param is
	 * the InputStream
	 * @return
	 * the JSON object represented by the given JSON string
	 * @throws JsonStringFormatException
	 * @throws IOException 
	 */
	public final static JSON parse(InputStream is) throws JsonStringFormatException, IOException {
		Reader r = null;
		JSON json = null;
		
		r = new InputStreamReader(is);
		json = parse(r);
		r.close();
		
		return json; 
	}
	
	/**
	 * parse a standard JSON string to a JSON object, objects in json will be parsed as HashMap<String,JSON>
	 * @param json_str
	 * the JSON string to be parse
	 * @return
	 * the JSON object represented by the given JSON string
	 * @throws JsonStringFormatException
	 */
	public final static JSON parse(String jsonStr) throws JsonStringFormatException, IOException {
		Reader r = null;
		JSON json = null;
		
		r = new StringReader(jsonStr);
		json = parse(r);
		r.close();
		
		return json; 
	}
	
	/**
	 * read a a standard JSON string from Reader r, objects in json will be parsed as HashMap<String,JSON>
	 * @param r
	 * the Reader
	 * @return
	 * the JSON object represented by the given JSON string
	 * @throws JsonStringFormatException
	 * @throws IOException 
	 */
	public final static JSON parse(Reader r) throws JsonStringFormatException, IOException {
		ArrayList<JsonToken> tokens;
		JSON root;
		
		if (!r.ready()) {
			throw new IOException("Input device not ready");
		}
		
		tokens = ParseUtil.getTokenStream(r);
		root = ParseUtil.parseTokenStream(tokens,0,tokens.size());
		return root; 
	}
	
	/**
	 * Automatically compile an Java object to JSON object
	 * Equivalent to compile(o, null)
	 * @param obj
	 * the object to be compiled
	 * @return
	 * corresponding JSON object
	 */
	public final static JSON auto(Object o) {
		List<CustomCompiler.Identifier> location = new LinkedList<CustomCompiler.Identifier>();
		return CompileUtil.compile(o, location, null);
	}
	
	/**
	 * Create a general Compiler that compiles an Java object to JSON object using given custom compilers.
	 * Compilers are tested in order, the first compiler accepts who the object will take the responsibility to compile the object to JSON.
	 * If no compiler accepts the object, will use default system compiler.
	 */
	public final static class Compiler {
		final private Iterable<? extends CustomCompiler> ccs;
		public Compiler(final Iterable<? extends CustomCompiler> customCompilers) {
			ccs = customCompilers;
		}
		public final JSON compile(Object o) {
			List<CustomCompiler.Identifier> location = new LinkedList<CustomCompiler.Identifier>();
			return CompileUtil.compile(o, location, ccs);
		}
	}
}

