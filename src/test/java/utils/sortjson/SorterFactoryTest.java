package us.kbase.common.utils.sortjson.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.Test;

import us.kbase.common.utils.sortjson.FastUTF8JsonSorter;
import us.kbase.common.utils.sortjson.LowMemoryUTF8JsonSorter;
import us.kbase.common.utils.sortjson.UTF8JsonSorter;
import us.kbase.common.utils.sortjson.UTF8JsonSorterFactory;

public class SorterFactoryTest {

    @Test
    public void badInit() throws Exception {
        try {
            new UTF8JsonSorterFactory(0);
            fail("bad init passed");
        } catch (IllegalArgumentException iae) {
            assertThat("correct exp msg", iae.getLocalizedMessage(),
                    is("Max memory must be at least 1"));
        }
    }

    @Test
    public void chooseSorter() throws Exception {
        String twenty = "aaaaaaaaaaaaaaaaaaaa";
        UTF8JsonSorterFactory fac = new UTF8JsonSorterFactory(19);
        try {
            fac.getSorter(twenty.getBytes("UTF-8"));
            fail("bytes too large");
        } catch (IllegalArgumentException iae) {
            assertThat("correct exp msg", iae.getLocalizedMessage(),
                    is("Byte array size 20 is greater than memory allowed: 19"));
        }

        String three = "aaa";
        String four = "aaaa";
        fac = new UTF8JsonSorterFactory(30);

        checkCorrectSorterAndContents(fac, three, FastUTF8JsonSorter.class);
        LowMemoryUTF8JsonSorter sort = (LowMemoryUTF8JsonSorter)
                checkCorrectSorterAndContents(fac, four, LowMemoryUTF8JsonSorter.class);
        assertThat("correct memory allowance", sort.getMaxMemoryForKeyStoring(),
                is(26L));

        File temp3 = createTempFile(three);
        File temp4 = createTempFile(four);

        checkCorrectSorterAndContents(fac, temp3, FastUTF8JsonSorter.class);
        checkCorrectSorterAndContents(fac, temp4, LowMemoryUTF8JsonSorter.class);

        fac = new UTF8JsonSorterFactory(5);
        sort = (LowMemoryUTF8JsonSorter)
                checkCorrectSorterAndContents(fac, four, LowMemoryUTF8JsonSorter.class);
        assertThat("correct memory allowance", sort.getMaxMemoryForKeyStoring(),
                is(1L));
        File temp20 = createTempFile(twenty);
        sort = (LowMemoryUTF8JsonSorter)
                checkCorrectSorterAndContents(fac, temp20, LowMemoryUTF8JsonSorter.class);
        assertThat("correct memory allowance", sort.getMaxMemoryForKeyStoring(),
                is(5L));
    }

    private File createTempFile(String s) throws IOException {
        File t = File.createTempFile("sortfactest", null);
        t.deleteOnExit();
        FileWriter f = new FileWriter(t);
        f.write(s);
        f.close();
        return t;
    }

    private UTF8JsonSorter checkCorrectSorterAndContents(UTF8JsonSorterFactory fac,
            String s, @SuppressWarnings("rawtypes") Class c) throws Exception {
        UTF8JsonSorter sort = fac.getSorter(s.getBytes("UTF-8"));
        assertThat("got correct sorter", sort, is(c));
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        sort.writeIntoStream(o);
        assertThat("correct sorter contents", new String(o.toByteArray(), "UTF-8"),
                is(s));
        return sort;
    }

    private UTF8JsonSorter checkCorrectSorterAndContents(UTF8JsonSorterFactory fac,
            File f, @SuppressWarnings("rawtypes") Class c) throws Exception {
        UTF8JsonSorter sort = fac.getSorter(f);
        assertThat("got correct sorter", sort, is(c));
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        sort.writeIntoStream(o);
        String fcont = new String(Files.readAllBytes(f.toPath()), "UTF-8");
        assertThat("correct sorter contents", new String(o.toByteArray(), "UTF-8"),
                is(fcont));
        return sort;
    }

}
