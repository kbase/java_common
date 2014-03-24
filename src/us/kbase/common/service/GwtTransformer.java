package us.kbase.common.service;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GwtTransformer {
	@SuppressWarnings("unchecked")
	public static <T> T transform(Object source, Class<T> targetClass) throws Exception {
		return (T)transform(source, targetClass.getPackage().getName(), targetClass.getClassLoader());
	}

	public static Object transform(Object source, String targetPackage) throws Exception {
		return transform(source, targetPackage, null);
	}
	
	@SuppressWarnings("unchecked")
	public static Object transform(Object source, String targetPackage, ClassLoader cl) throws Exception {
		if (source == null) {
			return source;
		} else if (source.getClass().getName().startsWith("java.lang.")) {
			return source;
		} else if (source instanceof List) {
			List<Object> ret = new ArrayList<Object>();
			for (Object obj : ((List<Object>)source)) {
				ret.add(transform(obj, targetPackage));
			}
			return ret;
		} else if (source instanceof Map) {
			Map<String, Object> ret = new LinkedHashMap<String, Object>();
			for (Map.Entry<String, Object> entry : ((Map<String, Object>)source).entrySet()) {
				ret.put(entry.getKey(), transform(entry.getValue(), targetPackage));
			}
			return ret;
		} else {
			String retClassName = targetPackage + "." + source.getClass().getSimpleName() + "GWT";
			Class<?> retClass = cl == null ? Class.forName(retClassName) : cl.loadClass(retClassName);
			Object ret = retClass.newInstance();
			Map<String, Method> getters = getMethodMap(source.getClass(), "get");
			Map<String, Method> setters = getMethodMap(retClass, "set");
			for (Map.Entry<String, Method> entry : getters.entrySet()) {
				Method setter = setters.get(entry.getKey());
				if (setter != null) {
					Method getter = entry.getValue();
					Object val = transform(getter.invoke(source), targetPackage);
					setter.invoke(ret, val);
				}
			}
			return ret;
		}
	}
	
	private static Map<String, Method> getMethodMap(Class<?> type, String prefix) {
		Map<String, Method> ret = new HashMap<String, Method>();
		for (Method m : type.getMethods()) {
			if (Modifier.isStatic(m.getModifiers()) || !Modifier.isPublic(m.getModifiers()))
				continue;
			String key = m.getName();
			if (!key.startsWith(prefix))
				continue;
			ret.put(key.substring(prefix.length()), m);
		}
		return ret;
	}
}
