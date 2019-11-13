package us.kbase.common.test.service;

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
		// this test fails with the env var set
//		getenv().put("KB_SDK_LOGGER_TARGET_PACKAGE", "testlogger");
		// test that passing a null config param works
		final JsonServerSyslog l = new JsonServerSyslog("serv", null);
		assertThat("incorrect service name", l.getServiceName(), is("serv"));
		
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
	
	@Test
	public void getTargetPackageName() throws Exception {
		try {
			checkPackageName("us.kbase");
			// nulls aren't allowed in env
			getenv().put(JsonServerSyslog.ENV_VAR_PACKAGE_NAME, "   ");
			checkPackageName("us.kbase");
			getenv().put(JsonServerSyslog.ENV_VAR_PACKAGE_NAME, "   mypackage.foo   ");
			checkPackageName("mypackage.foo");
		} finally {
			getenv().remove(JsonServerSyslog.ENV_VAR_PACKAGE_NAME);
		}
	}

	private void checkPackageName(final String expectedPackage) {
		final JsonServerSyslog l = new JsonServerSyslog("serv", "fake");
		assertThat("incorrect package", l.getTargetPackageName(), is(expectedPackage));
	}
	
}
