package com.jz.springmvc;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.jz.anntation.JzAutowired;
import com.jz.anntation.JzController;
import com.jz.anntation.JzRequestMapping;
import com.jz.anntation.JzRequestParam;
import com.jz.service.ICeshiService;

@JzController
@JzRequestMapping("/jiang")
public class SpringController {

	
@JzAutowired
private ICeshiService ceshiService;
	
	@JzRequestMapping("/xx")
	public void get(HttpServletRequest req, HttpServletResponse resp,@JzRequestParam(value="name")String name ){
	resp.setHeader("Content-type", "text/html;charset=UTF-8");  
	String name1=ceshiService.getName(name);
	try {
		resp.getWriter().write(name1);
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	
	}
	
	@JzRequestMapping("/xx2")
	public void get2(HttpServletRequest req, HttpServletResponse resp,String name ){
	resp.setHeader("Content-type", "text/html;charset=UTF-8");  
	String name1=ceshiService.getName(name);
	try {
		resp.getWriter().write(name1);
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	
	}
	
}
