package us.kbase.common.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * A formatter/parser for dates in the format "yyyy-MM-dd'T'HH:mm:ssZ".
 * Date strings are always returned in UTC (+0000). Wraps SimpleDateFormat,
 * which is not thread safe, so neither is this.
 * 
 * @author gaprice@lbl.gov
 * 
 * @see SimpleDateFormat
 *
 */
public class UTCDateFormat {
	
	//TODO unit tests
	//TODO allow milliseconds (or at least test if MS are allowed
	
	private final SimpleDateFormat format =
			new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

	/**
	 * The sole constructor.
	 */
	public UTCDateFormat() {
		format.setTimeZone(TimeZone.getTimeZone("UTC"));
		format.setLenient(false);
	}
	
	/**
	 * Formats a date.
	 * @param d the date.
	 * @return the formatted date.
	 */
	public String formatDate(final Date d) {
		if (d == null) {
			return null;
		}
		return format.format(d);
	}
	
	/**
	 * Parses a date.
	 * @param d the date string.
	 * @return a date.
	 * @throws ParseException if the date cannot be parsed.
	 * @throws IllegalArgumentException if the date is null.
	 */
	public Date parseDate(final String d) throws ParseException {
		if (d == null) {
			throw new IllegalArgumentException("date string cannot be null");
		}
		return format.parse(d);
	}
	
	public static void main(String[] args) throws Exception {
		String date = "2014-06-26T21:32:55+0000";
		System.out.println(new UTCDateFormat().parseDate(date));
		String date2 = "2014-06-26T21:32:54+0000";
		System.out.println(new UTCDateFormat().parseDate(date2));
	}
}
