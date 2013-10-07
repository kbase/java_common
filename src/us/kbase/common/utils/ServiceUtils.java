package us.kbase.common.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public class ServiceUtils {

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

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

	public static String formatDate(final Date d) {
		if (d == null) {
			return null;
		}
		return DATE_FORMAT.format(d);
	}
}
