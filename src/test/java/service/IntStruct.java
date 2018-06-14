package us.kbase.common.service.test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * <p>Original spec-file type: int_struct</p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("com.googlecode.jsonschema2pojo")
@JsonPropertyOrder({
    "val1",
    "val2",
    "val3",
    "val4"
})
public class IntStruct {

    @JsonProperty("val1")
    private java.lang.Long val1;
    @JsonProperty("val2")
    private List<Long> val2;
    @JsonProperty("val3")
    private Map<String, Long> val3;
    @JsonProperty("val4")
    private Tuple2 <Long, Long> val4;
    private Map<java.lang.String, Object> additionalProperties = new HashMap<java.lang.String, Object>();

    @JsonProperty("val1")
    public java.lang.Long getVal1() {
        return val1;
    }

    @JsonProperty("val1")
    public void setVal1(java.lang.Long val1) {
        this.val1 = val1;
    }

    public IntStruct withVal1(java.lang.Long val1) {
        this.val1 = val1;
        return this;
    }

    @JsonProperty("val2")
    public List<Long> getVal2() {
        return val2;
    }

    @JsonProperty("val2")
    public void setVal2(List<Long> val2) {
        this.val2 = val2;
    }

    public IntStruct withVal2(List<Long> val2) {
        this.val2 = val2;
        return this;
    }

    @JsonProperty("val3")
    public Map<String, Long> getVal3() {
        return val3;
    }

    @JsonProperty("val3")
    public void setVal3(Map<String, Long> val3) {
        this.val3 = val3;
    }

    public IntStruct withVal3(Map<String, Long> val3) {
        this.val3 = val3;
        return this;
    }

    @JsonProperty("val4")
    public Tuple2 <Long, Long> getVal4() {
        return val4;
    }

    @JsonProperty("val4")
    public void setVal4(Tuple2 <Long, Long> val4) {
        this.val4 = val4;
    }

    public IntStruct withVal4(Tuple2 <Long, Long> val4) {
        this.val4 = val4;
        return this;
    }

    @JsonAnyGetter
    public Map<java.lang.String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperties(java.lang.String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    @Override
    public java.lang.String toString() {
        return ((((((((((("IntStruct"+" [val1=")+ val1)+", val2=")+ val2)+", val3=")+ val3)+", val4=")+ val4)+", additionalProperties=")+ additionalProperties)+"]");
    }

}
