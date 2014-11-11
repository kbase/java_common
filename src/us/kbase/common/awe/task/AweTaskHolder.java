package us.kbase.common.awe.task;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import us.kbase.common.awe.AweClient;
import us.kbase.common.awe.AwfEnviron;
import us.kbase.common.awe.AwfTemplate;
import us.kbase.common.service.UObject;
import us.kbase.common.utils.StringUtils;

import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;

public class AweTaskHolder {
	private String aweUrl;
	private String serviceJar;
	private Map<String, String> config;

	public AweTaskHolder(String serverJar, Map<String, String> config) {
		this(config.get("awe.profile"), config.get("awe.url"), serverJar, config);
	}
	
	public AweTaskHolder(String isProductionAwe, String aweUrl, String serviceJar, Map<String, String> config) {
		this.aweUrl = aweUrl;
		if (this.aweUrl == null) {
			if (isProductionAwe == null)
				isProductionAwe = "true";
			this.aweUrl = Boolean.parseBoolean(isProductionAwe) ? AweClient.DEFAULT_PROD_SERVER_URL : 
				AweClient.DEFAULT_DEV_SERVER_URL;
		}
		this.serviceJar = serviceJar;
		this.config = config;
		if (this.config == null)
			this.config = new TreeMap<String, String>();
	}
	
	@SuppressWarnings("unchecked")
	public <T> T prepareTask(final Class<T> type, final String token) throws Exception {
		ProxyFactory factory = new ProxyFactory();
	    factory.setSuperclass(type);
	    factory.setFilter(new MethodFilter() {
	    	@Override
	    	public boolean isHandled(Method arg0) {
	    		return true;
	    	}
	    });
	    MethodHandler handler = new MethodHandler() {
	        @Override
	        public Object invoke(Object self, Method method, Method proceed, Object[] args) throws Throwable {
	        	if (!(self instanceof AweJobInterface))
	        		throw new IllegalStateException("Task object doesn't implement " + AweJobInterface.class.getName());
	        	try {
	        		AweJobInterface.class.getMethod(method.getName(), method.getParameterTypes());
	        		return proceed.invoke(self, args);
	        	} catch (NoSuchMethodException ignore) {
	        	}
	        	if (!method.getReturnType().equals(String.class))
	        		throw new IllegalStateException("Return type of remote method [" + method.getName() + "] should be " +
	        				"a string for pushing job id back");
	        	AweJobInterface task = (AweJobInterface)self;
	        	String descr = task.getDescription();
	        	if (descr == null)
	        		descr = "No descritpion";
	        	String initProgressJson = task.getInitProgess();
	        	if (initProgressJson == null)
	        		initProgressJson = "none";
	        	Long time = task.getEstimatedFinishTime();
	        	String estComplete = time == null ? null : new SimpleDateFormat("YYYY-MM-DDThh:mm:ssZ").format(new Date(time));
	        	String jobId = task.getJobStatuses().createAndStartJob(token, "queued", descr, initProgressJson, estComplete);
	            String className = method.getDeclaringClass().getName();
	            String methodName = method.getName();
	            String inputJson = UObject.transformObjectToString(Arrays.asList(args));
	            String configJson = UObject.transformObjectToString(config);
	            String jarList = getJarList(serviceJar);
	            //System.out.println("Jar list: " + jarList);
	            //String classPath = JavaGenericScript.prepare(jarList);
	            //System.out.println("Classpath: " + classPath);
	            //JavaGenericScript.run(jobId, className, methodName, inputJson, configJson, token);
	    		AweClient client = new AweClient(AweClient.DEFAULT_DEV_SERVER_URL);
	    		String aweArgs = jarList + " " + jobId + " " + className + " " + methodName + " " + 
	    				StringUtils.stringToHex(inputJson) + " " + StringUtils.stringToHex(configJson);
	    		if (token != null)
	    			aweArgs += " KB_AUTH_TOKEN";
	    		AwfTemplate job = AweClient.createSimpleJobTemplate(type.getSimpleName(), method.getName(), 
	    				 aweArgs, "java_generic_script");
	    		AwfEnviron env = new AwfEnviron();
	    		if (token != null)
	    			env.getPrivate().put("KB_AUTH_TOKEN", token);
	    		job.getTasks().get(0).getCmd().setEnviron(env);
	    		client.submitJob(job);
	            return jobId;
	        }
	    };
	    return (T)factory.create(new Class<?>[0], new Object[0], handler);
	}
	
	private static String getJarList(String serviceJar) {
		Set<String> set = new LinkedHashSet<String>();
		if (serviceJar != null) {
			set.add(serviceJar);
		}
		for (URL url : ((URLClassLoader)(Thread.currentThread().getContextClassLoader())).getURLs()) {
			String path = url.getPath();
			if (path.endsWith(".jar")) {
				String jarName = path.substring(path.lastIndexOf('/') + 1);
				set.add(jarName);
			}
		}
		StringBuilder ret = new StringBuilder();
		for (String path : set) {
			if (ret.length() > 0)
				ret.append(':');
			ret.append(path);
		}
		return ret.toString();
	}
}
