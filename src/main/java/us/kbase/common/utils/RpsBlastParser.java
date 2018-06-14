package us.kbase.common.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.regex.Pattern;

public class RpsBlastParser {
    public static final String OUTPUT_FORMAT_STRING = "6 qseqid stitle qstart qseq sstart sseq evalue bitscore pident";

    public static void processRpsOutput(File file, RpsBlastCallback callback) throws Exception {
        BufferedReader br = new BufferedReader(new FileReader(file));
        try {
            processRpsOutput(br, callback);
        } finally {
            br.close();
        }
    }
    
    public static void processRpsOutput(BufferedReader br, RpsBlastCallback callback) throws Exception {
        Pattern tabSep = Pattern.compile(Pattern.quote("\t"));
        while (true) {
            String l = br.readLine();
            if (l == null)
                break;
            if (l.trim().length() == 0)
                continue;
            String[] parts = tabSep.split(l);
            String subj = parts[1].substring(0, parts[1].indexOf(','));
            callback.next(parts[0], subj, Integer.parseInt(parts[2]), parts[3], 
                    Integer.parseInt(parts[4]), parts[5], parts[6], 
                    Double.parseDouble(parts[7]), Double.parseDouble(parts[8]));
        }
    }
    
    public static interface RpsBlastCallback {
        public void next(String query, String subj, int qstart, String qseq, int sstart, 
                String sseq, String evalue, double bitscore, double ident) throws Exception;
    }
}
