package us.kbase.common.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * A formatter/parser for dates in the format "yyyy-MM-dd'T'HH:mm:ssZ".
 * Date strings are always returned in UTC (+0000).
 * 
 * @author gaprice@lbl.gov
 * 
 * @see SimpleDateFormat
 *
 */
public class UTCDateFormat {
	
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
}
