package us.kbase.common.awe;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class AwfCmd {
	private String args;
	private String description = "";
	private String name;
	private AwfEnviron environ = null;
	
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
	
	public AwfEnviron getEnviron() {
		return environ;
	}
	
	public void setEnviron(AwfEnviron environ) {
		this.environ = environ;
	}
}
