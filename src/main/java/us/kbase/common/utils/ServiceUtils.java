package us.kbase.common.utils;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

/**
 * Miscellaneous code snippets common to java services.
 * 
 * @author gaprice@lbl.gov
 *
 */
public class ServiceUtils {

	/**
	 * Throws an exception if there are additional arguments in a java
	 * typecompiler compiled class instance.
	 * @param addlargs the getAdditionalProperties() output from a java 
	 * typecompiler generated class.
	 * @param clazz the class of the java typecompiler generated class.
	 */
	public static void checkAddlArgs(final Map<String, Object> addlargs,
			@SuppressWarnings("rawtypes") final Class clazz) {
		if (addlargs.isEmpty()) {
			return;
		}
		throw new IllegalArgumentException(String.format(
				"Unexpected arguments in %s: %s",
				clazz.getName().substring(clazz.getName().lastIndexOf(".") + 1),
				StringUtils.join(addlargs.keySet(), " ")));
	}
}
