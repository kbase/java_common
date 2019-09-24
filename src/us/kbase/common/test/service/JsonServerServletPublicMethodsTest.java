package us.kbase.common.test.service;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashMap;
import java.util.Map;

import org.ini4j.Ini;
import org.ini4j.Profile.Section;
import org.junit.Test;
import org.productivity.java.syslog4j.SyslogIF;

import us.kbase.common.service.JsonServerServlet;
import us.kbase.common.service.JsonServerSyslog;
import us.kbase.common.service.JsonServerSyslog.SyslogOutput;

/** Tests some new public static methods in JsonServerServlet that can be
 *  reused in other services.
 * @author gaprice@lbl.gov
 *
 */
public class JsonServerServletPublicMethodsTest {

	@Test
	public void ipAddress() throws Exception {
		Map<String, String> config = new HashMap<String, String>();
		HttpServletRequestMock req = new HttpServletRequestMock();
		req.setIpAddress("000.000.000.001");
		assertThat("correct IP address", JsonServerServlet.getIpAddress(
				req, config), is("000.000.000.001"));
		
		req.setHeader("X-Real-IP", "");
		assertThat("correct IP address", JsonServerServlet.getIpAddress(
				req, config), is("000.000.000.001"));
		
		req.setHeader("X-Real-IP", "000.000.000.002");
		assertThat("correct IP address", JsonServerServlet.getIpAddress(
				req, config), is("000.000.000.002"));
		
		req.setHeader("X-Forwarded-For", "");
		assertThat("correct IP address", JsonServerServlet.getIpAddress(
				req, config), is("000.000.000.002"));
		
		req.setHeader("X-Forwarded-For", "000.000.000.003 , somecrap");
		assertThat("correct IP address", JsonServerServlet.getIpAddress(
				req, config), is("000.000.000.003"));
		
		config.put("dont_trust_x_ip_headers", "false");
		config.put("dont-trust-x-ip-headers", "tru");
		assertThat("correct IP address", JsonServerServlet.getIpAddress(
				req, config), is("000.000.000.003"));
		
		config.put("dont-trust-x-ip-headers", "true");
		assertThat("correct IP address", JsonServerServlet.getIpAddress(
				req, config), is("000.000.000.001"));
		
		config.put("dont_trust_x_ip_headers", "true");
		config.put("dont-trust-x-ip-headers", "tru");
		assertThat("correct IP address", JsonServerServlet.getIpAddress(
				req, config), is("000.000.000.001"));
		
		config.remove("X-Forwarded-For");
		assertThat("correct IP address", JsonServerServlet.getIpAddress(
				req, config), is("000.000.000.001"));
		
		config.put("dont_trust_x_ip_headers", "tru");
		config.put("dont-trust-x-ip-headers", "true");
		assertThat("correct IP address", JsonServerServlet.getIpAddress(
				req, config), is("000.000.000.001"));
	}
	
	//http://quirkygba.blogspot.com/2009/11/setting-environment-variables-in-java.html
	@SuppressWarnings("unchecked")
	private static Map<String, String> getenv() throws Exception {
		Map<String, String> unmodifiable = System.getenv();
		Class<?> cu = unmodifiable.getClass();
		Field m = cu.getDeclaredField("m");
		m.setAccessible(true);
		return (Map<String, String>) m.get(unmodifiable);
	}
	
	private static Path createTempFile() throws Exception {
		Path f = Files.createTempFile(
				"JsonServerServletPublicMethodsTest", "tempfile",
				PosixFilePermissions.asFileAttribute(
						PosixFilePermissions.fromString("rwx------")));
		f.toFile().deleteOnExit();
		return f;
	}
	
	class SysLogOutputMock extends SyslogOutput {
		
		public int lastSysLogInt = -1;
//		public int lastFileInt = -1;
		public String lastSysLogMsg = null;
//		public String lastFileMsg = null;
		
		@Override
		public void logToSystem(SyslogIF log, int level, String message) {
			lastSysLogInt = level;
			lastSysLogMsg = message;
		}
		
