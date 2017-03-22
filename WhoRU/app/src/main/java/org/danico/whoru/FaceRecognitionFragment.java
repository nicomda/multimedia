package org.danico.whoru;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.cameraview.CameraView;
import com.google.gson.Gson;
import com.kairos.Kairos;
import com.kairos.KairosListener;

import org.danico.whoru.API.APIRecognizedTeacher;
import org.json.JSONException;

import java.io.UnsupportedEncodingException;

import static android.content.ContentValues.TAG;


public class FaceRecognitionFragment extends Fragment {

    private OnFragmentInteractionListener mListener;
    private ImageView button_camera_flip;
    private ImageView button_camera_flash;
    private ImageView button_recon;
    private ImageView button_small_example;
    private CameraView camera;
    private final int PERMISSION_REQUEST_CAMERA = 1;
    private Bitmap bitmap;
    private Kairos myKairos;
    private KairosListener kListener;
    private Gson gson;
    private APIRecognizedTeacher teacher;


    public FaceRecognitionFragment() {
        // Required empty public constructor
    }

    public static FaceRecognitionFragment newInstance() {
        FaceRecognitionFragment fragment = new FaceRecognitionFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView =inflater.inflate(R.layout.fragment_face_recognition, container, false);
        setUpButtonsListeners(rootView);
        camera=(CameraView)rootView.findViewById(R.id.camera);
        askForCameraPermission();
        gson = new Gson();
        camera.addCallback(mCallback);
        setUpKairos();
        return rootView;
    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        camera.stop();
        mListener = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        camera.start();
    }

    @Override
    public void onPause() {
        camera.stop();
        super.onPause();
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }

    public void setUpButtonsListeners(View view) {

        //FAB Shoot camera listener
        button_recon = (ImageView) view.findViewById(R.id.identify_capture);
        button_recon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Picture Taken", Toast.LENGTH_SHORT).show();
                camera.takePicture();
            }
        });
        //Provisional Profile Access Button
        button_small_example=(ImageView)view.findViewById(R.id.identify_match_face);
        button_small_example.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i=new Intent(getActivity(),MatchFaceActivity.class);
                startActivity(i);
            }
        });


        //Flip camera button listener
        button_camera_flip = (ImageView) view.findViewById(R.id.identify_ic_camera_flip);
        button_camera_flip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (camera.getFacing() == CameraView.FACING_FRONT) {
                    camera.setFacing(CameraView.FACING_BACK);
                } else camera.setFacing(CameraView.FACING_FRONT);
            }
        });
        //Flash camera button listener
        button_camera_flash = (ImageView) view.findViewById(R.id.identify_ic_camera_flash);
        button_camera_flash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (camera.getFlash() == CameraView.FLASH_ON) {
                    camera.setFlash(CameraView.FLASH_OFF);
                    button_camera_flash.setImageDrawable(getResources().getDrawable(R.drawable.flash_disabled));
                } else if (camera.getFlash() == CameraView.FLASH_OFF) {
                    camera.setFlash(CameraView.FLASH_AUTO);
                    button_camera_flash.setImageDrawable(getResources().getDrawable(R.drawable.flash_auto));
                } else {
                    camera.setFlash(CameraView.FLASH_ON);
                    button_camera_flash.setImageDrawable(getResources().getDrawable(R.drawable.flash_enabled));
                }
            }
        });
    }

    public void askForCameraPermission() {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
        } else camera.start();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CAMERA: { //Permissions.READ_CONTACTS is an int constant
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //Permission granted
                    camera.start();
                }
            }
        }
    }

    private CameraView.Callback mCallback
            = new CameraView.Callback() {

        @Override
        public void onCameraOpened(CameraView cameraView) {
            Log.d(TAG, "onCameraOpened");
        }

        @Override
        public void onCameraClosed(CameraView cameraView) {
            Log.d(TAG, "onCameraClosed");
        }

        @Override
        public void onPictureTaken(CameraView cameraView, final byte[] data) {
            Log.d(TAG, "onPictureTaken " + data.length);
            Toast.makeText(cameraView.getContext(), "Picture Taken", Toast.LENGTH_SHORT)
                    .show();
            bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            bitmap = getResizedBitmap(bitmap, 500);
            try {
                myKairos.recognize(bitmap, getString(R.string.kairos_gallery_id), null, null, null, "1", kListener);
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

        }

    };

    void setUpKairos() {
        myKairos = new Kairos();
        myKairos.setAuthentication(getContext(), getString(R.string.kairos_app_id), getString(R.string.kairos_api_key));
        kListener = new KairosListener() {
            @Override
            public void onSuccess(String s) {
                teacher = gson.fromJson(s, APIRecognizedTeacher.class);
                if (teacher.getImages() != null) {
                    Toast.makeText(getContext(), "Teacher: " + teacher.getImages().get(0).getTransaction().getSubjectId() +
                            "\nConfidence:" + teacher.getImages().get(0).getTransaction().getConfidence(), Toast.LENGTH_SHORT).show();
                }
                Log.d("KAIROS DEMO", s);

            }

            @Override
            public void onFail(String s) {
                Log.d("KAIROS DEMO", s);

            }
        };
    }

    public Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }
}
