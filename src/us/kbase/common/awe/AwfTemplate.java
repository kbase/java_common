package us.kbase.common.awe;

import java.util.ArrayList;
import java.util.List;

public class AwfTemplate {
	private AwfInfo info;
	private List<AwfTask> tasks = new ArrayList<AwfTask>();
	
	public AwfInfo getInfo() {
		return info;
	}
	
	public void setInfo(AwfInfo info) {
		this.info = info;
	}
	
	public List<AwfTask> getTasks() {
		return tasks;
	}
	
	public void setTasks(List<AwfTask> tasks) {
		this.tasks = tasks;
	}
}
