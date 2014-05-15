package us.kbase.common.awe;

import java.util.ArrayList;
import java.util.List;

public class AwfTask {
	private AwfCmd cmd;
	private List<String> dependsOn = new ArrayList<String>();
	private String taskid = "0";
	private int skip = 0;
	private int totalwork = 1;
	
	public AwfCmd getCmd() {
		return cmd;
	}
	
	public void setCmd(AwfCmd cmd) {
		this.cmd = cmd;
	}
	
	public List<String> getDependsOn() {
		return dependsOn;
	}
	
	public void setDependsOn(List<String> dependsOn) {
		this.dependsOn = dependsOn;
	}
	
	public String getTaskid() {
		return taskid;
	}
	
	public void setTaskid(String taskid) {
		this.taskid = taskid;
	}
	
	public int getSkip() {
		return skip;
	}
	
	public void setSkip(int skip) {
		this.skip = skip;
	}
	
	public int getTotalwork() {
		return totalwork;
	}
	
	public void setTotalwork(int totalwork) {
		this.totalwork = totalwork;
	}
}
