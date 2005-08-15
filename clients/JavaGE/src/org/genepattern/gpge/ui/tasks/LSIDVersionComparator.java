package org.genepattern.gpge.ui.tasks;

import java.util.Comparator;

public class LSIDVersionComparator implements Comparator {
	public static final LSIDVersionComparator INSTANCE = new LSIDVersionComparator();
	
	public int compare(Object arg0, Object arg1) {
		String s0 = (String) arg0;
		String s1 = (String) arg1;
		String[] s0Tokens = s0.split("\\.");
		String[] s1Tokens = s1.split("\\.");
		int min = Math.min(s0Tokens.length, s1Tokens.length);
		
		for (int i = 0; i < min; i++) {
			int s0Int = Integer.parseInt(s0Tokens[i]);
			int s1Int = Integer.parseInt(s1Tokens[i]);
			if (s0Int < s1Int) {
				return -1;
			} else if (s0Int > s1Int) {
				return 1;
			}
		}
		if (s0Tokens.length > s1Tokens.length) {
			return 1;
		} else if (s0Tokens.length < s1Tokens.length) {
			return -1;
		} else {
			return 0;
		}
	}

}
