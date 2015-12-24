package us.kbase.common.service.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import us.kbase.common.service.JsonServerServlet;

/** Tests some new public static methods that can be reused in other services.
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
	
	@Test
	public void getConfig() throws Exception {
		
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
