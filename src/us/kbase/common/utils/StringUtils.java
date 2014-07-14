package us.kbase.common.utils;

/**
 * Miscellaneous methods for dealing with strings.
 * 
 * @author gaprice@lbl.gov
 *
 */
public class StringUtils {
	
	/**
	 * Checks that a string is neither null or empty.
	 * @param s the string to check.
	 * @param sname the name of the variable. This name will be used in the 
	 * thrown exception.
	 * @throws IllegalArgumentException if the string is null or empty.
	 */
	public static void checkString(final String s, final String sname) {
		checkString(s, sname, -1);
	}
	
	/**
	 * Checks that a string is neither null or empty and is below a maximum
	 * length.
	 * @param s the string to check.
	 * @param sname the name of the variable. This name will be used in the 
	 * thrown exception.
	 * @param length the maximum allowed length of the string. If 0 or less,
	 * no checking is performed.
	 * @throws IllegalArgumentException if the string is null, empty, or
	 * longer than the maximum length allowed.
	 */
	public static void checkString(final String s, final String sname,
			final int length) {
		if (s == null || s.isEmpty()) {
			throw new IllegalArgumentException(sname + 
					" cannot be null or the empty string");
		}
		if (length > 0 && s.length() > length) {
			throw new IllegalArgumentException(sname + 
					" exceeds the maximum length of " + length);
		}
	}
	
	/**
	 * Checks that a string is below a maximum length. Null or empty strings
	 * are allowed.
	 * @param s the string to check.
	 * @param sname the name of the variable. This name will be used in the 
	 * thrown exception.
	 * @param length the maximum allowed length of the string.
	 * @throws IllegalArgumentException if the string is longer than the
	 * maximum length allowed.
	 */
	public static void checkMaxLen(final String s, final String sname,
			final int length) {
		if (s != null) {
			if (s.length() > length) {
				throw new IllegalArgumentException(sname + 
						" exceeds the maximum length of " + length);
			}
		}
	}
	
	/**
	 * Checks that a string is below a maximum length and not empty. Null
	 * strings are allowed.
	 * @param s the string to check.
	 * @param sname the name of the variable. This name will be used in the 
	 * thrown exception.
	 * @param length the maximum allowed length of the string.
	 * @throws IllegalArgumentException if the string is longer than the
	 * maximum length allowed or is empty.
	 */
	public static void checkMaxLenAndNonEmpty(final String s,
			final String sname, final int length) {
		if (s != null) {
			if (s.isEmpty()) {
				throw new IllegalArgumentException(sname + 
						" cannot be the empty string");
			}
			if (s.length() > length) {
				throw new IllegalArgumentException(sname + 
						" exceeds the maximum length of " + length);
			}
		}
	}
}
