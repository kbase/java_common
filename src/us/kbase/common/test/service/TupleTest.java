package us.kbase.common.test.service;

import junit.framework.Assert;

import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import us.kbase.common.service.UObject;

public class TupleTest {

    @Test
    public void testOk() throws Exception {
        String json = "[1,\"test\"]";
        Tuple2<Integer, String> t = UObject.getMapper().readValue(json, new TypeReference<Tuple2<Integer, String>>() {});
        Assert.assertEquals(Integer.valueOf(1), t.getE1());
        Assert.assertEquals("test", t.getE2());
        String json2 = "[1,\"test\",\"added_later\"]";
        Tuple2<Integer, String> t2 = UObject.getMapper().readValue(json2, new TypeReference<Tuple2<Integer, String>>() {});
        Assert.assertEquals(0, t2.getAdditionalProperties().size());
    }
    
    @Test
    public void testBad() throws Exception {
        try {
            UObject.getMapper().readValue("1", new TypeReference<Tuple2<Integer, String>>() {});
            Assert.fail("Error should happen");
        } catch (JsonMappingException ex) {
            Assert.assertTrue(ex.getMessage().contains("Tuple array is expected but found"));
        }
        try {
            UObject.getMapper().readValue("2.0", new TypeReference<Tuple2<Integer, String>>() {});
            Assert.fail("Error should happen");
        } catch (JsonMappingException ex) {
            Assert.assertTrue(ex.getMessage().contains("Tuple array is expected but found"));
        }
        try {
            UObject.getMapper().readValue("\"test\"", new TypeReference<Tuple2<Integer, String>>() {});
            Assert.fail("Error should happen");
        } catch (JsonMappingException ex) {
            Assert.assertTrue(ex.getMessage().contains("Tuple array is expected but found"));
        }
        try {
            UObject.getMapper().readValue("{}", new TypeReference<Tuple2<Integer, String>>() {});
            Assert.fail("Error should happen");
        } catch (JsonMappingException ex) {
            Assert.assertTrue(ex.getMessage().contains("Tuple array is expected but found"));
        }
        try {
            UObject.getMapper().readValue("[]", new TypeReference<Tuple2<Integer, String>>() {});
            Assert.fail("Error should happen");
        } catch (JsonMappingException ex) {
            Assert.assertTrue(ex.getMessage().contains("No content to map due to end-of-input"));
        }
        try {
            UObject.getMapper().readValue("[\"test\",\"test\"]", new TypeReference<Tuple2<Integer, String>>() {});
            Assert.fail("Error should happen");
        } catch (InvalidFormatException ex) {
            System.out.println(ex.getMessage());
            Assert.assertTrue(ex.getMessage().contains("Can not construct instance of int from String value"));
        }
    }
}
