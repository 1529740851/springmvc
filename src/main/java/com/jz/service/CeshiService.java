package com.jz.service;

import com.jz.anntation.JzService;

@JzService
public class CeshiService implements ICeshiService {

	public String getName(String name){
		return "�ҽ�"+name;
		
	}

	public String getNameAge(String name, String age) {
		return "�ҽ�"+name+"����"+age+"��";
	}

	
}
