package com.github.cassioconti.oldsmartphonesecuritycamera;

import android.content.Context;
import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

/**
 * Created by cassi on 8/24/2016.
 */
public class CameraUtils {
    public static void getCamera(Context context){
        Camera camera = Camera.open();
        SurfaceView surfaceView = new SurfaceView(context);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        try {
            camera.setPreviewDisplay(surfaceHolder);
        } catch (IOException e) {
            e.printStackTrace();
        }

        camera.startPreview();

        // https://github.com/phishman3579/android-motion-detection/blob/master/src/com/jwetherell/motion_detection/MotionDetectionActivity.java
        
    }
}
