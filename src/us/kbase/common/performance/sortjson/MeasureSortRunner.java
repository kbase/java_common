package us.kbase.common.performance.sortjson;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItem;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.fasterxml.jackson.databind.ObjectMapper;

import us.kbase.common.performance.PerformanceMeasurement;
import us.kbase.common.service.Tuple11;
import us.kbase.workspace.ObjectData;
import us.kbase.workspace.ObjectIdentity;
import us.kbase.workspace.WorkspaceClient;


public class MeasureSortRunner {
	
	final static String WORKSPACE_URL = "http://kbase.us/services/ws";
	
	final static int NUM_SORTS = 500;
	final static int TIME_INTERVAL = 100; //ms
	
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
	final static String CLASSPATH;
	static {
		String classpath = CODE_ROOT;
		for (String j: JARS) {
			classpath += ":" + j;
		}
		CLASSPATH = classpath;
	}
	
	final static String MEAS_CLASS_FILE =
			"us.kbase.common.performance.sortjson.MeasureSortJsonMem";
	
	final static String MEAS_JAVA_FILE =
			CODE_ROOT + "/" + MEAS_CLASS_FILE.replace(".", "/");
	
	private static WorkspaceClient ws;
	
	public static void main(String[] args) throws Exception {
		
		System.setProperty("java.awt.headless", "true");
		compileMeasureSort();
		
		Path p = Paths.get(".");
		
		ws = new WorkspaceClient(new URL(WORKSPACE_URL));
		
		List<ObjectIdentity> objs = new ArrayList<ObjectIdentity>();
		objs.add(new ObjectIdentity().withRef("637/35"));
//		objs.add(new ObjectIdentity().withRef("637/308"));
//		objs.add(new ObjectIdentity().withRef("1200/MinimalMedia"));
		for (ObjectIdentity oi: objs) {
			measureObjectMemAndSpeed(p,oi);
		}
	}

	private static void measureObjectMemAndSpeed(Path dir, ObjectIdentity oi)
			throws Exception {
		
		int numSorts = NUM_SORTS;
		int interval = TIME_INTERVAL;
		
		ObjectData data = ws.getObjects(Arrays.asList(oi)).get(0);
		Tuple11<Long, String, String, String, Long, String, Long, String, String, Long, Map<String, String>>
				info = data.getInfo();
		String ref = info.getE7() + "/" + info.getE1() + "/" + info.getE5();
		String name = info.getE8() + "/" + info.getE2() + "/" + info.getE5();
		String title = ref + " " + name + " " + info.getE3();
		String memOutputPrefix = ref.replace("/", "_") + ".memresults"; 
		
		Path d = dir.resolve(info.getE3());
		Files.createDirectories(d);

		Path input = d.resolve(ref.replace("/", "_") + ".object.txt");
		input.toFile().createNewFile();
		input.toFile().deleteOnExit();
		new ObjectMapper().writeValue(input.toFile(), data.getData().asInstance());
		data = null;

		System.out.println("Recording memory usage for " + ref);
		measureSorterMemUsage(numSorts, interval, input, title, d, memOutputPrefix);
		
		System.out.println("Recording speed for " + ref);
		measureSorterSpeed(numSorts, input, title,
				d.resolve(ref.replace("/", "_") + ".speed.txt"));
		
		Files.deleteIfExists(input);
	}

	private static void measureSorterSpeed(int numSorts, Path input,
			String title, Path output) throws Exception {
		
		byte[] b = Files.readAllBytes(input);
		
		PerformanceMeasurement js = MeasureSortJsonSpeed.measureJsonSort(b, numSorts);

		PerformanceMeasurement skjb = MeasureSortJsonSpeed.measureSKJBSort(b, numSorts);

		PerformanceMeasurement skjfb = MeasureSortJsonSpeed.measureSKJFSort(b, numSorts);

		b = null;
		PerformanceMeasurement skjff = MeasureSortJsonSpeed.measureSKJFSort(input.toFile(), numSorts);
		
		BufferedWriter bw = Files.newBufferedWriter(output, Charset.forName("UTF-8"));
		bw.write(title + String.format(" Size (MB): %,.2f",
				input.toFile().length() / 1000000.0) + "\n");
		MeasureSortJsonSpeed.renderResults(Arrays.asList(js, skjb, skjfb, skjff), bw);
		bw.close();
	}

