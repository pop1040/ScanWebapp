package net.pop1040.json;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class Util {

	static public String[] subset(String list[], int start) {
		return subset(list, start, list.length - start);
	}

	static public String[] subset(String list[], int start, int count) {
		String output[] = new String[count];
		System.arraycopy(list, start, output, 0, count);
		return output;
	}

	static public String[] expand(String list[]) {
		return expand(list, list.length > 0 ? list.length << 1 : 1);
	}

	static public String[] expand(String list[], int newSize) {
		String temp[] = new String[newSize];
		// in case the new size is smaller than list.length
		System.arraycopy(list, 0, temp, 0, Math.min(newSize, list.length));
		return temp;
	}

	static public int[] subset(int list[], int start) {
		return subset(list, start, list.length - start);
	}

	static public int[] subset(int list[], int start, int count) {
		int output[] = new int[count];
		System.arraycopy(list, start, output, 0, count);
		return output;
	}

	static public int[] expand(int list[]) {
		return expand(list, list.length > 0 ? list.length << 1 : 1);
	}

	static public int[] expand(int list[], int newSize) {
		int temp[] = new int[newSize];
		System.arraycopy(list, 0, temp, 0, Math.min(newSize, list.length));
		return temp;
	}

	static public float[] subset(float list[], int start) {
		return subset(list, start, list.length - start);
	}

	static public float[] subset(float list[], int start, int count) {
		float output[] = new float[count];
		System.arraycopy(list, start, output, 0, count);
		return output;
	}

	static public float[] expand(float list[]) {
		return expand(list, list.length > 0 ? list.length << 1 : 1);
	}

	static public float[] expand(float list[], int newSize) {
		float temp[] = new float[newSize];
		System.arraycopy(list, 0, temp, 0, Math.min(newSize, list.length));
		return temp;
	}

	static final public float parseFloat(String what, float otherwise) {
		try {
			return new Float(what).floatValue();
		} catch (NumberFormatException e) {
		}

		return otherwise;
	}

	static public String[] loadStrings(InputStream input) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"));
			return loadStrings(reader);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	static public String[] loadStrings(BufferedReader reader) {
		try {
			String lines[] = new String[100];
			int lineCount = 0;
			String line = null;
			while ((line = reader.readLine()) != null) {
				if (lineCount == lines.length) {
					String temp[] = new String[lineCount << 1];
					System.arraycopy(lines, 0, temp, 0, lineCount);
					lines = temp;
				}
				lines[lineCount++] = line;
			}
			reader.close();

			if (lineCount == lines.length) {
				return lines;
			}

			// resize array to appropriate amount for these lines
			String output[] = new String[lineCount];
			System.arraycopy(lines, 0, output, 0, lineCount);
			return output;

		} catch (IOException e) {
			e.printStackTrace();
			// throw new RuntimeException("Error inside loadStrings()");
		}
		return null;
	}

	static final public int parseInt(String what) {
		return parseInt(what, 0);
	}

	/**
	 * Parse a String to an int, and provide an alternate value that should be used
	 * when the number is invalid.
	 */
	static final public int parseInt(String what, int otherwise) {
		try {
			int offset = what.indexOf('.');
			if (offset == -1) {
				return Integer.parseInt(what);
			} else {
				return Integer.parseInt(what.substring(0, offset));
			}
		} catch (NumberFormatException e) {
		}
		return otherwise;
	}

	/**
	 * ( begin auto-generated from split.xml )
	 *
	 * The split() function breaks a string into pieces using a character or string
	 * as the divider. The <b>delim</b> parameter specifies the character or
	 * characters that mark the boundaries between each piece. A String[] array is
	 * returned that contains each of the pieces. <br/>
	 * <br/>
	 * If the result is a set of numbers, you can convert the String[] array to to a
	 * float[] or int[] array using the datatype conversion functions <b>int()</b>
	 * and <b>float()</b> (see example above). <br/>
	 * <br/>
	 * The <b>splitTokens()</b> function works in a similar fashion, except that it
	 * splits using a range of characters instead of a specific character or
	 * sequence. <!-- /><br />
	 * This function uses regular expressions to determine how the <b>delim</b>
	 * parameter divides the <b>str</b> parameter. Therefore, if you use characters
	 * such parentheses and brackets that are used with regular expressions as a
	 * part of the <b>delim</b> parameter, you'll need to put two blackslashes
	 * (\\\\) in front of the character (see example above). You can read more about
	 * <a href="http://en.wikipedia.org/wiki/Regular_expression">regular
	 * expressions</a> and
	 * <a href="http://en.wikipedia.org/wiki/Escape_character">escape characters</a>
	 * on Wikipedia. -->
	 *
	 * ( end auto-generated )
	 * 
	 * @webref data:string_functions
	 * @usage web_application
	 * @param value
	 *            the String to be split
	 * @param delim
	 *            the character or String used to separate the data
	 */
	static public String[] split(String value, char delim) {
		// do this so that the exception occurs inside the user's
		// program, rather than appearing to be a bug inside split()
		if (value == null)
			return null;
		// return split(what, String.valueOf(delim)); // huh

		char chars[] = value.toCharArray();
		int splitCount = 0; // 1;
		for (int i = 0; i < chars.length; i++) {
			if (chars[i] == delim)
				splitCount++;
		}
		// make sure that there is something in the input string
		// if (chars.length > 0) {
		// if the last char is a delimeter, get rid of it..
		// if (chars[chars.length-1] == delim) splitCount--;
		// on second thought, i don't agree with this, will disable
		// }
		if (splitCount == 0) {
			String splits[] = new String[1];
			splits[0] = value;
			return splits;
		}
		// int pieceCount = splitCount + 1;
		String splits[] = new String[splitCount + 1];
		int splitIndex = 0;
		int startIndex = 0;
		for (int i = 0; i < chars.length; i++) {
			if (chars[i] == delim) {
				splits[splitIndex++] = new String(chars, startIndex, i - startIndex);
				startIndex = i + 1;
			}
		}
		// if (startIndex != chars.length) {
		splits[splitIndex] = new String(chars, startIndex, chars.length - startIndex);
		// }
		return splits;
	}

	static public String[] split(String value, String delim) {
		ArrayList<String> items = new ArrayList<String>();
		int index;
		int offset = 0;
		while ((index = value.indexOf(delim, offset)) != -1) {
			items.add(value.substring(offset, index));
			offset = index + delim.length();
		}
		items.add(value.substring(offset));
		String[] outgoing = new String[items.size()];
		items.toArray(outgoing);
		return outgoing;
	}

	static final public float parseFloat(String what) {
		return parseFloat(what, Float.NaN);
	}

}
