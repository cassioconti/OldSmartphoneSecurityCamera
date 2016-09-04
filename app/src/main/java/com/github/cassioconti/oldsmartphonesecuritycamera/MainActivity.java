package com.github.cassioconti.oldsmartphonesecuritycamera;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private Camera camera;
    private SurfaceHolder surfaceHolder;
    private boolean motionDetectionActive = true;
    private boolean inPreview = false;
    private byte[] previousPreview = null;
    private int motionThreshold = 10000000;
    private Date lastDate = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.toogleMotionDetectionButton);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (motionDetectionActive) {
                    Snackbar.make(view, "Motion detection deactivated", Snackbar.LENGTH_SHORT).show();
                    motionDetectionActive = false;
                    fab.setImageResource(android.R.drawable.checkbox_off_background);
                } else {
                    Snackbar.make(view, "Motion detection activated", Snackbar.LENGTH_SHORT).show();
                    motionDetectionActive = true;
                    fab.setImageResource(android.R.drawable.checkbox_on_background);
                }
            }
        });

        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        this.surfaceHolder = surfaceView.getHolder();
        this.surfaceHolder.addCallback(surfaceHolderCallbackHandler);
        this.surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();

        this.camera.setPreviewCallback(null);
        if (inPreview) {
            camera.stopPreview();
            inPreview = false;
        }

        this.camera.release();
        this.camera = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.camera = Camera.open();
        // https://github.com/phishman3579/android-motion-detection/blob/master/src/com/jwetherell/motion_detection/MotionDetectionActivity.java
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    Camera.PreviewCallback previewCallbackHandler = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            if (data == null) {
                return;
            }

            Camera.Size size = camera.getParameters().getPreviewSize();
            if (size == null) {
                return;
            }

            if (previousPreview == null) {
                previousPreview = data;
                lastDate = new Date();
            } else {
                Date now = new Date();
                if (((now.getTime() - lastDate.getTime()) / 1000) > 1) {
                    // If 1 second has passed since last compare
                    // See how different they are
                    int sum = 0;
                    for (int i = 0; i < data.length; i++) {
                        sum += Math.abs(previousPreview[i] - data[i]);
                    }

                    if (sum > motionThreshold) {
                        // Motion detected
                        Camera.Parameters parameters = camera.getParameters();
                        int width = parameters.getPreviewSize().width;
                        int height = parameters.getPreviewSize().height;
                        YuvImage yuv = new YuvImage(data, parameters.getPreviewFormat(), width, height, null);
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        yuv.compressToJpeg(new Rect(0, 0, width, height), 50, out);
                        byte[] bytes = out.toByteArray();

                        Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
                        String fname = sdf.format(new Date()) + ".jpg";

                        MediaStore.Images.Media.insertImage(getContentResolver(), bmp, fname , fname);
                    }

                    previousPreview = data;
                    lastDate = new Date();
                }
            }
        }
    };

    private SurfaceHolder.Callback surfaceHolderCallbackHandler = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                camera.setPreviewDisplay(surfaceHolder);
                camera.setPreviewCallback(previewCallbackHandler);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Camera.Parameters parameters = camera.getParameters();
            Camera.Size size = getLargestSupportedSizeThatFits(width, height, parameters);
            if (size != null) {
                parameters.setPreviewSize(size.width, size.height);
                Log.d("MsgLog", "Using width=" + size.width + " height=" + size.height);
            }

            camera.setParameters(parameters);
            camera.startPreview();
            inPreview = true;
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            // do nothing
        }
    };

    private static Camera.Size getLargestSupportedSizeThatFits(int width, int height, Camera.Parameters parameters) {
        Camera.Size result = null;
        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width <= width && size.height <= height) {
                if (result == null) {
                    result = size;
                } else {
                    int resultArea = result.width * result.height;
                    int newArea = size.width * size.height;

                    if (newArea > resultArea) result = size;
                }
            }
        }

        return result;
    }
}
