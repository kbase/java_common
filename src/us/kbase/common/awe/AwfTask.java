package us.kbase.common.awe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class AwfTask {
	private AwfCmd cmd;
	private List<String> dependsOn = new ArrayList<String>();
	private String taskid = "0";
	private int skip = 0;
	private int totalwork = 1;
	private Map<String, Map<String, String>> inputs = null;
	private Map<String, Map<String, String>> outputs = null;
	private String predata = null;
	private Integer maxworksize = null;
	private Integer remainwork = null;
	private String state = null;
	private String createddate = null;
	private String starteddate = null;
	private String completeddate = null;
	private Integer computetime = null;
	private Map<String, Object> userattr = null;
    @JsonProperty("AppVariables")
	private Map<String, Object> appVariables = null;
    private Map<java.lang.String, Object> additionalProperties = new HashMap<java.lang.String, Object>();
    
    @JsonAnyGetter
    public Map<java.lang.String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperties(java.lang.String name, Object value) {
        this.additionalProperties.put(name, value);
    }
	
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
	
	public Map<String, Map<String, String>> getInputs() {
		return inputs;
	}
	
	public void setInputs(Map<String, Map<String, String>> inputs) {
		this.inputs = inputs;
	}
	
	public Map<String, Map<String, String>> getOutputs() {
		return outputs;
	}
	
	public void setOutputs(Map<String, Map<String, String>> outputs) {
		this.outputs = outputs;
	}
	
	public String getPredata() {
		return predata;
	}
	
	public void setPredata(String predata) {
		this.predata = predata;
	}
	
	public Integer getMaxworksize() {
		return maxworksize;
	}
	
	public void setMaxworksize(Integer maxworksize) {
		this.maxworksize = maxworksize;
	}
	
	public Integer getRemainwork() {
		return remainwork;
	}
	
	public void setRemainwork(Integer remainwork) {
		this.remainwork = remainwork;
	}
	
	public String getState() {
		return state;
	}
	
	public void setState(String state) {
		this.state = state;
	}
	
	public String getCreateddate() {
		return createddate;
	}
	
	public void setCreateddate(String createddate) {
		this.createddate = createddate;
	}
	
	public String getStarteddate() {
		return starteddate;
	}
	
	public void setStarteddate(String starteddate) {
		this.starteddate = starteddate;
	}

	public String getCompleteddate() {
		return completeddate;
	}
	
	public void setCompleteddate(String completeddate) {
		this.completeddate = completeddate;
	}
	
	public Integer getComputetime() {
		return computetime;
	}
	
	public void setComputetime(Integer computetime) {
		this.computetime = computetime;
	}
	
	public Map<String, Object> getUserattr() {
		return userattr;
	}
	
	public void setUserattr(Map<String, Object> userattr) {
		this.userattr = userattr;
	}
	
    @JsonProperty("AppVariables")
	public Map<String, Object> getAppVariables() {
		return appVariables;
	}
	
    @JsonProperty("AppVariables")
	public void setAppVariables(Map<String, Object> appVariables) {
		this.appVariables = appVariables;
	}
}
