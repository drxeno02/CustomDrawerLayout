package example.com.customdrawerlayout;

import android.util.Log;

/**
 * Created by leonard on 4/4/2017.
 */

public class Logger {
    /**
     * Helper method for logging e-verbose
     *
     * @param tag
     * @param msg
     */
    public static void e(String tag, String msg) {
        if (!FrameworkUtils.checkIfNull(msg)) {
            if (Constants.DEBUG) {
                Log.e(tag, msg);
            }
        }
    }

    /**
     * Helper method for logging d-verbose
     *
     * @param tag
     * @param msg
     */
    public static void d(String tag, String msg) {
        if (!FrameworkUtils.checkIfNull(msg)) {
            if (Constants.DEBUG) {
                Log.d(tag, msg);
            }
        }
    }

    /**
     * Helper method for logging i-verbose
     *
     * @param tag
     * @param msg
     */
    public static void i(String tag, String msg) {
        if (!FrameworkUtils.checkIfNull(msg)) {
            if (Constants.DEBUG) {
                Log.i(tag, msg);
            }
        }
    }

    /**
     * Helper method for logging v-verbose
     *
     * @param tag
     * @param msg
     */
    public static void v(String tag, String msg) {
        if (!FrameworkUtils.checkIfNull(msg)) {
            if (Constants.DEBUG) {
                Log.v(tag, msg);
            }
        }
    }

    /**
     * Helper method for logging wtf-verbose
     *
     * @param tag
     * @param msg
     */
    public static void wtf(String tag, String msg) {
        if (!FrameworkUtils.checkIfNull(msg)) {
            if (Constants.DEBUG) {
                Log.wtf(tag, msg);
            }
        }
    }
}
