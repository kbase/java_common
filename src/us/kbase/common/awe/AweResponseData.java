package us.kbase.common.awe;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

public class AweResponseData {
	private String id;
	private String jid;
	private AwfInfo info;
	private List<AwfTask> tasks;
	private String state;
	private Boolean registered;
	private Integer remaintasks;
	private String updatetime;
	private String notes;
	private Integer resumed;
	private String lastfailed;
	private String shockhost;
    private Map<java.lang.String, Object> additionalProperties = new HashMap<java.lang.String, Object>();
    
    @JsonAnyGetter
    public Map<java.lang.String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperties(java.lang.String name, Object value) {
        this.additionalProperties.put(name, value);
    }
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getJid() {
		return jid;
	}
	
	public void setJid(String jid) {
		this.jid = jid;
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
	
	public String getState() {
		return state;
	}
	
	public void setState(String state) {
		this.state = state;
	}
	
	public Boolean getRegistered() {
		return registered;
	}
	
	public void setRegistered(Boolean registered) {
		this.registered = registered;
	}
	
	public Integer getRemaintasks() {
		return remaintasks;
	}
	
	public void setRemaintasks(Integer remaintasks) {
		this.remaintasks = remaintasks;
	}
	
	public String getUpdatetime() {
		return updatetime;
	}
	
	public void setUpdatetime(String updatetime) {
		this.updatetime = updatetime;
	}
	
	public String getNotes() {
		return notes;
	}
	
	public void setNotes(String notes) {
		this.notes = notes;
	}
	
	public Integer getResumed() {
		return resumed;
	}
	
	public void setResumed(Integer resumed) {
		this.resumed = resumed;
	}
	
	public String getLastfailed() {
		return lastfailed;
	}
	
	public void setLastfailed(String lastfailed) {
		this.lastfailed = lastfailed;
	}
	
	public String getShockhost() {
		return shockhost;
	}
	
	public void setShockhost(String shockhost) {
		this.shockhost = shockhost;
	}
}
