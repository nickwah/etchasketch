/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mylikes.likes.etchasketch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.LinkedList;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mylikes.likes.etchasketch.ToolButton.SwatchButton;

public class MarkersActivity extends Activity implements ShakeSensor.ShakeListener {
    final static int LOAD_IMAGE = 1000;

    private static final String TAG = "Markers";
    private static final boolean DEBUG = true;

    public static final String IMAGE_SAVE_DIRNAME = "Drawings";
    public static final String IMAGE_TEMP_DIRNAME = IMAGE_SAVE_DIRNAME + "/.temporary";
    public static final String WIP_FILENAME = "temporary.png";

    public static final String PREF_LAST_TOOL = "tool";
    public static final String PREF_LAST_TOOL_TYPE = "tool_type";
    public static final String PREF_LAST_COLOR = "color";
    public static final String PREF_LAST_HUDSTATE = "hudup";
    public static final String LAST_PEN_SIZE = "LAST_PEN_SIZE";

    private boolean mJustLoadedImage = false;

    private Slate mSlate;
    private ZoomTouchView mZoomView;

    private ToolButton mLastTool, mActiveTool;
    private ToolButton mLastColor, mActiveColor;
    private ToolButton mLastPenType, mActivePenType;
    private SpotSizeBar spotSizeTool;

    private View mDebugButton;
    private View mColorsView;
    private View mActionBarView;
    private View mToolsView;

    private Dialog mMenuDialog;

    private SharedPreferences mPrefs;

    private ShakeSensor shakeSensor;

    private LinkedList<String> mDrawingsToScan = new LinkedList<String>();
    private boolean firstTextClick = true;

    protected MediaScannerConnection mMediaScannerConnection;
    private String mPendingShareFile;
    private MediaScannerConnectionClient mMediaScannerClient =
            new MediaScannerConnection.MediaScannerConnectionClient() {
                @Override
                public void onMediaScannerConnected() {
                    if (DEBUG) Log.v(TAG, "media scanner connected");
                    scanNext();
                }

                private void scanNext() {
                    synchronized (mDrawingsToScan) {
                        if (mDrawingsToScan.isEmpty()) {
                            mMediaScannerConnection.disconnect();
                            return;
                        }
                        String fn = mDrawingsToScan.removeFirst();
                        Log.i("telmer","scanning");
                        mMediaScannerConnection.scanFile(fn, "image/png");
                    }
                }

                @Override
                public void onScanCompleted(String path, Uri uri) {
                    if (DEBUG) Log.v(TAG, "File scanned: " + path);
                    synchronized (mDrawingsToScan) {
                        if (path.equals(mPendingShareFile)) {
                            Intent sendIntent = new Intent(Intent.ACTION_SEND);
                            sendIntent.setType("image/png");
                            sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
                            startActivity(Intent.createChooser(sendIntent, "Send drawing to:"));
                            mPendingShareFile = null;
                        }
                        scanNext();
                    }
                }
            };


    @Override
    public void onShake() {
        clickClear(null);
    }


    public static class ColorList extends LinearLayout {
        public ColorList(Context c, AttributeSet as) {
            super(c, as);
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            super.onLayout(changed, left, top, right, bottom);
            int newOrientation = (((right-left) > (bottom-top)) ? HORIZONTAL : VERTICAL);
            if (newOrientation != getOrientation()) {
                setOrientation(newOrientation);
            }
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent e) {
            return true;
        }
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
    	((ViewGroup)mSlate.getParent()).removeView(mSlate);
        return mSlate;
    }

    public static interface ViewFunc {
        public void apply(View v);
    }
    public static void descend(ViewGroup parent, ViewFunc func) {
        for (int i=0; i<parent.getChildCount(); i++) {
            final View v = parent.getChildAt(i);
            if (v instanceof ViewGroup) {
                descend((ViewGroup) v, func);
            } else {
                func.apply(v);
            }
        }
    }

