package com.silverwzw.json.exception;

@SuppressWarnings("serial")
public final class ParsingException extends JsonStringFormatException {
	public ParsingException(String error, String inputSlice) {
		super(error, inputSlice);
	}
	public ParsingException(String error) {
		super(error);
	}
}