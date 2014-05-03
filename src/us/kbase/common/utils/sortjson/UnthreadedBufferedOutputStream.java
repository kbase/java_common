package us.kbase.common.utils.sortjson;

import java.io.IOException;
import java.io.OutputStream;

//removes thread safety code; per Roman 5x faster without it
/** Buffered output stream without thread safety.
 */
public class UnthreadedBufferedOutputStream extends OutputStream {
	OutputStream out;
	byte buffer[];
	int bufSize;

	public UnthreadedBufferedOutputStream(OutputStream out, int size)
			throws IOException {
		this.out = out;
		if (size <= 0) {
			throw new IOException("Buffer size should be a positive number");
		}
		buffer = new byte[size];
	}

	void flushBuffer() throws IOException {
		if (bufSize > 0) {
			out.write(buffer, 0, bufSize);
			bufSize = 0;
		}
	}

	@Override
	public void write(int b) throws IOException {
		if (bufSize >= buffer.length) {
			flushBuffer();
		}
		buffer[bufSize++] = (byte)b;
	}

	@Override
	public void write(byte b[]) throws IOException {
		write(b, 0, b.length);
	}

	@Override
	public void write(byte b[], int off, int len) throws IOException {
		if (len >= buffer.length) {
			flushBuffer();
			out.write(b, off, len);
			return;
		}
		if (len > buffer.length - bufSize) {
			flushBuffer();
		}
		System.arraycopy(b, off, buffer, bufSize, len);
		bufSize += len;
	}

	@Override
	public void flush() throws IOException {
		flushBuffer();
		out.flush();
	}

	@Override
	public void close() throws IOException {
		flush();
		out.close();
	}
}
