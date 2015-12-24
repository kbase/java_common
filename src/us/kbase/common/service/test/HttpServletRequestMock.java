package us.kbase.common.service.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class HttpServletRequestMock implements HttpServletRequest {

	private Map<String, String> headers = new HashMap<String, String>();
	private String ipAddress = null;
	
	public void setIpAddress(String ip) {
		ipAddress = ip;
	}
	
	public void setHeader(String key, String value) {
		headers.put(key, value);
	}
	
	@Override
	public Object getAttribute(String arg0) {
		throw new NotImplementedException();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Enumeration getAttributeNames() {
		throw new NotImplementedException();
	}

	@Override
	public String getCharacterEncoding() {
		throw new NotImplementedException();
	}

	@Override
	public int getContentLength() {
		throw new NotImplementedException();
	}

	@Override
	public String getContentType() {
		throw new NotImplementedException();
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		throw new NotImplementedException();
	}

	@Override
	public String getLocalAddr() {
		throw new NotImplementedException();
	}

	@Override
	public String getLocalName() {
		throw new NotImplementedException();
	}

	@Override
	public int getLocalPort() {
		throw new NotImplementedException();
	}

	@Override
	public Locale getLocale() {
		throw new NotImplementedException();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Enumeration getLocales() {
		throw new NotImplementedException();
	}

	@Override
	public String getParameter(String arg0) {
		throw new NotImplementedException();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Map getParameterMap() {
		throw new NotImplementedException();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Enumeration getParameterNames() {
		throw new NotImplementedException();
	}

	@Override
	public String[] getParameterValues(String arg0) {
		throw new NotImplementedException();
	}

	@Override
	public String getProtocol() {
		throw new NotImplementedException();
	}

	@Override
	public BufferedReader getReader() throws IOException {
		throw new NotImplementedException();
	}

	@Override
	public String getRealPath(String arg0) {
		throw new NotImplementedException();
	}

	@Override
	public String getRemoteAddr() {
		return ipAddress;
	}

	@Override
	public String getRemoteHost() {
		throw new NotImplementedException();
	}

	@Override
	public int getRemotePort() {
		throw new NotImplementedException();
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String arg0) {
		throw new NotImplementedException();
	}

	@Override
	public String getScheme() {
		throw new NotImplementedException();
	}

	@Override
	public String getServerName() {
		throw new NotImplementedException();
	}

	@Override
	public int getServerPort() {
		throw new NotImplementedException();
	}

	@Override
	public boolean isSecure() {
		throw new NotImplementedException();
	}

	@Override
	public void removeAttribute(String arg0) {
		throw new NotImplementedException();
		
	}

	@Override
	public void setAttribute(String arg0, Object arg1) {
		throw new NotImplementedException();
	}

	@Override
	public void setCharacterEncoding(String arg0)
			throws UnsupportedEncodingException {
		throw new NotImplementedException();
	}

	@Override
	public String getAuthType() {
		throw new NotImplementedException();
	}

	@Override
	public String getContextPath() {
		throw new NotImplementedException();
	}

	@Override
	public Cookie[] getCookies() {
		return null;
	}

	@Override
	public long getDateHeader(String arg0) {
		throw new NotImplementedException();
	}

	@Override
	public String getHeader(String header) {
		return headers.get(header);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Enumeration getHeaderNames() {
		throw new NotImplementedException();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Enumeration getHeaders(String arg0) {
		throw new NotImplementedException();
	}

	@Override
	public int getIntHeader(String arg0) {
		throw new NotImplementedException();
	}

	@Override
	public String getMethod() {
		throw new NotImplementedException();
	}

	@Override
	public String getPathInfo() {
		throw new NotImplementedException();
	}

	@Override
	public String getPathTranslated() {
		throw new NotImplementedException();
	}

	@Override
	public String getQueryString() {
		throw new NotImplementedException();
	}

	@Override
	public String getRemoteUser() {
		throw new NotImplementedException();
	}

	@Override
	public String getRequestURI() {
		throw new NotImplementedException();
	}

	@Override
	public StringBuffer getRequestURL() {
		throw new NotImplementedException();
	}

	@Override
	public String getRequestedSessionId() {
		throw new NotImplementedException();
	}

	@Override
	public String getServletPath() {
		throw new NotImplementedException();
	}

	@Override
	public HttpSession getSession() {
		throw new NotImplementedException();
	}

	@Override
	public HttpSession getSession(boolean arg0) {
		throw new NotImplementedException();
	}

	@Override
	public Principal getUserPrincipal() {
		throw new NotImplementedException();
	}

	@Override
	public boolean isRequestedSessionIdFromCookie() {
		throw new NotImplementedException();
	}

	@Override
	public boolean isRequestedSessionIdFromURL() {
		throw new NotImplementedException();
	}

	@Override
	public boolean isRequestedSessionIdFromUrl() {
		throw new NotImplementedException();
	}

	@Override
	public boolean isRequestedSessionIdValid() {
		throw new NotImplementedException();
	}

	@Override
	public boolean isUserInRole(String arg0) {
		throw new NotImplementedException();
	}
}
