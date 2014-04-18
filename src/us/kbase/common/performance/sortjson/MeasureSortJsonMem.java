package us.kbase.common.performance.sortjson;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import us.kbase.common.utils.sortjson.SortedKeysJsonBytes;
import us.kbase.common.utils.sortjson.SortedKeysJsonFile;

//import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/** expects a JSON file with no whitespace for testing */
public class MeasureSortJsonMem {
	
	final static Map<String, NullSort> SORTERS = new HashMap<String, NullSort>();
	static {
		SORTERS.put("Jackson", new JacksonSorter());
		SORTERS.put("SortedJsonBytes", new SortedKeyJsonBytesSorter());
		SORTERS.put("SortedJsonFile-bytes", new SortedKeysJsonFileBytesSorter());
		SORTERS.put("SortedJsonFile-file", new SortedKeysJsonFileSorter());
	}

	final static int NUM_SORTS = 500;
	final static int TIME_INTERVAL = 100; //ms
	final static File FILE = new File("src/us/kbase/common/performance/sortjson/83333.2.txt");
	final static String SORTER = "SortedJsonFile-file";
	
	public static void main(String[] args) throws Exception {
		final int numSorts;
		final int interval;
		final File file;
		final String sorter;
		if (args.length < 1) {
			System.out.println("started: " + new Date());
			System.out.println("max mem: " + Runtime.getRuntime().maxMemory());
			System.out.println("file: " + FILE);
			System.out.println("size: " + FILE.length());
			System.out.println("sorter: " + SORTER + " " + SORTERS.get(SORTER).getClass());
			System.out.println("Starting tests");
			numSorts = NUM_SORTS;
			interval = TIME_INTERVAL;
			file = FILE;
			sorter = SORTER;
		} else {
			numSorts = Integer.parseInt(args[0]);
			interval = Integer.parseInt(args[1]);
			file = new File(args[2]);
			sorter = args[3];
		}
		
		List<Long> mem = recordMemory(interval, SORTERS.get(sorter), file,
				numSorts);
		
		if (args.length < 1) {
			System.out.println("Complete: " + new Date());
			System.out.println(SORTER);
		}
		
		for (int i = 0; i < mem.size(); i++) {
			System.out.println(mem.get(i) / 1000000.0);
		}
	}
	
	private static List<Long> recordMemory(int gcTimeInterval,
			NullSort sorter, File file, int numSorts) throws Exception {
		RecordMem mem = new RecordMem(gcTimeInterval);
		Thread.sleep(1000);
		sorter.sort(file, numSorts);
		mem.stop();
		return mem.getUsedMemOverTime();
	}
	
	private interface NullSort {
		public void sort(File f, int sorts) throws Exception;
	}
	
	private static class RecordMem {
		
		private final Thread t;
		private final List<Long> freeMemList;
		private volatile boolean stop = false;
		
		public RecordMem(final int intervalMS) {
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

	static class SortedKeysJsonFileBytesSorter implements NullSort {
		
		@Override
		public void sort(File f, int sorts) throws Exception {
			byte[] b = Files.readAllBytes(f.toPath());
			for (int i = 0; i < sorts; i++) {
				new SortedKeysJsonFile(b).writeIntoStream(new NullOutputStream());
			}
		}
	}
	
	static class SortedKeysJsonFileSorter implements NullSort {
		
		@Override
		public void sort(File f, int sorts) throws Exception {
			for (int i = 0; i < sorts; i++) {
				new SortedKeysJsonFile(f).writeIntoStream(new NullOutputStream());
			}
		}
	}
	
	static class SortedKeyJsonBytesSorter implements NullSort {
		
		@Override
		public void sort(File f, int sorts) throws Exception {
			byte[] b = Files.readAllBytes(f.toPath());
			for (int i = 0; i < sorts; i++) {
				new SortedKeysJsonBytes(b).writeIntoStream(new NullOutputStream());
			}
		}
	}
	
	static class JacksonSorter implements NullSort {
		
		private static final ObjectMapper MAPPER = new ObjectMapper();
		static {
			MAPPER.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
			//Jackson 2.3 + only, comment out for 2.2 or <
//			SORT_MAPPER.configure(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY, true); 
		}
		
		@Override
		public void sort(File f, int sorts) throws Exception {
			byte[] b = Files.readAllBytes(f.toPath());
			for (int i = 0; i < sorts; i++) {
				JsonNode jn = MAPPER.readTree(b); //detects duplicate keys with Jackson 2.3+
				@SuppressWarnings("unchecked")
				Map<String, Object> d = MAPPER.treeToValue(jn, Map.class); //only Maps are sorted, not JsonNodes, as of 2.3+
				jn = null;
				MAPPER.writeValue(new NullOutputStream(), d);
			}
		}
	}
	
	static class NullOutputStream extends OutputStream {
		@Override
		public void write(int b) throws IOException {
			//buh bye byte
		}
	}
}
