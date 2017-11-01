package com.blog.ljtatum.drxenocustomlayout.gui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewConfigurationCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.blog.ljtatum.drxenocustomlayout.R;
import com.blog.ljtatum.drxenocustomlayout.utils.Utils;

import static com.blog.ljtatum.drxenocustomlayout.utils.CustomDrawerLayoutUtils.getRawDisplayHeight;
import static com.blog.ljtatum.drxenocustomlayout.utils.CustomDrawerLayoutUtils.isClicked;

/**
 * Created by LJTat on 11/1/2017.
 */

public class CustomDrawerLayout extends FrameLayout {

    /**
     * Special value for the position of the layer.
     * <p>GRAVITY_BOTTOM means that the view will stay attached to the bottom part of
     * the screen, and come from there into the viewable area</p>
     */
    public static final int GRAVITY_BOTTOM = 1;
    private static final String TAG = CustomDrawerLayout.class.getSimpleName();
    /**
     * The default size of the panel that sticks out when closed
     */
    private static final int DEFAULT_SLIDING_LAYER_OFFSET = 100; // arbitrary value

    /**
     * Duration for animations
     */
    private static final int TRANSLATION_ANIM_DURATION_SHORT = 200;

    /**
     * Minimum distance to indicate fling
     */
    private static final int MIN_DISTANCE_FOR_FLING = 10; // in dp

    /**
     * The default lock mode state
     */
    private static final LockMode DEFAULT_LOCK_MODE_STATE = LockMode.LOCK_MODE_CLOSED; // default lock mode
    // position of the last motion event
    private float mInitialCoordinate;
    // offset between rawY coordinate and view-related Y coordinate
    private float mTouchOffsetY;
    // drag threshold
    private int mTouchSlop;
    private int mMinimumVelocity, mMaximumVelocity, mFlingDistance, mDelta, mLastCoordinate;
    // the height of the panel that sticks out when closed
    private int mOffsetHeight;
    // value for the position of the layer in the screen
    private int mStickTo;
    // lock mode state
    private LockMode mLockMode;
    // value for the orientation of the layer in the screen
    private ScrollState mScrollOrientation;
    // flag for when drawer is initialized
    private boolean isDrawerInitialized;
    // flag to track if drawer is animating
    private boolean isAnimating;
    // flag to disable touch events
    private boolean isGlobalTouchEventDisabled, isFirstVisibleItemPos;
    // interaction listener
    private OnInteractListener mOnInteractListener;
    // velocity tracker
    @Nullable
    private VelocityTracker mVelocityTracker;
    private long mPressStartTime;

    /**
     * Constructor
     *
     * @param context Interface to global information about an application environment
     */
    public CustomDrawerLayout(@NonNull Context context) {
        this(context, null);
    }

    /**
     * Constructor
     *
     * @param context Interface to global information about an application environment
     * @param attrs   A collection of attributes, as found associated with a tag in an XML document
     */
    public CustomDrawerLayout(@NonNull Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Constructor
     *
     * @param context      Interface to global information about an application environment
     * @param attrs        A collection of attributes, as found associated with a tag in an XML document
     * @param defStyleAttr The defined style
     */
    public CustomDrawerLayout(@NonNull Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        // get the attributes specified in attrs.xml
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.CustomDrawerLayout, 0, 0);

        try {
            // set default values for gravity, offset, lock mode
            mLockMode = DEFAULT_LOCK_MODE_STATE;
            mStickTo = a.getInteger(R.styleable.CustomDrawerLayout_stickTo, GRAVITY_BOTTOM);
            mOffsetHeight = a.getDimensionPixelSize(R.styleable.CustomDrawerLayout_offsetDistance,
                    DEFAULT_SLIDING_LAYER_OFFSET);

            // set flag that drawer is initialized
            isDrawerInitialized = true;
        } finally {
            a.recycle();
        }

        // get system constants for touch thresholds
        final ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = ViewConfigurationCompat.getScaledPagingTouchSlop(configuration);
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity() * 2;
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        // set fling distance
        final float density = context.getResources().getDisplayMetrics().density;
        mFlingDistance = (int) (MIN_DISTANCE_FOR_FLING * density);

        // set scroll orientation
        if (mStickTo == GRAVITY_BOTTOM) {
            // vertical scrolling
            mScrollOrientation = ScrollState.VERTICAL;
        } else {
            // horizontal scrolling
            mScrollOrientation = ScrollState.HORIZONTAL;
        }
    }

