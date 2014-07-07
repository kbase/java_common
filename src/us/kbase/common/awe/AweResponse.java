package us.kbase.common.awe;

import java.util.List;

public class AweResponse {
	private Integer status;
	private Data data;
	private List<String> error;
	
	public Integer getStatus() {
		return status;
	}
	
	public void setStatus(Integer status) {
		this.status = status;
	}
	
	public Data getData() {
		return data;
	}
	
	public void setData(Data data) {
		this.data = data;
	}
	
	public List<String> getError() {
		return error;
	}
	
	public void setError(List<String> error) {
		this.error = error;
	}
	
	public static class Data {
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
	}
}
