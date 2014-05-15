package us.kbase.common.awe;

public class AwfInfo {
	private String pipeline;
	private String name;
	private String project = "default";
	private String user = "default";
	private String clientgroups = "";
	private String sessionId;
	
	public String getPipeline() {
		return pipeline;
	}
	
	public void setPipeline(String pipeline) {
		this.pipeline = pipeline;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getProject() {
		return project;
	}
	
	public void setProject(String project) {
		this.project = project;
	}
	
	public String getUser() {
		return user;
	}
	
	public void setUser(String user) {
		this.user = user;
	}
	
	public String getClientgroups() {
		return clientgroups;
	}
	
	public void setClientgroups(String clientgroups) {
		this.clientgroups = clientgroups;
	}
	
	public String getSessionId() {
		return sessionId;
	}
	
	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}
}
