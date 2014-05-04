package us.kbase.common.utils.sortjson;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/** Constructs a sorter based on memory requirements and data size.
 * @author gaprice@lbl.gov
 *
 */
public class UTF8JsonSorterFactory {
	
	final int maxMem;
	final int maxFastSortSize;
	
	/** Construct the factory
	 * @param maxMemoryUse the maximum memory to use when sorting *including*
	 * the input bytes.
	 */
	public UTF8JsonSorterFactory(final int maxMemoryUse) {
		if (maxMemoryUse < 1) {
			throw new IllegalArgumentException("Max memory must be at least 1");
		}
		maxMem = maxMemoryUse;
		//the fast sorter usually uses memory = 5-10x the size of the file. 
		maxFastSortSize = maxMemoryUse / 10; 
	}
	
	public UTF8JsonSorter getSorter(final File f) throws IOException {
		if (f.length() <= maxFastSortSize) {
			return new FastUTF8JsonSorter(Files.readAllBytes(f.toPath()));
		}
		return new LowMemoryUTF8JsonSorter(f).setMaxMemoryForKeyStoring(maxMem);
	}
	
	public UTF8JsonSorter getSorter(final byte[] b) {
		if (b.length > maxMem) {
			throw new IllegalArgumentException(String.format(
					"Byte array size %s is greater than memory allowed: %s",
					b.length, maxMem));
		}
		if (b.length <= maxFastSortSize) {
			return new FastUTF8JsonSorter(b);
		}
		return new LowMemoryUTF8JsonSorter(b).setMaxMemoryForKeyStoring(
				maxMem - b.length);
	}

}