		@Override
		public PrintWriter logToFile(File f, PrintWriter pw, int level,
				String message) throws Exception {
			throw new UnsupportedOperationException();
//			lastFileInt = level;
//			lastFileMsg = message;
//			return null;
		}
		
		public void reset() {
			lastSysLogInt = -1;
			lastSysLogMsg = null;
		}
	}

	@Test
	public void getConfig() throws Exception {

		//set up config result maps
		Map<String, String> empty = new HashMap<String, String>();
		Map<String, String> expect = new HashMap<String, String>();
		expect.put("somekey", "somevalue");
		
		//set up ini file
		Path temp = createTempFile();
		Ini ini = new Ini(temp.toFile());
		Section sec = ini.add("ServerSection");
		sec.add("somekey", "somevalue");
		ini.store();

		//possible error strings
		String[] noFileErr = new String[2];
		noFileErr[0] = 
				"There was an IO Error reading the deploy file somenonexistantfilewhichshouldntexist. Traceback:\njava.io.FileNotFoundException:";
		noFileErr[1] = 
			"somenonexistantfilewhichshouldntexist (No such file or directory)";
		String[] noSecErr = new String[2];
		noSecErr[0] =  "The configuration file ";
		noSecErr[1] =  " has no section BadServerSection";
		
		//set up the logger and mock output
		JsonServerSyslog log = new JsonServerSyslog("foo", "foo");
		SysLogOutputMock mockout = new SysLogOutputMock();
		log.changeOutput(mockout);
		
		//test with null args
		try {
			JsonServerServlet.getConfig(null, log);
			fail("got config with null servername");
		} catch (NullPointerException npe) {
			assertThat("correct exception messsage", npe.getLocalizedMessage(),
					is("service name cannot be null"));
		}
		try {
			JsonServerServlet.getConfig("thing", null);
			fail("got config with null logger");
		} catch (NullPointerException npe) {
			assertThat("correct exception messsage", npe.getLocalizedMessage(),
					is("logger cannot be null"));
		}
		
		// test with no file specified in env or sys props
		Map<String, String> cfg = JsonServerServlet.getConfig("foo", log);
		
		assertThat("empty config", cfg, is(empty));
		assertThat("no logged error", mockout.lastSysLogInt,
				is(-1));
		assertThat("no logged message", mockout.lastSysLogMsg,
				is((String) null));
		
		// test with bad filename, set in sysprops in this case
		mockout.reset();
		System.getProperties().setProperty(JsonServerServlet.KB_DEP,
				"somenonexistantfilewhichshouldntexist");
		
		cfg = JsonServerServlet.getConfig("foo", log);
		
		assertThat("empty config", cfg, is(empty));
		assertThat("logged error correct", mockout.lastSysLogInt,
				is(JsonServerServlet.LOG_LEVEL_ERR));
		checkSyslogMsg(mockout.lastSysLogMsg, noFileErr[0], noFileErr[1]);
		
		//test with good file set in sys props
		mockout.reset();
		System.getProperties().setProperty(JsonServerServlet.KB_DEP,
				temp.toAbsolutePath().toString());
		
		cfg = JsonServerServlet.getConfig("ServerSection", log);
		
		assertThat("correct config", cfg, is(expect));
		assertThat("no logged error", mockout.lastSysLogInt, is(-1));
		assertThat("no logged msg", mockout.lastSysLogMsg, is((String) null));
		System.getProperties().remove(JsonServerServlet.KB_DEP);
		
		//test with good file set in env
		//also test trimming server name
		mockout.reset();
		getenv().put(JsonServerServlet.KB_DEP, temp.toAbsolutePath().toString());
		
		cfg = JsonServerServlet.getConfig("ServerSection:somestuff", log);
		
		assertThat("correct config", cfg, is(expect));
		assertThat("no logged error", mockout.lastSysLogInt, is(-1));
		assertThat("no logged msg", mockout.lastSysLogMsg, is((String) null));
		
		//test with a bad servername, file still set in env
		mockout.reset();
		
		cfg = JsonServerServlet.getConfig("BadServerSection", log);
		
		assertThat("correct config", cfg, is(empty));
		assertThat("logged error correct", mockout.lastSysLogInt,
				is(JsonServerServlet.LOG_LEVEL_ERR));
		checkSyslogMsg(mockout.lastSysLogMsg,
				noSecErr[0] + temp.toAbsolutePath().toString(), noSecErr[1]);
		
		//test with servername set in sysprops, file still set in env
		mockout.reset();
		System.getProperties().setProperty(JsonServerServlet.KB_SERVNAME, "ServerSection");

		cfg = JsonServerServlet.getConfig("BadServerSection", log);
		
		assertThat("correct config", cfg, is(expect));
		assertThat("no logged error", mockout.lastSysLogInt, is(-1));
		assertThat("no logged msg", mockout.lastSysLogMsg, is((String) null));
		System.getProperties().remove(JsonServerServlet.KB_SERVNAME);
		
		//test with servername set in env, file still set in env
		mockout.reset();
		getenv().put(JsonServerServlet.KB_SERVNAME, "ServerSection");
		
		cfg = JsonServerServlet.getConfig("BadServerSection", log);
		
		assertThat("correct config", cfg, is(expect));
		assertThat("no logged error", mockout.lastSysLogInt, is(-1));
		assertThat("no logged msg", mockout.lastSysLogMsg, is((String) null));
		getenv().remove(JsonServerServlet.KB_SERVNAME);
		
	}
	