    public void setColors() {
        /*
        http://www.colourlovers.com/palette/150341/Finger_Painting
        http://www.colourlovers.com/palette/2795912/Her_Finger_Paints
         */
        String[] colors = {
                "#A8C0A8", "#489078", "#784800", "#D86048", "#903060", "#CED9DF",
                "#FF9182", "#FFA75D", "#4BCAD1", "#95B138", "#18252D", "#767D5E",
                "#EBF55B", "#FFC94D", "#28A14C", "#315668", "#B44868", "#505A26",
                "#C0AC8B", "#C71427", "#9C284E", "#61FFEA", "#F55E47", "#BEF547",
                "#000000", "#333333", "#666666", "#999999", "#CCCCCC", "#FFFFFF",
        };

        LinearLayout container = (LinearLayout)mColorsView;
        for (int i = 0; i < 10; i++) {
            LinearLayout stack = (LinearLayout)container.getChildAt(i);
            for (int j = 0; j < 3; j++) {
                SwatchButton button = (SwatchButton)stack.getChildAt(j);
                button.color = Color.parseColor(colors[i * 3 + j]);
            }
        }
    }

    private static final int[] ITEM_DRAWABLES = {
            R.drawable.fountainpen, R.drawable.scribble };



    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        final Window win = getWindow();
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(win.getAttributes());
        lp.format = PixelFormat.RGBA_8888;
        win.setBackgroundDrawableResource(R.drawable.transparent);
        win.setAttributes(lp);
        //win.requestFeature(Window.FEATURE_NO_TITLE);

        // Hide the status bar.
        if (Build.VERSION.SDK_INT < 16) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | Window.FEATURE_NO_TITLE,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN | Window.FEATURE_NO_TITLE);
        } else {
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
            //getActionBar().hide();
        }

        setContentView(R.layout.fragment_editor);
        mSlate = (Slate) getLastNonConfigurationInstance();
        if (mSlate == null) {
        	mSlate = new Slate(this);
            int w = ViewGroup.LayoutParams.MATCH_PARENT, h = ViewGroup.LayoutParams.MATCH_PARENT;
            if (getIntent().hasExtra("width")) {
                w = getIntent().getIntExtra("width", w);
                h = getIntent().getIntExtra("height", h);
            }
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(w, h);
            params.addRule(RelativeLayout.RIGHT_OF, R.id.tools);
            params.addRule(RelativeLayout.ABOVE, R.id.colors);
            //params.addRule(RelativeLayout.BELOW, R.id.actionbar);
            mSlate.setLayoutParams(params);

        	// Load the old buffer if necessary
            if (getIntent().hasExtra("picture")) {
                loadDrawing(getIntent().getStringExtra("picture"));
            } else if (!mJustLoadedImage) {
                loadDrawing(WIP_FILENAME, true, false);
            } else {
                mJustLoadedImage = false;
            }
        }
        final ViewGroup root = ((ViewGroup)findViewById(R.id.root));
        root.addView(mSlate, 1); //1 because it's after R.id.photo
        mZoomView = new ZoomTouchView(this);
        mZoomView.setSlate(mSlate);
        mZoomView.setEnabled(false);
        if (hasAnimations()) {
            mZoomView.setAlpha(0);
        }
        root.addView(mZoomView, 0);

        mMediaScannerConnection =
                new MediaScannerConnection(MarkersActivity.this, mMediaScannerClient);


        if (icicle != null) {
            onRestoreInstanceState(icicle);
        }

        //mActionBarView = findViewById(R.id.actionbar);
        mToolsView = findViewById(R.id.tools);
        mColorsView = findViewById(R.id.colors);
        setColors();
        spotSizeTool = (SpotSizeBar)findViewById(R.id.pen_size);
        spotSizeTool.setOnSizeChangedListener(new SpotSizeBar.OnSizeChangedListener() {
            @Override
            public void sizeChanged(float size) {
                setPenSize(size);
                Log.d(TAG, "Size changed to: " + size);
                mPrefs.edit().putInt(PREF_LAST_HUDSTATE, (int)size).commit();
            }
        });

        mDebugButton = findViewById(R.id.debug);

        //TextView title = (TextView) mActionBarView.findViewById(R.id.logotype);
        //Typeface light = Typeface.create("sans-serif-light", Typeface.NORMAL);
        //title.setTypeface(light);

        final ToolButton.ToolCallback toolCB = new ToolButton.ToolCallback() {
            @Override
            public void setPenMode(ToolButton tool, float min, float max) {
                mSlate.setZoomMode(false);
                mZoomView.setEnabled(false);
                setPenSize((min + max) / 2.0f);
                mLastTool = mActiveTool;
                mActiveTool = tool;

                if (mLastTool != mActiveTool) {
                    mLastTool.deactivate();
                    mPrefs.edit().putString(PREF_LAST_TOOL, (String) mActiveTool.getTag())
                        .commit();
                }
            }
            @Override
            public void setPenColor(ToolButton tool, int color) {
                MarkersActivity.this.setPenColor(color);
                mLastColor = mActiveColor;
                mActiveColor = tool;
                if (mLastColor != mActiveColor) {
                    mLastColor.deactivate();
                    mPrefs.edit().putInt(PREF_LAST_COLOR, color).commit();
                }
                if (mActiveTool instanceof ToolButton.ZoomToolButton) {
                    // you probably want to use a pen now
                    restore(mActiveTool);
                }
            }
            @Override
            public void setPenType(ToolButton tool, int penType) {
                MarkersActivity.this.setPenType(penType);
                mLastPenType = mActivePenType;
                mActivePenType = tool;
                if (mLastPenType != mActivePenType) {
                    mLastPenType.deactivate();
                    mPrefs.edit().putString(PREF_LAST_TOOL_TYPE, (String) mActivePenType.getTag())
                        .commit();
                }
            }
            @Override
            public void restore(ToolButton tool) {
                if (tool == mActiveTool && tool != mLastTool) {
                    mLastTool.click();
                    mPrefs.edit().putString(PREF_LAST_TOOL, (String) mActiveTool.getTag())
                        .commit();
                } else if (tool == mActiveColor && tool != mLastColor) {
                    mLastColor.click();
                    mPrefs.edit().putInt(PREF_LAST_COLOR, ((SwatchButton) mLastColor).color)
                        .commit();
                }
            }
            @Override
            public void setBackgroundColor(ToolButton tool, int color) {
                mSlate.setDrawingBackground(color);
            }
            @Override
            public void setZoomMode(ToolButton me) {
                mSlate.setZoomMode(true);
                mZoomView.setEnabled(true);
                mLastTool = mActiveTool;
                mActiveTool = me;

                if (mLastTool != mActiveTool) {
                    mLastTool.deactivate();
                    mPrefs.edit().putString(PREF_LAST_TOOL, (String) mActiveTool.getTag())
                        .commit();
                }
            }

            @Override
            public void resetZoom(ToolButton tool) {
                mSlate.resetZoom();
            }
        };

        descend((ViewGroup) mColorsView, new ViewFunc() {
            @Override
            public void apply(View v) {
                final ToolButton.SwatchButton swatch = (ToolButton.SwatchButton) v;
                if (swatch != null) {
                    swatch.setCallback(toolCB);
                }
            }
        });

