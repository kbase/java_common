package us.kbase.common.performance.sortjson;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItem;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.block.Block;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;


public class MeasureSortRunner {
	
	final static boolean HEADLESS = true;
	
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
		
		
		compileMeasureSort();
		
		int numSorts = NUM_SORTS;
		int interval = TIME_INTERVAL;
		String file = FILE;
		
		Map<String, List<Double>> mems = new HashMap<String, List<Double>>();
		for (String sorter: SORTERS) {
			System.out.println("Running sorter: " + sorter);
			mems.put(sorter, runMeasureSort(numSorts, interval, file, sorter));
		}
		
		String title = "Title";//TODO info
		final JFreeChart chart = saveChart(new File("output.png"), mems, title);

		

		final FooFrame demo = new FooFrame("Title", chart);
		demo.pack();
		RefineryUtilities.centerFrameOnScreen(demo);
		demo.setVisible(true);
	}

	private static JFreeChart saveChart(File f, Map<String, List<Double>> mems,
			String title) throws IOException {
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
				title, 
				"Measurement #",
				"Used Memory (MB)",
				xyc,
				PlotOrientation.VERTICAL,
				true, // include legend
				true, // tooltips
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
	
	private static class FooFrame extends ApplicationFrame {
		
		FooFrame(String title, JFreeChart chart) {
			super(title);
			final ChartPanel chartPanel = new ChartPanel(chart);
//			chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
			setContentPane(chartPanel);
		}
	}

	private static List<Double> runMeasureSort(int numSorts,
			int interval, String file, String sorter) throws IOException,
			InterruptedException {
		Process p = Runtime.getRuntime().exec(new String [] {
				"java", "-cp", CLASSPATH, MEAS_CLASS_FILE,
				Integer.toString(numSorts), Integer.toString(interval), file, sorter
		});
		List<Double> mem = new ArrayList<Double>();
		BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
		while (true) {
			String l = br.readLine();
			if (l == null) break;
			mem.add(Double.parseDouble(l));
		}
		p.waitFor();
		if (p.exitValue() != 0) {
			System.out.println("Run failed with exit value " + p.exitValue());
			System.out.println("STDOUT:");
			print(p.getInputStream());
			System.out.println("STDERR:");
			print(p.getErrorStream());
			System.exit(1);
		}
		return mem;
	}

	private static void compileMeasureSort()
			throws IOException, InterruptedException {
		Process p = Runtime.getRuntime().exec(new String[] {
				"javac", "-cp", CLASSPATH, MEAS_JAVA_FILE + ".java"});
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
