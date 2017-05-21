package example.com.customdrawerlayout;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.ClipData;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewConfigurationCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import static example.com.customdrawerlayout.CustomDrawerLayoutUtils.getLocationInYAxis;
import static example.com.customdrawerlayout.CustomDrawerLayoutUtils.getRawDisplayHeight;
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
    public enum LockMode {
        LOCK_MODE_OPEN, LOCK_MODE_CLOSED
    }

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
                    default:
                        break;
                }
                setLayoutParams(layoutParams);
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

                        if (Math.abs(diff) > mTouchSlop) {
                            // drag action
                            // smooth scroll
                            smoothScrollToAndNotify(diff);
                        } else if (absoluteVelocity > mMinimumVelocity) {
                            // fling action
                            if (tapCoordinate > parent.getHeight() - mOffsetHeight &&
                                    mLockMode == LockMode.LOCK_MODE_CLOSED) {
                                notifyActionAndAnimateForState(LockMode.LOCK_MODE_OPEN, getTranslationFor(LockMode.LOCK_MODE_OPEN), true);
                            } else if (Math.abs(getRawDisplayHeight(getContext()) -
                                    tapCoordinate - getHeight()) < mOffsetHeight &&
                                    mLockMode == LockMode.LOCK_MODE_OPEN) {
                                notifyActionAndAnimateForState(LockMode.LOCK_MODE_CLOSED, getTranslationFor(LockMode.LOCK_MODE_CLOSED), true);

                            }
                        } else {
                            // tap action
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
                                // smooth scroll
                                smoothScrollToAndNotify(diff);
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
            default:
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
        }
    }

    /**
     * Method is used to update params based on gravity in order to position stickyTo
     *
     * @param stateToApply
     * @param notify
     */
    private void notifyActionForState(LockMode stateToApply, boolean notify) {
        // create params
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) getLayoutParams();

        switch (mStickTo) {
            case GRAVITY_BOTTOM:
                switch (stateToApply) {
                    case LOCK_MODE_OPEN:
                        params.bottomMargin = 0;
                        params.topMargin = 0;
                        break;
                    case LOCK_MODE_CLOSED:
                        params.bottomMargin = mOffsetHeight - getHeight();
                        params.topMargin = -(mOffsetHeight - getHeight());
                        break;
                }
                break;
            default:
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
     *
     * @param lockMode
     */
    public void setDefaultLockMode(LockMode lockMode) {
        mLockMode = lockMode;
        notifyActionForState(mLockMode, false);
    }
}
