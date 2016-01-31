package com.jktuxiyang.simpleparkingsolution;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseImageView;
import com.parse.ParseObject;
import com.parse.SaveCallback;



public class NewReportFragment extends Fragment {

    private Button photoButton;
    private Button saveButton;
    private Button cancelButton;
    private ParseImageView reportPreview;

    public NewReportFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_new_report, parent, false);

        photoButton = (Button) v.findViewById(R.id.photo_button);
        photoButton.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                startCamera();
            }
        });

        saveButton = ((Button) v.findViewById(R.id.save_button));
        saveButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                ParseObject report = ((ReportViolation) getActivity()).getCurrentReport();
                report.saveInBackground(new SaveCallback() {

                    @Override
                    public void done(ParseException e) {
                        if (e == null) {
                            getActivity().setResult(Activity.RESULT_OK);
                            getActivity().finish();
                        } else {
                            Toast.makeText(
                                    getActivity().getApplicationContext(),
                                    "Error saving: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                });

            }
        });

        cancelButton = ((Button) v.findViewById(R.id.cancel_button));
        cancelButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                getActivity().setResult(Activity.RESULT_CANCELED);
                getActivity().finish();
            }
        });

        // Until the user has taken a photo, hide the preview
        reportPreview = (ParseImageView) v.findViewById(R.id.report_preview_image);
        reportPreview.setVisibility(View.INVISIBLE);

        return v;

    }

    public void startCamera() {
        Fragment cameraFragment = new CameraFragment();
        FragmentTransaction transaction = getActivity().getFragmentManager()
                .beginTransaction();
        transaction.replace(R.id.fragmentContainer, cameraFragment);
        transaction.addToBackStack("NewReportFragment");
        transaction.commit();
    }



    @Override
    public void onResume() {
        super.onResume();
        ParseFile photoFile = ((ReportViolation) getActivity())
                .getCurrentReport().getParseFile("photo");
        if (photoFile != null) {
            reportPreview.setParseFile(photoFile);
            reportPreview.loadInBackground(new GetDataCallback() {
                @Override
                public void done(byte[] data, ParseException e) {
                    reportPreview.setVisibility(View.VISIBLE);
                }
            });
        }
    }

}
