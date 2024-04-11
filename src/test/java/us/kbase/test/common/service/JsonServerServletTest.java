package us.kbase.test.common.service;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

import us.kbase.auth.AuthToken;
import us.kbase.common.service.JsonServerMethod;
import us.kbase.common.service.JsonServerServlet;
import us.kbase.common.service.JsonServerServlet.AuthenticationHandler;
import us.kbase.common.service.JsonServerSyslog;
import us.kbase.common.service.RpcContext;
import us.kbase.testutils.TestCommon;

public class JsonServerServletTest {
	
	// TODO TEST more tests, more complete tests. Current tests are very basic happy path and don't check logging, etc.
	
	@SuppressWarnings("serial")
	public class FakeServer extends JsonServerServlet {
		
		public int onRpcMethodDoneCalls = 0;
		public List<AuthToken> tokens = new ArrayList<>();

		public FakeServer(
				final AuthenticationHandler auth,
				final boolean trustX_IPHeaders,
				final JsonServerSyslog syslog,
				final JsonServerSyslog userlog) {
			super(auth, trustX_IPHeaders, syslog, userlog);
		}
		
		@JsonServerMethod(rpc = "FakeServer.do_the_thing1", async=true)
		public Integer doTheThing1(Integer input, RpcContext jsonRpcContext) throws Exception {
			return input + 1;
		}
		
		@JsonServerMethod(rpc = "FakeServer.do_the_thing2", authOptional=true, async=true)
		public Integer doTheThing2(Integer input, AuthToken authPart, RpcContext jsonRpcContext)
				throws Exception {
			tokens.add(authPart);
			return input + 2;
		}
		
		@JsonServerMethod(rpc = "FakeServer.do_the_thing3", authOptional=false, async=true)
		public Integer doTheThing3(Integer input, AuthToken authPart, RpcContext jsonRpcContext)
				throws Exception {
			tokens.add(authPart);
			return input + 3;
		}
		
		@Override
		public void doPost(HttpServletRequest request, final HttpServletResponse response)
				throws ServletException, IOException {
			super.doPost(request, response);
		}
		
		@Override
		public void onRpcMethodDone() {
			onRpcMethodDoneCalls++;
		}
		
		@Override
		public String getDefaultServiceName() {
			return super.getDefaultServiceName();
		}
		
		public Map<String, String> getConfig() {
			return super.config;
		}
		
	}
	
	private class ServletInputStreamWrapper extends ServletInputStream {
		
		private final InputStream in;

		public ServletInputStreamWrapper(final InputStream in) {
			this.in = in;
		}

		@Override
		public int read() throws IOException {
			return in.read();
		}
	}
	
	private class ServletOutputStreamWrapper extends ServletOutputStream {

		private final OutputStream out;

		public ServletOutputStreamWrapper(final OutputStream out) {
			this.out = out;
		}
		
		@Override
		public void write(int b) throws IOException {
			out.write(b);
		}
	}
	
	@Test
	public void constructServerFail() throws Exception {
		final AuthenticationHandler ah = mock(AuthenticationHandler.class);
		final JsonServerSyslog log = mock(JsonServerSyslog.class);
		
		constructFail(null, log, log, new NullPointerException("auth"));
		constructFail(ah, null, log, new NullPointerException("sysLogger"));
		constructFail(ah, log, null, new NullPointerException("userLogger"));
	}
	
