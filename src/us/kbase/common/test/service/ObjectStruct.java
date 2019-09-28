package us.kbase.common.test.service;

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
 * <p>Original spec-file type: object_struct</p>
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
public class ObjectStruct {

    @JsonProperty("val1")
    private UObject val1;
    @JsonProperty("val2")
    private List<UObject> val2;
    @JsonProperty("val3")
    private Map<String, UObject> val3;
    @JsonProperty("val4")
    private Tuple2 <UObject, UObject> val4;
    private Map<java.lang.String, Object> additionalProperties = new HashMap<java.lang.String, Object>();

    @JsonProperty("val1")
    public UObject getVal1() {
        return val1;
    }

    @JsonProperty("val1")
    public void setVal1(UObject val1) {
        this.val1 = val1;
    }

    public ObjectStruct withVal1(UObject val1) {
        this.val1 = val1;
        return this;
    }

    @JsonProperty("val2")
    public List<UObject> getVal2() {
        return val2;
    }

    @JsonProperty("val2")
    public void setVal2(List<UObject> val2) {
        this.val2 = val2;
    }

    public ObjectStruct withVal2(List<UObject> val2) {
        this.val2 = val2;
        return this;
    }

    @JsonProperty("val3")
    public Map<String, UObject> getVal3() {
        return val3;
    }

    @JsonProperty("val3")
    public void setVal3(Map<String, UObject> val3) {
        this.val3 = val3;
    }

    public ObjectStruct withVal3(Map<String, UObject> val3) {
        this.val3 = val3;
        return this;
    }

    @JsonProperty("val4")
    public Tuple2 <UObject, UObject> getVal4() {
        return val4;
    }

    @JsonProperty("val4")
    public void setVal4(Tuple2 <UObject, UObject> val4) {
        this.val4 = val4;
    }

    public ObjectStruct withVal4(Tuple2 <UObject, UObject> val4) {
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
        return ((((((((((("ObjectStruct"+" [val1=")+ val1)+", val2=")+ val2)+", val3=")+ val3)+", val4=")+ val4)+", additionalProperties=")+ additionalProperties)+"]");
    }

}
