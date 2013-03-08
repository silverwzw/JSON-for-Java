package com.silverwzw.JSON.test;

import java.util.ArrayList;

public class TestObject {
	public int i1,i2,ai1[],aai[][];
	public ArrayList<Object> list;
	public String s;
	public boolean b;
	TestObject() {
		int ai2[] = {10,11,12};
		i1 = 1;
		i2 = 2;
		ai1 = new int[2];
		aai = new int[2][2];
		aai[0][1] = 1;
		aai[0][0] = 0;
		aai[1][1] = 11;
		aai[1][0] = 10;
		ai1[0]=3;
		ai1[1]=4;
		b = false;
		list = new ArrayList<Object>();
		list.add(new ArrayList<Boolean>());
		list.add(ai2);
		list.add(null);
		list.add(5.5);
		s = "\tabc\n\n\\";
	}
}