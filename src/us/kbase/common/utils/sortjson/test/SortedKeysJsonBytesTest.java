package us.kbase.common.utils.sortjson.test;

import java.nio.charset.Charset;

import junit.framework.Assert;

import org.junit.Test;

import us.kbase.common.utils.sortjson.KeyDuplicationException;
import us.kbase.common.utils.sortjson.FastUTF8JsonSorter;

public class SortedKeysJsonBytesTest {
	
	@Test
	public void testArrayWithMap() {
		assertSort(
				"[1, 2.0 , \"4{\\\"\", {\"kkk\":\"vvv\",\n\"aaa\":\"bbb\" } , \"}3\\\\\", true ]", 
				"[1,2.0,\"4{\\\"\",{\"aaa\":\"bbb\",\"kkk\":\"vvv\"},\"}3\\\\\",true]");
	}

	@Test
	public void testMapWithMaps() {
		assertSort(
				"{\"kkk\":[1,{\"k2\":\"vvv\",\"k1\":\"v1\"},null], \"aaa\":{\"bbb\":{}}}", 
				"{\"aaa\":{\"bbb\":{}},\"kkk\":[1,{\"k1\":\"v1\",\"k2\":\"vvv\"},null]}");
	}

	@Test
	public void testDoubleKeys() {
		assertSort("{\"kkk\":1, \"kkk\":2}", "{\"kkk\":1}");
	}

	@Test
	public void testDoubleKeysError() throws Exception {
		try {
			sort("{\"1\":-1,\"ro/ot\":[0,1,{\"kkk\":1, \"kkk\":2}]}", false);
			Assert.fail("Should be exception");
		} catch (KeyDuplicationException e) {
			Assert.assertEquals("Duplicated key 'kkk' was found at /ro\\/ot/2", e.getMessage());
		}
	}

	private static void assertSort(String before, String after) {
		try {
			String actual = sort(before, true);
			Assert.assertEquals(after, actual);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
	
	private static String sort(String json, boolean skipDoubleKeys) throws Exception {
		Charset ch = Charset.forName("UTF-8");
		byte[] data = json.getBytes(ch);
		return new String(new FastUTF8JsonSorter(data).setSkipKeyDuplication(skipDoubleKeys).getSorted(), ch);
	}
}
