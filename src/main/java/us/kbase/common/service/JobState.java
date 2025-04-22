package us.kbase.common.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


// NOTE: This class is not used in this repo, but is required for kb_sdk[_plus] Java async clients.

/**
 * <p>Original spec-file type: JobState</p>
 * <pre>
 * finished - indicates whether job is done (including error cases) or not,
 *     if the value is true then either of 'returned_data' or 'detailed_error'
 *     should be defined;
 * ujs_url - url of UserAndJobState service used by job service
 * status - tuple returned by UserAndJobState.get_job_status method
 * result - keeps exact copy of what original server method puts
 *     in result block of JSON RPC response;
 * error - keeps exact copy of what original server method puts
 *     in error block of JSON RPC response.
 * </pre>
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("com.googlecode.jsonschema2pojo")
@JsonPropertyOrder({
    "finished",
    "ujs_url",
    "status",
    "result",
    "error"
})
public class JobState<T> {

    @JsonProperty("finished")
    private Long finished;
    @JsonProperty("ujs_url")
    private String ujsUrl;
    @JsonProperty("status")
    private List<Object> status;
    @JsonProperty("result")
    private T result;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("finished")
    public Long getFinished() {
        return finished;
    }

    @JsonProperty("finished")
    public void setFinished(Long finished) {
        this.finished = finished;
    }

    @JsonProperty("ujs_url")
    public String getUjsUrl() {
        return ujsUrl;
    }

    @JsonProperty("ujs_url")
    public void setUjsUrl(String ujsUrl) {
        this.ujsUrl = ujsUrl;
    }

    @JsonProperty("status")
    public List<Object> getStatus() {
        return status;
    }

    @JsonProperty("status")
    public void setStatus(List<Object> status) {
        this.status = status;
    }

    @JsonProperty("result")
    public T getResult() {
        return result;
    }

    @JsonProperty("result")
    public void setResult(T result) {
        this.result = result;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperties(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    @Override
    public String toString() {
        return ((((((((((("JobState"+" [finished=")+ finished)+", ujsUrl=")+ ujsUrl)+", status=")+ status)+", result=")+ result)+", additionalProperties=")+ additionalProperties)+"]");
    }
}

