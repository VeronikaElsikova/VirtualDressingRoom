package cz.upol.inf.virtualdressingroom;

import android.app.Activity;
import android.content.Context;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

public class ButtonsFragment extends Fragment {
    private static final String TAG = "CameraButtonsFragment";
    ButtonsFragmentListener activityCallback;

    private Button buttonGlasses;
    private Button buttonMask;
    private Button buttonTShirt;
    private Button buttonDress;

    private boolean buttonGlassesOn = false;
    private boolean buttonMaskOn = false;
    private boolean buttonTShirtOn = false;
    private boolean buttonDressOn = false;

    public interface ButtonsFragmentListener {
        void onButtonGlassesClick();
        void onButtonMaskClick();
        void onButtonTopClick();
        void onButtonDressClick();
        void onButtonSwitchClick();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);
        _onAttach(activity);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        _onAttach(context);
    }

    private void _onAttach(Context context) {
        try {
            activityCallback = (ButtonsFragmentListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement ButtonsFragmentListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        RelativeLayout view = (RelativeLayout) inflater.inflate(R.layout.buttons_fragment, container, false);

        /*
           Due to issues with OpenCv's implementation of Camera2, camera doesn't work as intended in PORTRAIT mode,
           the activity is set to LANDSCAPE mode. To compensate for this, this fragment is rotated and resized
           to fit the screen in the right orientation.
         */
        DisplayMetrics displayMetrics = new DisplayMetrics();
        try {
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int h = displayMetrics.heightPixels;
            int w = displayMetrics.widthPixels;

            view.setRotation(270);
            view.setTranslationX((float)((w - h) / 2));
            view.setTranslationY((float)(h - w) / 2);
            ViewGroup.LayoutParams lp = view.getLayoutParams();
            lp.height = w;
            lp.width = h;
        } catch (IllegalStateException | NullPointerException e) {
            Log.e(TAG, "Buttons fragment couldn't be rotated without context" + System.lineSeparator() + e);
        }

        // Buttons listeners
        buttonGlasses = view.findViewById(R.id.buttonGlasses);
        buttonGlasses.setOnClickListener(this::buttonGlassesClicked);

        buttonMask = view.findViewById(R.id.buttonMask);
        buttonMask.setOnClickListener(this::buttonMaskClicked);

        buttonTShirt = view.findViewById(R.id.buttonTShirt);
        buttonTShirt.setOnClickListener(this::buttonTopClicked);

        buttonDress = view.findViewById(R.id.buttonDress);
        buttonDress.setOnClickListener(this::buttonDressClicked);

        Button buttonSwitch = view.findViewById(R.id.buttonSwitch);
        buttonSwitch.setOnClickListener(this::buttonSwitchClicked);

        return view;
    }



    public void buttonGlassesClicked(View view) {
        activityCallback.onButtonGlassesClick();
    }
    public void buttonGlassesClicked() {
        if(buttonGlassesOn) colorButtonOff(buttonGlasses);
        else colorButtonOn(buttonGlasses);

        buttonGlassesOn = !buttonGlassesOn;
    }

    public void buttonMaskClicked(View view) {
        activityCallback.onButtonMaskClick();
    }
    public void buttonMaskClicked() {
        if(buttonMaskOn) colorButtonOff(buttonMask);
        else colorButtonOn(buttonMask);

        buttonMaskOn = !buttonMaskOn;
    }

    public void buttonTopClicked(View view) {
        activityCallback.onButtonTopClick();
    }
    public void buttonTopClicked() {
        if(buttonTShirtOn) colorButtonOff(buttonTShirt);
        else colorButtonOn(buttonTShirt);

        buttonTShirtOn = !buttonTShirtOn;
    }

    public void buttonDressClicked(View view) {
        activityCallback.onButtonDressClick();
    }
    public void buttonDressClicked() {
        if(buttonDressOn) colorButtonOff(buttonDress);
        else colorButtonOn(buttonDress);

        buttonDressOn = !buttonDressOn;
    }




    public void buttonSwitchClicked(View view) {
        activityCallback.onButtonSwitchClick();
    }


    private void colorButtonOn(Button button) {
        button.getBackground().setColorFilter(ContextCompat.getColor(getContext(), R.color.purple_500), PorterDuff.Mode.SRC_ATOP);
        button.setTextColor(ContextCompat.getColor(getContext(),  R.color.white));
    }

    private void colorButtonOff(Button button) {
        button.getBackground().setColorFilter(ContextCompat.getColor(getContext(), R.color.light_grey), PorterDuff.Mode.SRC_ATOP);
        button.setTextColor(ContextCompat.getColor(getContext(),  R.color.black));

    }
}
