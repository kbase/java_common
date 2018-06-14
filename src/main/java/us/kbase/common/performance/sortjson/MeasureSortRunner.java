package us.kbase.common.performance.sortjson;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

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

import us.kbase.common.service.Tuple11;
import us.kbase.common.service.Tuple9;
import us.kbase.common.utils.MD5;
import us.kbase.workspace.ListObjectsParams;
import us.kbase.workspace.ListWorkspaceInfoParams;
import us.kbase.workspace.ObjectData;
import us.kbase.workspace.ObjectIdentity;
import us.kbase.workspace.WorkspaceClient;

import com.fasterxml.jackson.databind.ObjectMapper;


public class MeasureSortRunner {

    final static Path OUTPUT_DIR = Paths.get(".");
    //set to 0 or less to use pre chosen test objects below
    final static int NUM_OBJECTS_TO_TEST = 0;
    //random tester won't use objects below this size
    final static int MIN_SIZE_B = 0;
    //set the max memory used by the memory recorder
    final static String MEM_XMX = "1G";
    final static boolean CHECK_SORT_CORRECTNESS = false;
    final static boolean DONT_USE_PARALLEL_GC = false;
    final static boolean SKIP_MEM_MEAS = true;

    final static List<ObjectIdentity> TEST_OBJECTS =
            new ArrayList<ObjectIdentity>();
    static {
        TEST_OBJECTS.add(new ObjectIdentity().withRef("637/35"));
        TEST_OBJECTS.add(new ObjectIdentity().withRef("637/308"));
//        TEST_OBJECTS.add(new ObjectIdentity().withRef("970/1"));
//        TEST_OBJECTS.add(new ObjectIdentity().withRef("970/2"));
//        TEST_OBJECTS.add(new ObjectIdentity().withRef("970/3"));
//        TEST_OBJECTS.add(new ObjectIdentity().withRef("1267/1"));
//        TEST_OBJECTS.add(new ObjectIdentity().withRef("1267/2"));
//        TEST_OBJECTS.add(new ObjectIdentity().withRef("1267/3"));
//        TEST_OBJECTS.add(new ObjectIdentity().withRef("1267/4"));
//        TEST_OBJECTS.add(new ObjectIdentity().withRef("1267/5"));
//        TEST_OBJECTS.add(new ObjectIdentity().withRef("1267/6"));
//        TEST_OBJECTS.add(new ObjectIdentity().withRef("1267/7"));
//        TEST_OBJECTS.add(new ObjectIdentity().withRef("1267/8"));
//        TEST_OBJECTS.add(new ObjectIdentity().withRef("1200/MinimalMedia"));
    }

    final static String WORKSPACE_URL = "http://kbase.us/services/ws";

    final static String JACKSON = "Jackson";
    final static List<String> SORTERS = new ArrayList<String>();
    static {
        SORTERS.add("Jackson");
        SORTERS.add("SortedJsonBytes");
        SORTERS.add("SortedJsonFile-bytes");
        SORTERS.add("SortedJsonFile-file");
    }

    final static Map<String, String> SPEED_NOMENCLATURE_MAP = new HashMap<String, String>();
    static {
        SPEED_NOMENCLATURE_MAP.put("Jackson", "Jackson");
        SPEED_NOMENCLATURE_MAP.put("SortedJsonBytes", "Structural");
        SPEED_NOMENCLATURE_MAP.put("SortedJsonFile-bytes", "Byte");
        SPEED_NOMENCLATURE_MAP.put("SortedJsonFile-file", "File");
    }

