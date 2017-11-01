package com.blog.ljtatum.drxenocustomlayout.utils;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.view.View;

/**
 * Created by LJTat on 11/1/2017.
 */


public class CustomDrawerLayoutUtils {

    /**
     * Max allowed duration for a "click", in milliseconds.
     */
    private static final int MAX_CLICK_DURATION = 1000;

    /**
     * Max allowed distance to move during a "click", in DP.
     */
    private static final int MAX_CLICK_DISTANCE = 5;

    /**
     * @param context Interface to global information about an application environment
     * @return The current display metrics that are in effect for this resource object
     */
    public static int getRawDisplayHeight(@NonNull Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return metrics.heightPixels;
    }

    /**
     * @param context Interface to global information about an application environment
     * @return The current display metrics that are in effect for this resource object
     */
    public static int getRawDisplayWidth(@NonNull Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return metrics.widthPixels;
    }

    /**
     * @param v This class represents the basic building block for user interface components
     * @return The y-axis value
     */
    public static int getLocationInYAxis(@NonNull View v) {
        final int[] globalPos = new int[2];
        v.getLocationInWindow(globalPos);
        return globalPos[1];
    }

    /**
     * @param v This class represents the basic building block for user interface components
     * @return The x-axis value
     */
    public static int getLocationInXAxis(@NonNull View v) {
        final int[] globalPos = new int[2];
        v.getLocationInWindow(globalPos);
        return globalPos[0];
    }

    /**
     * @param context       Interface to global information about an application environment
     * @param diff          The difference in time (milliseconds) between interaction with CTA
     * @param pressDuration The duration the user interacted with the CTA
     * @return True if user interaction was a valid single click
     */
    public static boolean isClicked(@NonNull Context context, float diff, long pressDuration) {
        return pressDuration < MAX_CLICK_DURATION &&
                distance(context, diff) < MAX_CLICK_DISTANCE;
    }

    /**
     * @param context Interface to global information about an application environment
     * @param diff    The difference in time (milliseconds) between interaction with CTA
     * @return The distance the view's position has changed
     */
    private static float distance(@NonNull Context context, float diff) {
        float distanceInPx = (float) Math.sqrt(diff * diff);
        return Utils.convertPixelToDp(context, distanceInPx);
    }
}
