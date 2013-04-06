package com.silverwzw.JSON.test;
import com.silverwzw.JSON.*;

public class test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.print(JSON.parse("{\"a\" :{ \"x1\":5,      \"x2\"    :[\"aa\",\"bb\"]}   ,\"b\":[{\"y\":true},false]}").format());
	}
}