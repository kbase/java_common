package us.kbase.common.service;

import static java.util.Objects.requireNonNull;

import us.kbase.auth.AuthConfig;
import us.kbase.auth.AuthException;
import us.kbase.auth.AuthToken;
import us.kbase.auth.ConfigurableAuthService;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.ini4j.Ini;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Helper class used as ancestor of generated server side servlets for JSON RPC calling.
 * @author rsutormin
 */
public class JsonServerServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final String APP_JSON = "application/json";
	private static final String DONT_TRUST_X_IP_HEADERS =
			"dont_trust_x_ip_headers";
	private static final String DONT_TRUST_X_IP_HEADERS2 =
			"dont-trust-x-ip-headers";
	private static final String STRING_TRUE = "true";
	private static final String X_FORWARDED_FOR = "X-Forwarded-For";
	private static final String X_REAL_IP = "X-Real-IP";
	private ObjectMapper mapper;
	private final Map<String, Method> rpcCache = new HashMap<>();
	public static final int LOG_LEVEL_ERR = JsonServerSyslog.LOG_LEVEL_ERR;
	public static final int LOG_LEVEL_INFO = JsonServerSyslog.LOG_LEVEL_INFO;
	public static final int LOG_LEVEL_DEBUG = JsonServerSyslog.LOG_LEVEL_DEBUG;
	public static final int LOG_LEVEL_DEBUG2 = JsonServerSyslog.LOG_LEVEL_DEBUG + 1;
	public static final int LOG_LEVEL_DEBUG3 = JsonServerSyslog.LOG_LEVEL_DEBUG + 2;
	private JsonServerSyslog sysLogger;
	private JsonServerSyslog userLogger;
	
	/** The key for the environment variable or JVM variable with the value of
	 * the server config file location.
	 */
	public static final String KB_DEP = "KB_DEPLOYMENT_CONFIG";
	
	/** The key for the environment variable or JVM variable with the value of
	 * the server name.
	 */
	public static final String KB_SERVNAME = "KB_SERVICE_NAME";
	private static final String CONFIG_AUTH_SERVICE_URL_PARAM =
			"auth-service-url";
	//set to 'true' for true, anything else for false.
	private static final String CONFIG_AUTH_SERVICE_ALLOW_INSECURE_URL_PARAM =
			"auth-service-url-allow-insecure";
	private final AuthenticationHandler auth;
	protected Map<String, String> config; // would like to be final but might break stuff
	private Server jettyServer = null;
	private Integer jettyPort = null;
	private boolean startupFailed = false;
	private Long maxRPCPackageSize = null;
	private int maxRpcMemoryCacheSize = 16 * 1024 * 1024;
	private File rpcDiskCacheTempDir = null;
	private final String specServiceName;
	private String serviceVersion = null;
	private final boolean trustX_IPHeaders;
		
	private final static DateTimeFormatter DATE_FORMATTER =
			DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZ").withZoneUTC();

	/**
	 * Starts a test jetty server on an OS-determined port. Blocks until the
	 * server is terminated.
	 * @throws Exception if the server couldn't be started.
	 */
	public void startupServer() throws Exception {
		startupServer(0);
	}
	
	/**
	 * Starts a test jetty server. Blocks until the
	 * server is terminated.
	 * @param port the port to which the server will connect.
	 * @throws Exception if the server couldn't be started.
	 */
	public void startupServer(int port) throws Exception {
		jettyServer = new Server(port);
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/");
		jettyServer.setHandler(context);
		context.addServlet(new ServletHolder(this),"/*");
		jettyServer.start();
		jettyPort = jettyServer.getConnectors()[0].getLocalPort();
		jettyServer.join();
	}
	
	/**
	 * Get the jetty test server port. Returns null if the server is not running or starting up.
	 * @return the port
	 */
	public Integer getServerPort() {
		return jettyPort;
	}
	
	/**
	 * Stops the test jetty server.
	 * @throws Exception if there was an error stopping the server.
	 */
	public void stopServer() throws Exception {
		jettyServer.stop();
		jettyServer = null;
		jettyPort = null;
		
	}
	
	/** Sets the server to failed mode. All API calls will return an error. */
	public void startupFailed() {
		this.startupFailed = true;
	}
	
	/** Create a new Servlet with default authentication handling.
	 * The servlet will read a *.ini based configuration file, where the file location is
	 * specified by the environment variable or system property KB_DEPLOYMENT_CONFIG. The system
	 * property takes precedence.
	 * 
	 * The section of the ini file that the server will read is designated by, in order of
	 * precedence:
	 * * The system property KB_SERVICE_NAME
	 * * The environment variable KB_SERVICE_NAME
	 * * The specServiceName parameter with any characters including and after the first colon (:)
	 *   removed.
	 * 
	 * The configuration read is available in the {@link #config} variable.
	 * 
	 * The server treats the following properties of the ini file section specially:
	 * * auth-service-url: the URL of the KBase legacy endpoint for the KBase auth server
	 *   (https://github.com/kbase/auth2).
	 * * auth-service-url-allow-insecure: set to "true" (no quotes) to allow auth-service-url
	 *   to be insecure. Do not do this in production or anywhere else you don't want tokens
	 *   to leak.
	 * * dont-trust-x-ip-headers or dont_trust_x_ip_headers: if either is true, the
	 *   X-Forwarded-For and X-Real-IP headers will be ignored when determining the requester's
	 *   IP address. See ({@link #getIpAddress(HttpServletRequest, boolean)} for more information.
	 *   
	 * For logging properties, refer to {@link JsonServerSyslog}.
	 * 
	 * @param specServiceName the name of this server.
	 */
	public JsonServerServlet(final String specServiceName) {
		this.specServiceName = specServiceName;
		this.mapper = new ObjectMapper().registerModule(new JacksonTupleModule());
		setUpMethodCache();
		sysLogger = new JsonServerSyslog(getServiceName(specServiceName),
				KB_DEP, LOG_LEVEL_INFO, false);
		userLogger = new JsonServerSyslog(sysLogger, true);
		config = getConfig(specServiceName, sysLogger);
		auth = getAuth(config);
		this.trustX_IPHeaders =
				!STRING_TRUE.equals(config.get(DONT_TRUST_X_IP_HEADERS)) &&
				!STRING_TRUE.equals(config.get(DONT_TRUST_X_IP_HEADERS2));
		//TODO TEST with default authentication. Needs auth server test mode running
	}

	private void setUpMethodCache() {
		for (final Method m : getClass().getMethods()) {
			if (m.isAnnotationPresent(JsonServerMethod.class)) {
				JsonServerMethod ann = m.getAnnotation(JsonServerMethod.class);
				rpcCache.put(ann.rpc(), m);
			}
		}
	}
	
	/** A handler for authentication information. Given a token, it returns the validated token
	 * and username of the user.
	 *
	 */
	public interface AuthenticationHandler {
		
		/** Validate a token.
		 * @param token the token to be validated.
		 * @return the validated token.
		 * @throws IOException if an IO error occurs.
		 * @throws AuthException if the token could not be validated.
		 */
		AuthToken validateToken(String token) throws IOException, AuthException;
	}
	
	/** Create a new Servlet with custom authentication handling. 
	 * 
	 * The servlet does not read from any configuration files, and the {@link #config} variable
	 * will be an empty map.
	 * 
	 * For this constructor {@link #getDefaultServiceName()} will return the service name from the
	 * sysLogger.
	 * 
	 * @param auth the authentication handler.
	 * @param trustX_IPHeaders true to trust the X-Forwarded-For and X-Real-IP headers (see
	 * {@link #getIpAddress(HttpServletRequest, boolean)})
	 * @param sysLogger the logger to use for system messages - e.g. messages generated by this
	 * class.
	 * @param userLogger the logger to use for user messages - e.g. messages generated by calling
	 * {@link #logDebug(String)}, {@link #logErr(String)}, {@link #logInfo(String)}, etc.
	 */
	public JsonServerServlet(
			final AuthenticationHandler auth,
			final boolean trustX_IPHeaders,
			final JsonServerSyslog sysLogger,
			final JsonServerSyslog userLogger) {
		//TODO NOW use repackaged jackson jars
		//TODO CODE hopefully remove rpc context?
		// may also need to repackage jetty jar, not sure. I hope not
		this.auth = requireNonNull(auth, "auth");
		this.trustX_IPHeaders = trustX_IPHeaders;
		this.mapper = new ObjectMapper().registerModule(new JacksonTupleModule());
		setUpMethodCache();
		
		this.sysLogger = requireNonNull(sysLogger, "sysLogger");
		this.userLogger = requireNonNull(userLogger, "userLogger");
		this.specServiceName = sysLogger.getServiceName();
		config = new HashMap<>(); // this should really be immutable...
	}
	
	protected String getAuthUrlFromConfig(final Map<String, String> config) {
		return config.get(CONFIG_AUTH_SERVICE_URL_PARAM);
	}
	
	protected String getAuthAllowInsecureFromConfig(final Map<String, String> config) {
		return config.get(CONFIG_AUTH_SERVICE_ALLOW_INSECURE_URL_PARAM);
	}
	
	private class DefaultAuthenticationHandler implements AuthenticationHandler {
		
		private final ConfigurableAuthService auth;

		private DefaultAuthenticationHandler(final ConfigurableAuthService auth) {
			this.auth = auth;
		}
		
		public AuthToken validateToken(final String token) throws IOException, AuthException {
			return auth.validateToken(token);
		}
	}
	
	protected AuthenticationHandler getAuth(final Map<String, String> config) {
		final String authURL = getAuthUrlFromConfig(config);
		final AuthConfig c = new AuthConfig();
		if (authURL != null && !authURL.isEmpty()) {
			if (STRING_TRUE.equals(getAuthAllowInsecureFromConfig(config))) {
				c.withAllowInsecureURLs(true);
			}
			try {
				c.withKBaseAuthServerURL(new URL(authURL));
			} catch (URISyntaxException | MalformedURLException e) {
				startupFailed();
				sysLogger.log(LOG_LEVEL_ERR, getClass().getName(),
						String.format(
								"Authentication url %s is invalid", authURL));
				return null;
			}
		}
		try {
			return new DefaultAuthenticationHandler(new ConfigurableAuthService(c));
		} catch (IOException e) {
			startupFailed();
			sysLogger.log(LOG_LEVEL_ERR, getClass().getName(),
					"Couldn't connect to authentication service at " +
					c.getAuthServerURL() + " : " + e.getLocalizedMessage());
			return null;
		}
	}
	
	/**
	 * Returns the configuration from the KBase deploy.cfg config file.
	 * Returns an empty map if no config file is specified, if the file
	 * can't be read, or if there is no section of the config file matching
	 * serviceName.
	 * @param defaultServiceName the default name of the service to use if it
	 * can't be found in the environment, and the section of the config file
	 * where the service's configuration exists
	 * @param logger a logger for logging errors
	 * @return the server config from the deploy.cfg file
	 */
	public static Map<String, String> getConfig(
			final String defaultServiceName,
			final JsonServerSyslog logger) {
		if (logger == null) {
			throw new NullPointerException("logger cannot be null");
		}
		final String serviceName = getServiceName(defaultServiceName);
		final String file = System.getProperty(KB_DEP) == null ?
				System.getenv(KB_DEP) : System.getProperty(KB_DEP);
		if (file == null) {
			return new HashMap<String, String>();
		}
		final File deploy = new File(file);
		final Ini ini;
		try {
			ini = new Ini(deploy);
		} catch (IOException ioe) {
			logger.log(LOG_LEVEL_ERR, JsonServerServlet.class.getName(),
					"There was an IO Error reading the deploy file "
							+ deploy + ". Traceback:\n" + ioe);
			return new HashMap<String, String>();
		}
		Map<String, String> config = ini.get(serviceName);
		if (config == null) {
			config = new HashMap<String, String>();
			logger.log(LOG_LEVEL_ERR, JsonServerServlet.class.getName(),
					"The configuration file " + deploy + " has no section " +
					serviceName);
		}
		return config; // should really be immutable...
	}

	protected String getDefaultServiceName() {
		return specServiceName;
	}
	
	protected static String getServiceName(final String defaultServiceName) {
		//TODO CODE test null or empty here
		if (defaultServiceName == null) {
			throw new NullPointerException("service name cannot be null");
		}
		String serviceName = System.getProperty(KB_SERVNAME) == null ?
				System.getenv(KB_SERVNAME) : System.getProperty(KB_SERVNAME);
		if (serviceName == null) {
			serviceName = defaultServiceName;
			if (serviceName.contains(":"))
				serviceName = serviceName.substring(0, serviceName.indexOf(':')).trim();
		}
		return serviceName;
	}

	/**
	 * WARNING! Please use this method for testing purposes only.
	 * @param output
	 */
	public void changeSyslogOutput(JsonServerSyslog.SyslogOutput output) {
		sysLogger.changeOutput(output);
		userLogger.changeOutput(output);
	}
	
	public void logErr(String message) {
		userLogger.logErr(message);
	}

	public void logErr(Throwable err) {
		userLogger.logErr(err);
	}
	
	public void logInfo(String message) {
		userLogger.logInfo(message);
	}
	
	public void logDebug(String message) {
		userLogger.logDebug(message);
	}
	
	public void logDebug(String message, int debugLevelFrom1to3) {
		userLogger.logDebug(message, debugLevelFrom1to3);
	}

	public int getLogLevel() {
		return userLogger.getLogLevel();
	}
	
	public void setLogLevel(int level) {
		userLogger.setLogLevel(level);
	}
	
	public void clearLogLevel() {
		userLogger.clearLogLevel();
	}
	
	public boolean isLogDebugEnabled() {
		return userLogger.getLogLevel() >= LOG_LEVEL_DEBUG;
	}
	
	@Override
	protected void doOptions(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		setupResponseHeaders(request, response);
		response.setContentLength(0);
		response.getOutputStream().print("");
		response.getOutputStream().flush();
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		JsonServerSyslog.RpcInfo info = JsonServerSyslog.getCurrentRpcInfo().reset();
		info.setIp(getIpAddress(request, trustX_IPHeaders));
		response.setContentType(APP_JSON);
		OutputStream output = response.getOutputStream();
		JsonServerSyslog.getCurrentRpcInfo().reset();
		if (startupFailed) {
			writeError(wrap(response), -32603, "The server did not start up properly. Please check the log files for the cause.", output);
			return;
		}
		writeError(wrap(response), -32300, "HTTP GET not allowed.", output);
	}

	/** Set up server response headers.
	 * Sets Access-Control-Allow-Origin: *
	 * Sets HTTP_ACCESS_CONTROL_REQUEST_HEADERS to the contents of the
	 * request Access-Control-Allow-Headers, or a minimum of "authorization"
	 * Sets the content type to application/json
	 * @param request the HTTP request
	 * @param response the HTTP response
	 */
	public static void setupResponseHeaders(HttpServletRequest request, HttpServletResponse response) {
		response.setHeader("Access-Control-Allow-Origin", "*");
		String allowedHeaders = request.getHeader("HTTP_ACCESS_CONTROL_REQUEST_HEADERS");
		response.setHeader("Access-Control-Allow-Headers", allowedHeaders == null ? "authorization" : allowedHeaders);
		response.setContentType(APP_JSON);
	}
	
	@Override
	protected void doPost(HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
		checkMemoryForRpc();
		String remoteIp = getIpAddress(request, trustX_IPHeaders);
		setupResponseHeaders(request, response);
		OutputStream output = response.getOutputStream();
		ResponseStatusSetter respStatus = new ResponseStatusSetter() {
			@Override
			public void setStatus(int status) {
				response.setStatus(status);
			}
		};
		JsonServerSyslog.RpcInfo info = JsonServerSyslog.getCurrentRpcInfo().reset();
		info.setIp(remoteIp);
		JsonTokenStream jts = null; 
		File tempFile = null;
		try {
			InputStream input = request.getInputStream();
			byte[] buffer = new byte[100000];
			ByteArrayOutputStream bufferOs = new ByteArrayOutputStream();
			long rpcSize = 0;
			while (rpcSize < maxRpcMemoryCacheSize) {
				int count = input.read(buffer, 0, Math.min(buffer.length, maxRpcMemoryCacheSize - (int)rpcSize));
				if (count < 0)
					break;
				bufferOs.write(buffer, 0, count);
				rpcSize += count;
			}
			if (rpcSize >= maxRpcMemoryCacheSize) {
				OutputStream os;
				if (rpcDiskCacheTempDir == null) {
					os = bufferOs;
				} else {
					tempFile = generateTempFile();
					os = new BufferedOutputStream(new FileOutputStream(tempFile));
					bufferOs.close();
					os.write(bufferOs.toByteArray());
					bufferOs = null;
				}
				while (true) {
					int count = input.read(buffer, 0, buffer.length);
					if (count < 0)
						break;
					os.write(buffer, 0, count);
					rpcSize += count;
					if (maxRPCPackageSize != null && rpcSize > maxRPCPackageSize) {
						writeError(respStatus, -32700, "Object is too big, length is more than " + maxRPCPackageSize + " bytes", output);
						os.close();
						input.close();
						return;
					}
				}
				os.close();
				if (tempFile == null) {
					jts = new JsonTokenStream(((ByteArrayOutputStream)os).toByteArray());
					bufferOs = null;
				} else {
					jts = new JsonTokenStream(tempFile);
				}
			} else {
				bufferOs.close();
				jts = new JsonTokenStream(bufferOs.toByteArray());
			}
			bufferOs = null;
			String token = request.getHeader("Authorization");
			String requestHeaderXFF = request.getHeader(X_FORWARDED_FOR);
			RpcCallData rpcCallData;
			try {
				rpcCallData = mapper.readValue(jts, RpcCallData.class);
			} catch (Exception ex) {
				writeError(respStatus, -32700, "Parse error (" + ex.getMessage() + ")", ex, output);
				return;
			} finally {
				jts.close();
			}
			Object idNode = rpcCallData.getId();
			try {
				info.setId(idNode == null ? null : "" + idNode);
			} catch (Exception ex) {}
			String rpcName = rpcCallData.getMethod();
			if (rpcName == null) {
				writeError(respStatus, -32601, "JSON RPC method property is not defined", output);
				return;
			}
			rpcName = correctRpcMethod(rpcName);
			List<UObject> paramsList = rpcCallData.getParams();
			if (paramsList == null) {
				writeError(respStatus, -32601, "JSON RPC params property is not defined", output);
				return;
			}
			if (!rpcName.contains(".")) {
				rpcName = specServiceName + "." + rpcName;
				rpcCallData.setMethod(rpcName);
			}
			RpcContext context = rpcCallData.getContext();
			if (context == null) {
				context = new RpcContext();
				rpcCallData.setContext(context);
			}
			if (context.getCallStack() == null)
				context.setCallStack(new ArrayList<MethodCall>());
			context.getCallStack().add(new MethodCall().withMethod(rpcName)
					.withTime(DATE_FORMATTER.print(new DateTime())));
			processRpcCall(rpcCallData, token, info, requestHeaderXFF, respStatus, output, false);
		} catch (Exception ex) {
			writeError(respStatus, -32400, "Unexpected internal error (" + ex.getMessage() + ")", ex, output);    
		} finally {
			if (jts != null) {
				try {
					jts.close();
				} catch (Exception ignore) {}
				if (tempFile != null) {
					try {
						tempFile.delete();
					} catch (Exception ignore) {}
				}
			}
		}
	}

	protected int processRpcCall(File input, File output, String token) {
		JsonServerSyslog.RpcInfo info = JsonServerSyslog.getCurrentRpcInfo().reset();
		final int[] responseCode = {0};
		ResponseStatusSetter response = new ResponseStatusSetter() {
			@Override
			public void setStatus(int status) {
				responseCode[0] = status;
			}
		};
		OutputStream os = null;
		try {
			File tokenFile = new File(token);
			if (tokenFile.exists()) {
				BufferedReader br = new BufferedReader(new FileReader(tokenFile));
				token = br.readLine();
				br.close();
			}
			os = new FileOutputStream(output);
			RpcCallData rpcCallData = mapper.readValue(input, RpcCallData.class);
			processRpcCall(rpcCallData, token, info, null, response, os, true);
		} catch (Throwable ex) {
			writeError(response, -32400, "Unexpected internal error (" + ex.getMessage() + ")", ex, os);    
		} finally {
			if (os != null)
				try {
					os.close();
				} catch (Exception ignore) {}
		}
		return responseCode[0];
	}

	protected void processRpcCall(RpcCallData rpcCallData, String token, JsonServerSyslog.RpcInfo info, 
			String requestHeaderXForwardedFor, ResponseStatusSetter response, OutputStream output,
			boolean commandLine) {
		RpcContext context = rpcCallData.getContext();
		String rpcName = rpcCallData.getMethod();
		String[] serviceAndMethod = rpcName.split("\\.");
		info.setModule(serviceAndMethod[0]);
		info.setMethod(serviceAndMethod[1]);
		List<UObject> paramsList = rpcCallData.getParams();
		AuthToken userProfile = null;
		try {
			Method rpcMethod = rpcCache.get(rpcName);
			if (rpcMethod == null) {
				writeError(response, -32601, "Can not find method [" + rpcName + "] in server class " + getClass().getName(), output);
				return;
			}
			int rpcArgCount = rpcMethod.getGenericParameterTypes().length;
			Object[] methodValues = new Object[rpcArgCount];
			boolean lastParamRpcContext = rpcArgCount > 0 && rpcMethod.getParameterTypes()[rpcArgCount - 1].equals(RpcContext.class);
			boolean lastParamRpcContextArr = rpcArgCount > 0 && rpcMethod.getParameterTypes()[rpcArgCount - 1].isArray() && 
					rpcMethod.getParameterTypes()[rpcArgCount - 1].getComponentType().equals(RpcContext.class);
			if (lastParamRpcContext || lastParamRpcContextArr) {
				rpcArgCount--;
				if (lastParamRpcContext) {
					methodValues[rpcArgCount] = context;
				} else {
					methodValues[rpcArgCount] = new RpcContext[] {context};
				}
			}
			if (rpcArgCount > 0 && rpcMethod.getParameterTypes()[rpcArgCount - 1].equals(AuthToken.class)) {
				if (token != null || !rpcMethod.getAnnotation(JsonServerMethod.class).authOptional()) {
					try {
						userProfile = validateToken(token);
						if (userProfile != null) {
							info.setUser(userProfile.getUserName());
						}
					} catch (Throwable ex) {
						writeError(response, -32400, "Token validation failed: " + ex.getMessage(), ex, output);
						return;
					}
				}
				rpcArgCount--;
				methodValues[rpcArgCount] = userProfile;
			}
			if (startupFailed) {
				writeError(response, -32603, "The server did not start up properly. Please check the log files for the cause.", output);
				return;
			}
			if (paramsList.size() != rpcArgCount) {
				writeError(response, -32602, "Wrong parameter count for method " + rpcName, output);
				return;
			}
			for (int typePos = 0; typePos < paramsList.size(); typePos++) {
				UObject jsonData = paramsList.get(typePos);
				Type paramType = rpcMethod.getGenericParameterTypes()[typePos];
				PlainTypeRef paramJavaType = new PlainTypeRef(paramType);
				try {
					Object obj;
					if (jsonData == null) {
						obj = null;
					} else if (paramType instanceof Class && paramType.equals(UObject.class)) {
						obj = jsonData;
					} else {
						try {
							obj = mapper.readValue(jsonData.getPlacedStream(), paramJavaType);
						} finally {
							if (jsonData.isTokenStream())
								((JsonTokenStream)jsonData.getUserObject()).close();
						}
					}
					methodValues[typePos] = obj;
				} catch (Exception ex) {
					writeError(response, -32602, "Wrong type of parameter " + typePos + " for method " + rpcName + " (" + ex.getMessage() + ")", ex, output);	
							return;
				}
			}
			Object result;
			try {
				logHeaders(requestHeaderXForwardedFor);
				sysLogger.log(LOG_LEVEL_INFO, getClass().getName(), "start method");
				result = rpcMethod.invoke(this, methodValues);
				sysLogger.log(LOG_LEVEL_INFO, getClass().getName(), "end method");
			} catch (Throwable ex) {
				if (ex instanceof InvocationTargetException && ex.getCause() != null) {
					ex = ex.getCause();
				}
				writeError(response, -32500, ex, output);
				onRpcMethodDone();
				return;
			}
			try {
				boolean notVoid = !rpcMethod.getReturnType().equals(Void.TYPE);
				boolean isTuple = rpcMethod.getAnnotation(JsonServerMethod.class).tuple();
				if (notVoid && !isTuple) {
					result = Arrays.asList(result);
				}
				Map<String, Object> ret = new LinkedHashMap<String, Object>();
				ret.put("version", "1.1");
				ret.put("result", result);
				mapper.writeValue(new UnclosableOutputStream(output), ret);
				output.flush();
			} finally {
				try {
					onRpcMethodDone();
				} catch (Exception ignore) {}
			}
		} catch (Exception ex) {
			writeError(response, -32400, "Unexpected internal error (" + ex.getMessage() + ")", ex, output);
		}
	}

	protected AuthToken validateToken(final String token)
			throws AuthException, IOException {
		if (token == null || token.isEmpty()) {
			throw new AuthException(
					"Authorization is required for this method but no " +
					"credentials were provided");
		}
		return auth.validateToken(token);
	}
	
	protected void logHeaders(final String xFF) {
		if (xFF != null && !xFF.isEmpty()) {
			sysLogger.log(LOG_LEVEL_INFO, getClass().getName(),
					X_FORWARDED_FOR + ": " + xFF);
		}
	}

	/** Get the IP address of the client. In order of precedence:
	 * 1. The first address in X-Forwarded-For
	 * 2. X-Real-IP
	 * 3. The remote address.
	 * @param request the HTTP request
	 * @param trustX_IPHeaders if true, always return the remote address, ignoring any other
	 * headers.
	 * @return the IP address of the client.
	 */
	public static String getIpAddress(
			final HttpServletRequest request,
			final boolean trustX_IPHeaders) {
		final String xFF = request.getHeader(X_FORWARDED_FOR);
		final String realIP = request.getHeader(X_REAL_IP);

		if (trustX_IPHeaders) {
			if (xFF != null && !xFF.isEmpty()) {
				return xFF.split(",")[0].trim();
			}
			if (realIP != null && !realIP.isEmpty()) {
				return realIP.trim();
			}
		}
		return request.getRemoteAddr();
	}

	protected String correctRpcMethod(String methodWithModule) {
		// Do nothing. Inherited classes can use for method/module name correction.
		return methodWithModule;
	}

	protected void checkMemoryForRpc() {
		// Do nothing. Inherited classes could define proper implementation.
	}

	protected void onRpcMethodDone() {
		// Do nothing. Inherited classes could define proper implementation.
	}

	protected File generateTempFile() {
		File tempFile = null;
		long suffix = System.currentTimeMillis();
		while (true) {
			tempFile = new File(rpcDiskCacheTempDir, "rpc" + suffix + ".json");
			if (!tempFile.exists())
				break;
			suffix++;
		}
		return tempFile;
	}
	
	protected Long getMaxRPCPackageSize() {
		return this.maxRPCPackageSize;
	}
	
	protected void setMaxRPCPackageSize(Long maxRPCPackageSize) {
		this.maxRPCPackageSize = maxRPCPackageSize;
	}
	
	protected void writeError(ResponseStatusSetter response, int code, String message, OutputStream output) {
		writeError(response, code, message, null, output);
	}

	protected void writeError(ResponseStatusSetter response, int code, Throwable ex, OutputStream output) {
		writeError(response, code, ex.getMessage(), ex, output);
	}

	protected void writeError(ResponseStatusSetter response, int code, String message, Throwable ex, OutputStream output) {
		//new Exception(message, ex).printStackTrace();
		String data = null;
		if (ex != null) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			ex.printStackTrace(pw);
			pw.close();
			data = sw.toString();
			sysLogger.logErr(ex, getClass().getName());
		} else {
			sysLogger.log(LOG_LEVEL_ERR, getClass().getName(), message);
		}
		response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		ObjectNode ret = mapper.createObjectNode();
		ObjectNode error = mapper.createObjectNode();
		error.put("name", "JSONRPCError");
		error.put("code", code);
		error.put("message", message);
		error.put("error", data);
		ret.put("version", "1.1");
		ret.put("error", error);
		String id = JsonServerSyslog.getCurrentRpcInfo().getId();
		if (id != null)
			ret.put("id", id);
		try {
			ByteArrayOutputStream bais = new ByteArrayOutputStream();
			mapper.writeValue(bais, ret);
			bais.close();
			//String logMessage = new String(bytes);
			//sysLogger.log(LOG_LEVEL_ERR, getClass().getName(), logMessage);
			output.write(bais.toByteArray());
			output.flush();
		} catch (Exception e) {
			System.err.println(
					"Unable to write error to output - current exception:");
			e.printStackTrace();
			System.err.println("original exception:");
			ex.printStackTrace();
		}
	}
	
	public int getMaxRpcMemoryCacheSize() {
		return maxRpcMemoryCacheSize;
	}
	
	public void setMaxRpcMemoryCacheSize(int maxRpcMemoryCacheSize) {
		this.maxRpcMemoryCacheSize = maxRpcMemoryCacheSize;
	}
	
	public File getRpcDiskCacheTempDir() {
		return rpcDiskCacheTempDir;
	}
	
	public void setRpcDiskCacheTempDir(File rpcDiskCacheTempDir) {
		this.rpcDiskCacheTempDir = rpcDiskCacheTempDir;
	}

	protected ResponseStatusSetter wrap(final HttpServletResponse response) {
		return new ResponseStatusSetter() {
			@Override
			public void setStatus(int status) {
				response.setStatus(status);
			}
		};
	}

	public String getServiceVersion() {
		return serviceVersion;
	}

	public void setServiceVersion(String serviceVersion) {
		this.serviceVersion = serviceVersion;
	}

	public static class RpcCallData {
		private Object id;
		private String method;
		private List<UObject> params;
		private Object version;
		private RpcContext context;

		public Object getId() {
			return id;
		}

		public void setId(Object id) {
			this.id = id;
		}

		public String getMethod() {
			return method;
		}

		public void setMethod(String method) {
			this.method = method;
		}

		public List<UObject> getParams() {
			return params;
		}

		public void setParams(List<UObject> params) {
			this.params = params;
		}

		public Object getVersion() {
			return version;
		}

		public void setVersion(Object version) {
			this.version = version;
		}

		public RpcContext getContext() {
			return context;
		}

		public void setContext(RpcContext context) {
			this.context = context;
		}
	}
	
	protected static class PlainTypeRef extends TypeReference<Object> {
		Type type;
		PlainTypeRef(Type type) {
			this.type = type;
		}
		
		@Override
		public Type getType() {
			return type;
		}
	}
	
	protected static class UnclosableOutputStream extends OutputStream {
		OutputStream inner;
		boolean isClosed = false;
		
		public UnclosableOutputStream(OutputStream inner) {
			this.inner = inner;
		}
		
		@Override
		public void write(int b) throws IOException {
			if (isClosed)
				return;
			inner.write(b);
		}
		
		@Override
		public void close() throws IOException {
			isClosed = true;
		}
		
		@Override
		public void flush() throws IOException {
			inner.flush();
		}
		
		@Override
		public void write(byte[] b) throws IOException {
			if (isClosed)
				return;
			inner.write(b);
		}
		
		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			if (isClosed)
				return;
			inner.write(b, off, len);
		}
	}

	public static interface ResponseStatusSetter {
		public void setStatus(int status);
	}
}
