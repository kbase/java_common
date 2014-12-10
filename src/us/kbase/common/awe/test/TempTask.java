package us.kbase.common.awe.test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import us.kbase.common.awe.task.AweJobInterface;
import us.kbase.common.taskqueue.JobStatuses;

public class TempTask implements AweJobInterface {
	private String token;
	
	@Override
	public String getDescription() {
		return null;
	}
	
	@Override
	public Long getEstimatedFinishTime() {
		return null;
	}
	
	@Override
	public String getInitProgess() {
		return null;
	}
	
	@Override
	public void init(String jobId, String token, Map<String, String> config) throws Exception {
		this.token = token;
		System.out.println("Token: " + this.token);
	}
	
	@Override
	public JobStatuses getJobStatuses() throws Exception {
		return new JobStatuses() {
			@Override
			public void updateJob(String job, String token, String status,
					String estComplete) throws Exception {
				System.out.println("TempTask.updateJob: status=" + status);
			}
			
			@Override
			public String createAndStartJob(String token, String status, String desc,
					String initProgressPtype, String estComplete) throws Exception {
				System.out.println("TempTask.createAndStartJob: status=" + status);
				return "12345";
			}
			
			@Override
			public void completeJob(String job, String token, String status,
					String error, String wsUrl, String outRef) throws Exception {
				System.out.println("TempTask.completeJob: status=" + status);
			}
		};
	}
	
	public String listToMap(List<String> keyVals) {
		Map<String, String> ret = new LinkedHashMap<String, String>();
		for (int i = 0; i < keyVals.size(); i += 2) {
			ret.put(keyVals.get(i), keyVals.get(i + 1));
		}
		System.out.println("Map: " + ret);
		return null;
	}
}
