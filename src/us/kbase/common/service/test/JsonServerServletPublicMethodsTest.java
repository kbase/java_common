package us.kbase.common.service.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import us.kbase.common.service.JsonServerServlet;

/** Tests some new public static methods that can be reused in other services.
 * @author gaprice@lbl.gov
 *
 */
public class JsonServerServletPublicMethodsTest {

	@Test
	public void ipAddress() throws Exception {
		
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