    /**
     * Sets the listener to be invoked after a switch change
     * {@link OnInteractListener}.
     *
     * @param listener Callback for when the user interacts with the drawer
     */
    @SuppressWarnings("unused")
    public void setOnInteractListener(OnInteractListener listener) {
        mOnInteractListener = listener;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        if (isDrawerInitialized) {
            post(new Runnable() {
                @Override
                public void run() {
                    notifyActionForState(mLockMode, false);
                }
            });
            // reset initialized flag
            isDrawerInitialized = false;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(@NonNull MotionEvent event) {
        // ignore touch events if disabled
        if (isGlobalTouchEventDisabled) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                switch (mStickTo) {
                    case GRAVITY_BOTTOM:
                        mInitialCoordinate = event.getY();
                        break;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                float coordinate = 0;
                switch (mStickTo) {
                    case GRAVITY_BOTTOM:
                        coordinate = event.getY();
                        break;
                    default:
                        break;
                }

                final int diff = (int) Math.abs(coordinate - mInitialCoordinate);

                // confirm that difference is enough to indicate drag action
                if (diff > mTouchSlop) {
                    // start capturing events
                    Log.d(TAG, "drag is being captured");
                    mTouchOffsetY = Math.abs(getY() - event.getRawY());
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
                if (!Utils.checkIfNull(mVelocityTracker)) {
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }
                break;
        }
        // add velocity movements
        if (Utils.checkIfNull(mVelocityTracker)) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);

        return super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && event.getEdgeFlags() != 0) {
            return false;
        }

        // add velocity movements
        if (Utils.checkIfNull(mVelocityTracker)) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);

        final View parent = (View) getParent();
        final int coordinate;
        final int tapCoordinate;

        switch (mStickTo) {
            case GRAVITY_BOTTOM:
                coordinate = (int) event.getRawY();
                tapCoordinate = (int) event.getRawY();
                break;
            // if view position is not initialized throw an error
            default:
                throw new IllegalStateException("Failed to initialize coordinates");
        }

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                switch (mStickTo) {
                    case GRAVITY_BOTTOM:
                        mDelta = coordinate - ((RelativeLayout.LayoutParams) getLayoutParams()).topMargin;
                        break;
                    default:
                        break;
                }

                mLastCoordinate = coordinate;
                mTouchOffsetY = Math.abs(getY() - mLastCoordinate);
                mPressStartTime = System.currentTimeMillis();
                break;

            case MotionEvent.ACTION_MOVE:
                float newY = coordinate - mTouchOffsetY;
                if (newY < 0) {
                    setY(0);
                } else if (newY > parent.getHeight() - mOffsetHeight) {
                    setY(parent.getHeight() - mOffsetHeight);
                } else {
                    setY(newY);
                }
                break;

            case MotionEvent.ACTION_UP:
                final int diff = coordinate - mLastCoordinate;
                final long pressDuration = System.currentTimeMillis() - mPressStartTime;

                switch (mStickTo) {
                    case GRAVITY_BOTTOM:
                        // determine velocity
                        int relativeVelocity;
                        mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                        final int initialVelocityY = (int) mVelocityTracker.getYVelocity();
                        relativeVelocity = initialVelocityY * -1;

                        // take absolute value to have positive values
                        final int absoluteVelocity = Math.abs(relativeVelocity);

                        if (!isAnimating) {
                            if (Math.abs(diff) > mTouchSlop && mDelta > mFlingDistance) {
                                // drag action
                                // smooth scroll
                                smoothScrollToAndNotify(diff);
                            } else if (absoluteVelocity > mMinimumVelocity && mDelta > mFlingDistance) {
                                // fling action
                                if (tapCoordinate > parent.getHeight() - mOffsetHeight &&
                                        mLockMode == LockMode.LOCK_MODE_CLOSED) {
                                    notifyActionAndAnimateForState(LockMode.LOCK_MODE_OPEN, true);
                                } else if (Math.abs(getRawDisplayHeight(getContext()) -
                                        tapCoordinate - getHeight()) < mOffsetHeight &&
                                        mLockMode == LockMode.LOCK_MODE_OPEN) {
                                    notifyActionAndAnimateForState(LockMode.LOCK_MODE_CLOSED, true);
                                } else {
                                    // no change in state, therefore no reason to notify state change. Boolean set to false
                                    notifyActionAndAnimateForState(mLockMode, false);
                                }
                            } else {
                                // tap action
                                if (isClicked(getContext(), diff, pressDuration)) {
                                    if (mLockMode == LockMode.LOCK_MODE_CLOSED) {
                                        notifyActionAndAnimateForState(LockMode.LOCK_MODE_OPEN, true);
                                    } else {
                                        notifyActionAndAnimateForState(LockMode.LOCK_MODE_CLOSED, true);
                                    }
                                } else {
                                    // no change in state, therefore no reason to notify state change. Boolean set to false
                                    notifyActionAndAnimateForState(mLockMode, false);
                                }
                            }
                        }
                        break;
                }
                break;
        }
        return true;
    }

    /**
     * Method is used to animate the view to the given position
     *
     * @param diff The difference in position of the drawer
     */
    private void smoothScrollToAndNotify(int diff) {
        int length = getLength();
        if (diff > 0) {
            if (diff > length / 5.0) {
                notifyActionAndAnimateForState(LockMode.LOCK_MODE_CLOSED, true);
            } else {
                // no change in state, therefore no reason to notify state change. Boolean set to false
                notifyActionAndAnimateForState(mLockMode, false);
            }
        } else {
            if (Math.abs(diff) > length / 5.0) {
                notifyActionAndAnimateForState(LockMode.LOCK_MODE_OPEN, true);
            } else {
                // no change in state, therefore no reason to notify state change. Boolean set to false
                notifyActionAndAnimateForState(mLockMode, false);
            }
        }
    }

    /**
     * Method is used to perform the animations
     *
     * @param stateToApply The drawer architecture has multiple states e.g.
     *                     LOCK_MODE_OPEN, LOCK_MODE_CLOSED
     * @param notify       True to log the animation progress of the drawer, otherwise false
     */
    private void notifyActionAndAnimateForState(@NonNull final LockMode stateToApply, final boolean notify) {
        final View parent = (View) getParent();

        switch (mStickTo) {
            case GRAVITY_BOTTOM:
                switch (stateToApply) {
                    case LOCK_MODE_OPEN:
                        isAnimating = true;
                        animate().y(0)
                                .setDuration(TRANSLATION_ANIM_DURATION_SHORT)
                                .setInterpolator(new DecelerateInterpolator())
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        super.onAnimationEnd(animation);
                                        isAnimating = false;
                                        notifyActionForState(stateToApply, notify);
                                        animate().setListener(null);
                                    }
                                });
                        break;
                    case LOCK_MODE_CLOSED:
                        isAnimating = true;
                        animate().y(parent.getHeight() - mOffsetHeight)
                                .setDuration(TRANSLATION_ANIM_DURATION_SHORT)
                                .setInterpolator(new DecelerateInterpolator())
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        super.onAnimationEnd(animation);
                                        isAnimating = false;
                                        notifyActionForState(stateToApply, notify);
                                        animate().setListener(null);
                                    }
                                });
                        break;
                }
                break;
        }
    }

    /**
     * Method is used to update params based on gravity in order to position stickyTo
     *
     * @param stateToApply The drawer architecture has multiple states e.g.
     *                     LOCK_MODE_OPEN, LOCK_MODE_CLOSED
     * @param notify       True to log the animation progress of the drawer, otherwise false
     */
    public void notifyActionForState(LockMode stateToApply, boolean notify) {
        if (Utils.checkIfNull(stateToApply)) {
            stateToApply = mLockMode;
        }

        switch (mStickTo) {
            case GRAVITY_BOTTOM:
                switch (stateToApply) {
                    case LOCK_MODE_OPEN:
                        setY(0);
                        break;
                    case LOCK_MODE_CLOSED:
                        setY(getHeight() - mOffsetHeight);
                        break;
                }
                break;
            default:
                break;
        }
        if (notify) {
            notifyActionFinished(stateToApply);
        }
    }

    /**
     * Method is used to notify a change in the lock state of the drawer
     *
     * @param stateToApply The drawer architecture has multiple states e.g.
     *                     LOCK_MODE_OPEN, LOCK_MODE_CLOSED
     */
    private void notifyActionFinished(@NonNull LockMode stateToApply) {

        switch (stateToApply) {
            case LOCK_MODE_OPEN:
                mLockMode = LockMode.LOCK_MODE_OPEN;
                if (!Utils.checkIfNull(mOnInteractListener)) {
                    mOnInteractListener.onDrawerOpened();
                }
                break;
            case LOCK_MODE_CLOSED:
                mLockMode = LockMode.LOCK_MODE_CLOSED;
                if (!Utils.checkIfNull(mOnInteractListener)) {
                    mOnInteractListener.onDrawerClosed();
                }
                break;
        }
    }

    /**
     * Check if drawer is opened
     *
     * @return True if drawer is opened, otherwise false
     */
    public boolean isOpened() {
        return mLockMode == LockMode.LOCK_MODE_OPEN;
    }

    /**
     * Check if drawer is closed
     *
     * @return True if drawer is closed, otherwise false
     */
    public boolean isClosed() {
        return mLockMode == LockMode.LOCK_MODE_CLOSED;
    }

    /**
     * Method is used to open drawer
     */
    @SuppressWarnings("unused")
    public void openDrawer() {
        notifyActionAndAnimateForState(LockMode.LOCK_MODE_OPEN, !isOpened());
    }

    /**
     * Method is used to close drawer
     */
    @SuppressWarnings("unused")
    public void closeDrawer() {
        notifyActionAndAnimateForState(LockMode.LOCK_MODE_CLOSED, !isClosed());
    }

    /**
     * Method is used to get the view height of width
     *
     * @return The view height or width depending upon of view orientation
     */
    private int getLength() {
        switch (mScrollOrientation) {
            case VERTICAL:
                return getHeight();
            case HORIZONTAL:
                return getWidth();
        }
        throw new IllegalStateException("Scroll orientation is not initialized");
    }

    /**
     * Method is used to set the offset height for the sliding drawer. This is the how much you
     * want the drawer to stick out
     *
     * @param offsetHeight The height to which you want the drawer to stick out
     */
    public void setOffsetHeight(int offsetHeight) {
        mOffsetHeight = offsetHeight;
    }

    /**
     * Method is used to set the default lock mode, e.g. OPEN OR CLOSED
     *
     * @param stateToApply The drawer architecture has multiple states e.g.
     *                     LOCK_MODE_OPEN, LOCK_MODE_CLOSED
     */
    public void setDefaultLockMode(@NonNull LockMode stateToApply) {
        mLockMode = stateToApply;
        notifyActionForState(mLockMode, false);
    }

    /**
     * Method is used to enable/disable global touch event
     *
     * @param isGlobalTouchEventDisabled True to enable interaction with entire drawer,
     *                                   otherwise false
     */
    public void toggleGlobalTouchEvent(boolean isGlobalTouchEventDisabled) {
        this.isGlobalTouchEventDisabled = isGlobalTouchEventDisabled;
    }

    /**
     * Method is used to check if global touch events are enabled/disabled
     *
     * @return True if interaction with entire drawer is enabled, otherwise false
     */
    public boolean isGlobalTouchEventDisabled() {
        return isGlobalTouchEventDisabled;
    }

    /**
     * Method is used to setScrollableView. The scrollableView will disable global touch events
     * by default to pass touch events to the passed in recyclerView
     *
     * @param recyclerView Flexible view for providing a limited window into a large data set
     */
    @SuppressLint("ClickableViewAccessibility")
    public void setScrollableView(@NonNull final RecyclerView recyclerView) {
        // disable global touch events
        isGlobalTouchEventDisabled = true;

        // OnScrollListener
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
                int pos = lm.findFirstVisibleItemPosition();
                isFirstVisibleItemPos = lm.findViewByPosition(pos).getTop() == 0 && pos == 0;
            }
        });

        // OnTouchListener
        recyclerView.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, @NonNull MotionEvent event) {
                if (mLockMode == LockMode.LOCK_MODE_OPEN) {
                    if (isFirstVisibleItemPos && event.getAction() == MotionEvent.ACTION_UP) {
                        notifyActionAndAnimateForState(LockMode.LOCK_MODE_CLOSED, true);
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        notifyActionAndAnimateForState(LockMode.LOCK_MODE_OPEN, true);
                    }
                    return false;
                }
            }
        });

    }

    // enums
    public enum LockMode {
        LOCK_MODE_OPEN, LOCK_MODE_CLOSED
    }

    private enum ScrollState {VERTICAL, HORIZONTAL}

    @SuppressWarnings("unused")
    public interface OnInteractListener {
        void onDrawerOpened();

        void onDrawerClosed();
    }

}
