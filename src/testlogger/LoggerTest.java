package testlogger;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.productivity.java.syslog4j.SyslogIF;

import us.kbase.common.service.JsonServerSyslog;
import us.kbase.common.service.JsonServerSyslog.SyslogOutput;

public class LoggerTest {

	// test that the logger doesn't throw an exception when started from a non- us.kbase package.
	
	//http://quirkygba.blogspot.com/2009/11/setting-environment-variables-in-java.html
	@SuppressWarnings("unchecked")
	private static Map<String, String> getenv() throws Exception {
		Map<String, String> unmodifiable = System.getenv();
		Class<?> cu = unmodifiable.getClass();
		Field m = cu.getDeclaredField("m");
		m.setAccessible(true);
		return (Map<String, String>) m.get(unmodifiable);
	}
	
	@Test
	public void checkLoggerWorks() throws Exception {
		// this test fails without the env var set
		getenv().put("KB_SDK_LOGGER_TARGET_PACKAGE", "testlogger");
		// test that passing a null config param works
		final JsonServerSyslog l = new JsonServerSyslog("serv", null);
		final List<String> logout = new LinkedList<>();
		
		l.changeOutput(new SyslogOutput() {
			
			@Override
			public void logToSystem(
					final SyslogIF log,
					final int level,
					final String message) {
				logout.add(message);
			}
		});
		
		l.logErr("foo");
		
		assertThat("incorrect size", logout.size(), is(1));
		// upgrade hamcrest(?) to get containsString matcher. Don't worry about it for now
		assertThat("incorrect log", logout.get(0).contains("[serv] [ERR]"), is(true));
		assertThat("incorrect log", logout.get(0).contains("[" + getClass().getName() + "]"),
				is(true));
		assertThat("incorrect log", logout.get(0).contains("[-] [-] [-] [-] [-]: foo"), is(true));
	}
	
}
