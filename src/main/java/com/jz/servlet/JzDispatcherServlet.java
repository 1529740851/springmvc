package com.jz.servlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;



import com.jz.anntation.JzAutowired;
import com.jz.anntation.JzController;
import com.jz.anntation.JzRequestMapping;
import com.jz.anntation.JzRequestParam;
import com.jz.anntation.JzService;


public class JzDispatcherServlet extends HttpServlet {

	private Properties pconfig = new Properties();
	private List<String> classNames = new ArrayList<String>();
	private Map<String, Object> ioc = new HashMap<String, Object>();
	// private Map<String,Method> handlerMapping=new HashMap<String, Method>();
	private List<Handler> handlerMapping = new ArrayList<Handler>();

	@Override
	public void init(ServletConfig config) throws ServletException {
		// 1.加载配置文件
		doConfig(config.getInitParameter("contextConfigLocation"));
		// 2.初始化所有相关的类，扫描用户下所有的类
		doScanner(pconfig.getProperty("scanPackage"));
		// 3.通过反射机制 实现初始化,放到IOC容器中
		doInstance();
		// 4.实现依赖注入
		doAutowried();
		// 5.初始化HandlerMapping
		initHandlerMapping();
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		this.doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			doDispatch(req, resp);
		} catch (Exception e) {
			resp.getWriter().write("500" + Arrays.toString(e.getStackTrace()));
		}

	}

	private void doDispatch(HttpServletRequest req, HttpServletResponse resp)
			throws IOException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		String url = req.getRequestURI();
		System.out.println("url "+url);
		String contextpath = req.getContextPath();
		url = url.replace(contextpath, "").replaceAll("/+", "/");
		Handler handler = getHandler(url);
		if (handler == null) {
			resp.getWriter().write("404 Not found");
			return;
		}
		try {
		 System.out.println(handler.getController());
		 Method method = handler.getMethod();
	     Class<?>[] parametype=method.getParameterTypes();
	     Object [] paramevalues=new Object[parametype.length];
	     Map<String, String[]> parames=req.getParameterMap();
	     for (Entry<String, String[]> entry : parames.entrySet()) {
	    	 System.out.println(entry.getValue()+"parame");
			String value=Arrays.toString(entry.getValue()).replaceAll("\\[|\\]", "").replaceAll(",\\s", ",");
			if(!handler.paramMap.containsKey(entry.getKey())){continue;}
			int index=handler.paramMap.get(entry.getKey());
			paramevalues[index]=convert(parametype[index],value);
	     }
	     System.out.println(HttpServletRequest.class.getName());
	     int reqIndex=handler.paramMap.get(HttpServletRequest.class.getName());
	     paramevalues[reqIndex]=req;
	     int respIndex=handler.paramMap.get(HttpServletResponse.class.getName());
	     paramevalues[respIndex]=resp;
		System.out.println(method.getName());
	
			method.invoke(handler.getController(), paramevalues);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	private Object convert(Class<?> type,String value){
		if(Integer.class==type){
			return Integer.valueOf(value);
		}
		return value;
	}
	private Handler getHandler(String url) {
		if (handlerMapping.isEmpty()) {
			return null;
		}
		for (Handler handler : handlerMapping) {
			if (!url.equals(handler.getUrl())) {
				continue;
			}
			return handler;
		}
		return null;
	}

	private void initHandlerMapping() {
		if (ioc.isEmpty()) {
			return;
		}
		for (Entry<String, Object> entry : ioc.entrySet()) {
			Class<?> clazz = entry.getValue().getClass();
			if (!clazz.isAnnotationPresent(JzController.class)) {
				continue;
			}
			String baseUrl = "";
			if (clazz.isAnnotationPresent(JzRequestMapping.class)) {
				JzRequestMapping jzrequestMapping = clazz.getAnnotation(JzRequestMapping.class);
				baseUrl = jzrequestMapping.value();
			}
			Method[] methods = clazz.getMethods();
			for (Method method : methods) {
				if (!method.isAnnotationPresent(JzRequestMapping.class)) {
					continue;
				}
				JzRequestMapping jzrequestMapping = method.getAnnotation(JzRequestMapping.class);
				String url = jzrequestMapping.value();
				url = (baseUrl + "/" + url).replaceAll("/+", "/");

				handlerMapping.add(new Handler(url, method, entry.getValue()));
				System.out.println("Mapping+---------" + url + "," + method);
			}
		}
	}

	private void doAutowried() {
		if (ioc.isEmpty()) {
			return;
		}
		for (Entry<String, Object> entry : ioc.entrySet()) {
			// getFidlds()只能获取public值
			Field[] fields = entry.getValue().getClass().getDeclaredFields();
			for (Field field : fields) {
				if (!field.isAnnotationPresent(JzAutowired.class)) {
					continue;
				}
				JzAutowired autowirid = field.getAnnotation(JzAutowired.class);
				String beanName = autowirid.value();
				if ("".equals(beanName)) {
					beanName = field.getType().getName();
				}
				//设置可以访问私有属性
				field.setAccessible(true);
				try {
					System.out.println(beanName);
					System.out.println(entry.getValue()+"222");
					field.set(entry.getValue(), ioc.get(beanName));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		}

	}

	private void doInstance() {
		for (String classname : classNames) {
			try {
				Class<?> clazz = Class.class.forName(classname);
				// 用反射实现实例化
				if (clazz.isAnnotationPresent(JzController.class)) {
					String beanName = lowerFirst(clazz.getSimpleName());
					ioc.put(beanName, clazz.newInstance());
				} else if (clazz.isAnnotationPresent(JzService.class)) {
					// 1:自己设置了名字
					JzService service = clazz.getAnnotation(JzService.class);
					String beanName = service.value();
					// 2:没设置名字,设置名字首字母开头
					if ("".equals(beanName)) {
					    beanName = lowerFirst(clazz.getSimpleName());
					}
					ioc.put(beanName, clazz.newInstance());
					// 3:@Autowried是个接口注入
					Class<?>[] interfaces = clazz.getInterfaces();
					for (Class<?> i : interfaces) {
						ioc.put(i.getName(), clazz.newInstance());
					}

				} else {
					continue;
				}
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			}
		}
		for (Entry<String, Object> string : ioc.entrySet()) {
			System.out.println("beanName:  "+string.getKey());
		}
	}

	private void doScanner(String packageName) {
		URL url = this.getClass().getClassLoader().getResource("/" + packageName.replaceAll("\\.", "/"));
		File dir = new File(url.getFile());
		for (File file : dir.listFiles()) {
			if (file.isDirectory()) {
				doScanner(packageName + "." + file.getName());
			} else {
				String className = packageName + "." + file.getName().replace(".class", "");
				classNames.add(className);
			}
		}
	}

	private void doConfig(String location) {
		InputStream is = this.getClass().getClassLoader().getResourceAsStream(location);
		try {
			pconfig.load(is);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (null != is) {
					is.close();
				}
			} catch (Exception e2) {
				e2.printStackTrace();
			}

		}

	}

	public String lowerFirst(String name) {
		char[] chars = name.toCharArray();
		chars[0] += 32;
		return String.valueOf(chars);
	}

	@Override
	public void destroy() {
		super.destroy();
	}
}

class Handler {
	public Object controller;
	public Method method;
	public String url;
	public Map<String,Integer> paramMap;
	public Map<String, Integer> getParamMap() {
		return paramMap;
	}

	public void setParamMap(Map<String, Integer> paramMap) {
		this.paramMap = paramMap;
	}

	public Handler(String url, Method method, Object controller) {
		this.controller = controller;
		this.method = method;
		this.url = url;
		this.paramMap=new HashMap<String, Integer>();//参数名称和顺序
		putParamMap(method);
	
	}
	private  void putParamMap(Method method){
		Annotation[][] pa=method.getParameterAnnotations();
		//获取所有参数注解
		for (int i = 0; i < pa.length; i++) {
		for (Annotation a:pa[i]) {
			if(a instanceof JzRequestParam){
				String paramName=((JzRequestParam) a).value();
				if(!"".equals(paramName.trim())){//参数名字不是null
					paramMap.put(paramName, i);
				}
			}
			
		}
		}
		//如果参数为Request或者response参数
		Class<?> [] pareamType=method.getParameterTypes();
		for (int i = 0; i < pareamType.length; i++) {
			if(pareamType[i]==HttpServletRequest.class||pareamType[i]==HttpServletResponse.class){
				paramMap.put(pareamType[i].getName(), i);
			}
		}
	}
	public Object getController() {
		return controller;
	}

	public void setController(Object controller) {
		this.controller = controller;
	}

	public Method getMethod() {
		return method;
	}

	public void setMethod(Method method) {
		this.method = method;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

}