package com.silverwzw.json;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;

import com.silverwzw.json.JsonNumber;
import com.silverwzw.json.JsonString;
import com.silverwzw.json.exception.LexicalException;
import com.silverwzw.json.exception.ParsingException;

public class ParseUtil {
	
	static JSON parseTokenStream(ArrayList<JsonToken> tks, int start, int end) throws ParsingException {
		JsonToken first;
		int stack;
		
		if (start >= end){
			throw new ParsingException("trying to parse token #" + start + " to token #" + (end - 1)
					+ ". Ending token is prior to starting token. Dump for starting token:\n"
					+ tks.get(start).dump().toString() + "Dump for ending token:\n"
					+ tks.get(end - 1).dump().toString());
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
				throw new ParsingException("Unexpected JSON value token", tks.get(start + 1).dump().toString());
			}
		}
		if (first.typeEquals(JsonToken.LEFT_BRACKET)) {
			if (end < start +2) {
				throw new ParsingException(mismatch(first, tks.get(end -1)));
			}
			if (!tks.get(end - 1).typeEquals(JsonToken.RIGHT_BRACKET)) {
				throw new ParsingException(mismatch(first, tks.get(end -1)));
			}
			if (tks.get(start +1).typeEquals(JsonToken.RIGHT_BRACKET)) {
				if (end == start + 2) {
					return new JsonArray();
				} else {
					throw new ParsingException(mismatch(first, tks.get(end -1)));
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
						if (curr != end - 1) {
							throw new ParsingException("Unexpected token: ", tks.get(curr + 1).dump().toString());
						} else if (!tks.get(curr).typeEquals(JsonToken.RIGHT_BRACKET)) {
							throw new ParsingException("Bracket/brace mismatch, expecting right bracket.", tks.get(curr).dump().toString());
						} else {
							tmpList.add(parseTokenStream(tks,currentElementStart,curr));
							JSON a[],ret = new JsonArray();
							a = new JSON[tmpList.size()];
							ret.data = tmpList.toArray(a);
							return ret;
						}
					}
				} else if (stack == 0 && tks.get(curr).typeEquals(JsonToken.COMMA)) {
					tmpList.add(parseTokenStream(tks,currentElementStart,curr));
					currentElementStart = curr + 1;
				}
				curr++;
			}
			throw new ParsingException(mismatch(first, tks.get(end -1)));
		}
		if (first.typeEquals(JsonToken.LEFT_BRACE)) {
			if (end < start +2) {
				throw new ParsingException(mismatch(first, tks.get(end -1)));
			}
			if (!tks.get(end - 1).typeEquals(JsonToken.RIGHT_BRACE)) {
				throw new ParsingException(mismatch(first, tks.get(end -1)));
			}
			if (tks.get(start +1).typeEquals(JsonToken.RIGHT_BRACE)) {
				if (end == start + 2) {
					return new JsonMap();
				} else {
					throw new ParsingException(mismatch(first, tks.get(end -1)));
				}
			}
			int currentFieldValueStart, curr;
			String currentFieldName;
			HashMap<String,JSON> tmpMap;
			tmpMap = new HashMap<String,JSON>();
			curr = start + 1;
			stack = 0;
			while(curr < end) {
				if ((!tks.get(curr).typeEquals(JsonToken.STRING))) { 
					throw new ParsingException("Expecting string token as key for json map", tks.get(curr).dump().toString());
				}
				if (!tks.get(curr + 1).typeEquals(JsonToken.COLON)) { 
					throw new ParsingException("Expecting colon token for json map key-value pair", tks.get(curr + 1).dump().toString());
				}
				currentFieldName = (String) tks.get(curr).jsonObj.toObject();
				curr += 2;
				currentFieldValueStart = curr;
				
				while(curr < end) {
					if (tks.get(curr).typeEquals(JsonToken.LEFT_BRACKET) || tks.get(curr).typeEquals(JsonToken.LEFT_BRACE)) {
						stack++;
					} else if (tks.get(curr).typeEquals(JsonToken.RIGHT_BRACKET) || tks.get(curr).typeEquals(JsonToken.RIGHT_BRACE)) {
						stack--;
						if (stack < 0) {
							if (curr != end - 1) {
								throw new ParsingException("Unexpected token: ", tks.get(curr + 1).dump().toString());
							} else if (!tks.get(curr).typeEquals(JsonToken.RIGHT_BRACE)) {
								throw new ParsingException("Bracket/brace mismatch, expecting right brace.", tks.get(curr).dump().toString());
							} else {
								tmpMap.put(currentFieldName,parseTokenStream(tks,currentFieldValueStart,curr));
								JsonMap ret = new JsonMap();
								ret.data = tmpMap;
								return ret;
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
			throw new ParsingException(mismatch(first, tks.get(end -1)));
		}
		throw new ParsingException("Unexpected starting token for a json value", first.dump().toString());
	}
	
	private final static String mismatch(JsonToken start, JsonToken end) {
		return "Number of left bracket/brace and right bracket/brace mismatch, \n starting token:"
               + start.dump().toString() + " ending token: " + end.dump().toString();
	}

	public static ArrayList<JsonToken> getTokenStream(Reader jsonReader) throws LexicalException, IOException {
		CharInput input = new CharInput(jsonReader);
		ArrayList<JsonToken> tokenStream = new ArrayList<JsonToken>();
		int i;
		i = eatSpaces(input);
		while(i != -1) {
			switch (i) {
				case '[':
					tokenStream.add(JsonToken.leftBrace(input.dump()));
					i = eatSpaces(input);
					break;
				case '{':
					tokenStream.add(JsonToken.leftBracket(input.dump()));
					i = eatSpaces(input);
					break;
				case '}':
					tokenStream.add(JsonToken.rightBrace(input.dump()));
					i = eatSpaces(input);
					break;
				case ']':
					tokenStream.add(JsonToken.rightBracket(input.dump()));
					i = eatSpaces(input);
					break;
				case ':':
					tokenStream.add(JsonToken.colon(input.dump()));
					i = eatSpaces(input);
					break;
				case ',':
					tokenStream.add(JsonToken.comma(input.dump()));
					i = eatSpaces(input);
					break;
				case '"':
				case '\'':
					tokenStream.add(JsonToken.string(eatString(input, i), input.dump()));
					i = eatSpaces(input);
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
					Object[] o;
					o = eatNumber(input, i);
					tokenStream.add(JsonToken.number((JsonNumber)o[0], input.dump()));
					i = (Integer) o[1];
					if (isSpace(i)) {
						i = eatSpaces(input);
					}
					break;
				case 'n':
					if (input.read() != 'u' || input.read()!= 'l' || input.read() != 'l') {
						throw new LexicalException("Unknown keyword", input.dump().toString());
					}
					tokenStream.add(JsonToken.empty(input.dump()));
					i = eatSpaces(input);
					break;
				case 't':
					if (input.read() != 'r' || input.read()!= 'u' || input.read() != 'e') {
						throw new LexicalException("Unknown keyword", input.dump().toString());
					}
					tokenStream.add(JsonToken.bool(true, input.dump()));
					i = eatSpaces(input);
					break;
				case 'f':
					if (input.read() != 'a' || input.read()!= 'l' || input.read() != 's'|| input.read() != 'e') {
						throw new LexicalException("Unknown keyword", input.dump().toString());
					}
					tokenStream.add(JsonToken.bool(false, input.dump()));
					i = eatSpaces(input);
					break;
				case '\t':
				case '\r':
				case ' ':
				case '\n':
					i = eatSpaces(input);
					break;
				default:
					if ((i <= 'z' && i >= 'a') || (i <= 'Z' && i >= 'A') || i == '_') {
						throw new LexicalException("Unknown keyword", input.dump().toString());
					}
					throw new LexicalException("Unknown token", input.dump().toString());
			}
		}
		return tokenStream;
	}
	private static int eatSpaces(CharInput r) throws IOException {
		int in;
		in = r.read();
		while (isSpace(in)) {
			in = r.read();
		}
		return in;
	}
	private static Object[] eatNumber(CharInput r, int tmpc) throws LexicalException, IOException {
		double frac = 0, esp = 0.1;
		int i = 0, exp = 0;
		boolean positive = true, epositive = true, hasFrac = false;
		//eat sign
		if (tmpc == '-') {
			positive = false;
			tmpc = r.read(); 
		} else if (tmpc == '+') {
			tmpc = r.read();
		}
		
		//eat int
		if (tmpc == '0') {
			tmpc = r.read();
			if (tmpc <= '9' && tmpc >= '0' ) {
				throw new LexicalException("Illegal number format", r.dump().toString());
			}
		} else {
			while (tmpc <= '9' && tmpc >= '0'){
				i *= 10;
				i += tmpc - '0';
				tmpc = r.read();
			}
		}
		//eat frac
		if (tmpc == '.') {
			hasFrac = true;
			tmpc = r.read();
			while(tmpc <= '9' && tmpc >= '0') {
				frac += esp * (tmpc - '0');
				esp /= 10;
				tmpc = r.read();
			}
		}
		//eat exp
		if (tmpc == 'e' || tmpc == 'E') {
			tmpc = r.read();
			if (tmpc == '-') {
				epositive = false;
				tmpc = r.read();
			} else if (tmpc == '+') {
				tmpc = r.read();
			}
			if (tmpc == '0') {
				tmpc = r.read();
				if (tmpc <= '9' && tmpc >= '0') {
					throw new LexicalException("Illegal number format", r.dump().toString());
				}
			} else {
				while (tmpc >= '0' && tmpc <= '9') {
					exp *= 10;
					exp += tmpc - '0';
					tmpc = r.read();
				}
			}
		}
		Object[] o;
		o = new Object[2];
		if (epositive && !hasFrac) {
			Long nu = (positive ? 1 : -1) * i * (long) Math.pow(10, exp);
			if (nu == 1) {
				o[0] = JSONFactory.jsonL1;
			} else if (nu == 0) {
				o[0] = JSONFactory.jsonL0;
			}
			o[0] = new JsonNumber(nu);
		} else {
			o[0] = new JsonNumber((positive?1:-1)*(i+frac)*Math.pow(10, (epositive?1:-1)*exp));
		}
		o[1] = tmpc;
		return o;
	}
	
	private static JsonString eatString(CharInput r, int strQuote) throws LexicalException, IOException {
		String realString="";
		int charCode;
		int in;
		in = r.read();
		while (in != strQuote) {
			if (in == '\\') {
				int in2;
				in2 = r.read();
				switch (in2) {
					case 'u':
						charCode = getUnicode(r);
						if (charCode < 0) {
							throw new LexicalException("Illegel Unicode", r.dump().toString());
						}
						realString += (char) charCode;
						break;
					case '"':
					case '\'':
					case '/':
					case '\\':
						realString += (char) in2;
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
						throw new LexicalException("Illegal escape character", r.dump().toString());
				}
			} else if (in == -1){
				throw new LexicalException("Ending quotation mark missing for string", r.dump().toString());
			} else {
				realString += (char)in;
			}
			in = r.read();
		}
		return new JsonString(realString);
	}
	
	private static boolean isSpace(int c) {
		return c ==' ' || c == '\t' || c == '\n' || c == '\r';
	}
	
	private static int hexCh2int(int c) {
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
	private static int getUnicode(CharInput r) throws IOException {
		int i,h;
		int charCode = 0;
		for (i = 0; i < 4; i++) {
			h = hexCh2int(r.read());
			if (h < 0) {
				return -1;
			}
			charCode += h * (0x1000 >> i);
		}
		return charCode;
	}
}

final class JsonToken {
	
	final int tokenType;
	
	final JSON jsonObj;
	final DumpObj dmp;
	
	static JsonToken STRING = JsonToken.string(null, null);
	static JsonToken NUMBER = JsonToken.number(null, null);
	static JsonToken BOOLEAN = JsonToken.bool(null, null);
	static JsonToken NULL = JsonToken.empty(null);
	static JsonToken LEFT_BRACE = JsonToken.leftBrace(null);
	static JsonToken LEFT_BRACKET = JsonToken.leftBracket(null);
	static JsonToken RIGHT_BRACKET = JsonToken.rightBracket(null);
	static JsonToken RIGHT_BRACE = JsonToken.rightBrace(null);
	static JsonToken COLON = JsonToken.colon(null);
	static JsonToken COMMA = JsonToken.comma(null);
	
	private JsonToken(int i, DumpObj dump) {
		this(i, null,dump);
	}
	
	private JsonToken(int i, JSON js, DumpObj dump) {
		tokenType = i;
		jsonObj = js;
		dmp = dump;
	}
	
	public boolean typeEquals(Object obj) {
		if (obj.getClass() == Integer.class) {
			return tokenType == (int)(Integer)obj;
		} else if (obj.getClass() == JsonToken.class) {
			return tokenType == ((JsonToken)obj).tokenType;
		}
		return false;
	}
	
	public DumpObj dump() {
		return dmp;
	}
	
	static JsonToken string(JsonString js, DumpObj dump) {
		return new JsonToken(0, js, dump);
	}
	
	static JsonToken number(JsonNumber js, DumpObj dump) {
		return new JsonToken(1, js, dump);
	}
	
	static JsonToken empty(DumpObj dump) {
		return new JsonToken(3, JSONFactory.jsonNull, dump);
	}
	
	static JsonToken bool(Boolean b, DumpObj dump) {
		return new JsonToken(5, b ? JSONFactory.jsonTrue : JSONFactory.jsonFalse, dump);
	}
	
	static JsonToken leftBrace(DumpObj dump) {
		return new JsonToken(6, dump);
	}
	
	static JsonToken rightBrace(DumpObj dump) {
		return new JsonToken(7, dump);
	}
	
	static JsonToken leftBracket(DumpObj dump) {
		return new JsonToken(8, dump);
	}
	
	static JsonToken rightBracket(DumpObj dump) {
		return new JsonToken(9, dump);
	}
	
	static JsonToken colon(DumpObj dump) {
		return new JsonToken(10, dump);
	}
	
	static JsonToken comma(DumpObj dump) {
		return new JsonToken(11, dump);
	}
}

class CharInput {
	
	private final Reader r;
	
	long ln;
	long pos;
	long ch;
	
	final int past[];
	int head;
	final int  max;
	final int  min;
	
	{
		ln = 0;
		pos = 0;
		ch = 0;
		
		head = 0;
	}
	
	CharInput(Reader reader, int bufferSize, int minOutput) {
		
		if (minOutput >= bufferSize) {
			throw new IllegalArgumentException("CharInput: bufferSize should be greater than minOutput");
		}
		
		if (reader.markSupported()) {
			r = reader;
		} else {
			r = new BufferedReader(reader, bufferSize + 10);
		}
		
		max = bufferSize;
		min = minOutput;
		past = new int[max];
		for (int i = 0; i < max; ++i) {
			past[i] = -1;
		}
	}
	
	CharInput(Reader reader) {
		this(reader, 64, 20);
	}
	
	int read() throws IOException {
		
		int ret;
		ret = r.read();
		
		if (ret == -1) {
			return -1;
		}
		
		if (ret == '\n') {
			ln++;
			pos = 0;
		} else {
			pos++;
		}
		
		ch++;

		past[head] = ret;
		head = (head + 1) % max;
		
		return ret;
	}
	
	private static boolean isValidChar(int i) {
		return (i <= '9' && i >= '0') || i == '_' || (i <= 'z' && i >= 'a') || (i <= 'Z' && i >= 'A');
	}
	
	private int getPastChar(int i) {
		int index = head - i - 1;
		if (index < 0) {
			index = index + max;
		}
		return past[index];
	}
	
	private static char getPrintable(int i) {
		if (i == '\n' || i == '\r' || i == '\t') {
			return ' ';
		}
		if (i > 127 || i < 20) {
			return '?';
		}
		return (char) i;
	}
	
	private String lookback() {
		
		StringBuilder sb;
		boolean status;
		int key, count;
		
		sb = new StringBuilder("");
		status = false;
		key = 0;
		count = 0;
		
		int tmp;
		while ((key < 2 || count < min) && (tmp = getPastChar(count)) != -1 && count < max) {
			count++;
			if (isValidChar(tmp)) {
				status = true;
			} else {
				if (status == true) {
					key++;
				}
				status = false;
			}
		}
		
		while (count >= 0) {
			count--;
			tmp = getPastChar(count);
			sb.append(getPrintable(tmp));
		}
		
		return sb.toString();
	}
	
	private String lookahead() throws IOException {

		StringBuilder sb;
		boolean status;
		int key, count;
		
		sb = new StringBuilder("");
		status = false;
		key = 0;
		count = 0;

		r.mark(max);
		
		int tmp;
		while ((key < 2 || count < min) && (tmp = r.read()) != -1 && count < max) {
			count++;
			if (isValidChar(tmp)) {
				status = true;
			} else {
				if (status == true) {
					key++;
				}
				status = false;
			}
			sb.append(getPrintable(tmp));
		}

		r.reset();
		
		return sb.toString();
	}
	
	DumpObj dump() throws IOException {
		return new DumpObj(ln, pos, ch, lookback(), lookahead());
	}
}

final class DumpObj {
	final long ln;
	final long pos;
	final long ch;
	final String back;
	final String ahead;
	
	DumpObj(long line, long position, long charactor, String backString, String aheadString) {
		ln = line;
		pos = position;
		ch = charactor;
		back = backString;
		ahead = aheadString;
	}
	
	final private static void appendPlaceHolder(StringBuilder sb, int size) {
		while (size > 0) {
			sb.append('=');
			size--;
		}
	}
	
	@Override
	final public String toString() {
		StringBuilder sb;
		
		sb = new StringBuilder("At line ");
		
		sb.append(ln).append(" pos ").append(ch).append(", charactor ").append(ch).append(":\n");
		sb.append(back);
		appendPlaceHolder(sb, ahead.length());
		sb.append('\n');
		appendPlaceHolder(sb, back.length());
		sb.append(ahead).append('\n');
		
		return sb.toString();
	}
}