# ![1] Sliding Drawer by Leonard Tatum

I created this sliding drawer to counter the many sliding drawers that are architectured as a ViewGroup that can have only 2 children. The 1st one is the <b>non slidable view</b>; the 2nd is the <b>slidable view</b>, which can slide over the <b>non slidable view</b>.<br/><br/> There are major disadvantages to this. My sliding drawer has no such limitations. And another substantial difference from all other implementations is that it is easy to position the <b>slidable view</b> relative to any view. My sliding drawer can hide content out of the screen and allow the user to drag a handle to bring the content on screen. Currently this drawer is meant only for vertical orientations. The size of the sliding drawer can be defined, an offset can be set, the drawer can take on any shape, content and/or child views. 

[More](http://developer.android.com/reference/android/widget/SlidingDrawer.html) Android Developer Documentation :)


## Setup

Add this to your dependencies:

```
dependencies {
    
    implementation project(':drxenocustomlayout')
}
```


## Example

##### In Layout

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:layout="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#FFFFFF">
    
    <com.blog.ljtatum.drxenocustomlayout.gui.CustomDrawerLayout
        android:id="@+id/sliding_layout"
        android:layout_width="match_parent"
        android:layout_height="500dp" >

    </com.blog.ljtatum.drxenocustomlayout.gui.CustomDrawerLayout>
    
</LinearLayout>
```

##### In Code

```java
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
```

Check the sample for more details.


## Contact

[Leonard Tatum](ljtatum@hotmail.com)


## License

```
Copyright 2017 Leonard Tatum
Copyright (C) 2008 The Android Open Source Project
    
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
    
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## BSD license

```
Copyright (c) 2017 Leonard Tatum. All rights reserved.

Redistribution and use in source and binary forms are permitted provided that 
the above copyright notice and this paragraph are duplicated in all such forms 
and that any documentation, advertising materials, and other materials related 
to such distribution and use acknowledge that the software was developed by 
Leonard Tatum. The names of the developers may not be used to endorse or 
promote products derived from this software without specific prior written 
permission. THIS SOFTWARE IS PROVIDED ``AS IS'' AND WITHOUT ANY EXPRESS OR 
IMPLIED WARRANTIES, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTIES OF 
MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.
```

[1]: https://github.com/drxeno02/androidprojects-book2-slidingdrawer

