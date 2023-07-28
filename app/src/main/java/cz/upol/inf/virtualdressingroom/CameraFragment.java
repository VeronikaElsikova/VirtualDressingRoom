package cz.upol.inf.virtualdressingroom;

import android.os.Bundle;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCamera2View;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Rect;

import java.util.concurrent.atomic.AtomicBoolean;

import cz.upol.inf.dressingroom.DressingRoom;
import cz.upol.inf.dressingroom.Outfit;

public class CameraFragment extends Fragment implements CameraBridgeViewBase.CvCameraViewListener2 {
    private volatile Outfit outfit = new Outfit();

    private JavaCamera2View javaCameraView;
    private View view;

    // background thread
    private static FaceDetection faceDetectionRunnable;
    private static Handler handler;

    // shared variables
    private static final AtomicBoolean isThreadReadyForImage;
    private static final Object rSync = new Object();
    private static volatile Mat inputImageShared;
    private static volatile Rect faceROIShared = new Rect();

    static {
        isThreadReadyForImage = new AtomicBoolean(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        view = inflater.inflate(R.layout.camera_fragment, container, false);
        javaCameraView = view.findViewById(R.id.javaCameraView);

        inputImageShared = new Mat();
        faceROIShared = new Rect();

        // background thread
        HandlerThread handlerThread = new HandlerThread("HandlerThread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        faceDetectionRunnable = new FaceDetection();

        return view;
    }

    protected synchronized Outfit getOutfit() {
        return new Outfit(outfit);
    }
    protected synchronized void setOutfit(Outfit outfit) {
        this.outfit = new Outfit(outfit);
    }


    protected void activateCamera() {
        if (view==null) return;
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCvCameraViewListener(this);
        javaCameraView.setCameraPermissionGranted();
        javaCameraView.enableView();
    }



    @Override
    public void onPause() {
        super.onPause();
        if(javaCameraView!=null) javaCameraView.disableView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(javaCameraView!=null) javaCameraView.disableView();
    }




    @Override
    public void onCameraViewStarted(int width, int height) {
        mRGBA = new Mat();
        mResult = new Mat();
    }

    @Override
    public void onCameraViewStopped() {
        if(mRGBA != null) mRGBA.release();
        if(mResult != null) mResult.release();
    }

    private Mat mRGBA, mResult;
    private Rect rFace = new Rect();
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        //getting mat from InputFrame
        mRGBA = inputFrame.rgba();

        // rotating camera input (compensating for landscape mode)
        Core.transpose(mRGBA, mResult);
        Core.flip(mResult, mResult, 1);

        // giving input to background thread
        if (isThreadReadyForImage.compareAndSet(true, false)) {
            mResult.copyTo(inputImageShared);
            handler.post(faceDetectionRunnable);
        }

        // getting face detection output
        synchronized (rSync) {
            rFace = faceROIShared;
        }

        // if face is detected add clothes
        if(!rFace.empty()) {
            DressingRoom.addClothesRT(mResult, getOutfit(), rFace);
            // rotating result back, so it can be displayed properly
            Core.transpose(mResult, mRGBA);
            Core.flip(mRGBA, mRGBA, 0);
        }

        return mRGBA;
    }





    public static class FaceDetection implements Runnable {
        Rect rThreadFace;
        @Override
        public void run() {
            if(!inputImageShared.empty()) {
                // face detection
                rThreadFace = DressingRoom.getFace(inputImageShared);

                // giving UI thread face roi
                synchronized (rSync) {
                    faceROIShared = rThreadFace;
                }

                // is ready to accept new image
                isThreadReadyForImage.set(true);
            }
        }


    }

}