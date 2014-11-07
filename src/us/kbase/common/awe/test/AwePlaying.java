package us.kbase.common.awe.test;

import us.kbase.common.awe.AweClient;
import us.kbase.common.awe.AwfEnviron;
import us.kbase.common.awe.AwfTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

public class AwePlaying {
	public static void main(String[] args) throws Exception {
		AweClient client = new AweClient(AweClient.DEFAULT_DEV_SERVER_URL);
		AwfTemplate job = AweClient.createSimpleJobTemplate("protcmp_pipeline", "protcmp", "1 2 3 4 5 6 7", "java_generic_script");
		AwfEnviron env = new AwfEnviron();
		env.getPrivate().put("KB_AUTH_TOKEN", "secret1");
		job.getTasks().get(0).getCmd().setEnviron(env);
		System.out.println(new ObjectMapper().writeValueAsString(client.submitJob(job)));
	}
}
