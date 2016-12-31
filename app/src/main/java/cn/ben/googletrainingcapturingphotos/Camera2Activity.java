package cn.ben.googletrainingcapturingphotos;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import java.util.Arrays;

import static cn.ben.googletrainingcapturingphotos.Camera2Activity.CaptureState.STATE_PREVIEW;
import static cn.ben.googletrainingcapturingphotos.Camera2Activity.CaptureState.STATE_WAITING_CAPTURE;

public class Camera2Activity extends AppCompatActivity {

    private static final int PERMISSION_CAMERA_REQUEST_CODE = 1;
    private CameraManager mCameraManager;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private Handler mHandler;
    private String mCameraId;
    private ImageReader mImageReader;
    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder mPreviewBuilder;
    private CameraCaptureSession mSession;
    private CaptureState mState;
    private final CameraDevice.StateCallback DeviceStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.d("benyang", "DeviceStateCallback:camera was opend.");
//            mCameraOpenCloseLock.release(); // TODO: 2016/12/28
            mCameraDevice = camera;
            try {
                createCameraCaptureSession();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {

        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {

        }
    };
    private final CameraCaptureSession.StateCallback mSessionPreviewStateCallback = new
            CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    Log.d("benyang", "mSessionPreviewStateCallback onConfigured");
                    mSession = session;
                    try {
                        // TODO: 2016/12/27
                        mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                        mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                                CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                        session.setRepeatingRequest(mPreviewBuilder.build(), mSessionCaptureCallback, mHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                        Log.e("benyang", "set preview builder failed." + e.getMessage());
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                }
            };
    private final CameraCaptureSession.CaptureCallback mSessionCaptureCallback =
            new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    Log.d("benyang", "mSessionCaptureCallback, onCaptureCompleted");
                    mSession = session;
                    checkState(result);
                }

                @Override
                public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
                                                @NonNull CaptureResult partialResult) {
                    Log.d("benyang", "mSessionCaptureCallback,  onCaptureProgressed");
                    mSession = session;
                    checkState(partialResult);
                }

                private void checkState(CaptureResult result) {
                    switch (mState) {
                        case STATE_PREVIEW:
                            // NOTHING
                            break;
                        case STATE_WAITING_CAPTURE:
                            Object afStateObj = result.get(CaptureResult.CONTROL_AF_STATE);
                            if (afStateObj == null) return;

                            int afState = (int) afStateObj;

                            if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                                    CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState
                                    || CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED == afState
                                    || CaptureResult.CONTROL_AF_STATE_PASSIVE_UNFOCUSED == afState) {
                                //do something like save picture
                                // TODO: 2016/12/29  \
                                Log.d("benyang", "save picture");
                            }
                            break;
                    }
                }

            };
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader imageReader) {
            Log.d("benyang", "onImageAvailable");
        }
    };

    public enum CaptureState {
        STATE_PREVIEW,
        STATE_WAITING_CAPTURE
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera2);

        mCameraManager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceview);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Log.d("benyang", "surfaceCreated");
                initCameraAndPreview();
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
                Log.d("benyang", "surfaceChanged");
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                Log.d("benyang", "surfaceDestroyed");
            }
        });
    }

    private void initCameraAndPreview() {
        Log.d("benyang", "init camera and preview");
        HandlerThread handlerThread = new HandlerThread("Camera2");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());
        try {
            mCameraId = "" + CameraCharacteristics.LENS_FACING_FRONT;
            mImageReader = ImageReader.newInstance(mSurfaceView.getWidth(), mSurfaceView.getHeight(),
                    ImageFormat.JPEG,/*maxImages*/7);
            mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mHandler);

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_CAMERA_REQUEST_CODE);
                return;
            }
            mCameraManager.openCamera(mCameraId, DeviceStateCallback, mHandler);
        } catch (CameraAccessException e) {
            Log.e("benyang", "open camera failed." + e.getMessage());
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_CAMERA_REQUEST_CODE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "request permission failed", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        mCameraManager.openCamera(mCameraId, DeviceStateCallback, mHandler);
                    } catch (CameraAccessException e) {
                        Log.e("benyang", "open camera failed." + e.getMessage());
                    }
                } else {
                    Toast.makeText(this, "request permission failed", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void createCameraCaptureSession() throws CameraAccessException {
        Log.d("benyang", "createCameraCaptureSession");

        mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        mPreviewBuilder.addTarget(mSurfaceHolder.getSurface());
        mState = STATE_PREVIEW;
        mCameraDevice.createCaptureSession(
                Arrays.asList(mSurfaceHolder.getSurface(), mImageReader.getSurface()),
                mSessionPreviewStateCallback, mHandler);
    }

    public void Capture(@SuppressWarnings("UnusedParameters") View view) {
        try {
            Log.i("benyang", "take picture");
            mState = STATE_WAITING_CAPTURE;
            mSession.setRepeatingRequest(mPreviewBuilder.build(), mSessionCaptureCallback, mHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
}
