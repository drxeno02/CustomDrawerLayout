package example.com.customdrawerlayout;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v4.view.ViewConfigurationCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import static example.com.customdrawerlayout.CustomDrawerLayoutUtils.getLocationInXAxis;
import static example.com.customdrawerlayout.CustomDrawerLayoutUtils.getLocationInYAxis;
import static example.com.customdrawerlayout.CustomDrawerLayoutUtils.getRawDisplayHeight;
import static example.com.customdrawerlayout.CustomDrawerLayoutUtils.getRawDisplayWidth;
import static example.com.customdrawerlayout.CustomDrawerLayoutUtils.isClicked;

/**
 * Created by leonard on 4/4/2017.
 */

public class CustomDrawerLayout extends FrameLayout {

    private static final String TAG = CustomDrawerLayout.class.getSimpleName();

    /**
     * Special value for the position of the layer. GRAVITY_BOTTOM means that the
     * view will stay attached to the bottom part of the screen, and come from
     * there into the viewable area.
     */
    public static final int GRAVITY_BOTTOM = 1;


    /**
     * Special value for the position of the layer. GRAVITY_LEFT means that the
     * view shall be attached to the left side of the screen, and come from
     * there into the viewable area.
     */
    public static final int GRAVITY_LEFT = 2;

    /**
     * Special value for the position of the layer. GRAVITY_RIGHT means that the
     * view shall be attached to the right side of the screen, and come from
     * there into the viewable area.
     */
    public static final int GRAVITY_RIGHT = 3;

    /**
     * Special value for the position of the layer. GRAVITY_TOP means that the
     * view shall be attached to the top side of the screen, and come from
     * there into the viewable area.
     */
    public static final int GRAVITY_TOP = 4;

    /**
     * The default size of the panel that sticks out when closed
     */
    private static final int DEFAULT_SLIDING_LAYER_OFFSET = 100; // arbitrary value

    /**
     * Duration for animations
     */
    private static final int TRANSLATION_ANIM_DURATION = 250;

    /**
     * Minimum distance to indicate fling
     */
    private static final int MIN_DISTANCE_FOR_FLING = 10; // in dp

    /**
     * The default lock mode state
     */
    private static final LockMode DEFAULT_LOCK_MODE_STATE = LockMode.LOCK_MODE_CLOSED; // default lock mode

    // enums
    public enum LockMode {LOCK_MODE_OPEN, LOCK_MODE_CLOSED}
    private enum ScrollState {VERTICAL, HORIZONTAL}

    // position of the last motion event
    private float mInitialCoordinate;

    // drag threshold
    private int mTouchSlop;
    private int mMinimumVelocity;
    private int mMaximumVelocity;
    private int mFlingDistance;
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
    // interaction listener
    private OnInteractListener mOnInteractListener;
    // velocity tracker
    private VelocityTracker mVelocityTracker;

    private int mDelta;
    private int mLastCoordinate;
    private long mPressStartTime;

    // x, y coordinate tracker of views
    private int mActivePointerId;

    /**
     * Sets the listener to be invoked after a switch change
     * {@link OnInteractListener}.
     *
     * @param listener Listener to set
     */
    @SuppressWarnings("unused")
    public void setOnInteractListener(OnInteractListener listener) {
        mOnInteractListener = listener;
    }

    @SuppressWarnings("unused")
    public interface OnInteractListener {
        void onDrawerOpened();
        void onDrawerClosed();
    }

    /**
     * Constructor
     *
     * @param context
     */
    public CustomDrawerLayout(Context context) {
        this(context, null);
    }

