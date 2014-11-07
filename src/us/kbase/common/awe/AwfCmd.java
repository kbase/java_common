package us.kbase.common.awe;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class AwfCmd {
	private String args;
	private String description = "";
	private String name;
	private AwfEnviron environ = null;
	private List<Object> app_args = null;
	private List<Object> cmd_script = null;
    @JsonProperty("Dockerimage")
	private String dockerimage = null;
	private Boolean has_private_env = null;
    private Map<java.lang.String, Object> additionalProperties = new HashMap<java.lang.String, Object>();
    
    @JsonAnyGetter
    public Map<java.lang.String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperties(java.lang.String name, Object value) {
        this.additionalProperties.put(name, value);
    }
	
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
	
	public List<Object> getApp_args() {
		return app_args;
	}
	
	public void setApp_args(List<Object> app_args) {
		this.app_args = app_args;
	}
	
	public List<Object> getCmd_script() {
		return cmd_script;
	}
	
	public void setCmd_script(List<Object> cmd_script) {
		this.cmd_script = cmd_script;
	}
	
    @JsonProperty("Dockerimage")
	public String getDockerimage() {
		return dockerimage;
	}
	
    @JsonProperty("Dockerimage")
	public void setDockerimage(String dockerimage) {
		this.dockerimage = dockerimage;
	}
	
	public Boolean getHas_private_env() {
		return has_private_env;
	}
	
	public void setHas_private_env(Boolean has_private_env) {
		this.has_private_env = has_private_env;
	}
}