	@SuppressWarnings("unused")
	private void printArray(String[] array) {
		for (int i = 0; i < array.length; i++) {
			System.out.println(array[i]);
		}
	}
	
	private void checkSyslogMsg(String lastSysLogMsg, String prefix, String suffix) {
		String[] parts = lastSysLogMsg.split(":", 2);
		String[] headerParts = parts[0].split("]\\s*\\[");
		assertThat("server name correct", headerParts[0].substring(1),
				is("foo"));
		assertThat("record type correct", headerParts[1], is("ERR"));
		//2 is timestamp
		//3 is user running the service
		assertThat("caller correct", headerParts[4],
				is("us.kbase.common.service.JsonServerServlet"));
		//5 is pid
		assertThat("ip correct", headerParts[6], is("-"));
		assertThat("remote user correct", headerParts[7], is("-"));
		assertThat("module correct", headerParts[8], is("-"));
		assertThat("method correct", headerParts[9], is("-"));
		assertThat("call ID correct", headerParts[10].substring(
				0, headerParts[10].length() -1), is("-"));
		
		
//		System.out.println(parts[1]);
//		System.out.println(prefix);
//		System.out.println(suffix);
		assertThat("message prefix correct",
				parts[1].trim().startsWith(prefix), is (true));
		assertThat("message suffix correct",
				parts[1].trim().endsWith(suffix), is (true));
	}

	@Test
	public void setUpResponseHeaders() throws Exception {
		
		//test with allowed headers set
		HttpServletRequestMock req = new HttpServletRequestMock();
		req.setHeader("HTTP_ACCESS_CONTROL_REQUEST_HEADERS", "aHeader");
		HttpServletResponseMock res = new HttpServletResponseMock();
		JsonServerServlet.setupResponseHeaders(req, res);
		
		assertThat("correct access control origin",
				res.getHeader("Access-Control-Allow-Origin"), is("*"));
		assertThat("correct content type", res.getContentType(),
				is("application/json"));
		assertThat("correct access control origin",
				res.getHeader("Access-Control-Allow-Headers"), is("aHeader"));

		
		//test without allowed headers set
		req = new HttpServletRequestMock();
		res = new HttpServletResponseMock();
		JsonServerServlet.setupResponseHeaders(req, res);
		assertThat("correct access control origin",
				res.getHeader("Access-Control-Allow-Origin"), is("*"));
		assertThat("correct content type", res.getContentType(),
				is("application/json"));
		assertThat("correct access control origin",
				res.getHeader("Access-Control-Allow-Headers"),
				is("authorization"));
	}
	
}
