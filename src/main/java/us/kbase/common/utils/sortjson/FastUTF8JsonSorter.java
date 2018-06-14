package us.kbase.common.utils.sortjson;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;

//TODO this doc needs updating
//TODO lots of shared code with SortedKeysJsonFile
/**
 * Class sorts map keys in JSON data stored in byte array.
 * Note that the data *MUST* be in UTF-8 - this is assumed by the sorter.
 * Result of sorting is written into external output stream without modification
 * of original data source.
 * @author Roman Sutormin (rsutormin)
 */
public class FastUTF8JsonSorter implements UTF8JsonSorter {
    private byte[] data;
    private boolean skipKeyDuplication = false;
    private static final int DEFAULT_LIST_INIT_SIZE = 4;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Defines byte array as data source
     * @param byteSource byte array data source
     * @throws IOException
     */
    public FastUTF8JsonSorter(byte[] byteSource) {
        data = byteSource;
    }

    /**
     * @return true if key duplication is skipped (ignored). false is default value.
     */
    public boolean isSkipKeyDuplication() {
        return skipKeyDuplication;
    }

    /**
     * Defines if key duplication should be skipped (ignored) or not. false means
     * error is generated in case of duplication (default).
     * @param skipKeyDuplication value to set
     * @return this object for chaining
     */
    public FastUTF8JsonSorter setSkipKeyDuplication(boolean skipKeyDuplication) {
        this.skipKeyDuplication = skipKeyDuplication;
        return this;
    }

    /**
     * Method saves sorted data into output stream.
     * @param os output stream for saving sorted result
     * @throws IOException in case of problems with i/o or with JSON parsing
     * @throws KeyDuplicationException in case of duplicated keys are found in the same map
     * @throws TooManyKeysException
     */
    public void writeIntoStream(OutputStream os)
            throws IOException, KeyDuplicationException {
        int[] pos = {0};
        List<Object> path = new ArrayList<Object>();
        JsonElement root = searchForElement(pos, path);
        UnthreadedBufferedOutputStream ubos =
                new UnthreadedBufferedOutputStream(os, 100000);
        root.write(data, ubos);
        ubos.flush();
    }

