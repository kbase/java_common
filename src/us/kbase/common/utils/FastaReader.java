package us.kbase.common.utils;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringTokenizer;

public class FastaReader {
    BufferedReader br;
    String str1 = null;

    public FastaReader(File f) {
        try {
            br = new BufferedReader(new FileReader(f));
        } catch(Exception ex) {
            throw new RuntimeException("Wrong file name: " + f, ex);
        }
    }

    public FastaReader(Reader r) {
        br = new BufferedReader(r);
    }

    public static Map<String, String> readFromFile(File f) {
    	FastaReader fr = new FastaReader(f);
    	Map<String, String> ret = fr.readAll();
    	fr.close();
    	return ret;
    }
    
    public Map<String, String> readAll() {
    	Map<String, String> ret = new LinkedHashMap<String, String>();
    	while (true) {
    		String[] entry = read();
    		if (entry == null)
    			break;
    		ret.put(entry[0], entry[1]);
    	}
    	return ret;
    }
    
    public String[] read() {
        if(br == null)
            return null;
        String protName = null;
        String protSeq = null;
        String protDescr = null;
        try {
            if(str1 == null)
                str1 = br.readLine();
            for(;;) {
                if(str1 == null)
                    return null;
                String str2 = str1.trim();
                if(str2.length() > 1) {
                    if (str2.charAt(0) == '>' && str2.substring(1).trim().length() > 0) {
                        StringTokenizer st = new StringTokenizer(str2.substring(1), " \t");
                        protName = st.nextToken();
                        StringBuilder descr = new StringBuilder();
                        while (st.hasMoreTokens()) {
                            if (descr.length() > 0)
                                descr.append(" ");
                            descr.append(st.nextToken());
                        }
                        protDescr = descr.toString();
                        break;
                    } else {
                        throw new IllegalStateException("Wrong caption line: " + str2);
                    }
                }
                str1 = br.readLine();
            }
            StringBuilder sb = new StringBuilder();
            for(;;) {
                str1 = br.readLine();
                if(str1 == null || str1.trim().startsWith(">")) {
                    throw new IllegalStateException("No sequence for caption: " + protName);
                }
                if(str1.trim().length() > 0)
                    break;
            }
            for(;;) {
                sb.append(str1);
                str1 = br.readLine();
                if(str1 == null || str1.trim().startsWith(">"))
                    break;
            }
            protSeq = sb.toString();
            if (protSeq.length()==0) {
                throw new IllegalStateException("No sequence for caption: " + protName);
            }
            return new String[] {protName, protSeq, protDescr};
        } catch (IllegalStateException ex) {
        	throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
    public void close() {
        if(br == null)
            return;
        try {
            br.close();
        }catch(Exception ex) {
            System.err.println("WARNING: couldn't close fasta reader, ignored");
        }
        br = null;
    }
}