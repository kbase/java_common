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
	public static void isNonEmptyString(final String s, final String sname) {
		if (s == null || s.isEmpty()) {
			throw new IllegalArgumentException(sname + 
					" cannot be null or the empty string");
		}
	}

}
