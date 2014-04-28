package us.kbase.common.utils.sortjson.test;

import java.nio.charset.Charset;

import junit.framework.Assert;

import org.junit.Test;

import us.kbase.common.utils.sortjson.KeyDuplicationException;
import us.kbase.common.utils.sortjson.SortedKeysJsonBytes;

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

	@Test
	public void testKeyCaching() throws Exception {
		sort("[{\"keyForFieldNumber1\":\"val1\",\"keyForFieldNumber2\":\"val1\",\"keyForFieldNumber3\":\"val1\",\"keyForFieldNumber4\":\"val1\",\"keyForFieldNumber5\":\"val1\"},{\"keyForFieldNumber1\":\"val2\"}]", false, true);
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
		return sort(json, skipDoubleKeys, false);
	}
	
	private static String sort(String json, boolean skipDoubleKeys, boolean cacheKeys) throws Exception {
		Charset ch = Charset.forName("UTF-8");
		byte[] data = json.getBytes(ch);
		return new String(new SortedKeysJsonBytes(data).setSkipKeyDuplication(skipDoubleKeys)
				.setUseCacheForKeys(cacheKeys).getSorted(), ch);
	}
}
