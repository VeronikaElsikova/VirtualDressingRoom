package cz.upol.inf.virtualdressingroom;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.core.Core;
import org.opencv.core.Point;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cz.upol.inf.dressingroom.DressingRoom;
import cz.upol.inf.dressingroom.FaceMask;
import cz.upol.inf.dressingroom.Glasses;
import cz.upol.inf.dressingroom.Outfit;
import cz.upol.inf.dressingroom.Top;

public class PhotoActivity extends AppCompatActivity {

    private static final String TAG = "Photo Activity";
    private int testImagesIterator = 0;
    private ImageView imageView;
    private TextView tvCurrentPhoto;
    private final List<Bitmap> testImages = new ArrayList<>();
    private final Outfit outfit = new Outfit();

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent switchActivity = new Intent(this, CameraActivity.class);
        setContentView(R.layout.activity_photo);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        tvCurrentPhoto = findViewById(R.id.current_photo);
        imageView = findViewById(R.id.imageViewPhoto);

        // initializing DressingRoom (classifiers are loaded)
        DressingRoom.loadClassifiers(this);

        // getting images of clothing
        Bitmap tShirtBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.t_shirt_men);
        Bitmap sunglassesBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.sunglasses);
        Bitmap maskBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.face_mask);
        Bitmap dressBitmap =  BitmapFactory.decodeResource(getResources(), R.drawable.summer_dress);

        // getting test images
        testImages.add(BitmapFactory.decodeResource(getResources(), R.drawable.man));
        testImages.add(BitmapFactory.decodeResource(getResources(), R.drawable.woman));
        testImages.add(BitmapFactory.decodeResource(getResources(), R.drawable.woman2));
        testImages.add(BitmapFactory.decodeResource(getResources(), R.drawable.woman3)); // waist not detected in woman3.jpg, estimate is used
        testImages.add(BitmapFactory.decodeResource(getResources(), R.drawable.woman4));
        testImages.add(BitmapFactory.decodeResource(getResources(), R.drawable.test_noface)); // no face in the picture, dialog is shown

        // setting default photo and updating the text view
        updateCurrentPhoto();

        // initializing outfit
        Top tShirt = new Top(tShirtBitmap, new Point(190, 80), new Point(600, 80));
        Glasses glasses = new Glasses(sunglassesBitmap, new Point(15, 60), new Point(562, 60));
        FaceMask mask = new FaceMask(maskBitmap, new Point(65, 358), new Point(430, 358));
        Top dress = new Top(dressBitmap, new Point(217, 90), new Point(556, 90));

        // button to switch to CameraActivity
        Button buttonSwitch = findViewById(R.id.buttonSwitch);
        buttonSwitch.setOnClickListener(view -> startActivity(switchActivity));

        // clothes buttons
        Button buttonGlasses = findViewById(R.id.buttonGlasses);
        buttonGlasses.setOnClickListener(view -> {
            if(outfit.getGlasses().isEmpty()) outfit.setGlasses(glasses);
            else outfit.setGlasses(null);
            addClothes();
        });

        Button buttonMask = findViewById(R.id.buttonMask);
        buttonMask.setOnClickListener(view -> {
            if(outfit.getFaceMasks().isEmpty()) outfit.setFaceMask(mask);
            else outfit.setFaceMask(null);
            addClothes();
        });

        Button buttonTShirt = findViewById(R.id.buttonTShirt);
        buttonTShirt.setOnClickListener(view -> {
            if(outfit.getTops().contains(tShirt)) outfit.getTops().remove(tShirt);
            else outfit.addTop(tShirt);
            addClothes();
        });

        Button buttonDress = findViewById(R.id.buttonDress);
        buttonDress.setOnClickListener(view -> {
            if(outfit.getTops().contains(dress)) outfit.getTops().remove(dress);
            else outfit.addTop(dress);
            addClothes();
        });

        // buttons for changing photos
        Button buttonPrevious = findViewById(R.id.previous_photo);
        buttonPrevious.setOnClickListener(view -> {
            testImagesIterator = (testImagesIterator == 0) ? testImages.size()-1 : testImagesIterator-1;
            updateCurrentPhoto();
        });

        Button buttonNext = findViewById(R.id.next_photo);
        buttonNext.setOnClickListener(view -> {
            testImagesIterator = (testImagesIterator == testImages.size() -1) ? 0 : testImagesIterator+1;
            updateCurrentPhoto();
        });
    }

    private void updateCurrentPhoto() {
        imageView.setImageBitmap(testImages.get(testImagesIterator));
        addClothes();
        String currentPhotoText = (testImagesIterator +1) + "/" + testImages.size();
        tvCurrentPhoto.setText(currentPhotoText);
    }

    private void addClothes() {
        Bitmap image=testImages.get(testImagesIterator);
        Bitmap result;
        try {
            result = DressingRoom.detectAndAddClothing(image, outfit);
        } catch (IOException e) {
            Log.d(TAG, "no face detected");
            result = image;

            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setMessage("No face was detected");

            dialogBuilder.setPositiveButton(
                    "OK",
                    (dialog, id) -> dialog.cancel());

            AlertDialog alertNoFace = dialogBuilder.create();
            alertNoFace.show();
        }
        imageView.setImageBitmap(result);
    }

}