/*
        final ToolButton penThinButton = (ToolButton) findViewById(R.id.pen_thin);
        penThinButton.setCallback(toolCB);

        final ToolButton penMediumButton = (ToolButton) findViewById(R.id.pen_medium);
        if (penMediumButton != null) {
            penMediumButton.setCallback(toolCB);
        }

        final ToolButton penThickButton = (ToolButton) findViewById(R.id.pen_thick);
        penThickButton.setCallback(toolCB);

        final ToolButton fatMarkerButton = (ToolButton) findViewById(R.id.fat_marker);
        if (fatMarkerButton != null) {
            fatMarkerButton.setCallback(toolCB);
        }

*/
        final ToolButton typeWhiteboardButton = (ToolButton) findViewById(R.id.whiteboard_marker);
        typeWhiteboardButton.setCallback(toolCB);

        final ToolButton typeFeltTipButton = (ToolButton) findViewById(R.id.felttip_marker);
        if (typeFeltTipButton != null) {
            typeFeltTipButton.setCallback(toolCB);
        }

        final ToolButton typeAirbrushButton = (ToolButton) findViewById(R.id.airbrush_marker);
        if (typeAirbrushButton != null) {
            typeAirbrushButton.setCallback(toolCB);
        }

        final ToolButton typeFountainPenButton = (ToolButton) findViewById(R.id.fountainpen_marker);
        if (typeFountainPenButton != null) {
            typeFountainPenButton.setCallback(toolCB);
        }

        mLastPenType = mActivePenType = typeWhiteboardButton;

        loadSettings();

        //mActiveTool.click();
        mActivePenType.click();

        shakeSensor = new ShakeSensor(this);

        findViewById(R.id.text_tool).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSlate.setMoveMode(true);
                try {
                    View textInput = findViewById(R.id.text_input);
                    textInput.setVisibility(View.VISIBLE);
                    textInput.requestFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                    hideTools();
                    /*
                    findViewById(R.id.text_done).setVisibility(View.VISIBLE);
                    findViewById(R.id.text_cancel).setVisibility(View.VISIBLE);
                    */

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        EditText input = (EditText) findViewById(R.id.text_input);
        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                int result = actionId & EditorInfo.IME_MASK_ACTION;
                switch(result) {
                    case EditorInfo.IME_ACTION_DONE:
                        // done stuff
                        editTextDone();
                        break;
                    case EditorInfo.IME_ACTION_NEXT:
                        // next stuff
                        break;
                }
                return false;
            }
        });
        findViewById(R.id.text_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText input = (EditText) findViewById(R.id.text_input);
                input.setText("");
                input.setVisibility(View.GONE);
                input.clearFocus();
                showTools();
            }
        });
        findViewById(R.id.text_done).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editTextDone();
            }
        });

        findViewById(R.id.edit_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSlate.removeMoveable();
                showTools();
            }
        });
        findViewById(R.id.edit_done).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSlate.renderDrawing();
                showTools();
            }
        });

        findViewById(R.id.eraser_tool).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setPenType(Slate.TYPE_ERASER);
                ((RoundMenu)findViewById(R.id.tools_menu)).setImage(((RoundMenuItem)v).image);
            }
        });
        findViewById(R.id.paintbrush_tool).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setPenType(Slate.TYPE_WHITEBOARD);
                ((RoundMenu)findViewById(R.id.tools_menu)).setImage(((RoundMenuItem) v).image);
            }
        });
        findViewById(R.id.fountainpen_tool).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setPenType(Slate.TYPE_FOUNTAIN_PEN);
                ((RoundMenu)findViewById(R.id.tools_menu)).setImage(((RoundMenuItem) v).image);
            }
        });
        findViewById(R.id.spraypaint_tool).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setPenType(Slate.TYPE_AIRBRUSH);
                ((RoundMenu)findViewById(R.id.tools_menu)).setImage(((RoundMenuItem) v).image);
            }
        });
    }

    private void editTextDone() {
        EditText input = (EditText) findViewById(R.id.text_input);
        String text = input.getText().toString();
        int width = TextDrawing.measureWidth(text, 20.0f * getResources().getDisplayMetrics().density, this);
        input.setText("");
        input.clearFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(findViewById(R.id.text_input).getWindowToken(), 0);
        input.setVisibility(View.GONE);
        mSlate.addText((mSlate.getWidth()) / 2, (int) (260 * getResources().getDisplayMetrics().density), text);
        mSlate.setMoveMode(true);
        findViewById(R.id.edit_buttons).setVisibility(View.VISIBLE);
    }

    public Bitmap renderBitmap() {
        Bitmap base = mSlate.getBitmap();
        // TODO: render any text overlays or stickers at this point
        return base;
    }

    private void loadSettings() {
        mPrefs = getPreferences(MODE_PRIVATE);

        spotSizeTool.setSize(60);
        setPenSize(60);

        mLastTool = mActiveTool;
        if (mActiveTool != null) mActiveTool.click();

        final String typeTag = mPrefs.getString(PREF_LAST_TOOL_TYPE, "type_whiteboard");
        mLastPenType = mActivePenType = (ToolButton) mToolsView.findViewWithTag(typeTag);
        if (mActivePenType != null) mActivePenType.click();

        final int color = getResources().getColor(R.color.default_draw_color);
        descend((ViewGroup) mColorsView, new ViewFunc() {
            @Override
            public void apply(View v) {
                final ToolButton.SwatchButton swatch = (ToolButton.SwatchButton) v;
                if (swatch != null) {
                    if (color == swatch.color) {
                        mActiveColor = swatch;
                    }
                }
            }
        });
        mLastColor = mActiveColor;
        if (mActiveColor != null) mActiveColor.click();
    }

    private void setPenSize(float size) {
        float variation = mSlate.getPenType() == Slate.TYPE_FOUNTAIN_PEN ? 0 : 0.5f;
        mSlate.setPenSize(size * (1.0f - variation), size * (1.0f + variation));
    }

    @Override
    public void onPause() {
        super.onPause();
        shakeSensor.unregister();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(findViewById(R.id.text_input).getWindowToken(), 0);
        saveDrawing(WIP_FILENAME, true);
    }

    @Override
    public void onResume() {
        super.onResume();

        shakeSensor.register();
        shakeSensor.setShakeListener(this);
        String orientation = getString(R.string.orientation);

        setRequestedOrientation(
                "landscape".equals(orientation)
                        ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    public void onConfigurationChanged (Configuration newConfig) {
    	super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onAttachedToWindow() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private static int tools[] = {R.id.pen_size, R.id.tools_menu, R.id.top_buttons, R.id.colors_menu};
    public void hideTools() {
        // TODO: we could animate this
        for (int tool_id: tools) {
            findViewById(tool_id).setVisibility(View.GONE);
        }
    }
    public void showTools() {
        // TODO: we could animate this
        for (int tool_id: tools) {
            findViewById(tool_id).setVisibility(View.VISIBLE);
        }
        findViewById(R.id.text_done).setVisibility(View.GONE);
        findViewById(R.id.text_cancel).setVisibility(View.GONE);
        findViewById(R.id.edit_buttons).setVisibility(View.GONE);
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(findViewById(R.id.text_input).getWindowToken(), 0);
    }

    private String dumpBundle(Bundle b) {
        if (b == null) return "null";
        StringBuilder sb = new StringBuilder("Bundle{");
        boolean first = true;
        for (String key : b.keySet()) {
            if (!first) sb.append(" ");
            first = false;
            sb.append(key+"=(");
            sb.append(b.get(key));
        }
        sb.append("}");
        return sb.toString();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent startIntent = getIntent();
        String a = startIntent.getAction();
        if (DEBUG) Log.d(TAG, "starting with intent=" + startIntent + " action=" + a + " extras=" + dumpBundle(startIntent.getExtras()));
        if (a == null) return;
        if (a.equals(Intent.ACTION_EDIT)) {
            // XXX: what happens to the old drawing? we should really move to auto-save
            mSlate.clear();
            loadImageFromIntent(startIntent);
        } else if (a.equals(Intent.ACTION_SEND)) {
            // XXX: what happens to the old drawing? we should really move to auto-save
            mSlate.clear();
            loadImageFromContentUri((Uri)startIntent.getParcelableExtra(Intent.EXTRA_STREAM));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle icicle) {
    }

    @Override
    protected void onRestoreInstanceState(Bundle icicle) {
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            EditText input = (EditText)findViewById(R.id.text_input);
            if (input.getVisibility() == View.VISIBLE) {
                input.setText("");
                input.setVisibility(View.GONE);
                input.clearFocus();
                showTools();
                return true;
            }
            // TODO: exit the editor
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Do you want to ditch your creation?")
                    .setPositiveButton("Leave", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {


                            finish();
                        }
                            /*mSlate.clear();
                            if (getIntent().hasExtra("picture")) {
                                loadDrawing(getIntent().getStringExtra("picture"));
                            }*/

                    })
                    .setNegativeButton("Nevermind", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Nothing
                        }
                    });
            builder.create().show();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    final static boolean hasAnimations() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB);
    }

    final static boolean hasSystemUiFlags() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN);
    }

    final static boolean hasImmersive() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT);
    }

    public void clickLogo(View v) {
        //setHUDVisibility(!getHUDVisibility(), true);
    }

    public boolean getHUDVisibility() {
        return mActionBarView.getVisibility() == View.VISIBLE;
    }

    public void clickClear(View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Do you want to erase all your edits?")
                .setPositiveButton("Erase Them", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mSlate.clear();
                        if (getIntent().hasExtra("picture")) {
                            loadDrawing(getIntent().getStringExtra("picture"));
                        }
                    }
                })
                .setNegativeButton("Nevermind", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Nothing
                    }
                });
        builder.create().show();
    }

    public boolean loadDrawing(String filename) {
        return loadDrawing(filename, false, true);
    }

    @TargetApi(8)
    public File getPicturesDirectory() {
        final File d;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            d = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        } else {
            d = new File("/sdcard/Pictures");
        }
        return d;
    }

    public boolean loadDrawing(String filename, boolean temporary, boolean absolute) {
        File d = getPicturesDirectory();
        final String filePath;
        if (absolute) {
            filePath = filename;
        } else {
            d = new File(d, temporary ? IMAGE_TEMP_DIRNAME : IMAGE_SAVE_DIRNAME);
            filePath = new File(d, filename).toString();
        }
        if (DEBUG) Log.d(TAG, "loadDrawing: " + filePath);

        if (d.exists()) {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inDither = false;
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            opts.inScaled = false;
            Bitmap bits = BitmapFactory.decodeFile(filePath, opts);
            if (bits != null) {
                ((ImageView)findViewById(R.id.photo)).setImageBitmap(bits);
                //mSlate.setBitmap(bits); // messes with the bounds
                //mSlate.setBackground(new BitmapDrawable(bits));
                //mSlate.paintBitmap(bits);
                return true;
            }
        }
        return false;
    }

    public void saveDrawing(String filename) {
        saveDrawing(filename, false);
    }

    public void saveDrawing(String filename, boolean temporary) {
        saveDrawing(filename, temporary, /*animate=*/ false, /*share=*/ false, /*clear=*/ false);
    }

    public void saveDrawing(String filename, boolean temporary, boolean animate, boolean share, boolean clear) {
        final Bitmap localBits = mSlate.copyBitmap(/*withBackground=*/!temporary);
        if (localBits == null) {
            if (DEBUG) Log.e(TAG, "save: null bitmap");
            return;
        }

        final String _filename = filename;
        final boolean _temporary = temporary;
        final boolean _share = share;
        final boolean _clear = clear;

        new AsyncTask<Void,Void,String>() {
            @Override
            protected String doInBackground(Void... params) {
                String fn = null;
                try {
                    File d = getPicturesDirectory();
                    d = new File(d, _temporary ? IMAGE_TEMP_DIRNAME : IMAGE_SAVE_DIRNAME);
                    if (!d.exists()) {
                        if (d.mkdirs()) {
                            if (_temporary) {
                                final File noMediaFile = new File(d, MediaStore.MEDIA_IGNORE_FILENAME);
                                if (!noMediaFile.exists()) {
                                    new FileOutputStream(noMediaFile).write('\n');
                                }
                            }
                        } else {
                            throw new IOException("cannot create dirs: " + d);
                        }
                    }
                    File file = new File(d, _filename);
                    if (DEBUG) Log.d(TAG, "save: saving " + file);
                    OutputStream os = new FileOutputStream(file);
                    localBits.compress(Bitmap.CompressFormat.PNG, 0, os);
                    localBits.recycle();
                    os.close();

                    fn = file.toString();
                } catch (IOException e) {
                    Log.e(TAG, "save: error: " + e);
                }
                return fn;
            }

            @Override
            protected void onPostExecute(String fn) {
                if (fn != null) {
                    synchronized(mDrawingsToScan) {
                        mDrawingsToScan.add(fn);
                        if (_share) {
                            mPendingShareFile = fn;
                        }
                        if (!mMediaScannerConnection.isConnected()) {
                            mMediaScannerConnection.connect(); // will scan the files and share them
                        }
                    }
                }

                if (_clear) mSlate.clear();
            }
        }.execute();

    }

    public void clickSave(View v) {
        if (mSlate.isEmpty()) return;

        v.setEnabled(false);
        final String filename = System.currentTimeMillis() + ".png";
        saveDrawing(filename);
        Toast.makeText(this, "Drawing saved: " + filename, Toast.LENGTH_SHORT).show();
        v.setEnabled(true);
    }

    public void clickSaveAndClear(View v) {
        if (mSlate.isEmpty()) return;

        v.setEnabled(false);
        final String filename = System.currentTimeMillis() + ".png";
        saveDrawing(filename,
                /*temporary=*/ false, /*animate=*/ true, /*share=*/ false, /*clear=*/ true);
        Toast.makeText(this, "Drawing saved: " + filename, Toast.LENGTH_SHORT).show();
        v.setEnabled(true);
    }

    private void setThingyEnabled(Object v, boolean enabled) {
        if (v == null) return;
        if (v instanceof View) ((View)v).setEnabled(enabled);
        else if (v instanceof MenuItem) ((MenuItem)v).setEnabled(enabled);
    }

    public void clickShare(View v) {
        hideOverflow();
        setThingyEnabled(v, false);
        final String filename = System.currentTimeMillis() + ".png";
        // can't use a truly temporary file because:
        // - we want mediascanner to give us a content: URI for it; some apps don't like file: URIs
        // - if mediascanner scans it, it will show up in Gallery, so it might as well be a regular drawing
        saveDrawing(filename,
                /*temporary=*/ false, /*animate=*/ false, /*share=*/ true, /*clear=*/ false);
        setThingyEnabled(v, true);
    }

    public void clickLoad(View unused) {
        hideOverflow();
        Intent i = new Intent(Intent.ACTION_PICK,
                       android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(i, LOAD_IMAGE);
    }

    public void clickDebug(View unused) {
        hideOverflow();
        boolean debugMode = (mSlate.getDebugFlags() == 0); // toggle
        mSlate.setDebugFlags(debugMode
            ? Slate.FLAG_DEBUG_EVERYTHING
            : 0);
        mDebugButton.setSelected(debugMode);
        Toast.makeText(this, "Debug mode " + ((mSlate.getDebugFlags() == 0) ? "off" : "on"),
            Toast.LENGTH_SHORT).show();
    }

    public void clickUndo(View unused) {
        mSlate.undo();
    }

    private void showOverflow() {
        mMenuDialog.show();
    }
    private void hideOverflow() {
        mMenuDialog.dismiss();
    }
    public void clickOverflow(View v) {
        if (mMenuDialog == null) {
            LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
            View layout = inflater.inflate(R.layout.overflow_menu, null);

    //        TextView text = (TextView) layout.findViewById(R.id.text);
    //        text.setText("Hello, this is a custom dialog!");
    //        ImageView image = (ImageView) layout.findViewById(R.id.image);
    //        image.setImageResource(R.drawable.android);

            mMenuDialog = new Dialog(this);
            //mMenuDialog = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK).create();
            Window dialogWin  = mMenuDialog.getWindow();
            dialogWin.requestFeature(Window.FEATURE_NO_TITLE);
            dialogWin.setGravity(Gravity.TOP|Gravity.RIGHT);
            WindowManager.LayoutParams winParams = dialogWin.getAttributes();
            winParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
            winParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            winParams.y = getResources().getDimensionPixelOffset(R.dimen.action_bar_height);
            dialogWin.setAttributes(winParams);
            dialogWin.setWindowAnimations(android.R.style.Animation_Translucent);
            dialogWin.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            mMenuDialog.setCanceledOnTouchOutside(true);

            mMenuDialog.setContentView(layout);
            // bash the background
            final View decor = layout.getRootView();

            decor.setBackgroundDrawable(null);
            decor.setPadding(0,0,0,0);
        }

        showOverflow();
    }

    public void setPenColor(int color) {
        mSlate.setPenColor(color);
    }

    public void setPenType(int type) {
        mSlate.setPenType(type);
        //setPenSize(spotSizeTool.getSize()); //Wrong set Size method
        findViewById(R.id.enter_text).setSelected(false);
        mSlate.setMoveMode(false);
    }

    protected void loadImageFromIntent(Intent imageReturnedIntent) {
        Uri contentUri = imageReturnedIntent.getData();
        loadImageFromContentUri(contentUri);
    }

    protected void loadImageFromContentUri(Uri contentUri) {
        Toast.makeText(this, "Loading from " + contentUri, Toast.LENGTH_SHORT).show();

        loadDrawing(WIP_FILENAME, true, false);
        mJustLoadedImage = true;

        try {
            Bitmap b = MediaStore.Images.Media.getBitmap(getContentResolver(), contentUri);
            if (b != null) {
                mSlate.paintBitmap(b);
                if (DEBUG) Log.d(TAG, "successfully loaded bitmap: " + b);
            } else {
                Log.e(TAG, "couldn't get bitmap from " + contentUri);
            }
        } catch (java.io.FileNotFoundException ex) {
            Log.e(TAG, "error loading image from " + contentUri + ": " + ex);
        } catch (java.io.IOException ex) {
            Log.e(TAG, "error loading image from " + contentUri + ": " + ex);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch (requestCode) {
        case LOAD_IMAGE:
            if (resultCode == RESULT_OK) {
                loadImageFromIntent(imageReturnedIntent);
            }
        }
    }

}
