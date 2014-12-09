package com.mylikes.likes.etchasketch;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.view.KeyEvent;
import android.app.Activity;
import android.util.Log;
/**
 * Created by nick on 12/6/14.
 */
public class PhotoTakenFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        RelativeLayout view = (RelativeLayout)inflater.inflate(R.layout.fragment_photo_taken, container, false);
        ((ImageView)view.findViewById(R.id.photo_preview)).setImageBitmap(((CameraActivity)getActivity()).getCurrentPhoto());

        view.findViewById(R.id.photo_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((CameraActivity)getActivity()).onCancelPreview();

            }
        });
        view.findViewById(R.id.photo_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((CameraActivity)getActivity()).onSavePhoto();
            }
        });
        view.findViewById(R.id.photo_edit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((CameraActivity)getActivity()).onEditPhoto();
            }
        });
        return view;
    }


}
