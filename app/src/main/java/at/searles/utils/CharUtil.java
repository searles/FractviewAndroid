package at.searles.utils;

/**
 * Created by searles on 15.08.17.
 */

public class CharUtil {

    public static boolean isNum(String string, int index) {
        if(0 <= index && index < string.length()) {
            return string.charAt(index) >= '0' && string.charAt(index) <= '9';
        } else {
            return false;
        }
    }

    /**
     * returns the position in string of a possible index. Such an index may only be
     * terminated by ')' or nothing.
     * @param string
     * @return -1 if there is no such index
     */
    public static int suffixIndex(String string) {
        if(string.isEmpty()) {
            return -1;
        }

        int index = string.length() - 1;

        if(string.charAt(index) == ')') {
            index--;
        }

        if(isNum(string, index)) {
            return index;
        } else {
            return -1;
        }
    }

    public static int num(String string, int start, int end) {
        int num = 0;

        for(int i = start; i <= end; ++i) {
            num *= 10;
            num += string.charAt(i) - '0';
        }

        return num;
    }

    /**
     * If string ends with (number), then this method returns
     * head(number+1), otherwise it returns string(1).
     * @param string
     * @return
     */
    public static String nextIndex(String string) {
        int end = suffixIndex(string);

        if(end == -1) {
            return string + " 1";
        } else {
            // there is an index
            int start = end;

            while(isNum(string, start - 1)) {
                start--;
            }

            int index = num(string, start, end);

            index++;

            return string.substring(0, start) + index + string.substring(end + 1);
        }
    }

    /**
     * Shortens the string to abcde...xyz if it is longer than maxChars
     * @param string
     * @param maxChars Maximum number of chars in the returned string, must be at least 7
     * @return string if the length of string is shorter or same as maxChars
     */
    public static String shorten(String string, int maxChars) {
        if(maxChars < 7) {
            throw new IllegalArgumentException("maxChars must be at least 7");
        }

        if(string.length() > maxChars) {
            String head = string.substring(0, maxChars - 6);
            String end = string.substring(string.length() - 3);

            return head + "..." + end;
        } else {
            return string;
        }
    }

    public static boolean charEq(char a, char b) {
        return charCmp(a, b) == 0;
    }

    /**
     * Returns the longest common prefix of all strings
     * @param strings An iterable of all strings
     * @return The empty string if the iterable is empty or there is no common prefix
     */
    public static String commonPrefix(Iterable<String> strings) {
        for(int index = 0;; ++index) {
            String first = null;

            char current = '\0';

            for(String string : strings) {
                if(first == null) {
                    first = string;

                    if(first.length() == index) {
                        // reached length of first element, this means that the first element was
                        // successfully tested as a prefix so far.
                        return first;
                    }

                    current = Character.toUpperCase(first.charAt(index));
                } else if(string.length() == index || !charEq(string.charAt(index), current)) {
                    // not a prefix. this is the first difference.
                    return first.substring(0, index);
                }
            }

            if(first == null) {
                // the iterable is empty
                return "";
            }
        }
    }

    /**
     * Case insensitive char comparison
     * @param a
     * @param b
     * @return
     */
    public static int charCmp(char a, char b) {
        return Character.compare(Character.toUpperCase(Character.toLowerCase(a)), Character.toUpperCase(Character.toLowerCase(b)));
    }

    /**
     * Case insensitive string comparison (uses charCmp)
     * @param a
     * @param b
     * @return
     */
    public static int stringCmp(String a, String b) {
        for(int i = 0;;++i) {
            if(i < a.length() && i < b.length()) {

            } else {

            }
        }
    }

    /**
     * Comparison
     * -1: str is before prefix, 0: prefix is a prefix of str, 1: str comes after elements with this prefix
     * @param str
     * @param prefix
     * @return
     */
    public static int cmpPrefix(String str, String prefix) {
        for(int i = 0; i < prefix.length(); ++i) {
            if(str.length() == i) {
                // str is actually a prefix of "prefix"
                return -1;
            } else {
                int cmp = charCmp(str.charAt(i), prefix.charAt(i));
                if(cmp != 0) {
                    // characters are not equal.
                    return cmp;
                }
            }
        }

        // "prefix" is a prefix
        return 0;
    }
}
