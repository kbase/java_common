package us.kbase.common.test.service;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import us.kbase.common.service.JsonTokenStream;

public class JsonTokenStreamTest {

    private static final Charset utf8 = Charset.forName("UTF-8");

    private static final List<Charset> encodings = new LinkedList<Charset>();
    static {
        encodings.add(Charset.forName("UTF-8"));
        encodings.add(Charset.forName("UTF-16BE"));
        encodings.add(Charset.forName("UTF-16LE"));
        encodings.add(Charset.forName("UTF-32BE"));
        encodings.add(Charset.forName("UTF-32LE"));
    }

    private static final List<String> basicJsonData = new LinkedList<String>();
    static {
        basicJsonData.add("{\"this\":[\"is\",\"a JSON object\"]}");
        basicJsonData.add("[\"this\",{\"is\":\"a\"},\"JSON object\"]");
        basicJsonData.add("\"this is a JSON object\"");
        basicJsonData.add("null");
        basicJsonData.add("true");
        basicJsonData.add("false");
        basicJsonData.add("1");
        basicJsonData.add("1.2");
        basicJsonData.add("-1.4E10"); //should really allow e or E
    }

    @Test
    public void getSetTrustedWholeJSON() throws Exception {
        String data = "{\"foo\": \"bar\"}";
        JsonTokenStream jts = new JsonTokenStream(data);
        assertThat("default good whole json is false", jts.hasTrustedWholeJson(),
                is(false));
        assertThat("setting trusted json returns this", jts.setTrustedWholeJson(true),
                is(jts));
        assertThat("trusted json set correctly", jts.hasTrustedWholeJson(),
                is(true));

        jts.setRoot(null);
        jts.setRoot(new LinkedList<String>());
        assertThat("trusted json still set after setting null/emtpy root",
                jts.hasTrustedWholeJson(), is(true));

        jts.setRoot(Arrays.asList("foo"));
        assertThat("trusted json set to false after setting root inside json",
                jts.hasTrustedWholeJson(), is(false));

        try {
            jts.setTrustedWholeJson(true);
            fail("should fail to set trusted json");
        } catch (IllegalArgumentException e) {
            assertThat("correct exception message", e.getLocalizedMessage(),
                    is("Root is inside contained object, cannot set trustedWholeJson to true"));
        }
    }

    @Test
    public void getSetCopyBufferSize() throws Exception {
        String data = "{\"foo\": \"bar\"}";
        JsonTokenStream jts = new JsonTokenStream(data);
        assertThat("default copy buffer size", jts.getCopyBufferSize(),
                is(100000));
        assertThat("setting copy buffer size returns this",
                jts.setCopyBufferSize(10), is(jts));
        assertThat("copy buffer size set correctly", jts.getCopyBufferSize(),
                is(10));

        try {
            jts.setCopyBufferSize(9);
            fail("should fail to set copy buffer size");
        } catch (IllegalArgumentException e) {
            assertThat("correct exception message", e.getLocalizedMessage(),
                    is("Buffer size must be at least 10"));
        }
    }

    @Test
    public void emptyData() throws Exception {
        failInitJTS("", "Data must be at least 1 byte / char");
        failInitJTS(new byte[0], "Data must be at least 1 byte / char");
        File f = File.createTempFile("TestJsonTokenStream-", ".tmp");
        f.deleteOnExit();
        failInitJTS(f, "Data must be at least 1 byte / char");
        //this doesn't work - mapped to ""
//        JsonNode n = new ObjectMapper().valueToTree("");
//        failInitJTS(n, "Data must be at least 1 byte / char");
        failInitJTS(new HashMap<String, String>(),
                "Only String, File, JsonNode, and byte[]s are allowed as input");
    }

    private void failInitJTS(Object data, String exception) throws Exception {
        try (final JsonTokenStream jts = new JsonTokenStream(data)) {
            fail("Inited JTS with bad data");
        } catch (IllegalArgumentException e) {
            assertThat("correct exception message", e.getLocalizedMessage(),
                    is(exception));
        }
    }

    @SuppressWarnings("resource")
    @Test
    public void streamingData() throws Exception {
        for (String sdata: basicJsonData) {

            checkStreamingCorrectness(sdata, sdata);
            for (Charset enc: encodings) {
                byte[] bdata = sdata.getBytes(enc);
                checkStreamingCorrectness(sdata, bdata);

                File f = File.createTempFile("TestJsonTokenStream-", null);
                f.deleteOnExit();
                new FileOutputStream(f).write(bdata);
                checkStreamingCorrectness(sdata, f);
            }

            JsonNode jn = new ObjectMapper().readTree(sdata);
            checkStreamingCorrectness(sdata, jn);
        }
    }

