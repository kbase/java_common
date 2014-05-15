package us.kbase.common.awe;

public class AwfCmd {
	private String args;
	private String description = "";
	private String name;
	
	public String getArgs() {
		return args;
	}
	
	public void setArgs(String args) {
		this.args = args;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
}