	private void constructFail(
			final AuthenticationHandler auth,
			final JsonServerSyslog syslog,
			final JsonServerSyslog userlog,
			final Exception expected) {
		try {
			new FakeServer(auth, true, syslog, userlog);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
		
	}
	
	@Test
	public void postWithNoAuth() throws Exception {
		post("FakeServer.do_the_thing1", 7, null, false);
	}
	
	@Test
	public void postWithOptionalAuthNoToken() throws Exception {
		post("FakeServer.do_the_thing2", 8, null, true);
	}
	
	@Test
	public void postWithOptionalAuthWithToken() throws Exception {
		post("FakeServer.do_the_thing2", 8, new AuthToken("token", "foo"), true);
	}
	
	@Test
	public void postWithRequiredAuthn() throws Exception {
		post("FakeServer.do_the_thing3", 9, new AuthToken("token", "foo"), true);
	}
	
	private void post(
			final String method,
			final int result,
			final AuthToken token,
			final boolean expectToken)
			throws Exception {
		final AuthenticationHandler ah = mock(AuthenticationHandler.class);
		final JsonServerSyslog sysLog = mock(JsonServerSyslog.class);
		final JsonServerSyslog userLog = mock(JsonServerSyslog.class);
		final HttpServletRequest req = mock(HttpServletRequest.class);
		final HttpServletResponse resp = mock(HttpServletResponse.class);
		
		when(sysLog.getServiceName()).thenReturn("myserv");
		
		// note the trust x ip headers arg is untested - only affects logging the ip address
		final FakeServer fs = new FakeServer(ah, false, sysLog, userLog);
		
		assertThat("incorrect service name", fs.getDefaultServiceName(), is("myserv"));
		assertThat("incorrect config", fs.getConfig(), is(Collections.emptyMap()));
		
		when(req.getRemoteAddr()).thenReturn("123.456.789.123");
		
		if (token != null) {
			// caps important here since it's a mock
			when(req.getHeader("Authorization")).thenReturn(token.getToken());
			when(ah.validateToken(token.getToken())).thenReturn(token);
		}
		
		final Map<String, Object> packge = ImmutableMap.of(
				"method", method,
				"version", "1.1",
				"id", 56,
				"params", Arrays.asList(6));
		
		final InputStream input = new ByteArrayInputStream(
				new ObjectMapper().writeValueAsBytes(packge));
		
		final ByteArrayOutputStream output = new ByteArrayOutputStream();
		
		when(req.getInputStream()).thenReturn(new ServletInputStreamWrapper(input));
		
		when(resp.getOutputStream()).thenReturn(new ServletOutputStreamWrapper(output));
		
		fs.doPost(req, resp);
		
		final Map<String, Object> response = new ObjectMapper().readValue(
				output.toByteArray(), new TypeReference<Map<String, Object>>(){});
		assertThat("incorrect response", response,
				is(ImmutableMap.of("version", "1.1", "result", Arrays.asList(result))));
		if (expectToken) {
			assertThat("incorrect tokens", fs.tokens, is(Arrays.asList(token)));
		} else {
			assertThat("incorrect tokens", fs.tokens, is(Collections.emptyList()));
		}
		
		verify(resp, never()).setStatus(anyInt()); // status only set on error
		verify(resp).setHeader("Access-Control-Allow-Origin", "*");
		verify(resp).setHeader("Access-Control-Allow-Headers", "authorization");
		verify(resp).setContentType("application/json");
		if (token == null) {
			verifyZeroInteractions(ah);
		}
		assertThat("incorrect on rpc done calls", fs.onRpcMethodDoneCalls, is(1));
		verify(sysLog).log(6, FakeServer.class.getName(), "start method");
		verify(sysLog).log(6, FakeServer.class.getName(), "end method");
		verifyZeroInteractions(userLog);
	}
	
	@Test
	public void customUserLogger() throws Exception {
		/* tests that a user logger specified as part of the embed-oriented constructor gets
		 * appropriate log messages.
		 * Doesn't rehash all the stuff tested in the postWith* tests.
		 * Doesn't check all the methods, as the test is targeted to making sure the constructor
		 * works, not that all the methods work.
		 */
		
		final AuthenticationHandler ah = mock(AuthenticationHandler.class);
		final JsonServerSyslog sysLog = mock(JsonServerSyslog.class);
		final JsonServerSyslog userLog = mock(JsonServerSyslog.class);
		
		when(sysLog.getServiceName()).thenReturn("myserv");
		
		final FakeServer fs = new FakeServer(ah, false, sysLog, userLog);
		
		fs.logErr("oh crap");
		fs.logInfo("oh goody");
		fs.logDebug("oh what?");
		
		verify(userLog).logErr("oh crap");
		verify(userLog).logInfo("oh goody");
		verify(userLog).logDebug("oh what?");
		
		verify(sysLog).getServiceName();
		verifyNoMoreInteractions(sysLog);
	}
}
