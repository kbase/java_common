package us.kbase.common.awe.test;

import java.util.Arrays;
import java.util.Map;

import us.kbase.common.awe.AweClient;
import us.kbase.common.awe.AweTaskHolder;
import us.kbase.common.awe.AwfEnviron;
import us.kbase.common.awe.AwfTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

public class AwePlaying {
	public static void main(String[] args) throws Exception {
		//testClient();
		testTaskHolder();
	}
	
	private static void testClient() throws Exception {
		AweClient client = new AweClient(AweClient.DEFAULT_DEV_SERVER_URL);
		AwfTemplate job = AweClient.createSimpleJobTemplate("protcmp_pipeline", "protcmp", "1 2 3 4 5 6 8", "java_generic_script");
		AwfEnviron env = new AwfEnviron();
		env.getPrivate().put("KB_AUTH_TOKEN", "secret1");
		job.getTasks().get(0).getCmd().setEnviron(env);
		System.out.println(new ObjectMapper().writeValueAsString(client.submitJob(job)));
	}
	
	private static void testTaskHolder() throws Exception {
		AweTaskHolder th = new AweTaskHolder();
		th.prepareTask(TempTask.class, "sevret1").listToMap(Arrays.asList("k1", "v2", "k3", "v4"));
	}
}
