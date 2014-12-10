package us.kbase.common.awe;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

public class AwfEnviron {
	private Map<String, String> public_ = new LinkedHashMap<String, String>();
	private Map<String, String> private_ = new LinkedHashMap<String, String>();
    private Map<java.lang.String, Object> additionalProperties = new HashMap<java.lang.String, Object>();
    
    @JsonAnyGetter
    public Map<java.lang.String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperties(java.lang.String name, Object value) {
        this.additionalProperties.put(name, value);
    }
	
	public Map<String, String> getPublic() {
		return public_;
	}
	
	public void setPublic(Map<String, String> public_) {
		this.public_ = public_;
	}
	
	public Map<String, String> getPrivate() {
		return private_;
	}
	
	public void setPrivate(Map<String, String> private_) {
		this.private_ = private_;
	}
}
