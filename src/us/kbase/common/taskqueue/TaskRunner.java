package us.kbase.common.taskqueue;

import java.util.Map;

public interface TaskRunner <T> {
	
	public void init(TaskQueueConfig mainCfg, Map<String, String> configParams);
	
	public Class<T> getInputDataType();
	
	public String getTaskDescription();
	
	public String getOutRef(T inputData);
	
	public void run(String token, T inputData, TaskQueueConfig config) throws Exception;
}