	private static void measureSorterMemUsage(int numSorts, int interval,
			Path input, String title, Path dir, String outputPrefix)
			throws IOException, InterruptedException {
		
		Map<String, List<Double>> mems = new LinkedHashMap<String, List<Double>>();
		for (String sorter: SORTERS) {
			System.out.println("Running sorter: " + sorter);
			mems.put(sorter, runMeasureSort(numSorts, interval, input, sorter));
		}
		
		String params = String.format(
				"Sorts: %s, Interval (ms): %s, size (MB): %,.2f",
				numSorts, interval, input.toFile().length() / 1000000.0);
		
		saveMemChart(dir.resolve(Paths.get(outputPrefix + ".png")), mems, title, params);
		saveMemData(dir.resolve(Paths.get(outputPrefix + ".txt")), mems, title, params);
	}

	private static void saveMemData(Path file, Map<String, List<Double>> mems,
			String title, String params) throws IOException {
		BufferedWriter bw = Files.newBufferedWriter(file, Charset.forName("UTF-8"));
		bw.write(title + "\n");
		bw.write(params + "\n");
		for (String s: mems.keySet()) {
			bw.write(s + "\n");
			for (Double m: mems.get(s)) {
				bw.write(Double.toString(m) + "\n");
			}
			bw.write("\n");
		}
		bw.flush();
		bw.close();
	}

	private static JFreeChart saveMemChart(Path f, Map<String, List<Double>> mems,
			String title, String params) throws IOException {
		XYSeriesCollection xyc = new XYSeriesCollection();
		for (String sorter: SORTERS) {
			XYSeries s = new XYSeries(sorter, false, false);
			double count = 1;
			for (Double mem: mems.get(sorter)) {
				s.add(++count, mem);
			}
			xyc.addSeries(s);
		}
		
		final JFreeChart chart = ChartFactory.createXYLineChart(
				title + "\n" + params, 
				"Measurement #",
				"Used Memory (MB)",
				xyc,
				PlotOrientation.VERTICAL,
				true, // include legend
				false, // tooltips
				false // urls
				);
		
		chart.setBackgroundPaint(Color.white);

		final LegendTitle legend = chart.getLegend();
		for (Object li: legend.getItemContainer().getBlocks()) {
			((LegendItem)li).setShapeVisible(true);
		}

		final XYPlot plot = chart.getXYPlot();
		plot.setBackgroundPaint(Color.white);
		plot.setDomainGridlinePaint(Color.gray);
		plot.setRangeGridlinePaint(Color.gray);
		
		plot.getRenderer().setSeriesPaint(2, new Color(20, 184, 69));
		plot.getRenderer().setSeriesPaint(3, new Color(184, 173, 20));

		final ValueAxis domainAxis = plot.getDomainAxis();
		domainAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		
		ChartUtilities.saveChartAsPNG(f.toFile(), chart, 700, 500);
		return chart;
	}
	
	private static List<Double> runMeasureSort(int numSorts,
			int interval, Path file, String sorter) throws IOException,
			InterruptedException {
		Process p = Runtime.getRuntime().exec(new String [] {
				"java", "-cp", CLASSPATH, MEAS_CLASS_FILE,
				Integer.toString(numSorts), Integer.toString(interval), file.toString(), sorter
		});
		List<Double> mem = new ArrayList<Double>();
		BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
		finishProcess(p, "Run failed");
		while (true) {
			String l = br.readLine();
			if (l == null) break;
			mem.add(Double.parseDouble(l));
		}
		return mem;
	}

	private static void compileMeasureSort()
			throws IOException, InterruptedException {
		Process p = Runtime.getRuntime().exec(new String[] {
				"javac", "-cp", CLASSPATH, MEAS_JAVA_FILE + ".java"});
		finishProcess(p, "Compile failed");
	}

	private static void finishProcess(Process p, String err)
			throws InterruptedException, IOException {
		p.waitFor();
		if (p.exitValue() != 0) {
			System.out.println(err + ". Exit value " + p.exitValue());
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
