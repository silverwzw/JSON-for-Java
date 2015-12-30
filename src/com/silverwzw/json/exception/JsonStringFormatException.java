package com.silverwzw.json.exception;

@SuppressWarnings("serial")
public class JsonStringFormatException extends IllegalArgumentException {
	public JsonStringFormatException(String error, String inputSlice) {
		super(error + ", " + inputSlice);
	}
	public JsonStringFormatException(String s) { super(s); };
}