    /**
     * Constructor
     *
     * @param context
     * @param attrs
     */
    public CustomDrawerLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Constructor
     *
     * @param context
     * @param attrs
     * @param defStyleAttr
     */
    public CustomDrawerLayout(Context context, AttributeSet attrs, int defStyleAttr) {
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
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        // set fling distance
        final float density = context.getResources().getDisplayMetrics().density;
        mFlingDistance = (int) (MIN_DISTANCE_FOR_FLING * density);

        // set scroll orientation
        if (mStickTo == GRAVITY_BOTTOM || mStickTo == GRAVITY_TOP) {
            // vertical scrolling
            mScrollOrientation = ScrollState.VERTICAL;
        } else {
            // horizontal scrolling
            mScrollOrientation = ScrollState.HORIZONTAL;
        }
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
    public boolean onInterceptTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = MotionEventCompat.getPointerId(event, 0);
                switch (mStickTo) {
                    case GRAVITY_BOTTOM:
                    case GRAVITY_TOP:
                        mInitialCoordinate = event.getY();
                        break;
                    case GRAVITY_LEFT:
                    case GRAVITY_RIGHT:
                        mInitialCoordinate = event.getX();
                        break;
                }
                break;

            case MotionEvent.ACTION_MOVE:

                float coordinate = 0;
                switch (mStickTo) {
                    case GRAVITY_BOTTOM:
                    case GRAVITY_TOP:
                        coordinate = event.getY();

                        break;
                    case GRAVITY_LEFT:
                    case GRAVITY_RIGHT:
                        coordinate = event.getX();
                        break;
                }

                final int diff = (int) Math.abs(coordinate - mInitialCoordinate);

                // confirm that difference is enough to indicate drag action
                if (diff > mTouchSlop) {
                    // start capturing events
                    Logger.d(TAG, "drag is being captured");
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
                if (!FrameworkUtils.checkIfNull(mVelocityTracker)) {
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }
                break;
        }
        // add velocity movements
        if (FrameworkUtils.checkIfNull(mVelocityTracker)) {
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
        if (FrameworkUtils.checkIfNull(mVelocityTracker)) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);

        final View parent = (View) getParent();
        final int coordinate;
        final int distance = getDistance();
        final int tapCoordinate;

        switch (mStickTo) {
            case GRAVITY_BOTTOM:
                coordinate = (int) event.getRawY();
                tapCoordinate = (int) event.getRawY();
                break;
            case GRAVITY_LEFT:
                coordinate = parent.getWidth() - (int) event.getRawX();
                tapCoordinate = (int) event.getRawX();
                break;
            case GRAVITY_RIGHT:
                coordinate = (int) event.getRawX();
                tapCoordinate = (int) event.getRawX();
                break;
            case GRAVITY_TOP:
                coordinate = getRawDisplayHeight(getContext()) - (int) event.getRawY();
                tapCoordinate = (int) event.getRawY();
                break;
            // if view position is not initialized throw an error
            default:
                throw new IllegalStateException("Failed to initialize coordinates");
        }

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:

                /*
                 * Return the pointer identifier associated with a particular pointer data index is
                 * this event. The identifier tells you the actual pointer number associated with
                 * the data, accounting for individual pointers going up and down since the start
                 * of the current gesture.
                 */
                mActivePointerId = event.getPointerId(0);

                switch (mStickTo) {
                    case GRAVITY_BOTTOM:
                        mDelta = coordinate - ((RelativeLayout.LayoutParams) getLayoutParams()).topMargin;
                        break;
                    case GRAVITY_LEFT:
                        mDelta = coordinate - ((RelativeLayout.LayoutParams) getLayoutParams()).rightMargin;
                        break;
                    case GRAVITY_RIGHT:
                        mDelta = coordinate - ((RelativeLayout.LayoutParams) getLayoutParams()).leftMargin;
                        break;
                    case GRAVITY_TOP:
                        mDelta = coordinate - ((RelativeLayout.LayoutParams) getLayoutParams()).bottomMargin;
                        break;
                }

                mLastCoordinate = coordinate;
                mPressStartTime = System.currentTimeMillis();
                break;

            case MotionEvent.ACTION_MOVE:

                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) getLayoutParams();
                final int farMargin = coordinate - mDelta;
                final int closeMargin = distance - farMargin;