    public byte[] getSorted()
            throws IOException, KeyDuplicationException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writeIntoStream(baos);
        baos.close();
        return baos.toByteArray();
    }

    private static String getPathText(List<Object> path) {
        if (path.size() == 0)
            return "/";
        StringBuilder sb = new StringBuilder();
        for (Object obj : path) {
            String item = "" + obj;
            item = item.replaceAll(Pattern.quote("/"), "\\\\/");
            sb.append("/").append(item);
        }
        return sb.toString();
    }

    private JsonElement searchForElement(int[] pos, List<Object> path)
            throws IOException, KeyDuplicationException {
        int b = -1;
        while (true) {
            if (pos[0] >= data.length)
                throw new IOException("Mapping close bracket wasn't found");
            b = data[pos[0]++] & 0xff;
            if (b != ' ' && b != '\r' && b != '\n' && b != '\t')
                break;
        }
        if (b == '{') {
            return searchForMapCloseBracket(pos, path);
        } else if (b == '[') {
            return searchForArrayCloseBracket(pos, path);
        } else {
            int start = pos[0] - 1;
            if (b == '\"') {
                searchForEndQuot(pos, false);
            } else {
                while (true) {
                    if (pos[0] >= data.length)
                        break;
                    b = data[pos[0]++] & 0xff;
                    if (b == '}' || b == ']' || b == ',' || b == ' ' || b == '\t' || b == '\r' || b == '\n') {
                        pos[0]--;
                        break;
                    }
                }
            }
            int length = pos[0] - start;
            return new JsonPrimitiveElement(start, length);
        }
    }


    private JsonMapElement searchForMapCloseBracket(int[] pos, List<Object> path)
            throws IOException, KeyDuplicationException {
        List<KeyValueLocation> ret =
                new ArrayList<KeyValueLocation>(DEFAULT_LIST_INIT_SIZE);
        boolean isBeforeField = true;
        String currentKey = null;
        int currentKeyStart = -1;
        int currentKeyStop = -1;
        JsonElement currentValue = null;
        path.add("{");
        while (true) {
            if (pos[0] >= data.length)
                throw new IOException("Mapping close bracket wasn't found");
            int b = data[pos[0]++] & 0xff;
            if (b == '}') {
                if (currentKey != null) {
                    if (currentKeyStart < 0 || currentKeyStop < 0)
                        throw new IOException("Value without key in mapping");
                    ret.add(new KeyValueLocation(currentKey,currentKeyStart, currentKeyStop, currentValue));
                    currentKey = null;
                    currentKeyStart = -1;
                    currentKeyStop = -1;
                    currentValue = null;
                }
                break;
            } else if (b == '"') {
                if (isBeforeField) {
                    currentKeyStart = pos[0] - 1;
                    currentKey = searchForEndQuot(pos, true);
                    currentKeyStop = pos[0] - 1;
                } else {
                    throw new IllegalStateException();
                }
            } else if (b == ':') {
                if (!isBeforeField)
                    throw new IOException("Unexpected colon sign in the middle of value text");
                path.set(path.size() - 1, currentKey);
                currentValue = searchForElement(pos, path);
                isBeforeField = false;
            } else if (b == ',') {
                    if (currentKey == null)
                        throw new IOException("Comma in mapping without key-value pair before");
                    if (currentKeyStart < 0 || currentKeyStop < 0)
                        throw new IOException("Value without key in mapping");
                    ret.add(new KeyValueLocation(currentKey, currentKeyStart, currentKeyStop, currentValue));
                    currentKey = null;
                    currentKeyStart = -1;
                    currentKeyStop = -1;
                    currentValue = null;
                isBeforeField = true;
            } else  {
                if (b != ' ' && b != '\r' && b != '\n' && b != '\t')
                    throw new IOException("Unexpected character: " + (char)b);
            }
        }
        path.remove(path.size() - 1);
        Collections.sort(ret);
        String prevKey = null;
        for (Iterator<KeyValueLocation> it = ret.iterator(); it.hasNext();) {
            String key = it.next().key;
            if (prevKey != null && prevKey.equals(key)) {
                if (isSkipKeyDuplication()) {
                    it.remove();
                } else {
                    throw new KeyDuplicationException(getPathText(path), key);
                }
            }
            prevKey = key;
        }
        return new JsonMapElement(ret);
    }

    private JsonArrayElement searchForArrayCloseBracket(int[] pos, List<Object> path)
            throws IOException, KeyDuplicationException {
        List<JsonElement> items =
                new ArrayList<JsonElement>(DEFAULT_LIST_INIT_SIZE);
        if (pos[0] >= data.length)
            throw new IOException("Array close bracket wasn't found");
        int b = data[pos[0]++] & 0xff;
        if (b != ']') {
            pos[0]--;
            path.add(0);
            while (true) {
                items.add(searchForElement(pos, path));
                if (pos[0] >= data.length)
                    throw new IOException("Array close bracket wasn't found");
                while (true) {
                    if (pos[0] >= data.length)
                        throw new IOException("Array close bracket wasn't found");
                    b = data[pos[0]++] & 0xff;
                    if (b != ' ' && b != '\r' && b != '\n' && b != '\t')
                        break;
                }
                if (b == ']') {
                    break;
                } else if (b != ',') {
                    throw new IOException("Unexpected character: " + (char)b);
                }
                int itemNum = (Integer)path.get(path.size() - 1);
                path.set(path.size() - 1, itemNum + 1);
            }
            path.remove(path.size() - 1);
        }
        return new JsonArrayElement(items);
    }

    private String searchForEndQuot(int[] pos, boolean createString) throws IOException {
        int start = pos[0] - 1;
        while (true) {
            if (pos[0] >= data.length)
                throw new IOException("String close quote wasn't found");
            int b = data[pos[0]++] & 0xff;
            if (b == '"')
                break;
            if (b == '\\') {
                if (pos[0] >= data.length)
                    throw new IOException("String close quote wasn't found");
                b = data[pos[0]++] & 0xff;
            }
        }
        if (createString)
            return MAPPER.readValue(data, start, pos[0], String.class);
        return null;
    }

    private static interface JsonElement {
        public void write(byte[] source, OutputStream os) throws IOException;
    }

    private static class JsonPrimitiveElement implements JsonElement {
        int start;
        int length;

        public JsonPrimitiveElement(int start, int length) {
            this.start = start;
            this.length = length;
        }

        @Override
        public void write(byte[] source, OutputStream os) throws IOException {
            os.write(source, start, length);
        }
    }

    private static class JsonArrayElement implements JsonElement {
        List<JsonElement> items;

        JsonArrayElement(List<JsonElement> items) {
            this.items = items;
        }

        @Override
        public void write(byte[] source, OutputStream os) throws IOException {
            os.write('[');
            boolean first = true;
            for (JsonElement item : items) {
                if (!first)
                    os.write(',');
                item.write(source, os);
                first = false;
            }
            os.write(']');
        }
    }

    private static class JsonMapElement implements JsonElement {
        List<KeyValueLocation> items;

        JsonMapElement(List<KeyValueLocation> items) {
            this.items = items;
        }

        @Override
        public void write(byte[] source, OutputStream os) throws IOException {
            os.write('{');
            boolean first = true;
            for (KeyValueLocation entry : items) {
                if (!first)
                    os.write(',');
                os.write(source, entry.keyStart, entry.keyStop + 1 - entry.keyStart);
                os.write(':');
                entry.value.write(source, os);
                first = false;
            }
            os.write('}');
        }
    }

    private static class KeyValueLocation implements Comparable<KeyValueLocation> {
        String key;
        int keyStart;
        int keyStop;
        JsonElement value;

        public KeyValueLocation(String key, int keyStart, int keyStop, JsonElement value) {
            this.key = key;
            this.keyStart = keyStart;
            this.keyStop = keyStop;
            this.value = value;
        }

        @Override
        public int compareTo(KeyValueLocation o) {
            return key.compareTo(o.key);
        }
    }
}
