package de.nerogar.sandstormBot.persistence;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.regex.Pattern;

public class FileNameComparator implements Comparator<String> {

	private static final Pattern SPLIT_PATTERN = Pattern.compile("(?<=\\d)(?=\\D)|(?<=\\D)(?=\\d)");

	@Override
	public int compare(String name1, String name2) {
		if (name1 == null) return 1;
		if (name2 == null) return -1;

		// if exactly one of the names starts with a number, do a normal string comparison
		if (isNumeric(name1.charAt(0)) != isNumeric(name2.charAt(0))) {
			return name1.compareToIgnoreCase(name2);
		}

		String[] name1Split = SPLIT_PATTERN.split(name1);
		String[] name2Split = SPLIT_PATTERN.split(name2);

		int segmentCount = Math.min(name1Split.length, name2Split.length);
		boolean isSegmentNumeric = isNumeric(name1Split[0].charAt(0));
		for (int i = 0; i < segmentCount; i++) {
			if (isSegmentNumeric) {
				BigInteger bigInt1 = new BigInteger(name1Split[i]);
				BigInteger bigInt2 = new BigInteger(name2Split[i]);

				int compare = bigInt1.compareTo(bigInt2);
				if (compare != 0) return compare;
			} else {
				int compare = name1Split[i].compareToIgnoreCase(name2Split[i]);
				if (compare != 0) return compare;
			}
			isSegmentNumeric = !isSegmentNumeric;
		}

		// if previous comparison failed, just sort by the number of segments
		return name1Split.length - name2Split.length;
	}

	private static boolean isNumeric(char c) {
		return c >= '0' && c <= '9';
	}

}
