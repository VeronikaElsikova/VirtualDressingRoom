package cz.upol.inf.virtualdressingroom;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import org.opencv.core.Core;
import org.opencv.core.Point;

import cz.upol.inf.dressingroom.DressingRoom;
import cz.upol.inf.dressingroom.FaceMask;
import cz.upol.inf.dressingroom.Glasses;
import cz.upol.inf.dressingroom.Outfit;
import cz.upol.inf.dressingroom.Top;

public class CameraActivity extends FragmentActivity implements
        ButtonsFragment.ButtonsFragmentListener {

    private static final int REQUEST_CODE_CAMERA = 1;

    private Outfit outfit;
    private Top tShirt;
    private Glasses glasses;
    private FaceMask mask;
    private Top dress;

    private CameraFragment cameraFragment;
    private ButtonsFragment buttonsFragment;

    Intent switchActivity;

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        switchActivity = new Intent(this, PhotoActivity.class);
        setContentView(R.layout.activity_camera);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        cameraFragment = (CameraFragment) getSupportFragmentManager().findFragmentById(R.id.camera_fragment);
        buttonsFragment = (ButtonsFragment) getSupportFragmentManager().findFragmentById(R.id.buttons_fragment);

        // get images
        Bitmap tShirtBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.t_shirt_men);
        Bitmap sunglassesBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.sunglasses);
        Bitmap maskBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.face_mask);
        Bitmap dressBitmap =  BitmapFactory.decodeResource(getResources(), R.drawable.summer_dress);
        // initialize outfit
        tShirt = new Top(tShirtBitmap, new Point(190, 80), new Point(600, 80));
        glasses = new Glasses(sunglassesBitmap, new Point(15, 60), new Point(562, 60));
        mask = new FaceMask(maskBitmap, new Point(65, 358), new Point(430, 358));
        dress = new Top(dressBitmap, new Point(217, 90), new Point(556, 90));
        outfit = new Outfit(null, null, null);

        // initialize dressing room
        DressingRoom.loadClassifiers(this);

        // Permissions check
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // if necessary permissions aren't granted, asks user to grant it
            FragmentManager fm = getSupportFragmentManager();
            fm.beginTransaction()
                    .hide(buttonsFragment)
                    .commit();
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_CAMERA);
        } else {
            activateCamera();
        }
    }

    private void activateCamera() {
        /*
           Due to issues with OpenCv's implementation of Camera2, camera doesn't work as intended in PORTRAIT mode,
           this activity is set to LANDSCAPE mode. To compensate for this, the fragment with buttons and other visible
           elements is rotated and resized to fit the screen in the right orientation. Landscape mode is set only after
           receiving needed permissions to avoid rotation of the permission request.
        */
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction()
                .show(buttonsFragment)
                .commit();
        cameraFragment.activateCamera();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CODE_CAMERA:
                if (grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(
                            this,
                            "Camera permission is required",
                            Toast.LENGTH_SHORT
                    ).show();
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_CAMERA);
                } else {
                    activateCamera();
                }
                break;
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        // Permissions check
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            activateCamera();
        }
    }


    @Override
    public void onButtonGlassesClick() {
        if(outfit.getGlasses().isEmpty()) outfit.setGlasses(glasses);
        else outfit.setGlasses(null);
        cameraFragment.setOutfit(outfit);
    }

    @Override
    public void onButtonMaskClick() {
        if(outfit.getFaceMasks().isEmpty()) outfit.setFaceMask(mask);
        else outfit.setFaceMask(null);
        cameraFragment.setOutfit(outfit);
    }

    @Override
    public void onButtonTopClick() {
        if(outfit.getTops().contains(tShirt)) outfit.getTops().remove(tShirt);
        else outfit.addTop(tShirt);
        cameraFragment.setOutfit(outfit);
    }

    @Override
    public void onButtonDressClick() {
        if(outfit.getTops().contains(dress)) outfit.getTops().remove(dress);
        else outfit.addTop(dress);
        cameraFragment.setOutfit(outfit);
    }

    @Override
    public void onButtonSwitchClick() {
        startActivity(switchActivity);
    }


}