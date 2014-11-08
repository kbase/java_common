package us.kbase.common.awe.test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import us.kbase.common.awe.AweJobInterface;

public class TempTask implements AweJobInterface {
	private String token;
	
	@Override
	public void init(String jobId, String token) throws Exception {
		this.token = token;
		System.out.println("Token: " + token);
	}
	
	public void listToMap(List<String> keyVals) {
		Map<String, String> ret = new LinkedHashMap<String, String>();
		for (int i = 0; i < keyVals.size(); i += 2) {
			ret.put(keyVals.get(i), keyVals.get(i + 1));
		}
		System.out.println("Map: " + ret);
	}
}
