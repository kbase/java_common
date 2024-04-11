package us.kbase.common.utils;

import java.io.*;

public class FastaWriter {
	PrintWriter pw;

	public FastaWriter(File f) {
		try {
			pw = new PrintWriter(new FileWriter(f));
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	public FastaWriter(PrintWriter pw) {
		this.pw = pw;
	}

	public void close() {
		pw.close();
	}

	public void write(String protName, String seq) {
		pw.println(">" + protName);
		int pos = 0;
		while (pos < seq.length()) {
			int end = Math.min(pos + 60, seq.length());
			pw.println(seq.substring(pos, end));
			pos = end;
		}
	}
}