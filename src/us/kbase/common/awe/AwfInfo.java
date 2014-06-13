package us.kbase.common.awe;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class AwfInfo {
	private String pipeline;
	private String name;
	private String project = "default";
	private String user = "default";
	private String clientgroups = "";
	private String xref = null;
	private String submittime = null;
	private String startedtime = null;
	private String completedtime = null;
	private Boolean auth = null;
	private Boolean noretry = null;
	
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
	
	public String getXref() {
		return xref;
	}
	
	public void setXref(String xref) {
		this.xref = xref;
	}
	
	public String getSubmittime() {
		return submittime;
	}
	
	public void setSubmittime(String submittime) {
		this.submittime = submittime;
	}
	
	public String getStartedtime() {
		return startedtime;
	}
	
	public void setStartedtime(String startedtime) {
		this.startedtime = startedtime;
	}
	
	public String getCompletedtime() {
		return completedtime;
	}
	
	public void setCompletedtime(String completedtime) {
		this.completedtime = completedtime;
	}
	
	public Boolean getAuth() {
		return auth;
	}
	
	public void setAuth(Boolean auth) {
		this.auth = auth;
	}
	
	public Boolean getNoretry() {
		return noretry;
	}
	
	public void setNoretry(Boolean noretry) {
		this.noretry = noretry;
	}
}
