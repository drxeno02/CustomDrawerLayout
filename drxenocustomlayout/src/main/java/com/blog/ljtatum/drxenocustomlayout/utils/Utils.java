package com.blog.ljtatum.drxenocustomlayout.utils;

import android.content.Context;

/**
 * Created by LJTat on 11/1/2017.
 */

public class Utils {

    /**
     * Method is used to check if objects are null
     *
     * @param objectToCheck Object to check if null or empty
     * @param <T>           Generic data value
     * @return True if object is null or empty
     */
    public static <T> boolean checkIfNull(T objectToCheck) {
        return objectToCheck == null;
    }

    /**
     * Method is used to convert dp to px
     *
     * @param px      The pixel value to convert to dp
     * @param context Interface to global information about an application environment
     * @return Converted dp value
     */
    public static float convertPixelToDp(Context context, final float px) {
        return !checkIfNull(px / context.getResources().getDisplayMetrics().density) ?
                (px / context.getResources().getDisplayMetrics().density) : 0f;
    }
}
