
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
import us.kbase.common.service.UObject;


/**
 * <p>Original spec-file type: MethodCall</p>
 * <pre>
 * time - the time (in the format YYYY-MM-DDThh:mm:ssZ) the call was started;
 * service - service defined in standard JSON RPC way, typically it's
 *     module name from spec-file like 'KBaseTrees';
 * service_ver - specific version of deployed service;
 * method - name of funcdef from spec-file corresponding to running method,
 *     like 'construct_species_tree' from trees service;
 * method_params - the parameters of the method that performed this call;
 * token - user token (required for any asynchronous method);
 * job_id - job id if method is asynchronous (optional field).
 * </pre>
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("com.googlecode.jsonschema2pojo")
@JsonPropertyOrder({
    "time",
    "service",
    "service_ver",
    "method",
    "method_params",
    "job_id"
})
public class MethodCall {

    @JsonProperty("time")
    private String time;
    @JsonProperty("service")
    private String service;
    @JsonProperty("service_ver")
    private String serviceVer;
    @JsonProperty("method")
    private String method;
    @JsonProperty("method_params")
    private List<UObject> methodParams;
    @JsonProperty("job_id")
    private String jobId;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("time")
    public String getTime() {
        return time;
    }

    @JsonProperty("time")
    public void setTime(String time) {
        this.time = time;
    }

    public MethodCall withTime(String time) {
        this.time = time;
        return this;
    }

    @JsonProperty("service")
    public String getService() {
        return service;
    }

    @JsonProperty("service")
    public void setService(String service) {
        this.service = service;
    }

    public MethodCall withService(String service) {
        this.service = service;
        return this;
    }

    @JsonProperty("service_ver")
    public String getServiceVer() {
        return serviceVer;
    }

    @JsonProperty("service_ver")
    public void setServiceVer(String serviceVer) {
        this.serviceVer = serviceVer;
    }

    public MethodCall withServiceVer(String serviceVer) {
        this.serviceVer = serviceVer;
        return this;
    }

    @JsonProperty("method")
    public String getMethod() {
        return method;
    }

    @JsonProperty("method")
    public void setMethod(String method) {
        this.method = method;
    }

    public MethodCall withMethod(String method) {
        this.method = method;
        return this;
    }

    @JsonProperty("method_params")
    public List<UObject> getMethodParams() {
        return methodParams;
    }

    @JsonProperty("method_params")
    public void setMethodParams(List<UObject> methodParams) {
        this.methodParams = methodParams;
    }

    public MethodCall withMethodParams(List<UObject> methodParams) {
        this.methodParams = methodParams;
        return this;
    }

    @JsonProperty("job_id")
    public String getJobId() {
        return jobId;
    }

    @JsonProperty("job_id")
    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public MethodCall withJobId(String jobId) {
        this.jobId = jobId;
        return this;
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
        return ((((((((((((((("MethodCall"+" [time=")+ time)+", service=")+ service)+", serviceVer=")+ serviceVer)+", method=")+ method)+", methodParams=")+ methodParams)+", jobId=")+ jobId)+", additionalProperties=")+ additionalProperties)+"]");
    }

}
