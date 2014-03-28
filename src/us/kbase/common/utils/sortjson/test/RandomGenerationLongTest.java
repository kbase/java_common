package us.kbase.common.utils.sortjson.test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import us.kbase.common.utils.sortjson.SortedKeysJsonFile;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class RandomGenerationLongTest {
	@Test
	public void testRandom() throws Exception {
		Random r = new Random(1234567890L);
		File dir = new File("temp_files");
		if (!dir.exists())
			dir.mkdir();
		File tempFile = File.createTempFile("tmp_rnd", ".json", dir);
		int maxSize = 0;
		for (int i = 0; i < 300; i++) {
			long timeGener = System.currentTimeMillis();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			JsonGenerator jgen = new ObjectMapper().getFactory().createGenerator(baos);
			generateRandomData(r, new int[] {0}, 0, jgen);
			jgen.close();
			byte[] unsertedJson = baos.toByteArray();
			timeGener = System.currentTimeMillis() - timeGener;
			if (maxSize < unsertedJson.length)
				maxSize = unsertedJson.length;
			long timeJackson = System.currentTimeMillis();
			byte[] expectedJson = sortWithJackson(unsertedJson);
			timeJackson = System.currentTimeMillis() - timeJackson;
			long timeBytes = System.currentTimeMillis();
			byte[] actualJson = sortWithFileSorter(unsertedJson, null);
			timeBytes = System.currentTimeMillis() - timeBytes;
			Assert.assertArrayEquals("i=" + i, expectedJson, actualJson);
			actualJson = null;
			long timeFile = System.currentTimeMillis();
			actualJson = sortWithFileSorter(unsertedJson, tempFile);
			timeFile = System.currentTimeMillis() - timeFile;
			Assert.assertArrayEquals("i=" + i + " (files)", expectedJson, actualJson);
			//System.out.println("i=" + i + ", size=" + unsertedJson.length + ", " +
			//		"Tg=" + timeGener + ", Tj=" + timeJackson + ", Tb=" + timeBytes + ", Tf=" + timeFile);
		}
		System.out.println("Max.size: " + maxSize);
	}
	
	private static byte[] sortWithFileSorter(byte[] data, File tempFile) throws Exception {
		SortedKeysJsonFile sorter;
		if (tempFile != null) {
			FileOutputStream fos = new FileOutputStream(tempFile);
			fos.write(data);
			fos.close();
			sorter = new SortedKeysJsonFile(tempFile);
		} else {
			sorter = new SortedKeysJsonFile(data);
		}
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		sorter.writeIntoStream(baos).close();
		baos.close();
		if (tempFile != null)
			tempFile.delete();
		return baos.toByteArray();
	}
	
	private static byte[] sortWithJackson(byte[] json) throws Exception {
		ObjectMapper SORT_MAPPER = new ObjectMapper();
		SORT_MAPPER.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
		Object d = SORT_MAPPER.readValue(json, Object.class);
		return SORT_MAPPER.writeValueAsBytes(d);
	}
	
	private static void generateRandomData(Random r, int[] largeStringCount, int depth, 
			JsonGenerator out) throws Exception {
		boolean isPrimitive = depth >=9 || r.nextInt(2) == 0;  // On 10th level we choose only primitives
		if (isPrimitive) {
			int typeKind = r.nextInt(4);	
			switch (typeKind) {
			case 0: out.writeNumber(r.nextLong()); break;
			case 1: out.writeNumber(r.nextDouble()); break;
			case 2: {
				if (r.nextInt(3) == 0) {
					out.writeNull();
				} else {
					out.writeBoolean(r.nextInt(2) == 0);
				}
			}
			break;
			case 3: {
				int textLen;
				if (r.nextInt(10) == 0 && largeStringCount[0] < 5) {
					textLen = r.nextInt(5000000);	// length of string is from 0 to 5M in 10% of the cases
					largeStringCount[0]++;
				} else {
					textLen = r.nextInt(1000);	// length of string is from 0 to 999 in 90% cases
				}
				out.writeString(generateRandomString(r, textLen));
			}
			break;
			default: throw new IllegalStateException("Unsupported type code: " + typeKind);
			}
		} else {
			int typeKind = r.nextInt(2);	
			switch (typeKind) {
			case 0: {
				out.writeStartArray();
				int size = r.nextInt(11);	// size of list can be from 0 to 10
				for (int i = 0; i < size; i++)
					generateRandomData(r, largeStringCount, depth + 1, out);
				out.writeEndArray();
			}
			break;
			case 1 : {
				out.writeStartObject();
				int size = r.nextInt(11);	// size of list can be from 0 to 10
				for (int i = 0; i < size; i++) {
					out.writeFieldName(generateRandomString(r, 100));
					generateRandomData(r, largeStringCount, depth + 1, out);
				}
				out.writeEndObject();
			}
			break;
			default: throw new IllegalStateException("Unsupported type code: " + typeKind);
			}
		}
	}

	private static String generateRandomString(Random r, int len) {
		char[] arr = new char[len];
		for (int i = 0; i < len; i++)
			arr[i] = (char)(32 + r.nextInt(127 - 32));
		return new String(arr);
	}
}
