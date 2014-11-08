package us.kbase.common.awe;

import java.lang.reflect.Method;
import java.util.List;

import us.kbase.common.service.UObject;

public class JavaGenericScript {
	public static void main(String[] args) throws Exception {
		if (args.length < 4 || args.length > 5) {
			System.out.println("Usage: <java_generic_script> <class_name> <method_name> <input_json> <job_id> [<token_env_var>]");
			return;
		}
		String className = args[0];
		String methodName = args[1];
		String inputJson = args[2];
		String jobId = args[3];
		String tokenVar = args.length == 5 ? args[4] : null;
		run(className, methodName, inputJson, jobId, tokenVar);
	}
	
	public static void run(String className, String methodName, String inputJson, String jobId, String token) throws Exception {
		Class<?> type = Class.forName(className);
		Object inst = type.newInstance();
		if (inst instanceof AweJobInterface) {
			((AweJobInterface)inst).init(jobId, token);
		}
		for (Method m : type.getMethods()) {
			if (m.getName().equals(methodName)) {
				@SuppressWarnings("unchecked")
				List<Object> inputVals = UObject.getMapper().readValue(inputJson, List.class);
				Object[] methodArgs = new Object[m.getParameterTypes().length];
				for (int i = 0; i < methodArgs.length; i++)
					methodArgs[i] = UObject.transformObjectToObject(inputVals.get(i), m.getParameterTypes()[i]);
				m.invoke(inst, methodArgs);
			}
		}
	}
}
