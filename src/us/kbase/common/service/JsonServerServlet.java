package us.kbase.common.service;

import us.kbase.auth.AuthException;
import us.kbase.auth.AuthService;
import us.kbase.auth.AuthToken;
import us.kbase.auth.AuthUser;
import us.kbase.common.utils.UTCDateFormat;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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
	private static final String STRING_TRUE = "true";
	private static final String X_FORWARDED_FOR = "X-Forwarded-For";
	private static final String X_REAL_IP = "X-Real-IP";
	private ObjectMapper mapper;
	private Map<String, Method> rpcCache;
	public static final int LOG_LEVEL_ERR = JsonServerSyslog.LOG_LEVEL_ERR;
	public static final int LOG_LEVEL_INFO = JsonServerSyslog.LOG_LEVEL_INFO;
	public static final int LOG_LEVEL_DEBUG = JsonServerSyslog.LOG_LEVEL_DEBUG;
	public static final int LOG_LEVEL_DEBUG2 = JsonServerSyslog.LOG_LEVEL_DEBUG + 1;
	public static final int LOG_LEVEL_DEBUG3 = JsonServerSyslog.LOG_LEVEL_DEBUG + 2;
	private JsonServerSyslog sysLogger;
	private JsonServerSyslog userLogger;
	final private static String KB_DEP = "KB_DEPLOYMENT_CONFIG";
	final private static String KB_SERVNAME = "KB_SERVICE_NAME";
    private static final String KB_JOB_SERVICE_URL = "KB_JOB_SERVICE_URL";
	private static final String CONFIG_JOB_SERVICE_URL_PARAM = "job-service-url";
	protected Map<String, String> config = new HashMap<String, String>();
	private Server jettyServer = null;
	private Integer jettyPort = null;
	private boolean startupFailed = false;
	private Long maxRPCPackageSize = null;
	private int maxRpcMemoryCacheSize = 16 * 1024 * 1024;
	private File rpcDiskCacheTempDir = null;
	private final String specServiceName;
	private final UTCDateFormat utcDatetimeFormat = new UTCDateFormat();
	private String serviceVersion = null;
		
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
	
	public void startupFailed() {
		this.startupFailed = true;
	}
	
	public JsonServerServlet(String specServiceName) {
	    this.specServiceName = specServiceName;
		this.mapper = new ObjectMapper().registerModule(new JacksonTupleModule());
		this.rpcCache = new HashMap<String, Method>();
		for (Method m : getClass().getMethods()) {
			if (m.isAnnotationPresent(JsonServerMethod.class)) {
				JsonServerMethod ann = m.getAnnotation(JsonServerMethod.class);
				rpcCache.put(ann.rpc(), m);
				if (ann.async()) {
	                rpcCache.put(ann.rpc() + "_async", m);
                    rpcCache.put(ann.rpc() + "_check", m);
				}
			}
		}
		
		String serviceName = System.getProperty(KB_SERVNAME) == null ?
				System.getenv(KB_SERVNAME) : System.getProperty(KB_SERVNAME);
		if (serviceName == null) {
			serviceName = specServiceName;
			if (serviceName.contains(":"))
				serviceName = serviceName.substring(0, serviceName.indexOf(':')).trim();
		}
		String file = System.getProperty(KB_DEP) == null ?
				System.getenv(KB_DEP) : System.getProperty(KB_DEP);
		sysLogger = new JsonServerSyslog(serviceName, KB_DEP, LOG_LEVEL_INFO);
		userLogger = new JsonServerSyslog(sysLogger);
		//read the config file
		if (file == null) {
			return;
		}
		File deploy = new File(file);
		Ini ini = null;
		try {
			ini = new Ini(deploy);
		} catch (IOException ioe) {
			sysLogger.log(LOG_LEVEL_ERR, getClass().getName(), "There was an IO Error reading the deploy file "
							+ deploy + ". Traceback:\n" + ioe);
			return;
		}
		config = ini.get(serviceName);
		if (config == null) {
			config = new HashMap<String, String>();
			sysLogger.log(LOG_LEVEL_ERR, getClass().getName(), "The configuration file " + deploy + 
							" has no section " + serviceName);
		}
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
		info.setIp(request.getRemoteAddr());
		response.setContentType(APP_JSON);
		OutputStream output	= response.getOutputStream();
		JsonServerSyslog.getCurrentRpcInfo().reset();
		if (startupFailed) {
			writeError(wrap(response), -32603, "The server did not start up properly. Please check the log files for the cause.", output);
			return;
		}
		writeError(wrap(response), -32300, "HTTP GET not allowed.", output);
	}

	private void setupResponseHeaders(HttpServletRequest request, HttpServletResponse response) {
		response.setHeader("Access-Control-Allow-Origin", "*");
		String allowedHeaders = request.getHeader("HTTP_ACCESS_CONTROL_REQUEST_HEADERS");
		response.setHeader("Access-Control-Allow-Headers", allowedHeaders == null ? "authorization" : allowedHeaders);
		response.setContentType(APP_JSON);
	}
	
	@Override
	protected void doPost(HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
		checkMemoryForRpc();
		String remoteIp = getIpAddress(request);
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
                    .withTime(utcDatetimeFormat.formatDate(new Date())));
			processRpcCall(rpcCallData, token, info, requestHeaderXFF, respStatus, output);
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
	        os = new FileOutputStream(output);
	        RpcCallData rpcCallData = mapper.readValue(input, RpcCallData.class);
	        processRpcCall(rpcCallData, token, info, null, response, os);
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
	        String requestHeaderXForwardedFor, ResponseStatusSetter response, OutputStream output) {
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
			String origRpcName = rpcMethod.getAnnotation(JsonServerMethod.class).rpc();
			if (origRpcName.equals(rpcName)) {
			    int rpcArgCount = rpcMethod.getGenericParameterTypes().length;
			    Object[] methodValues = new Object[rpcArgCount];			
                if (rpcArgCount > 0 && rpcMethod.getParameterTypes()[rpcArgCount - 1].isArray() && 
                        rpcMethod.getParameterTypes()[rpcArgCount - 1].getComponentType().equals(RpcContext.class)) {
                    rpcArgCount--;
                    methodValues[rpcArgCount] = new RpcContext[] {context};
                }
			    if (rpcArgCount > 0 && rpcMethod.getParameterTypes()[rpcArgCount - 1].equals(AuthToken.class)) {
			        if (token != null || !rpcMethod.getAnnotation(JsonServerMethod.class).authOptional()) {
			            try {
			                userProfile = validateToken(token);
			                if (userProfile != null)
			                    info.setUser(userProfile.getClientId());
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
			} else {
	            if (token == null) {
                    writeError(response, -32400, "Authentication is required for using job service", output);
                    return;
	            }
                try {
                    userProfile = validateToken(token);
                    if (userProfile != null)
                        info.setUser(userProfile.getClientId());
                } catch (Throwable ex) {
                    writeError(response, -32400, "Token validation failed: " + ex.getMessage(), ex, output);
                    return;
                }
                JsonClientCaller jobService = getJobServiceClient(userProfile);
                List<Object> result = null;
			    if (rpcName.equals(origRpcName + "_async")) {
	                List<Object> runJobParams = new ArrayList<Object>();
	                Map<String, Object> paramMap = new LinkedHashMap<String, Object>();
	                runJobParams.add(paramMap);
	                paramMap.put("service_ver", getServiceVersion());
	                paramMap.put("method", origRpcName);
	                paramMap.put("params", paramsList);
	                paramMap.put("rpc_context", context);
	                TypeReference<List<Object>> retType = new TypeReference<List<Object>>() {};
	                result = jobService.jsonrpcCall("KBaseJobService.run_job", runJobParams, retType, true, true);
			    } else if (rpcName.equals(origRpcName + "_check")) {
	                TypeReference<List<JobState<UObject>>> retType = new TypeReference<List<JobState<UObject>>>() {};
	                List<JobState<UObject>> jobStateList = jobService.jsonrpcCall("KBaseJobService.check_job", paramsList, retType, true, true);
	                JobState<UObject> jobState = jobStateList.get(0);
	                Long finished = jobState.getFinished();
	                if (finished != 0L) {
	                    Object error = jobState.getAdditionalProperties().get("error");
	                    if (error != null) {
	                        Map<String, Object> ret = new LinkedHashMap<String, Object>();
	                        ret.put("version", "1.1");
	                        ret.put("error", error);
	                        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	                        mapper.writeValue(new UnclosableOutputStream(output), ret);
	                        output.flush();
	                        return;
	                    }
	                }
	                result = new ArrayList<Object>();
	                result.add(jobState);
			    } else {
			        writeError(response, -32601, "Can not find method [" + rpcName + "] in server class " + getClass().getName(), output);
			        return;
			    }
                Map<String, Object> ret = new LinkedHashMap<String, Object>();
                ret.put("version", "1.1");
                ret.put("result", result);
                mapper.writeValue(new UnclosableOutputStream(output), ret);
                output.flush();
            }
        } catch (Exception ex) {
            writeError(response, -32400, "Unexpected internal error (" + ex.getMessage() + ")", ex, output);
        }
	}

	private void logHeaders(final String xFF) {
		if (xFF != null && !xFF.isEmpty()) {
			sysLogger.log(LOG_LEVEL_INFO, getClass().getName(),
					X_FORWARDED_FOR + ": " + xFF);
		}
	}

	private String getIpAddress(HttpServletRequest request) {
		final String xFF = request.getHeader(X_FORWARDED_FOR);
		final String realIP = request.getHeader(X_REAL_IP);
		final boolean trustXHeaders = !STRING_TRUE.equals(
				config.get(DONT_TRUST_X_IP_HEADERS));
		
		if (trustXHeaders) {
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
	
	private static AuthToken validateToken(String token)
			throws AuthException, IOException {
		if (token == null)
			throw new AuthException(
					"Authorization is required for this method but no credentials were provided");
		final AuthToken ret = new AuthToken(token);
		final boolean validToken;
		try {
			validToken = AuthService.validateToken(ret);
		} catch (UnknownHostException uhe) {
			//message from UHE is only the host name
			throw new AuthException(
					"Could not contact Authorization Service host to validate user token: "
							+ uhe.getMessage(), uhe);
		}
		if (!validToken) {
			throw new AuthException("User token was invalid");
		}
		return ret;
	}

	public static AuthUser getUserProfile(AuthToken token)
			throws IOException, AuthException {
		return AuthService.getUserFromToken(token);
	}
	
	private void writeError(ResponseStatusSetter response, int code, String message, OutputStream output) {
		writeError(response, code, message, null, output);
	}
	
	private void writeError(ResponseStatusSetter response, int code, Throwable ex, OutputStream output) {
		writeError(response, code, ex.getMessage(), ex, output);
	}
	
	private void writeError(ResponseStatusSetter response, int code, String message, Throwable ex, OutputStream output) {
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
	
	private ResponseStatusSetter wrap(final HttpServletResponse response) {
	    return new ResponseStatusSetter() {
            @Override
            public void setStatus(int status) {
                response.setStatus(status);
            }
        };
	}
	
	private JsonClientCaller getJobServiceClient(AuthToken token) throws Exception {
	    String url = System.getProperty(KB_JOB_SERVICE_URL);
	    if (url == null)
	        url = System.getenv(KB_JOB_SERVICE_URL);
	    if (url == null)
	        url = config.get(CONFIG_JOB_SERVICE_URL_PARAM);
	    if (url == null)
	        throw new IllegalStateException("Neither '" + CONFIG_JOB_SERVICE_URL_PARAM + "' " +
	                "parameter is defined in configuration nor '" + KB_JOB_SERVICE_URL + "' " +
	                "variable is defined in system");
	    JsonClientCaller ret = new JsonClientCaller(new URL(url), token);
	    ret.setInsecureHttpConnectionAllowed(true);
	    return ret;
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
	
	private static class PlainTypeRef extends TypeReference<Object> {
		Type type;
		PlainTypeRef(Type type) {
			this.type = type;
		}
		
		@Override
		public Type getType() {
			return type;
		}
	}
	
	private static class UnclosableOutputStream extends OutputStream {
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
