package us.kbase.common.performance.sortjson;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.nocrala.tools.texttablefmt.Table;

import us.kbase.common.performance.PerformanceMeasurement;
import us.kbase.common.utils.sortjson.SortedKeysJsonBytes;
import us.kbase.common.utils.sortjson.SortedKeysJsonFile;

import com.fasterxml.jackson.databind.DeserializationFeature;
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
	
	public static void main(String[] args) throws Exception {
		System.out.println("started: " + new Date());
		System.out.println("max mem: " + Runtime.getRuntime().maxMemory());
		System.out.println("file: " + FILE);
		
		System.out.println("size: " + FILE.length());
		byte[] b = Files.readAllBytes(FILE.toPath());
		if (PAUSE_FOR_PROFILER) {
			System.out.println("File read into bytes[]. Start profiler, then hit enter to continue");
			Scanner s = new Scanner(System.in);
			s.nextLine();
		}
		System.out.println("Starting tests");
		
//		RecordMem memjs = new RecordMem(100, "Jackson");
//		Thread.sleep(1000);
		PerformanceMeasurement js = measureJsonSort(b, NUM_SORTS);
//		memjs.stop();
//		
//		RecordMem memskjb = new RecordMem(100, "SortedJsonBytes");
//		Thread.sleep(1000);
		PerformanceMeasurement skjb = measureSKJBSort(b, NUM_SORTS);
//		memskjb.stop();

//		RecordMem memskjfb = new RecordMem(100, "SortedJsonFile - bytes");
//		Thread.sleep(1000);
		PerformanceMeasurement skjfb = measureSKJFSort(b, NUM_SORTS);
//		memskjfb.stop();
//		
		b = null;
//		RecordMem memskjff = new RecordMem(100, "SortedJsonFile - file");
//		Thread.sleep(1000);
		PerformanceMeasurement skjff = measureSKJFSort(FILE, NUM_SORTS);
//		memskjff.stop();
//		
//		
		System.out.println("Complete: " + new Date());
////		PerformanceMeasurement skfjs = measureSKJFSortStringKeys(b, sorts);
		renderResults(Arrays.asList(js, skjb, skjfb, skjff));//, skfjs));
		
//		printMemoryHistory(memjs);//, memskjb, memskjfb, memskjff);
	}
	
	private static void printMemoryHistory(RecordMem... mems) {
		List<Integer> lens = new LinkedList<Integer>();
		for (int i = 0; i < mems.length; i++) {
			lens.add(mems[i].getUsedMemOverTime().size());
			System.out.print(mems[i].getName() + "\t");
		}
		System.out.println();
		int maxlen = Collections.max(lens);
		for (int c = 0; c < maxlen; c++) {
			for (int i = 0; i < mems.length; i++) {
				if (mems[i].getUsedMemOverTime().size() > c) {
					System.out.print(mems[i].getUsedMemOverTime().get(c) / 1000000.0);
				}
				System.out.print("\t");
			}
			System.out.println();
			
		}
	}

	private static class RecordMem {
		
		private final Thread t;
		private final List<Long> freeMemList;
		private volatile boolean stop = false;
		private final String name;
		
		public RecordMem(final int intervalMS, String name) {
			this.name = name;
			freeMemList = new ArrayList<Long>();
			t = new Thread(new Runnable() {
				@Override
				public void run() {
					Runtime r = Runtime.getRuntime();
					while (true) {
						if (stop)
							break;
						System.gc();
						freeMemList.add(r.totalMemory() - r.freeMemory());
						try {
							Thread.sleep(intervalMS);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			});
			t.start();
		}
		
		public String getName() {
			return name;
		}
		
		public void stop() {
			stop = true;
			try {
				t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		public List<Long> getUsedMemOverTime() {
			return freeMemList;
		}
	}

	private static void renderResults(List<PerformanceMeasurement> pms) {
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
		System.out.println(tbl.render());
	}

	private static PerformanceMeasurement measureSKJFSortStringKeys(byte[] b, int sorts)
			throws Exception {
		List<Long> m = new LinkedList<Long>();
		for (int i = 0; i < sorts; i++) {
			long start = System.nanoTime();
			new SortedKeysJsonFile(b).setUseStringsForKeyStoring(true)
					.writeIntoStream(new NullOutputStream());
			m.add(System.nanoTime() - start);
		}
		return new PerformanceMeasurement(m, "SortedKeysJsonFile JSON sort with String keys");
	}
	
	private static PerformanceMeasurement measureSKJFSort(byte[] b, int sorts)
			throws Exception {
		List<Long> m = new LinkedList<Long>();
		for (int i = 0; i < sorts; i++) {
			long start = System.nanoTime();
			new SortedKeysJsonFile(b).writeIntoStream(new NullOutputStream());
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

	private static PerformanceMeasurement measureSKJFSort(File temp, int sorts)
			throws Exception {
		List<Long> m = new LinkedList<Long>();
		for (int i = 0; i < sorts; i++) {
			long start = System.nanoTime();
			new SortedKeysJsonFile(temp).writeIntoStream(new NullOutputStream());
			m.add(System.nanoTime() - start);
		}
		return new PerformanceMeasurement(m, "SortedKeysJsonFile JSON file sort");
	}
	
	private static PerformanceMeasurement measureSKJBSort(byte[] b, int sorts)
			throws Exception {
		List<Long> m = new LinkedList<Long>();
		for (int i = 0; i < sorts; i++) {
			long start = System.nanoTime();
			new SortedKeysJsonBytes(b).writeIntoStream(new NullOutputStream());
			m.add(System.nanoTime() - start);
		}
		return new PerformanceMeasurement(m, "SortedKeysJsonBytes JSON sort");
	}

	@SuppressWarnings("unused")
	private static PerformanceMeasurement measureJsonSort(byte[] b, int sorts)
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
