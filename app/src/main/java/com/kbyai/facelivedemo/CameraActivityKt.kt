package com.kbyai.facelivedemo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Size
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.kbyai.facesdk.FaceSDK
import io.fotoapparat.Fotoapparat
import io.fotoapparat.parameter.Resolution
import io.fotoapparat.preview.Frame
import io.fotoapparat.preview.FrameProcessor
import io.fotoapparat.selector.front
import io.fotoapparat.view.CameraView

class CameraActivityKt : AppCompatActivity() {

    val TAG = CameraActivity::class.java.simpleName
    val PREVIEW_WIDTH = 720
    val PREVIEW_HEIGHT = 1280

    private lateinit var cameraView: CameraView
    private lateinit var faceView: FaceView
    private lateinit var initView: TextView
    private lateinit var fotoapparat: Fotoapparat
    private lateinit var context: Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_kt)

        context = this
        cameraView = findViewById(R.id.preview)
        faceView = findViewById(R.id.faceView)
        initView = findViewById(R.id.initView)

        var ret = FaceSDK.setActivation(
            "vFctIxR3uOl4qcSokcOtKxwqg9UJL+BvXWZGSzdapOiPfR4LKnmJTMu9QlBuREvP3btz3lFRRG64\n" +
                    "wq6kZHTeJagmDUrNwoStgQddnJ8ps9uksGTv/xQskZtzIAYGX7CpIy9OuD/34zFljbh8eIqpkS4p\n" +
                    "g/6dr3dFoYl7qA6KfdzEFVp9iVKSOJh8F0eXk/0HWBaijMKLzaPbYRT6J969zqN0F4uj1/3tlRMu\n" +
                    "iwOC5h/xYjZQ7EruKSg/QiI6WXVJ2QeuvotNulUSX+NFK7Avyco6qk3nHpxU4G5Vu1katfjwYrwS\n" +
                    "wbus4Nx2wn6ySkgc//HhIi7zZ2gVUdMwPSV9LQ=="
        )

        if (ret == FaceSDK.SDK_SUCCESS) {
            ret = FaceSDK.init(assets)
        }

        if (ret != FaceSDK.SDK_SUCCESS) {
            initView.setVisibility(View.VISIBLE)
            if (ret == FaceSDK.SDK_LICENSE_KEY_ERROR) {
                initView.setText("Invalid license!")
            } else if (ret == FaceSDK.SDK_LICENSE_APPID_ERROR) {
                initView.setText("Invalid error!")
            } else if (ret == FaceSDK.SDK_LICENSE_EXPIRED) {
                initView.setText("License expired!")
            } else if (ret == FaceSDK.SDK_NO_ACTIVATED) {
                initView.setText("No activated!")
            } else if (ret == FaceSDK.SDK_INIT_ERROR) {
                initView.setText("Init error!")
            }
        }

        fotoapparat = Fotoapparat.with(this)
            .into(cameraView)
            .lensPosition(front())
            .frameProcessor(FaceFrameProcessor())
            .previewResolution { Resolution(PREVIEW_HEIGHT,PREVIEW_WIDTH) }
            .build()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_DENIED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1)
        } else {
            fotoapparat.start()
        }
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fotoapparat.start()
        }
    }

    override fun onPause() {
        super.onPause()
        fotoapparat.stop()
        faceView.setFaceBoxes(null)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                fotoapparat.start()
            }
        }
    }

    inner class FaceFrameProcessor : FrameProcessor {

        override fun process(frame: Frame) {

            val bitmap = FaceSDK.yuv2Bitmap(frame.image, frame.size.width, frame.size.height, 7)
            val faceBoxes = FaceSDK.faceDetection(bitmap)

            runOnUiThread {
                faceView.setFrameSize(Size(bitmap.width, bitmap.height))
                faceView.setFaceBoxes(faceBoxes)
            }

        }
    }
}