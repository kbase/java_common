package us.kbase.common.performance.sortjson;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
//import java.lang.management.GarbageCollectorMXBean;
//import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.nocrala.tools.texttablefmt.Table;

import us.kbase.common.performance.PerformanceMeasurement;
import us.kbase.common.utils.sortjson.FastUTF8JsonSorter;
import us.kbase.common.utils.sortjson.LowMemoryUTF8JsonSorter;

//import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/** expects a JSON file with no whitespace for testing */
public class MeasureSortJsonSpeed {
	
	private static final ObjectMapper SORT_MAPPER = new ObjectMapper();
	static {
		SORT_MAPPER.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
		//Jackson 2.3 + only, comment out for 2.2 or <
//		SORT_MAPPER.configure(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY, true); 
	}

	final static boolean PAUSE_FOR_PROFILER = false;
	final static int NUM_SORTS = 5000;
	final static File FILE = new File("src/us/kbase/common/performance/sortjson/83333.2.txt");

	final static String JACKSON_SORT = "Jackson";
	final static String STRUCTURAL_SORT = "Structural";
	final static String BYTE_BYTE_SORT = "Byte";
	final static String BYTE_FILE_SORT = "File";
	final static List<String> ALL_SORTERS = Arrays.asList(
			JACKSON_SORT, STRUCTURAL_SORT, BYTE_BYTE_SORT, BYTE_FILE_SORT);
	
	public static void main(String[] args) throws Exception {
		final int numSorts;
		final File file;
		final Writer output;
		final Set<String> sorters = new HashSet<String>();
		if (args.length < 1) {
			System.out.println("started: " + new Date());
			System.out.println("max mem: " + Runtime.getRuntime().maxMemory());
			System.out.println("file: " + FILE);
			System.out.println("size: " + FILE.length());
			numSorts = NUM_SORTS;
			file = FILE;
			output = new OutputStreamWriter(System.out);
			sorters.addAll(ALL_SORTERS);
		} else {
			numSorts = Integer.parseInt(args[0]);
			file = new File(args[1]);
			file.createNewFile();
			output = new FileWriter(new File(args[2]));
			for (int i = 3; i < args.length; i++) {
				sorters.add(args[i]);
			}
			if (sorters.isEmpty()) {
				sorters.addAll(ALL_SORTERS);
			}
		}
		
		byte[] b = Files.readAllBytes(file.toPath());
		if (PAUSE_FOR_PROFILER) {
			System.out.println("File read into bytes[]. Start profiler, then hit enter to continue");
			Scanner s = new Scanner(System.in);
			s.nextLine();
		}
		if (args.length < 1) {
			System.out.println("Starting tests");
		}
		
//		System.err.println("Java version: " + System.getProperty("java.version"));
//		System.err.println("Mem: total: " + Runtime.getRuntime().totalMemory() + 
//				" max: " + Runtime.getRuntime().maxMemory());
//		
//		for (GarbageCollectorMXBean g: ManagementFactory.getGarbageCollectorMXBeans()) {
//			System.err.println(g.getName() + " - Valid: " + g.isValid());
//			String[] m = g.getMemoryPoolNames();
//			for (int i = 0; i < m.length; i++) {
//				System.err.println("\t" + m[i]);
//			}
//		}
		
		List<PerformanceMeasurement> meas = new ArrayList<PerformanceMeasurement>();
		if (sorters.contains(JACKSON_SORT)) {
			meas.add(measureJsonSort(b, numSorts));
		}
		if (sorters.contains(STRUCTURAL_SORT)) {
			meas.add(measureSKJBSort(b, numSorts));
		}
		if (sorters.contains(BYTE_BYTE_SORT)) {
			meas.add(measureSKJFSort(b, numSorts));
		}
		b = null;
		if (sorters.contains(BYTE_FILE_SORT)) {
			meas.add(measureSKJFSort(file, numSorts));
		}

		if (args.length < 1) {
			System.out.println("Complete: " + new Date());
		}
		
		renderResults(meas, output);
		output.close();
	}
	
	static void renderResults(List<PerformanceMeasurement> pms, Writer w)
			throws Exception {
		final int width = 4;
		Table tbl = new Table(width);
		tbl.addCell("Operation");
		tbl.addCell("N");
		tbl.addCell("Mean time (s)");
		tbl.addCell("Std dev (s)");
		for (PerformanceMeasurement pm: pms) {
			tbl.addCell(pm.getName());
			tbl.addCell("" + pm.getN());
			tbl.addCell(String.format("%,.4f", pm.getAverageInSec()));
			tbl.addCell(String.format("%,.4f", pm.getStdDevInSec()));
		}
		w.write(tbl.render());
	}

	@SuppressWarnings("unused")
	private static PerformanceMeasurement measureSKJFSortStringKeys(byte[] b, int sorts)
			throws Exception {
		List<Long> m = new LinkedList<Long>();
		for (int i = 0; i < sorts; i++) {
			long start = System.nanoTime();
			new LowMemoryUTF8JsonSorter(b).setUseStringsForKeyStoring(true)
					.writeIntoStream(new NullOutputStream());
			m.add(System.nanoTime() - start);
		}
		return new PerformanceMeasurement(m, "SortedKeysJsonFile JSON sort with String keys");
	}
	
	static PerformanceMeasurement measureSKJFSort(byte[] b, int sorts)
			throws Exception {
		List<Long> m = new LinkedList<Long>();
		for (int i = 0; i < sorts; i++) {
			long start = System.nanoTime();
			new LowMemoryUTF8JsonSorter(b).writeIntoStream(new NullOutputStream());
			m.add(System.nanoTime() - start);
		}
		return new PerformanceMeasurement(m, "SortedKeysJsonFile JSON byte sort");
	}

	/**Writes to nowhere*/
	static class NullOutputStream extends OutputStream {
		@Override
		public void write(int b) throws IOException {
		}
	}

	static PerformanceMeasurement measureSKJFSort(File temp, int sorts)
			throws Exception {
		List<Long> m = new LinkedList<Long>();
		for (int i = 0; i < sorts; i++) {
			long start = System.nanoTime();
			LowMemoryUTF8JsonSorter sk = new LowMemoryUTF8JsonSorter(temp);
			sk.writeIntoStream(new NullOutputStream());
			m.add(System.nanoTime() - start);
		}
		return new PerformanceMeasurement(m, "SortedKeysJsonFile JSON file sort");
	}
	
	static PerformanceMeasurement measureSKJBSort(byte[] b, int sorts)
			throws Exception {
		List<Long> m = new LinkedList<Long>();
		for (int i = 0; i < sorts; i++) {
			long start = System.nanoTime();
			new FastUTF8JsonSorter(b).writeIntoStream(new NullOutputStream());
			m.add(System.nanoTime() - start);
		}
		return new PerformanceMeasurement(m, "SortedKeysJsonBytes JSON sort");
	}

	static PerformanceMeasurement measureJsonSort(byte[] b, int sorts)
			throws Exception {
		List<Long> m = new LinkedList<Long>();
		for (int i = 0; i < sorts; i++) {
			long start = System.nanoTime();
			JsonNode jn = SORT_MAPPER.readTree(b); //detects duplicate keys with Jackson 2.3+
			@SuppressWarnings("unchecked")
			Map<String, Object> d = SORT_MAPPER.treeToValue(jn, Map.class);
			jn = null;
			SORT_MAPPER.writeValue(new NullOutputStream(), d);
			m.add(System.nanoTime() - start);
		}
		return new PerformanceMeasurement(m, "ObjectMapper JSON sort");
	}
}
