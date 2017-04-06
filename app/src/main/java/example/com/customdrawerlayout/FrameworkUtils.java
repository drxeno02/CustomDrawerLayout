/*
 * Copyright (c) 2014-present, ZTRIP. All rights reserved.
 */

package example.com.customdrawerlayout;

/**
 * Created by leonard on 4/4/2017.
 */

public class FrameworkUtils {
    private static final String EMPTY = "";
    private static final String NULL = "null";

    /**
     * Method checks if String value is empty
     *
     * @param str
     * @return string
     */
    public static boolean isStringEmpty(String str) {
        return str == null || str.length() == 0 || EMPTY.equals(str.trim()) || NULL.equals(str);
    }

    /**
     * Method is used to check if objects are null
     *
     * @param objectToCheck
     * @param <T>
     * @return true if objectToCheck is null
     */
    public static <T> boolean checkIfNull(T objectToCheck) {
        return objectToCheck == null;
    }


}
