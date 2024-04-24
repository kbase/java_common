package us.kbase.common.utils.sortjson;

import java.io.IOException;
import java.io.OutputStream;

public interface UTF8JsonSorter {

	public void writeIntoStream(OutputStream os) throws IOException,
			KeyDuplicationException, TooManyKeysException;
}
