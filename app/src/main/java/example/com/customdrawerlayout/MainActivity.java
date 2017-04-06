package example.com.customdrawerlayout;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;

public class MainActivity extends AppCompatActivity {

    private RelativeLayout rlTop;
    private CustomDrawerLayout mCustomDrawerLayout;
    private int mOffsetHeight;
    private boolean isDrawerMeasured, isViewMeasured;

    // custom listener
    private OnMeasureHeightReadyListener mMeasureHeightReadyListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rlTop = (RelativeLayout) findViewById(R.id.rl_top);
        mCustomDrawerLayout = (CustomDrawerLayout) findViewById(R.id.sliding_layout);
        // setup drawer attributes
        setOffsetHeight(mCustomDrawerLayout, rlTop);
        mCustomDrawerLayout.setDefaultLockMode(CustomDrawerLayout.LockMode.LOCK_MODE_CLOSED);

        // initialize listeners
        initializeListeners();
    }

    /**
     * Initialize custom listeners
     */
    private void initializeListeners() {
        onMeasureHeightReadyListener(new OnMeasureHeightReadyListener() {
            @Override
            public void onMeasuredHeight(int viewId, int height) {
                // update offset height
                mOffsetHeight = mOffsetHeight + height;

                if (viewId == mCustomDrawerLayout.getId()) {
                    isDrawerMeasured = true;
                } else if (viewId == rlTop.getId()) {
                    isViewMeasured = true;
                }

                if (isDrawerMeasured && isViewMeasured) {
                    mCustomDrawerLayout.setOffsetHeight(mOffsetHeight / 2);
                }
            }
        });
    }

    /**
     * Method is used to set callback for when view has been measured. This is used for setting
     * the drawer offset
     */
    private void onMeasureHeightReadyListener(OnMeasureHeightReadyListener listener) {
        mMeasureHeightReadyListener = listener;
    }

    /**
     * Interface to track when views are measured. This is used for setting the drawer offset
     */
    private interface OnMeasureHeightReadyListener {
        void onMeasuredHeight(int viewId, int height);
    }

    /**
     * Method is used to set drawer offset height
     *
     * @param params
     */
    private void setOffsetHeight(View... params) {
        for (final View v : params) {
            if (!FrameworkUtils.checkIfNull(v)) {
                v.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        // remove view tree observer
                        v.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        // set listener
                        mMeasureHeightReadyListener.onMeasuredHeight(v.getId(), v.getMeasuredHeight());
                    }
                });
            }
        }
    }
}
