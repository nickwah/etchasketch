package com.mylikes.likes.etchasketch;

/*
 * Copyright (c) 2014 Rex St. John on behalf of AirPair.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

import android.app.ActionBar;
import android.app.FragmentManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuItem;

import com.mylikes.likes.etchasketch.fragments.BaseFragment;
import com.mylikes.likes.etchasketch.fragments.HorizontalPhotoGalleryFragment;
import com.mylikes.likes.etchasketch.fragments.NativeCameraFragment;
import com.mylikes.likes.etchasketch.fragments.SimpleAndroidImagePickerFragment;
import com.mylikes.likes.etchasketch.fragments.SimpleCameraIntentFragment;
import com.mylikes.likes.etchasketch.fragments.SimplePhotoGalleryListFragment;

import java.io.File;


/**
 * Created by Rex St. John (on behalf of AirPair.com) on 3/4/14.
 */
public class MainActivity extends CameraActivity
        implements BaseFragment.OnFragmentInteractionListener {

    /**
     * Actions
     */
    public static final int SELECT_PHOTO_ACTION = 0;

    /**
     * Fragment Identifiers
     */
    public static final int SIMPLE_CAMERA_INTENT_FRAGMENT = 0;
    public static final int SIMPLE_PHOTO_GALLERY_FRAGMENT = 1;
    public static final int SIMPLE_PHOTO_PICKER_FRAGMENT = 2;
    public static final int NATIVE_CAMERA_FRAGMENT = 3;
    public static final int HORIZONTAL_GALLERY_FRAGMENT = 4;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTitle = getTitle();
        openCamera();
    }

    public void openCamera() {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getFragmentManager();
        BaseFragment targetFragment = null;
        targetFragment = NativeCameraFragment.newInstance(1);
        // Select the fragment.
        fragmentManager.beginTransaction()
                .replace(R.id.container, targetFragment)
                .commit();
    }

    public void editPicture(File savedFile, int width, int height) {
        Intent intent = new Intent(this, MarkersActivity.class);
        intent.putExtra("picture", savedFile.getAbsolutePath());
        intent.putExtra("width", width);
        intent.putExtra("height", height);
        startActivity(intent);
    }

    public void restoreActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    /**
     * Handle Incoming messages from contained fragments.
     */

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    @Override
    public void onFragmentInteraction(String id) {

    }

    @Override
    public void onFragmentInteraction(int actionId) {

    }
}