    private void checkStreamingCorrectness(String expected, Object data)
            throws Exception {
        for (boolean skipParsing: Arrays.asList(true, false)) {
            @SuppressWarnings("resource")
            JsonTokenStream jts = new JsonTokenStream(data)
                    .setTrustedWholeJson(skipParsing);

            //test w/ JsonGenerator
            ByteArrayOutputStream target = new ByteArrayOutputStream();
            JsonGenerator jgen = new ObjectMapper().getFactory()
                    .createGenerator(target);
            jgen.writeStartObject();
            jgen.writeFieldName("data");
            jts.writeTokens(jgen);
            jgen.writeEndObject();
            jgen.flush();
            String res = new String(target.toByteArray(), utf8);
            assertThat("Correctly streamed in object via JsonGenerator as" +
                    data.getClass(), res,
                    is(String.format("{\"data\":%s}", expected)));

            //test w/ outputstream
            jts.setRoot(null);
            target = new ByteArrayOutputStream();
            target.write("{\"data\":".getBytes(utf8));
            jts.writeJson(target);
            target.write("}".getBytes(utf8));
            res = new String(target.toByteArray(), utf8);
            assertThat("Correctly streamed in object via OutputStream as " +
                    data.getClass(), res,
                    is(String.format("{\"data\":%s}", expected)));

            //test w/ writer
            jts.setRoot(null);
            target = new ByteArrayOutputStream();
            Writer w = new OutputStreamWriter(target, utf8);
            w.write("{\"data\":");
            jts.writeJson(w);
            w.write("}");
            w.flush();
            res = new String(target.toByteArray(), utf8);
            assertThat("Correctly streamed in object via Writer as " +
                    data.getClass(), res,
                    is(String.format("{\"data\":%s}", expected)));
        }
    }

    @SuppressWarnings("resource")
    @Test
    public void streamingDataWithMultiByteChars() throws Exception {

        StringBuilder sb = new StringBuilder();
        //28 ttl bytes in UTF-8
        sb.appendCodePoint(0x10310); //4 byte
        sb.appendCodePoint(0x4A);    //1 byte
        sb.appendCodePoint(0x103B0); //4 byte
        sb.appendCodePoint(0x120);   //2 byte
        sb.appendCodePoint(0x1D120); //4 byte
        sb.appendCodePoint(0x0A90);  //3 byte
        sb.appendCodePoint(0x6A);    //1 byte
        sb.appendCodePoint(0x6A);    //1 byte
        sb.appendCodePoint(0x1D120); //4 byte
        sb.appendCodePoint(0x1D120); //4 byte

        String exp = "[\"" + sb.toString() + sb.toString() + "\"]";
        for (Charset enc: encodings) {
            byte[] b = exp.getBytes(enc);
            File f = File.createTempFile("TestJsonTokenStream-", null);
            f.deleteOnExit();
            new FileOutputStream(f).write(b);
            for (int i = 10; i < 20; i++) {
                checkStreamingWithMultiByteChars(b, i, exp);
                checkStreamingWithMultiByteChars(f, i, exp);
            }
        }


        JsonNode jn = new ObjectMapper().readTree(exp);
        for (int i = 10; i < 20; i++) {
            checkStreamingWithMultiByteChars(exp, i, exp);
            checkStreamingWithMultiByteChars(jn, i, exp);
        }
    }

    //data needs to have multibyte char spanning buffersize-th byte for this test
    private void checkStreamingWithMultiByteChars(Object data, int buffersize, String exp)
            throws JsonParseException, IOException {
        for (boolean skipParsing: Arrays.asList(true, false)) {
            @SuppressWarnings("resource")
            JsonTokenStream jts = new JsonTokenStream(data)
                    .setTrustedWholeJson(skipParsing)
                    .setCopyBufferSize(buffersize);
            ByteArrayOutputStream target = new ByteArrayOutputStream();
            JsonGenerator jgen = new ObjectMapper().getFactory()
                    .createGenerator(target);
            jts.writeTokens(jgen);
            jgen.flush();
            @SuppressWarnings("unchecked")
            List<String> l = new ObjectMapper().readValue(target.toByteArray(), List.class);
            String res = "[\"" + l.get(0) + "\"]";
            assertThat("Multibyte chars past buffer end processed correctly " +
                    String.format("with data %s and buffersize %s and skipParsing=%s",
                            data, buffersize, skipParsing), res, is(exp));
        }
    }

    @Test
    public void detectEncoding() throws Exception {
        for (String data: basicJsonData) {
            for (Charset enc: encodings) {
                @SuppressWarnings("resource")
                JsonTokenStream jts = new JsonTokenStream(data.getBytes(enc));
                assertThat("encoding correctly detected", jts.getEncoding(),
                        is(enc));
            }
        }
    }
}
