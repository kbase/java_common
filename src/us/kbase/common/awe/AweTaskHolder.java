package us.kbase.common.awe;

import java.lang.reflect.Method;
import java.util.Arrays;

import us.kbase.common.service.UObject;

import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;

public class AweTaskHolder {

	@SuppressWarnings("unchecked")
	public <T> T prepareTask(Class<T> type, final String token) throws Exception {
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
	            String className = method.getDeclaringClass().getName();
	            String methodName = method.getName();
	            String inputJson = UObject.transformObjectToString(Arrays.asList(args));
	            String jobId = "12345";
	            JavaGenericScript.run(className, methodName, inputJson, jobId, token);
	            return null;
	        }
	    };
	    return (T)factory.create(new Class<?>[0], new Object[0], handler);
	}
}
