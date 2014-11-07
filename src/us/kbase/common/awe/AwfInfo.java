package us.kbase.common.awe;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
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
	private String service = null;
	private Long priority = null;
	private Map<String, Object> userattr = null;
	private String description = null;
	private Boolean tracking = null;
    private Map<java.lang.String, Object> additionalProperties = new HashMap<java.lang.String, Object>();
    
    @JsonAnyGetter
    public Map<java.lang.String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperties(java.lang.String name, Object value) {
        this.additionalProperties.put(name, value);
    }
	
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
	
	public String getService() {
		return service;
	}
	
	public void setService(String service) {
		this.service = service;
	}
	
	public Long getPriority() {
		return priority;
	}
	
	public void setPriority(Long priority) {
		this.priority = priority;
	}
	
	public Map<String, Object> getUserattr() {
		return userattr;
	}
	
	public void setUserattr(Map<String, Object> userattr) {
		this.userattr = userattr;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public Boolean getTracking() {
		return tracking;
	}
	
	public void setTracking(Boolean tracking) {
		this.tracking = tracking;
	}
}
