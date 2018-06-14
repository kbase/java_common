package us.kbase.common.utils;

import java.nio.charset.Charset;

/**
 * Miscellaneous methods for dealing with strings.
 *
 * @author gaprice@lbl.gov
 *
 */
public class StringUtils {
    private static final String HEXES = "0123456789ABCDEF";

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

    public static String stringToHex(String text) {
        return bytesToHex(text.getBytes(Charset.forName("utf-8")));
    }

    public static String hexToString(String hex) {
        return new String(hexToBytes(hex), Charset.forName("utf-8"));
    }

    public static String bytesToHex(byte[] raw) {
        final StringBuilder hex = new StringBuilder(2 * raw.length);
        for (final byte b : raw)
            hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(HEXES.charAt((b & 0x0F)));
        return hex.toString();
    }

    public static byte[] hexToBytes(String hex) {
        byte[] ret = new byte[hex.length() / 2];
        for (int i = 0; i < ret.length; i++)
            ret[i] = Byte.parseByte(hex.substring(i * 2, (i + 1) * 2), 16);
        return ret;
    }
}
