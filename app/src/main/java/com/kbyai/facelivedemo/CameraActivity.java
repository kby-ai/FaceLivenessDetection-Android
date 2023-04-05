package com.kbyai.facelivedemo;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.kbyai.facesdk.FaceBox;
import com.kbyai.facesdk.FaceSDK;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST;

public class CameraActivity extends AppCompatActivity {

    static String TAG = CameraActivity.class.getSimpleName();
    static int PREVIEW_WIDTH = 720;
    static int PREVIEW_HEIGHT = 1280;
    static float LIVENESS_THRESHOLD = 0.7f;

    private ExecutorService cameraExecutorService;
    private PreviewView viewFinder;
    private int lensFacing = CameraSelector.LENS_FACING_FRONT;

    private Preview preview        = null;
    private ImageAnalysis imageAnalyzer  = null;
    private Camera camera         = null;
    private CameraSelector        cameraSelector = null;
    private ProcessCameraProvider cameraProvider = null;

    private FaceView faceView;

    private TextView initView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        viewFinder = findViewById(R.id.preview);
        faceView = findViewById(R.id.faceView);
        initView = findViewById(R.id.initView);
        cameraExecutorService = Executors.newFixedThreadPool(1);

        int ret = FaceSDK.setActivation("UiVc2FOxdNGVgdX6PsntpvKZ4QXccD+wm/IndqeKOuDUoBDLYUF2LAgMim8zcRESesQWMfnzk3ad\n" +
                "IAhn/FYurPKk9zjMULHda+W2kLNxMB197FkWTeAbq4hHrN82s/jPWfXv+ira7byPY6tJUJikwnr+\n" +
                "OjCk+gAi2XFNaFze/yiVtbAEwRkD3cAYCW6CQdL0ZqXEq10XGr+2SUtY55FQGHdVGcgRNKr837+T\n" +
                "nptdLQ1tgbu2Y8zUKA23gG/uwEMUi/TKYu2eAG7ITCjKi5qQjrPvmw8cmriiQhQkDuAEStwGf0Sj\n" +
                "91NEjRbz7Tipmh1RZbG4YLxl83QfINhnmy6ioA==");

        if(ret == FaceSDK.SDK_SUCCESS) {
            ret = FaceSDK.init(getAssets());
        }

        if(ret != FaceSDK.SDK_SUCCESS) {
            initView.setVisibility(View.VISIBLE);
            if(ret == FaceSDK.SDK_LICENSE_KEY_ERROR) {
                initView.setText("Invalid license!");
            } else if(ret == FaceSDK.SDK_LICENSE_APPID_ERROR) {
                initView.setText("Invalid error!");
            } else if(ret == FaceSDK.SDK_LICENSE_EXPIRED) {
                initView.setText("License expired!");
            } else if(ret == FaceSDK.SDK_NO_ACTIVATED) {
                initView.setText("No activated!");
            } else if(ret == FaceSDK.SDK_INIT_ERROR) {
                initView.setText("Init error!");
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        } else {
            viewFinder.post(() ->
            {
                setUpCamera();
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == 1) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {

                viewFinder.post(() ->
                {
                    setUpCamera();
                });
            }
        }
    }

    private void setUpCamera()
    {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(CameraActivity.this);
        cameraProviderFuture.addListener(() -> {

            // CameraProvider
            try {
                cameraProvider = cameraProviderFuture.get();
            } catch (ExecutionException e) {
            } catch (InterruptedException e) {
            }

            // Build and bind the camera use cases
            bindCameraUseCases();

        }, ContextCompat.getMainExecutor(CameraActivity.this));
    }

    @SuppressLint({"RestrictedApi", "UnsafeExperimentalUsageError"})
    private void bindCameraUseCases()
    {
        int rotation = viewFinder.getDisplay().getRotation();

        cameraSelector = new CameraSelector.Builder().requireLensFacing(lensFacing).build();

        preview = new Preview.Builder()
                .setTargetResolution(new Size(PREVIEW_WIDTH, PREVIEW_HEIGHT))
                .setTargetRotation(rotation)
                .build();

        imageAnalyzer = new ImageAnalysis.Builder()
                .setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST)
                .setTargetResolution(new Size(PREVIEW_WIDTH, PREVIEW_HEIGHT))
                // Set initial target rotation, we will have to call this again if rotation changes
                // during the lifecycle of this use case
                .setTargetRotation(rotation)
                .build();

        imageAnalyzer.setAnalyzer(cameraExecutorService, new FaceAnalyzer());

        cameraProvider.unbindAll();

        try {
            camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer);

            preview.setSurfaceProvider(viewFinder.getSurfaceProvider());
        } catch (Exception exc) {
        }
    }

    class FaceAnalyzer implements ImageAnalysis.Analyzer
    {
        @SuppressLint("UnsafeExperimentalUsageError")
        @Override
        public void analyze(@NonNull ImageProxy imageProxy)
        {
            analyzeImage(imageProxy);
        }
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    private void analyzeImage(ImageProxy imageProxy)
    {
        try
        {
            Image image = imageProxy.getImage();

            Image.Plane[] planes = image.getPlanes();
            ByteBuffer yBuffer = planes[0].getBuffer();
            ByteBuffer uBuffer = planes[1].getBuffer();
            ByteBuffer vBuffer = planes[2].getBuffer();

            int ySize = yBuffer.remaining();
            int uSize = uBuffer.remaining();
            int vSize = vBuffer.remaining();

            byte[] nv21 = new byte[ySize + uSize + vSize];
            yBuffer.get(nv21, 0, ySize);
            vBuffer.get(nv21, ySize, vSize);
            uBuffer.get(nv21, ySize + vSize, uSize);

            Bitmap bitmap  = FaceSDK.yuv2Bitmap(nv21, image.getWidth(), image.getHeight(), 7);
            List<FaceBox> faceBoxes = FaceSDK.faceDetection(bitmap);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    faceView.setFrameSize(new Size(bitmap.getWidth(), bitmap.getHeight()));
                    faceView.setFaceBoxes(faceBoxes);
                }
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            imageProxy.close();
        }
    }
}