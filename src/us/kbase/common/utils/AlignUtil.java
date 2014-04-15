package us.kbase.common.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AlignUtil {
	public static int[] getPositions(String alignedSeq) {
		int[] ret = new int[alignedSeq.length()];
		int pos = -1;
		for (int i = 0; i < ret.length; i++) {
			if(alignedSeq.charAt(i) != '-') {
				pos++;
				ret[i] = pos;
			}
			else {
				ret[i] = -1;
			}
		}
		return ret;
	}

	public static String removeGaps(String alignedSeq) {
		StringBuffer sb = new StringBuffer(alignedSeq);
		for(int i = sb.length() - 1; i >= 0; i--)
			if (sb.charAt(i) == '-') 
				sb.deleteCharAt(i);
		return sb.toString();
	}
	
	public static String removeGapsFromSubject(int subjectLen,
			String qseq, int subjectStart, String subjectSeq) {
		int[] alnPos = AlignUtil.getPositions(subjectSeq);
		char[] out = new char[subjectLen];
		Arrays.fill(out, '-');
		for (int i = 0; i < qseq.length(); i++) {
			if (alnPos[i] < 0)
				continue;
			char ch = qseq.charAt(i);
			if (ch == '-')
				continue;
			out[alnPos[i] + subjectStart] = ch;
		}
		return new String(out);
	}

	public static Map<String, String> trimAlignment(Map<String, String> aln, double minNonGapPart) {
		List<char[]> tmp = new ArrayList<char[]>();
		for (String key : aln.keySet())
			tmp.add(aln.get(key).toCharArray());
		char[][] arr = tmp.toArray(new char[tmp.size()][]);
		List<Integer> posList = new ArrayList<Integer>();
		for (int pos = 0; pos < arr[0].length; pos++) {
			int nonGaps = 0;
			for (int n = 0; n < arr.length; n++) 
				if (arr[n][pos] != '-')
					nonGaps++;
			if (nonGaps >= minNonGapPart * arr.length)
				posList.add(pos);
		}
		Map<String, String> ret = new LinkedHashMap<String, String>();
		for (String key : aln.keySet()) {
			String seq = aln.get(key);
			char[] newSeq = new char[posList.size()];
			for (int i = 0; i < posList.size(); i++) 
				newSeq[i] = seq.charAt(posList.get(i));
			ret.put(key, new String(newSeq));
		}
		return ret;
	}
	
	public static int getGapPercent(String seq) {
		int gapCount = 0;
		for (int i = 0; i < seq.length(); i++)
			if (seq.charAt(i) == '-')
				gapCount++;
		return gapCount * 100 / seq.length();
	}
}
