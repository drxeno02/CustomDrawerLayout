package example.com.customdrawerlayout;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.blog.ljtatum.drxenocustomlayout.gui.CustomDrawerLayout;
import com.blog.ljtatum.drxenocustomlayout.utils.Utils;

/**
 * Created by LJTat on 11/1/2017.
 */

public class MainActivity extends AppCompatActivity {

    private CustomDrawerLayout mCustomDrawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCustomDrawerLayout = (CustomDrawerLayout) findViewById(R.id.sliding_layout);
        mCustomDrawerLayout.toggleGlobalTouchEvent(true);
        mCustomDrawerLayout.setDefaultLockMode(CustomDrawerLayout.LockMode.LOCK_MODE_OPEN);

        // initialize listeners
        initializeListeners();
    }

    /**
     * Initialize custom listeners
     */
    private void initializeListeners() {
        // OnInteractListener for drawer
        mCustomDrawerLayout.setOnInteractListener(new CustomDrawerLayout.OnInteractListener() {
            @Override
            public void onDrawerOpened() {
                // do whatever you want when drawer opens
                Toast.makeText(MainActivity.this, "drawer opened", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onDrawerClosed() {
                // do whatever you want when drawer closes
                Toast.makeText(MainActivity.this, "drawer closed", Toast.LENGTH_LONG).show();
            }
        });

        // animate top and bottom views
        ViewTreeObserver vto = mCustomDrawerLayout.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                mCustomDrawerLayout.setOffsetHeight(mCustomDrawerLayout.getHeight() / 4);
                mCustomDrawerLayout.setDefaultLockMode(CustomDrawerLayout.LockMode.LOCK_MODE_CLOSED);
            }
        });
    }
}
