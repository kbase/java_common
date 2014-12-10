package us.kbase.common.awe.task;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import us.kbase.common.service.UObject;
import us.kbase.common.taskqueue.JobStatuses;
import us.kbase.common.utils.StringUtils;

public class JavaGenericScript {
	public static final String PROP_WORKSPACE_URL = "workspace.url";
	public static final String DEFAULT_WORKSPACE_URL = "https://kbase.us/services/ws";
	
	public static void main(String[] args) throws Throwable {
		if (args.length != 2 && args.length != 5 && args.length != 6) {
			System.err.println("Usage: <java_generic_script> <job_id> <class_name> <method_name> <input_json> <config_json> [<token_env_var>]");
			System.err.println("   Or: <java_generic_script> <jar_list> <publisher>");
			System.exit(1);
		}
		if (args.length == 2) {
			String jarList = args[0];
			String pub = args[1];
			String gitRepoUrl = "https://github.com/" + pub + "/jars";
			System.out.println(prepare(jarList, gitRepoUrl));
			return;
		}
		String jobId = args[0];
		String className = args[1];
		String methodName = args[2];
		String inputJson = StringUtils.hexToString(args[3]);
		String configJson = StringUtils.hexToString(args[4]);
		String token = null;
		if (args.length == 6) {
			String tokenVar = args[5];
			token = System.getenv(tokenVar);
		}
		run(jobId, className, methodName, inputJson, configJson, token);
	}
	
	public static String prepare(String jarList, String gitRepoUrl) {
		String ret = prepareJarsAndGetClassPath(new File("/mnt/generic_java_script"), jarList, false, gitRepoUrl);
		if (ret == null)
			ret = prepareJarsAndGetClassPath(new File("/scratch/generic_java_script"), jarList, false, gitRepoUrl);
		if (ret == null)
			ret = prepareJarsAndGetClassPath(new File("."), jarList, true, gitRepoUrl);
		return ret;
	}
	
	private static String prepareJarsAndGetClassPath(File workDir, String jarList, 
			boolean throwError, String gitRepoUrl) {
		File jarsDir = new File(workDir, "jars");
		String[] jars = jarList.split(":");
		String classPath = null;
		try {
			if (!jarsDir.exists()) {
				gitClone(jarsDir, gitRepoUrl);
			} else {
				classPath = getClassPath(jarsDir, jars, false);
				if (classPath == null) 
					gitPull(jarsDir, gitRepoUrl);
			}
			if (classPath == null)
				classPath = getClassPath(jarsDir, jars, throwError);
			return classPath;
		} catch (Exception ex) {
			if (throwError)
				throw new IllegalStateException(ex);
			return null;
		}
	}
	
	private static String getClassPath(File jarsDir, String[] jars, boolean throwError) {
		Map<String, File> map = new TreeMap<String, File>();
		gatherJars(jarsDir, map);
		StringBuilder ret = new StringBuilder();
		for (String jarName : jars) {
			File jarFile = map.get(jarName);
			if (jarFile == null) {
				if (throwError)
					throw new IllegalStateException("Can't find jar: " + jarName);
				return null;
			}
			if (ret.length() > 0)
				ret.append(':');
			ret.append(jarFile.getAbsolutePath());
		}
		return ret.toString();
	}

	private static void gatherJars(File jarsDir, Map<String, File> ret) {
		for (File fd : jarsDir.listFiles()) {
			if (fd.isDirectory())
				gatherJars(fd, ret);
			if (fd.isFile() && fd.getName().endsWith(".jar"))
				ret.put(fd.getName(), fd);
		}
	}
	
