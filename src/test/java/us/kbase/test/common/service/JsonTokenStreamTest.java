package us.kbase.test.common.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
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
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.JsonTokenId;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

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
	public void newTokenInformationMethods() throws Exception {
		/* This test checks the new token information methods added in between Jackson 2.2.3 and
		 * Jackson 2.9.9.
		 */
		
		final JsonTokenStream jts = new JsonTokenStream(basicJsonData.get(0));
		assertThat("incorrect token ID", jts.getCurrentTokenId(), is(0));
		assertThat("incorrect has token ID", jts.hasTokenId(0), is(true));
		assertThat("incorrect has token ID", jts.hasTokenId(1), is(false));
		assertThat("incorrect has token", jts.hasToken(null), is(true));
		assertThat("incorrect has token", jts.hasToken(JsonToken.NOT_AVAILABLE), is(true));
		assertThat("incorrect has token", jts.hasToken(JsonToken.START_OBJECT), is(false));
		
		jts.nextToken();
		
		assertThat("incorrect token ID", jts.getCurrentTokenId(), is(JsonTokenId.ID_START_OBJECT));
		assertThat("incorrect has token ID", jts.hasTokenId(0), is(false));
		assertThat("incorrect has token ID", jts.hasTokenId(JsonTokenId.ID_START_OBJECT),
				is(true));
		assertThat("incorrect has token", jts.hasToken(null), is(false));
		assertThat("incorrect has token", jts.hasToken(JsonToken.NOT_AVAILABLE), is(false));
		assertThat("incorrect has token", jts.hasToken(JsonToken.START_OBJECT), is(true));
		
		jts.close();
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
	
	// could probably reuse some of code somehow but screw it for now
	
	@Test
	public void nextBooleanValue() throws Exception {
		final Object data = ImmutableMap.of(
				"foo", Arrays.asList(true, false),
				"bar", Arrays.asList(1, 2));
		final JsonTokenStream jts = new JsonTokenStream(
				new ObjectMapper().writeValueAsString(data));
		
		// foo tree
		assertThat("incorrect token", jts.nextToken(), is(JsonToken.START_OBJECT));
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList()));
		assertThat("incorrect token", jts.nextToken(), is(JsonToken.FIELD_NAME));
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList("foo")));
		assertThat("incorrect token", jts.nextToken(), is(JsonToken.START_ARRAY));
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList("foo")));
		assertThat("incorrect bool", jts.nextBooleanValue(), is(true));
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList("foo", "0")));
		assertThat("incorrect bool", jts.nextBooleanValue(), is(false));
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList("foo", "1")));
		assertThat("incorrect token", jts.nextToken(), is(JsonToken.END_ARRAY));
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList("foo")));
		
		// bar tree
		assertThat("incorrect token", jts.nextToken(), is(JsonToken.FIELD_NAME));
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList("bar")));
		assertThat("incorrect token", jts.nextToken(), is(JsonToken.START_ARRAY));
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList("bar")));
		assertThat("incorrect text", jts.nextBooleanValue(), nullValue());
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList("bar", "0")));
		assertThat("incorrect text", jts.nextBooleanValue(), nullValue());
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList("bar", "1")));
		assertThat("incorrect token", jts.nextToken(), is(JsonToken.END_ARRAY));
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList("bar")));
		
		// close object
		assertThat("incorrect token", jts.nextToken(), is(JsonToken.END_OBJECT));
		assertThat("incorrect closed", jts.isClosed(), is(false));
		assertThat("incorrect token", jts.nextToken(), nullValue());
		assertThat("incorrect closed", jts.isClosed(), is(true));
		
		assertThat("incorrect token", jts.nextToken(), nullValue());
		jts.close();
	}
	
	@Test
	public void nextFieldName() throws Exception {
		final Object data = ImmutableMap.of(
				"foo", "baz",
				"bar", "bat");
		final JsonTokenStream jts = new JsonTokenStream(
				new ObjectMapper().writeValueAsString(data));
		
		// foo tree
		assertThat("incorrect token", jts.nextToken(), is(JsonToken.START_OBJECT));
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList()));
		assertThat("incorrect name", jts.nextFieldName(), is("foo"));
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList("foo")));
		assertThat("incorrect name", jts.nextFieldName(), nullValue());
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList("foo")));
		assertThat("incorrect name", jts.nextFieldName(), is("bar"));
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList("bar")));
		assertThat("incorrect name", jts.nextFieldName(), is(nullValue()));
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList("bar")));
		
		// close object
		assertThat("incorrect token", jts.nextToken(), is(JsonToken.END_OBJECT));
		assertThat("incorrect closed", jts.isClosed(), is(false));
		assertThat("incorrect token", jts.nextToken(), nullValue());
		assertThat("incorrect closed", jts.isClosed(), is(true));
		
		assertThat("incorrect token", jts.nextToken(), nullValue());
		jts.close();
	}
	
	@Test
	public void nextFieldNameString() throws Exception {
		final Object data = ImmutableMap.of(
				"foo", "baz",
				"bar", "bat");
		final JsonTokenStream jts = new JsonTokenStream(
				new ObjectMapper().writeValueAsString(data));
		
		// foo tree
		assertThat("incorrect token", jts.nextToken(), is(JsonToken.START_OBJECT));
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList()));
		assertThat("incorrect token", jts.nextFieldName(new SerializedString("foo")), is(true));
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList("foo")));
		assertThat("incorrect token", jts.nextFieldName(new SerializedString("baz")), is(false));
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList("foo")));
		// 'bar' input returns true here
		assertThat("incorrect token", jts.nextFieldName(new SerializedString("bal")), is(false));
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList("bar")));
		assertThat("incorrect token", jts.nextFieldName(new SerializedString("bat")), is(false));
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList("bar")));
		
		// close object
		assertThat("incorrect token", jts.nextToken(), is(JsonToken.END_OBJECT));
		assertThat("incorrect closed", jts.isClosed(), is(false));
		assertThat("incorrect token", jts.nextToken(), nullValue());
		assertThat("incorrect closed", jts.isClosed(), is(true));
		
		assertThat("incorrect token", jts.nextToken(), nullValue());
		jts.close();
	}
	
	@Test
	public void nextIntValue() throws Exception {
		final Object data = ImmutableMap.of(
				"foo", Arrays.asList(1, 2),
				"bar", Arrays.asList(1.1, 2.2));
		final JsonTokenStream jts = new JsonTokenStream(
				new ObjectMapper().writeValueAsString(data));
		
		// foo tree
		assertThat("incorrect token", jts.nextToken(), is(JsonToken.START_OBJECT));
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList()));
		assertThat("incorrect token", jts.nextToken(), is(JsonToken.FIELD_NAME));
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList("foo")));
		assertThat("incorrect token", jts.nextToken(), is(JsonToken.START_ARRAY));
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList("foo")));
		assertThat("incorrect text", jts.nextIntValue(-1), is(1));
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList("foo", "0")));
		assertThat("incorrect text", jts.nextIntValue(-1), is(2));
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList("foo", "1")));
		assertThat("incorrect token", jts.nextToken(), is(JsonToken.END_ARRAY));
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList("foo")));
		
		// bar tree
		assertThat("incorrect token", jts.nextToken(), is(JsonToken.FIELD_NAME));
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList("bar")));
		assertThat("incorrect token", jts.nextToken(), is(JsonToken.START_ARRAY));
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList("bar")));
		assertThat("incorrect text", jts.nextIntValue(-1), is(-1));
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList("bar", "0")));
		assertThat("incorrect text", jts.nextIntValue(-1), is(-1));
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList("bar", "1")));
		assertThat("incorrect token", jts.nextToken(), is(JsonToken.END_ARRAY));
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList("bar")));
		
		// close object
		assertThat("incorrect token", jts.nextToken(), is(JsonToken.END_OBJECT));
		assertThat("incorrect closed", jts.isClosed(), is(false));
		assertThat("incorrect token", jts.nextToken(), nullValue());
		assertThat("incorrect closed", jts.isClosed(), is(true));
		
		assertThat("incorrect token", jts.nextToken(), nullValue());
		jts.close();
	}
	
	@Test
	public void nextLongValue() throws Exception {
		final Object data = ImmutableMap.of(
				"foo", Arrays.asList(1L, 2L),
				"bar", Arrays.asList(1.1, 2.2));
		final JsonTokenStream jts = new JsonTokenStream(
				new ObjectMapper().writeValueAsString(data));
		
		// foo tree
		assertThat("incorrect token", jts.nextToken(), is(JsonToken.START_OBJECT));
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList()));
		assertThat("incorrect token", jts.nextToken(), is(JsonToken.FIELD_NAME));
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList("foo")));
		assertThat("incorrect token", jts.nextToken(), is(JsonToken.START_ARRAY));
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList("foo")));
		assertThat("incorrect text", jts.nextLongValue(-1), is(1L));
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList("foo", "0")));
		assertThat("incorrect text", jts.nextLongValue(-1), is(2L));
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList("foo", "1")));
		assertThat("incorrect token", jts.nextToken(), is(JsonToken.END_ARRAY));
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList("foo")));
		
		// bar tree
		assertThat("incorrect token", jts.nextToken(), is(JsonToken.FIELD_NAME));
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList("bar")));
		assertThat("incorrect token", jts.nextToken(), is(JsonToken.START_ARRAY));
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList("bar")));
		assertThat("incorrect text", jts.nextLongValue(-1), is(-1L));
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList("bar", "0")));
		assertThat("incorrect text", jts.nextLongValue(-1), is(-1L));
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList("bar", "1")));
		assertThat("incorrect token", jts.nextToken(), is(JsonToken.END_ARRAY));
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList("bar")));
		
		// close object
		assertThat("incorrect token", jts.nextToken(), is(JsonToken.END_OBJECT));
		assertThat("incorrect closed", jts.isClosed(), is(false));
		assertThat("incorrect token", jts.nextToken(), nullValue());
		assertThat("incorrect closed", jts.isClosed(), is(true));
		
		assertThat("incorrect token", jts.nextToken(), nullValue());
		jts.close();
	}
	
	@Test
	public void nextValue() throws Exception {
		final Object data = ImmutableMap.of(
				"foo", Arrays.asList(1L, 2L),
				"bar", "baz");
		final JsonTokenStream jts = new JsonTokenStream(
				new ObjectMapper().writeValueAsString(data));
		
		// foo tree
		assertThat("incorrect value", jts.nextValue(), is(JsonToken.START_OBJECT));
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList()));
		assertThat("incorrect value", jts.nextValue(), is(JsonToken.START_ARRAY));
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList("foo")));
		assertThat("incorrect value", jts.nextValue(), is(JsonToken.VALUE_NUMBER_INT));
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList("foo", "0")));
		assertThat("incorrect value", jts.nextValue(), is(JsonToken.VALUE_NUMBER_INT));
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList("foo", "1")));
		assertThat("incorrect token", jts.nextToken(), is(JsonToken.END_ARRAY));
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList("foo")));
		
		// bar tree
		assertThat("incorrect token", jts.nextValue(), is(JsonToken.VALUE_STRING));
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList("bar")));
		
		// close object
		assertThat("incorrect token", jts.nextToken(), is(JsonToken.END_OBJECT));
		assertThat("incorrect closed", jts.isClosed(), is(false));
		assertThat("incorrect token", jts.nextToken(), nullValue());
		assertThat("incorrect closed", jts.isClosed(), is(true));
		
		assertThat("incorrect token", jts.nextToken(), nullValue());
		jts.close();
	}
	
	@Test
	public void nextTextValue() throws Exception {
		final Object data = ImmutableMap.of(
				"foo", Arrays.asList("one", "two"),
				"bar", Arrays.asList(1, 2));
		final JsonTokenStream jts = new JsonTokenStream(
				new ObjectMapper().writeValueAsString(data));
		
		// foo tree
		assertThat("incorrect token", jts.nextToken(), is(JsonToken.START_OBJECT));
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList()));
		assertThat("incorrect token", jts.nextToken(), is(JsonToken.FIELD_NAME));
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList("foo")));
		assertThat("incorrect token", jts.nextToken(), is(JsonToken.START_ARRAY));
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList("foo")));
		assertThat("incorrect text", jts.nextTextValue(), is("one"));
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList("foo", "0")));
		assertThat("incorrect text", jts.nextTextValue(), is("two"));
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList("foo", "1")));
		assertThat("incorrect token", jts.nextToken(), is(JsonToken.END_ARRAY));
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList("foo")));
		
		// bar tree
		assertThat("incorrect token", jts.nextToken(), is(JsonToken.FIELD_NAME));
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList("bar")));
		assertThat("incorrect token", jts.nextToken(), is(JsonToken.START_ARRAY));
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList("bar")));
		assertThat("incorrect text", jts.nextTextValue(), nullValue());
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList("bar", "0")));
		assertThat("incorrect text", jts.nextTextValue(), nullValue());
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList("bar", "1")));
		assertThat("incorrect token", jts.nextToken(), is(JsonToken.END_ARRAY));
		assertThat("incorrect path", jts.getCurrentPath(), is(Arrays.asList("bar")));
		
		// close object
		assertThat("incorrect token", jts.nextToken(), is(JsonToken.END_OBJECT));
		assertThat("incorrect closed", jts.isClosed(), is(false));
		assertThat("incorrect token", jts.nextToken(), nullValue());
		assertThat("incorrect closed", jts.isClosed(), is(true));
		
		assertThat("incorrect token", jts.nextToken(), nullValue());
		jts.close();
	}
	
	@Test
	public void emptyData() throws Exception {
		failInitJTS("", "Data must be at least 1 byte / char");
		failInitJTS(new byte[0], "Data must be at least 1 byte / char");
		File f = File.createTempFile("TestJsonTokenStream-", ".tmp");
		f.deleteOnExit();
		failInitJTS(f, "Data must be at least 1 byte / char");
		//this doesn't work - mapped to ""
//		JsonNode n = new ObjectMapper().valueToTree("");
//		failInitJTS(n, "Data must be at least 1 byte / char");
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
				JsonTokenStream jts = new JsonTokenStream(data.getBytes(enc));
				assertThat("encoding correctly detected", jts.getEncoding(),
						is(enc));
			}
		}
	}
}
