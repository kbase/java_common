package us.kbase.common.awe;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

public class AweResponse {
	private Integer status;
	private AweResponseData data;
	private List<String> error;
    private Map<java.lang.String, Object> additionalProperties = new HashMap<java.lang.String, Object>();
    
    @JsonAnyGetter
    public Map<java.lang.String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperties(java.lang.String name, Object value) {
        this.additionalProperties.put(name, value);
    }
	
	public Integer getStatus() {
		return status;
	}
	
	public void setStatus(Integer status) {
		this.status = status;
	}
	
	public AweResponseData getData() {
		return data;
	}
	
	public void setData(AweResponseData data) {
		this.data = data;
	}
	
	public List<String> getError() {
		return error;
	}
	
	public void setError(List<String> error) {
		this.error = error;
	}
}
