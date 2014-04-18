package us.kbase.common.performance.sortjson;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class MeasureSortRunner {
	
	final static int NUM_SORTS = 500;
	final static int TIME_INTERVAL = 100; //ms
	final static String FILE = "src/us/kbase/common/performance/sortjson/83333.2.txt";
	final static String SORTER = "SortedJsonBytes";
	
	final static List<String> SORTERS = new ArrayList<String>();
	static {
		SORTERS.add("Jackson");
		SORTERS.add("SortedJsonBytes");
		SORTERS.add("SortedJsonFile-bytes");
		SORTERS.add("SortedJsonFile-file");
	}
	
	final static List<String> JARS = new ArrayList<String>();
	static {
		JARS.add("../jars/lib/jars/jackson/jackson-annotations-2.2.3.jar");
		JARS.add("../jars/lib/jars/jackson/jackson-core-2.2.3.jar");
		JARS.add("../jars/lib/jars/jackson/jackson-databind-2.2.3.jar");
	}
	
	final static String CODE_ROOT = "src";
	final static String MEAS_CLASS_FILE =
			"us.kbase.common.performance.sortjson.MeasureSortJsonMem";
	
	final static String MEAS_JAVA_FILE =
			CODE_ROOT + "/" + MEAS_CLASS_FILE.replace(".", "/");
	
	public static void main(String[] args) throws Exception {
		String classpath = CODE_ROOT;
		for (String j: JARS) {
			classpath += ":" + j;
		}
		
		compileMeasureSort(classpath);
		
		Process p = Runtime.getRuntime().exec(new String [] {
				"java", "-cp", classpath, MEAS_CLASS_FILE,
				Integer.toString(NUM_SORTS), Integer.toString(TIME_INTERVAL), FILE, SORTER
		});
		p.waitFor();
		System.out.println("STDOUT:");
		print(p.getInputStream());
		System.out.println("STDERR:");
		print(p.getErrorStream());
	}

	private static void compileMeasureSort(String classpath)
			throws IOException, InterruptedException {
		Process p = Runtime.getRuntime().exec(new String[] {
				"javac", "-cp", classpath, MEAS_JAVA_FILE + ".java"});
		p.waitFor();
		
		if (p.exitValue() != 0) {
			System.out.println("Compile failed with exit value " + p.exitValue());
			System.out.println("STDOUT:");
			print(p.getInputStream());
			System.out.println("STDERR:");
			print(p.getErrorStream());
			System.exit(1);
		}
	}

	private static void print(InputStream is) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		while (true) {
			String l = br.readLine();
			if (l == null) break;
			System.out.println(l);
		}
		br.close();
	}
}
