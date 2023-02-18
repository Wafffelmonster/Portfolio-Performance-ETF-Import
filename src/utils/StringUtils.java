package utils;

public class StringUtils {

    // https://stackoverflow.com/questions/14018478/string-contains-ignore-case
    public static boolean containsIgnoreCase(String str, String searchStr){
        if(str == null || searchStr == null) return false;

        final int length = searchStr.length();
        if (length == 0)
            return true;

        for (int i = str.length() - length; i >= 0; i--) {
            if (str.regionMatches(true, i, searchStr, 0, length))
                return true;
        }
        return false;
    }
}