    final static List<String> JARS = new ArrayList<String>();
    static {
        JARS.add("../jars/lib/jars/jackson/jackson-annotations-2.2.3.jar");
        JARS.add("../jars/lib/jars/jackson/jackson-core-2.2.3.jar");
        JARS.add("../jars/lib/jars/jackson/jackson-databind-2.2.3.jar");
        JARS.add("../jars/lib/jars/texttable/text-table-formatter-1.1.1.jar");
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
    final static String SORT_CLASS_FILE =
            "us.kbase.common.performance.sortjson.MeasureSortJsonSpeed";

    final static String MEAS_JAVA_FILE =
            CODE_ROOT + "/" + MEAS_CLASS_FILE.replace(".", "/");
    final static String SORT_JAVA_FILE =
            CODE_ROOT + "/" + SORT_CLASS_FILE.replace(".", "/");

    private static final int NUM_SORTS_POS = 0;
    private static final int INTERVAL_POS = 1;

    private static final Map<Integer, List<Integer>> SIZE_CUTOFFS =
            new LinkedHashMap<Integer, List<Integer>>();
    static {
        SIZE_CUTOFFS.put(100000000, Arrays.asList(10, 5000));
        SIZE_CUTOFFS.put(20000000, Arrays.asList(20, 1000));
        SIZE_CUTOFFS.put(10000000, Arrays.asList(100, 500));
        SIZE_CUTOFFS.put(0, Arrays.asList(500, 100));
    }

    private static WorkspaceClient ws;

    public static void main(String[] args) throws Exception {

        System.setProperty("java.awt.headless", "true");
        System.out.println("Java version: " + System.getProperty("java.version"));
        System.out.println("Mem: total: " + Runtime.getRuntime().totalMemory() +
                " max: " + Runtime.getRuntime().maxMemory());
        System.out.println("Input args:");
        System.out.println(ManagementFactory.getRuntimeMXBean().getInputArguments());
        System.out.println("Garbage collectors:");
        for (GarbageCollectorMXBean g: ManagementFactory.getGarbageCollectorMXBeans()) {
            System.out.println(g.getName() + " - Valid: " + g.isValid());
            String[] m = g.getMemoryPoolNames();
            for (int i = 0; i < m.length; i++) {
                System.out.println("\t" + m[i]);
            }
        }

        System.out.println("For called memory usage and sort speed programs, using parameters:");
        System.out.println("-Xmx" + MEM_XMX);
        if (DONT_USE_PARALLEL_GC) {
            System.out.println("Using serial GC");
        } else {
            System.out.println("Using parallel GC");
        }
        System.out.println();

        compileMeasureSort(MEAS_JAVA_FILE);
        compileMeasureSort(SORT_JAVA_FILE);

        ws = new WorkspaceClient(new URL(WORKSPACE_URL));

        int numObjs = NUM_OBJECTS_TO_TEST;

        if (numObjs < 1) {
            int count = 1;
            for (ObjectIdentity oi: TEST_OBJECTS) {
                System.out.println(String.format("Testing object %s of %s",
                        count++, TEST_OBJECTS.size()));
                ObjectData data = ws.getObjects(Arrays.asList(oi)).get(0);
                measureObjectMemAndSpeed(OUTPUT_DIR, data);
            }
        } else {
            Random r = new Random();
            Set<String> seenObjs = new HashSet<String>();
            for (int i = 0; i < NUM_OBJECTS_TO_TEST; i++) {
                System.out.println(String.format("Testing object %s of %s",
                        i + 1, NUM_OBJECTS_TO_TEST));
                ObjectIdentity oi = getRandomObjectWithRetries(seenObjs, r);
                ObjectData d = getDataWithRetries(oi);
                measureObjectMemAndSpeed(OUTPUT_DIR, d);
            }
        }
    }

    private static ObjectIdentity getRandomObjectWithRetries(Set<String> seenObjs, Random rand) {
        ObjectIdentity oi = null;
        int count = 0;
        while (oi == null) {
            try {
                oi = getRandomObject(seenObjs, rand);
            } catch (Exception e) {
                System.out.println(e);
                count++;
            }
            if (count > 10) {
                System.exit(1);
            }
        }
        return oi;
    }
    private static ObjectIdentity getRandomObject(Set<String> seenObjs, Random rand)
            throws Exception {
        ObjectIdentity good = null;
        while (good == null) {
            List<Tuple9<Long, String, String, String, Long, String, String, String, Map<String, String>>>
                    workspaces = ws.listWorkspaceInfo(new ListWorkspaceInfoParams());
            int wsr = rand.nextInt(workspaces.size());
            List<Tuple11<Long, String, String, String, Long, String, Long, String, String, Long, Map<String, String>>>
                    objs = ws.listObjects(new ListObjectsParams()
                        .withIds(Arrays.asList(workspaces.get(wsr).getE1()))
                        .withShowHidden(1L));
            if (objs.size() == 0) continue;
            int objr = rand.nextInt(objs.size());
            long work = objs.get(objr).getE7();
            long objid = objs.get(objr).getE1();
            long size = objs.get(objr).getE10();
            String wsobj = work + "_" + objid;
            if (!seenObjs.contains(wsobj)) {
                seenObjs.add(wsobj);
                if (size > MIN_SIZE_B) {
                    good = new ObjectIdentity().withWsid(work).withObjid(objid);
                }
            }
        }
        return good;
    }

    private static ObjectData getDataWithRetries(ObjectIdentity oi){
        ObjectData data = null;
        int count = 0;
        while (data == null) {
            try {
                data = ws.getObjects(Arrays.asList(oi)).get(0);
            } catch (Exception e) {
                count++;
                System.out.println(e);
            }
            if (count > 10) {
                System.exit(1);
            }
        }
        return data;
    }

    private static void measureObjectMemAndSpeed(Path dir, ObjectData data)
            throws Exception {

        Tuple11<Long, String, String, String, Long, String, Long, String, String, Long, Map<String, String>>
                info = data.getInfo();
        String ref = info.getE7() + "/" + info.getE1() + "/" + info.getE5();
        String name = info.getE8() + "/" + info.getE2() + "/" + info.getE5();
        String title = ref + " " + name + " " + info.getE3();
        long size = info.getE10();
        String mb = String.format("%,.2fMB", size / 1000000.0);
        String outputPrefix = mb + "_" + ref.replace("/", "_");

        Path d = dir.resolve(info.getE3());
        Files.createDirectories(d);

        Path input = d.resolve(ref.replace("/", "_") + ".object.txt");
        input.toFile().createNewFile();
        input.toFile().deleteOnExit();
        new ObjectMapper().writeValue(input.toFile(), data.getData().asInstance());
        data = null;

        System.out.println(String.format("Testing object %s, %sB, %s",
                ref, info.getE10(), new Date()));

        if (CHECK_SORT_CORRECTNESS) {
            boolean good = true;
            Map<String, MD5> md5s = MeasureSortJsonMem.getMD5s(input);
            MD5 jMD5 = md5s.get(JACKSON);
            List<String> output = new ArrayList<String>();
            for (Entry<String, MD5> m: md5s.entrySet()) {
                output.add(m.getKey() + " " + m.getValue().getMD5());
                if (!m.getValue().equals(jMD5)) {
                    good = false;
                }
            }
            final Path out;
            if (good) {
                out = d.resolve(outputPrefix + ".md5.good.txt");
            } else {
                System.out.println("Sort correctness failed for " + ref);
                out = d.resolve(outputPrefix + ".md5.bad.txt");
            }
            Files.write(out, output, Charset.forName("UTF-8"));
        }



        int numSorts = SIZE_CUTOFFS.get(0).get(NUM_SORTS_POS);
        int interval = SIZE_CUTOFFS.get(0).get(INTERVAL_POS);
        for (Entry<Integer, List<Integer>> sz: SIZE_CUTOFFS.entrySet()) {
            if (size > sz.getKey()) {
                numSorts = sz.getValue().get(NUM_SORTS_POS);
                interval = sz.getValue().get(INTERVAL_POS);
                break;
            }
        }

        if (!SKIP_MEM_MEAS) {
            System.out.println(String.format("Recording memory usage. Sorts: %s, interval: %s, %s",
                    numSorts, interval, new Date()));
            measureSorterMemUsage(numSorts, interval, input, title, d, outputPrefix + ".memresults");
        }

        System.out.println(String.format("Recording sort speed. Sorts: %s, interval: %s, %s",
                numSorts, interval, new Date()));
        measureSorterSpeed(numSorts, input, title,
                d.resolve(outputPrefix +  ".speed.txt"));

        Files.deleteIfExists(input);
        System.out.println();
    }

    private static void measureSorterSpeed(int numSorts, Path input,
            String title, Path output) throws Exception {

        List<String> cmd = new ArrayList<String>();
        cmd.add("java");
        if (DONT_USE_PARALLEL_GC) {
            cmd.add("-XX:-UseParallelGC");
        }
        cmd.addAll(Arrays.asList("-Xmx" + MEM_XMX, "-cp", CLASSPATH, SORT_CLASS_FILE,
                    Integer.toString(numSorts), input.toString(),
                    output.toFile().getAbsolutePath()));
        for (String s: SORTERS) {
            cmd.add(SPEED_NOMENCLATURE_MAP.get(s));
        }
        Process p = new ProcessBuilder(cmd).start();
        finishProcess(p, "Sort run failed");
        BufferedReader err = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        String l = err.readLine();
        if (l != null) {
            System.out.println("Sort measurer STDERR:");
        }
        while (l != null) {
            System.out.println(l);
            l = err.readLine();
        }
        err.close();
        p.destroy();
    }

    private static void measureSorterMemUsage(int numSorts, int interval,
            Path input, String title, Path dir, String outputPrefix)
            throws IOException, InterruptedException {

        Map<String, List<Double>> mems = new LinkedHashMap<String, List<Double>>();
        for (String sorter: SORTERS) {
            System.out.println("Running sorter: " + sorter + " " + new Date());
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
        File tempfile = File.createTempFile("MeasureSortRunner", null);
        tempfile.deleteOnExit();
        List<String> cmd = new ArrayList<String>();
        cmd.add("java");
        if (DONT_USE_PARALLEL_GC) {
            cmd.add("-XX:-UseParallelGC");
        }
        cmd.addAll(Arrays.asList("-Xmx" + MEM_XMX, "-cp", CLASSPATH, MEAS_CLASS_FILE,
                    Integer.toString(numSorts), Integer.toString(interval),
                    file.toString(), sorter, tempfile.getAbsolutePath()));
        Process p = new ProcessBuilder(cmd).start();
        List<Double> mem = new ArrayList<Double>();
        finishProcess(p, "Memory run failed");
        BufferedReader br = new BufferedReader(new FileReader(tempfile));
        String l = br.readLine();
        while (l != null) {
            mem.add(Double.parseDouble(l));
            l = br.readLine();
        }
        br.close();
        BufferedReader err = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        l = err.readLine();
        if (l != null) {
            System.out.println("Sort measurer STDERR:");
        }
        while (l != null) {
            System.out.println(l);
            l = err.readLine();
        }
        err.close();
        p.destroy();
        return mem;
    }

    private static void compileMeasureSort(String javaFile)
            throws IOException, InterruptedException {
        Process p = new ProcessBuilder(new String[] {
                "javac", "-cp", CLASSPATH, javaFile + ".java"}).start();
        finishProcess(p, "Compile failed"); //dangerous, could deadlock here - may need to change
        p.destroy();
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
