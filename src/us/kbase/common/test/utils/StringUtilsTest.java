package us.kbase.common.test.utils;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import us.kbase.common.utils.StringUtils;

public class StringUtilsTest {

	//TODO tests for the entire repo, although these are tested via integration tests
	
	@Test
	public void maxlen() throws Exception {
		StringUtils.checkMaxLen(null, "foo", -1);
		StringUtils.checkMaxLen("a", "foo", 1);
		failMaxLen("", "foo", -1, new IllegalArgumentException("foo exceeds the maximum length of -1"));
		failMaxLen("aa", "foo", 1, new IllegalArgumentException("foo exceeds the maximum length of 1"));
	}
	
	private void failMaxLen(String string, String name, int len, Exception e) {
		try {
			StringUtils.checkMaxLen(string, name, len);
		} catch (Exception exp) {
			assertThat("correct exception", exp.getLocalizedMessage(),
					is(e.getLocalizedMessage()));
			assertThat("correct exception type", exp, is(e.getClass()));
		}
	}
}
