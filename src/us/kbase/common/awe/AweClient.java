package us.kbase.common.awe;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

public class AweClient {
	public static final String DEFAULT_SERVER_URL = "http://kbase.us/services/awe-api";
	
	private final String serverUrl;
	
	public AweClient() {
		this(DEFAULT_SERVER_URL);
	}
	
	public AweClient(String serverUrl) {
		this.serverUrl = serverUrl;
	}
	
	private String getServerUrl() {
		String ret = serverUrl;
		if (!ret.endsWith("/"))
			ret = ret + "/";
		return ret;
	}
	
	public static AwfTemplate createSimpleJobTemplate(String pipeline, String jobName, String args, String scriptName) {
		AwfTemplate ret = new AwfTemplate();
		AwfInfo info = new AwfInfo();
		info.setPipeline(pipeline);
		info.setName(jobName);
		ret.setInfo(info);
		AwfTask task = new AwfTask();
		AwfCmd cmd = new AwfCmd();
		cmd.setArgs(args);
		cmd.setName(scriptName);
		task.setCmd(cmd);
		ret.getTasks().add(task);
		return ret;
	}
	
	public AweResponse submitJob(AwfTemplate job) throws IOException {
		CloseableHttpClient httpClient = HttpClients.createDefault();
		HttpPost httpPost = new HttpPost(getServerUrl() + "job");
		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		ObjectMapper mapper = new ObjectMapper();
		mapper.writeValue(buffer, job);
		builder.addBinaryBody("upload", buffer.toByteArray(),
				ContentType.APPLICATION_OCTET_STREAM, "tempjob.json");
		httpPost.setEntity(builder.build());
		HttpResponse response = httpClient.execute(httpPost);
		String postResponse = EntityUtils.toString(response.getEntity());
		System.out.println(postResponse);
		return mapper.readValue(postResponse, AweResponse.class);
	}
}
