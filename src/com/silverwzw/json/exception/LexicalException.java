package com.silverwzw.json.exception;

@SuppressWarnings("serial")
public final class LexicalException extends JsonStringFormatException {
	public LexicalException(String error, String inputSlice) {
		super(error, inputSlice);
	};
}