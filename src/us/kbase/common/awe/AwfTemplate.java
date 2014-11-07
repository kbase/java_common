package us.kbase.common.awe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

public class AwfTemplate {
	private AwfInfo info;
	private List<AwfTask> tasks = new ArrayList<AwfTask>();
    private Map<java.lang.String, Object> additionalProperties = new HashMap<java.lang.String, Object>();
    
    @JsonAnyGetter
    public Map<java.lang.String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperties(java.lang.String name, Object value) {
        this.additionalProperties.put(name, value);
    }
	
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
