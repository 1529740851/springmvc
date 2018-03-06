package com.jz.service;

import com.jz.anntation.JzService;

@JzService
public class CeshiService implements ICeshiService {

	public String getName(String name){
		return "我叫"+name;
		
	}

	public String getNameAge(String name, String age) {
		return "我叫"+name+"今年"+age+"岁";
	}

	
}
