package us.kbase.common.performance.sortjson;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
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


public class MeasureSortRunner {
	
	final static int NUM_SORTS = 500;
	final static int TIME_INTERVAL = 100; //ms
	final static String FILE = "src/us/kbase/common/performance/sortjson/83333.2.txt";
	
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
	
	public static void main(String[] args) throws Exception {
		
		System.setProperty("java.awt.headless", "true");
		compileMeasureSort();
		
		int numSorts = NUM_SORTS;
		int interval = TIME_INTERVAL;
		File file = new File(FILE);
		String title = "Title";
		String outputPrefix = "output";
		
		System.out.println("Recording memory usage");
		measureSorterMemUsage(numSorts, interval, file, title, outputPrefix);
	}

	private static void measureSorterMemUsage(int numSorts, int interval,
			File input, String title, String outputPrefix)
			throws IOException, InterruptedException {
		
		Map<String, List<Double>> mems = new LinkedHashMap<String, List<Double>>();
		for (String sorter: SORTERS) {
			System.out.println("Running sorter: " + sorter);
			mems.put(sorter, runMeasureSort(numSorts, interval, input, sorter));
		}
		
		String params = String.format(
				"Sorts: %s, Interval (ms): %s, file: %s, size (MB): %,.2f",
				numSorts, interval, input, input.length() / 1000000.0);
		
		saveChart(new File(outputPrefix + ".png"), mems, title, params);
		saveData(new File(outputPrefix + ".txt"), mems, title, params);
	}

	private static void saveData(File file, Map<String, List<Double>> mems,
			String title, String params) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(file));
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

	private static JFreeChart saveChart(File f, Map<String, List<Double>> mems,
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

		final ValueAxis domainAxis = plot.getDomainAxis();
		domainAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		
		ChartUtilities.saveChartAsPNG(f, chart, 700, 500);
		return chart;
	}
	
	private static List<Double> runMeasureSort(int numSorts,
			int interval, File file, String sorter) throws IOException,
			InterruptedException {
		Process p = Runtime.getRuntime().exec(new String [] {
				"java", "-cp", CLASSPATH, MEAS_CLASS_FILE,
				Integer.toString(numSorts), Integer.toString(interval), file.toString(), sorter
		});
		List<Double> mem = new ArrayList<Double>();
		BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
		while (true) {
			String l = br.readLine();
			if (l == null) break;
			mem.add(Double.parseDouble(l));
		}
		finish(p, "Run failed");
		return mem;
	}

	private static void compileMeasureSort()
			throws IOException, InterruptedException {
		Process p = Runtime.getRuntime().exec(new String[] {
				"javac", "-cp", CLASSPATH, MEAS_JAVA_FILE + ".java"});
		finish(p, "Compile failed");
	}

	private static void finish(Process p, String err)
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