                switch (mStickTo) {
                    case GRAVITY_BOTTOM:
                        if (farMargin > distance && closeMargin > mOffsetHeight - getHeight()) {
                            layoutParams.bottomMargin = closeMargin;
                            layoutParams.topMargin = farMargin;
                        }
                        break;
                    case GRAVITY_LEFT:
                        if (farMargin > distance && closeMargin > mOffsetHeight - getWidth()) {
                            layoutParams.leftMargin = closeMargin;
                            layoutParams.rightMargin = farMargin;
                        }
                        break;
                    case GRAVITY_RIGHT:
                        if (farMargin > distance && closeMargin > mOffsetHeight - getWidth()) {
                            layoutParams.rightMargin = closeMargin;
                            layoutParams.leftMargin = farMargin;
                        }
                        break;
                    case GRAVITY_TOP:
                        if (farMargin > distance && closeMargin > mOffsetHeight - getHeight()) {
                            layoutParams.topMargin = closeMargin;
                            layoutParams.bottomMargin = farMargin;
                        }
                        break;
                }
                setLayoutParams(layoutParams);
                break;

            case MotionEvent.ACTION_UP:

                final int diff = coordinate - mLastCoordinate;
                final long pressDuration = System.currentTimeMillis() - mPressStartTime;

                switch (mStickTo) {
                    case GRAVITY_BOTTOM:

                        // determine if fling
                        int relativeVelocity;
                        final VelocityTracker velocityTracker = mVelocityTracker;
                        velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                        final int initialVelocityY = (int) VelocityTrackerCompat.getYVelocity(
                                velocityTracker, mActivePointerId);
                        relativeVelocity = initialVelocityY * -1;
                        // take absolute value to have positive values
                        final int absoluteVelocity = Math.abs(relativeVelocity);

                        if (Math.abs(diff) > mFlingDistance && absoluteVelocity > mMinimumVelocity) {
                            if (tapCoordinate > parent.getHeight() - mOffsetHeight &&
                                    mLockMode == LockMode.LOCK_MODE_CLOSED) {
                                notifyActionAndAnimateForState(LockMode.LOCK_MODE_OPEN, parent.getHeight() - mOffsetHeight, true);
                            } else if (Math.abs(getRawDisplayHeight(getContext()) -
                                    tapCoordinate - getHeight()) < mOffsetHeight &&
                                    mLockMode == LockMode.LOCK_MODE_OPEN) {
                                notifyActionAndAnimateForState(LockMode.LOCK_MODE_CLOSED, parent.getHeight() - mOffsetHeight, true);
                            }
                        } else {
                            if (isClicked(getContext(), diff, pressDuration)) {
                                if (tapCoordinate > parent.getHeight() - mOffsetHeight &&
                                        mLockMode == LockMode.LOCK_MODE_CLOSED) {
                                    notifyActionAndAnimateForState(LockMode.LOCK_MODE_OPEN, parent.getHeight() - mOffsetHeight, true);
                                } else if (Math.abs(getRawDisplayHeight(getContext()) -
                                        tapCoordinate - getHeight()) < mOffsetHeight &&
                                        mLockMode == LockMode.LOCK_MODE_OPEN) {
                                    notifyActionAndAnimateForState(LockMode.LOCK_MODE_CLOSED, parent.getHeight() - mOffsetHeight, true);
                                }
                            } else {
                                smoothScrollToAndNotify(diff);
                            }
                        }
                        break;
                    case GRAVITY_TOP:
                        if (isClicked(getContext(), diff, pressDuration)) {
                            final int y = getLocationInYAxis(this);
                            if (tapCoordinate - Math.abs(y) <= mOffsetHeight &&
                                    mLockMode == LockMode.LOCK_MODE_CLOSED) {
                                notifyActionAndAnimateForState(LockMode.LOCK_MODE_OPEN, parent.getHeight() - mOffsetHeight, true);
                            } else if (getHeight() - (tapCoordinate - Math.abs(y)) < mOffsetHeight &&
                                    mLockMode == LockMode.LOCK_MODE_OPEN) {
                                notifyActionAndAnimateForState(LockMode.LOCK_MODE_CLOSED, parent.getHeight() - mOffsetHeight, true);
                            }
                        } else {
                            smoothScrollToAndNotify(diff);
                        }
                        break;
                    case GRAVITY_LEFT:
                        if (isClicked(getContext(), diff, pressDuration)) {
                            if (tapCoordinate <= mOffsetHeight &&
                                    mLockMode == LockMode.LOCK_MODE_CLOSED) {
                                notifyActionAndAnimateForState(LockMode.LOCK_MODE_OPEN, getWidth() - mOffsetHeight, true);
                            } else if (tapCoordinate > getWidth() - mOffsetHeight &&
                                    mLockMode == LockMode.LOCK_MODE_OPEN) {
                                notifyActionAndAnimateForState(LockMode.LOCK_MODE_CLOSED, getWidth() - mOffsetHeight, true);
                            }
                        } else {
                            smoothScrollToAndNotify(diff);
                        }
                        break;
                    case GRAVITY_RIGHT:
                        if (isClicked(getContext(), diff, pressDuration)) {
                            if (parent.getWidth() - tapCoordinate <= mOffsetHeight &&
                                    mLockMode == LockMode.LOCK_MODE_CLOSED) {
                                notifyActionAndAnimateForState(LockMode.LOCK_MODE_OPEN, getWidth() - mOffsetHeight, true);
                            } else if (parent.getWidth() - tapCoordinate > getWidth() - mOffsetHeight &&
                                    mLockMode == LockMode.LOCK_MODE_OPEN) {
                                notifyActionAndAnimateForState(LockMode.LOCK_MODE_CLOSED, getWidth() - mOffsetHeight, true);
                            }
                        } else {
                            smoothScrollToAndNotify(diff);
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
     * @param diff
     */
    private void smoothScrollToAndNotify(int diff) {
        int length = getLength();
        LockMode stateToApply;
        if (diff > 0) {
            if (diff > length / 2.5) {
                stateToApply = LockMode.LOCK_MODE_CLOSED;
                notifyActionAndAnimateForState(stateToApply, getTranslationFor(stateToApply), true);
            } else if (mLockMode == LockMode.LOCK_MODE_OPEN) {
                stateToApply = LockMode.LOCK_MODE_OPEN;
                notifyActionAndAnimateForState(stateToApply, getTranslationFor(stateToApply), false);
            }
        } else {
            if (Math.abs(diff) > length / 2.5) {
                stateToApply = LockMode.LOCK_MODE_OPEN;
                notifyActionAndAnimateForState(stateToApply, getTranslationFor(stateToApply), true);
            } else if (mLockMode == LockMode.LOCK_MODE_CLOSED) {
                stateToApply = LockMode.LOCK_MODE_CLOSED;
                notifyActionAndAnimateForState(stateToApply, getTranslationFor(stateToApply), false);
            }
        }
    }

    /**
     * Method is used to retrieve dimensions meant for translation
     *
     * @param stateToApply
     * @return
     */
    private int getTranslationFor(LockMode stateToApply) {

        switch (mStickTo) {
            case GRAVITY_BOTTOM:
                switch (stateToApply) {
                    case LOCK_MODE_OPEN:
                        return getHeight() - (getRawDisplayHeight(getContext()) - getLocationInYAxis(this));

                    case LOCK_MODE_CLOSED:
                        return getRawDisplayHeight(getContext()) - getLocationInYAxis(this) - mOffsetHeight;
                }
                break;
            case GRAVITY_TOP:
                final int actionBarDiff = getRawDisplayHeight(getContext()) - ((View) getParent()).getHeight();
                final int y = getLocationInYAxis(this) + getHeight();

                switch (stateToApply) {
                    case LOCK_MODE_OPEN:
                        return getHeight() - y + actionBarDiff;

                    case LOCK_MODE_CLOSED:
                        return y - mOffsetHeight - actionBarDiff;
                }
                break;
            case GRAVITY_LEFT:
                final int x = getLocationInXAxis(this) + getWidth();
                switch (stateToApply) {
                    case LOCK_MODE_OPEN:
                        return getWidth() - x;

                    case LOCK_MODE_CLOSED:
                        return x - mOffsetHeight;
                }
                break;
            case GRAVITY_RIGHT:
                switch (stateToApply) {
                    case LOCK_MODE_OPEN:
                        return getWidth() - (getRawDisplayWidth(getContext()) - getLocationInXAxis(this));

                    case LOCK_MODE_CLOSED:
                        return getRawDisplayWidth(getContext()) - getLocationInXAxis(this) - mOffsetHeight;
                }
                break;
        }
        throw new IllegalStateException("Failed to return translation for drawer");
    }

    /**
     * Method is used to perform the animations
     *
     * @param stateToApply
     * @param translation
     * @param notify
     */
    private void notifyActionAndAnimateForState(final LockMode stateToApply,
                                                final int translation, final boolean notify) {

        switch (mStickTo) {
            case GRAVITY_BOTTOM:
                switch (stateToApply) {
                    case LOCK_MODE_OPEN:
                        animate().translationY(-translation)
                                .setDuration(TRANSLATION_ANIM_DURATION)
                                .setInterpolator(new DecelerateInterpolator())
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        super.onAnimationEnd(animation);
                                        notifyActionForState(stateToApply, notify);
                                        setTranslationY(0);
                                    }
                                });
                        break;
                    case LOCK_MODE_CLOSED:
                        animate().translationY(translation)
                                .setDuration(TRANSLATION_ANIM_DURATION)
                                .setInterpolator(new DecelerateInterpolator())
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        super.onAnimationEnd(animation);
                                        notifyActionForState(stateToApply, notify);
                                        setTranslationY(0);
                                    }
                                });
                        break;
                }
                break;
            case GRAVITY_TOP:
                switch (stateToApply) {
                    case LOCK_MODE_OPEN:
                        animate().translationY(translation)
                                .setDuration(TRANSLATION_ANIM_DURATION)
                                .setInterpolator(new DecelerateInterpolator())
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        super.onAnimationEnd(animation);
                                        notifyActionForState(stateToApply, notify);
                                        setTranslationY(0);
                                    }
                                });
                        break;
                    case LOCK_MODE_CLOSED:
                        animate().translationY(-translation)
                                .setDuration(TRANSLATION_ANIM_DURATION)
                                .setInterpolator(new DecelerateInterpolator())
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        super.onAnimationEnd(animation);
                                        notifyActionForState(stateToApply, notify);
                                        setTranslationY(0);
                                    }
                                });
                        break;
                }
                break;
            case GRAVITY_LEFT:
                switch (stateToApply) {
                    case LOCK_MODE_OPEN:
                        animate().translationX(translation)
                                .setDuration(TRANSLATION_ANIM_DURATION)
                                .setInterpolator(new DecelerateInterpolator())
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        super.onAnimationEnd(animation);
                                        notifyActionForState(stateToApply, notify);
                                        setTranslationX(0);
                                    }
                                });
                        break;
                    case LOCK_MODE_CLOSED:
                        animate().translationX(-translation)
                                .setDuration(TRANSLATION_ANIM_DURATION)
                                .setInterpolator(new DecelerateInterpolator())
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        super.onAnimationEnd(animation);
                                        notifyActionForState(stateToApply, notify);
                                        setTranslationX(0);
                                    }
                                });
                        break;
                }
                break;
            case GRAVITY_RIGHT:
                switch (stateToApply) {
                    case LOCK_MODE_OPEN:
                        animate().translationX(-translation)
                                .setDuration(TRANSLATION_ANIM_DURATION)
                                .setInterpolator(new DecelerateInterpolator())
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        super.onAnimationEnd(animation);
                                        notifyActionForState(stateToApply, notify);
                                        setTranslationX(0);
                                    }
                                });
                        break;
                    case LOCK_MODE_CLOSED:
                        animate().translationX(translation)
                                .setDuration(TRANSLATION_ANIM_DURATION)
                                .setInterpolator(new DecelerateInterpolator())
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        super.onAnimationEnd(animation);
                                        notifyActionForState(stateToApply, notify);
                                        setTranslationX(0);
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
     * @param stateToApply
     * @param notify
     */
    private void notifyActionForState(LockMode stateToApply, boolean notify) {

        final int distance = getDistance();
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) getLayoutParams();

        switch (mStickTo) {
            case GRAVITY_BOTTOM:
                switch (stateToApply) {
                    case LOCK_MODE_OPEN:
                        params.bottomMargin = 0;
                        params.topMargin = distance;
                        break;
                    case LOCK_MODE_CLOSED:
                        params.bottomMargin = mOffsetHeight - getHeight();
                        params.topMargin = distance - (mOffsetHeight - getHeight());
                        break;
                }
                break;
            case GRAVITY_LEFT:
                switch (stateToApply) {
                    case LOCK_MODE_OPEN:
                        params.leftMargin = 0;
                        params.rightMargin = distance;
                        break;
                    case LOCK_MODE_CLOSED:
                        params.leftMargin = mOffsetHeight - getWidth();
                        params.rightMargin = distance - (mOffsetHeight - getWidth());
                        break;
                }
                break;

            case GRAVITY_RIGHT:
                switch (stateToApply) {
                    case LOCK_MODE_OPEN:
                        params.rightMargin = 0;
                        params.leftMargin = distance;
                        break;
                    case LOCK_MODE_CLOSED:
                        params.rightMargin = mOffsetHeight - getWidth();
                        params.leftMargin = distance - (mOffsetHeight - getWidth());
                        break;
                }
                break;
            case GRAVITY_TOP:
                switch (stateToApply) {
                    case LOCK_MODE_OPEN:
                        params.topMargin = 0;
                        params.bottomMargin = distance;
                        break;
                    case LOCK_MODE_CLOSED:
                        params.topMargin = mOffsetHeight - getHeight();
                        params.bottomMargin = distance - (mOffsetHeight - getHeight());
                        break;
                }
                break;
        }
        if (notify) {
            notifyActionFinished(stateToApply);
        }
        setLayoutParams(params);
    }

    /**
     * Method is used to notify a change in the lock state of the drawer
     *
     * @param state
     */
    private void notifyActionFinished(LockMode state) {

        switch (state) {
            case LOCK_MODE_OPEN:
                mLockMode = LockMode.LOCK_MODE_OPEN;
                if (!FrameworkUtils.checkIfNull(mOnInteractListener)) {
                    mOnInteractListener.onDrawerOpened();
                }
                break;
            case LOCK_MODE_CLOSED:
                mLockMode = LockMode.LOCK_MODE_CLOSED;
                if (!FrameworkUtils.checkIfNull(mOnInteractListener)) {
                    mOnInteractListener.onDrawerClosed();
                }
                break;
        }
    }

    /**
     * Check if drawer is opened
     *
     * @return
     */
    public boolean isOpened() {
        return mLockMode == LockMode.LOCK_MODE_OPEN;
    }

    /**
     * Check if drawer is clsoed
     *
     * @return
     */
    public boolean isClosed() {
        return mLockMode == LockMode.LOCK_MODE_CLOSED;
    }

    @SuppressWarnings("unused")
    public void openDrawer() {
        notifyActionAndAnimateForState(LockMode.LOCK_MODE_OPEN, getLength() - mOffsetHeight, !isOpened());
    }

    @SuppressWarnings("unused")
    public void closeDrawer() {
        notifyActionAndAnimateForState(LockMode.LOCK_MODE_CLOSED, getLength() - mOffsetHeight, !isClosed());
    }

    private int getDistance() {
        final View parent = (View) getParent();

        switch (mScrollOrientation) {
            case VERTICAL:
                return parent.getHeight() -
                        parent.getPaddingTop() -
                        parent.getPaddingBottom() -
                        getHeight();
            case HORIZONTAL:
                return parent.getWidth() -
                        parent.getPaddingLeft() -
                        parent.getPaddingRight() -
                        getWidth();
        }
        throw new IllegalStateException("Error Scroll orientation is not initialized");
    }

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
     * @param offsetHeight
     */
    public void setOffsetHeight(int offsetHeight) {
        mOffsetHeight = offsetHeight;
    }

    /**
     * Method is used to set the default lock mode, e.g. OPEN OR CLOSED
     * @param lockMode
     */
    public void setDefaultLockMode(LockMode lockMode) {
        mLockMode = lockMode;
        notifyActionForState(mLockMode, false);
    }
}