	public static void run(String jobId, String className, String methodName, String inputJson, String configJson, String token) throws Throwable {
		@SuppressWarnings("unchecked")
		Map<String, String> config = UObject.getMapper().readValue(configJson, Map.class);
		String wsUrl = config.get(PROP_WORKSPACE_URL);
		if (wsUrl == null)
			wsUrl = DEFAULT_WORKSPACE_URL;
		Class<?> type = Class.forName(className);
		Object inst = type.newInstance();
		if (!(inst instanceof AweJobInterface))
			throw new IllegalStateException("Class doesn't implement " + AweJobInterface.class.getName());
		AweJobInterface task = (AweJobInterface)inst;
		JobStatuses js = task.getJobStatuses();
		try {
        	Long time = task.getEstimatedFinishTime();
        	String estComplete = time == null ? null : new SimpleDateFormat("YYYY-MM-DDThh:mm:ssZ").format(new Date(time));
			js.updateJob(jobId, token, "running", estComplete);
			task.init(jobId, token, config);
			for (Method m : type.getMethods()) {
				if (m.getName().equals(methodName)) {
					@SuppressWarnings("unchecked")
					List<Object> inputVals = UObject.getMapper().readValue(inputJson, List.class);
					Object[] methodArgs = new Object[m.getParameterTypes().length];
					for (int i = 0; i < methodArgs.length; i++)
						methodArgs[i] = UObject.transformObjectToObject(inputVals.get(i), m.getParameterTypes()[i]);
					Object ret = m.invoke(inst, methodArgs);
					String outRef = (ret != null && ret instanceof String) ? (String)ret : null;
					completeTaskState(js, jobId, token, outRef, wsUrl, null, null);
					return;
				}
			}
			completeTaskState(js, jobId, token, null, wsUrl, "Error: method " + methodName + " wasn't found ", "");
		} catch (Throwable ex) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			ex.printStackTrace(pw);
			pw.close();
			String stacktrace = sw.toString();
			completeTaskState(js, jobId, token, null, wsUrl, "Error: " + ex.getMessage(), stacktrace);
			throw ex;
		}
	}
	
	private static void completeTaskState(JobStatuses js, String jobId, String token, String outRef, 
			String wsUrl, String errorMessage, String errorStacktrace) throws Exception {
		if (errorMessage == null) {
			js.completeJob(jobId, token, "done", null, wsUrl, outRef);
		} else {
			js.completeJob(jobId, token, errorMessage, errorStacktrace, null, null); 
		}
	}
	
	private static String gitClone(File jarsDir, String gitRepoUrl) {
		jarsDir.mkdirs();
		return gitCommand("git clone " + gitRepoUrl + " " + jarsDir.getAbsolutePath(), 
				"clone", new File("."), gitRepoUrl);
	}
	
	private static String gitPull(File jarsDir, String gitRepoUrl) {
		try {
			return gitCommand("git pull", "pull", jarsDir, gitRepoUrl);
		} catch (Exception ex) {
			gitCommand("mv \"" + jarsDir.getAbsolutePath() + "\" " + "\"" + jarsDir.getAbsolutePath() + 
					"_" + System.currentTimeMillis() + "\"", "mv", new File("."), gitRepoUrl);
			return gitClone(jarsDir, gitRepoUrl);
		}
	}
	
	private static String gitCommand(String fullCmd, String nameOfCmd, File curDir, String gitRepoUrl) {
		try {
			Process p = Runtime.getRuntime().exec(fullCmd, null, curDir);
			BufferedReader stdOut = new BufferedReader(new InputStreamReader(p.getInputStream()));
			BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			StringBuilder out = new StringBuilder();
			StringBuilder error = new StringBuilder();
			Thread outT = readInThread(stdOut, out);
			Thread errT = readInThread(stdError, error);
			outT.join();
			errT.join();
			int exitcode = p.waitFor();
			if (exitcode != 0)
				throw new IllegalStateException("Cannot " + nameOfCmd + " " + gitRepoUrl + ": " + error);
			return out.toString();
		} catch (IllegalStateException ex) {
			throw ex;
		} catch (Exception e) {
			throw new IllegalStateException("Cannot " + nameOfCmd + " " + gitRepoUrl + ": " + e.getMessage(), e);
		}
	}

	private static Thread readInThread(final BufferedReader stdOut, final StringBuilder out) {
		Thread ret = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					String s1 = null;
					while ((s1 = stdOut.readLine()) != null) { out.append(s1 + "\n"); }
				} catch (IOException ex) {
					throw new IllegalStateException(ex);
				}
			}
		});
		ret.start();
		return ret;
	}
}
