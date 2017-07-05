package com.example.shintarooo0079.pic;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Bundle;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.shintarooo0079.pic.camera.BackgroundThreadHelper;
import com.example.shintarooo0079.pic.camera.BasicCamera;
import com.example.shintarooo0079.pic.camera.CameraInterface;
import com.example.shintarooo0079.pic.camera.ImageStore;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity implements CameraInterface {
    private static final String TAG = "Pic";
    private int REQUEST_CODE_CAMERA_PERMISSION = 0x01;

    private Size mPreviewSize;
    private TextureView mTextureView;

    private ImageReader mImageReader;
    private BackgroundThreadHelper mThread;
    private BasicCamera mCamera;

    private SeekBar mSensorSensitivitySeekBar;
    private SeekBar mSensorExposeTimeSeekBar;
    private TextView mSensorSensitivityTextView;    // 1/1500s ~ 1/200s
    private TextView mSensorExposeTimeTextView;

    private SeekBar mPerSecondSeekBar;
    private TextView mPerSecondTextView;

    private Range<Long> exposeTimeRange;
    private Range<Integer> sensitivityRange;
    private Integer mSensorSensitivitySeekBarMinValue = 0;
    private Integer mSensorExposeTimeSeekBarMinValue = 0;
    private Integer currentSensitivity = 400;
    private Integer currentExposeTime = 400;

    //    private Thread repeatThread;
    private Integer waitTime = 100;
    private Integer count = 0;

    private TextView mCountTextView;

    private Timer mTimer;
    private TakePicTimerTask mTimerTask = null;
    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextureView = (TextureView) findViewById(R.id.previewTexture);
        mThread = new BackgroundThreadHelper();

        mSensorSensitivitySeekBar = (SeekBar)findViewById(R.id.sensorSensitivitySeekBar);
        mSensorExposeTimeSeekBar = (SeekBar)findViewById(R.id.sensorExposeTimeSeekBar);
        mSensorSensitivityTextView = (TextView)findViewById(R.id.sensorSensitivityTextView);
        mSensorExposeTimeTextView = (TextView)findViewById(R.id.sensorExposeTimeTextView);

        mPerSecondSeekBar = (SeekBar)findViewById(R.id.perSecondSeekBar);
        mPerSecondTextView = (TextView)findViewById(R.id.perSecondTextView);

        mCountTextView = (TextView) findViewById(R.id.countTextView);

        mSensorSensitivitySeekBar.setProgress(currentSensitivity);
        mSensorExposeTimeSeekBar.setProgress(currentExposeTime);

        mCamera = new BasicCamera();
        mCamera.setInterface(this);

        findViewById(R.id.shutterButton).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                count = 0;
                mCountTextView.setText("0");
                mTimer = new Timer();
                mTimerTask = new TakePicTimerTask();
                mTimer.schedule(mTimerTask, 0, waitTime*1);
                return true;
            }
        });

        findViewById(R.id.shutterButton).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int action = motionEvent.getAction();
                switch (action) {
                    case MotionEvent.ACTION_UP:
                        mTimer.cancel();
                        mTimer = null;
                        break;
                }
                return false;
            }
        });


        mSensorSensitivitySeekBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                        mSensorSensitivityTextView.setText("ISO: "+
                                seekBar.getProgress()+mSensorSensitivitySeekBarMinValue);
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        currentSensitivity = seekBar.getProgress();
                        mCamera.configCamera(seekBar.getProgress(), currentExposeTime);
                    }
                }
        );

        mSensorExposeTimeSeekBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                        mSensorExposeTimeTextView.setText("ShutterSpeed: "+
                                mSensorExposeTimeSeekBar.getProgress()+mSensorExposeTimeSeekBarMinValue);
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        currentExposeTime = seekBar.getProgress();
                        mCamera.configCamera(currentSensitivity, seekBar.getProgress());
                    }
                }
        );

        mPerSecondSeekBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                        mPerSecondTextView.setText("Pic per: "+Float.valueOf(seekBar.getProgress())/1000+" s.");
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        mPerSecondTextView.setText("Pic per: "+Float.valueOf(seekBar.getProgress())/1000+" s.");
                        waitTime = seekBar.getProgress();
                    }
                }
        );

    }

    class TakePicTimerTask extends TimerTask {

        @Override
        public void run() {
            count++;
            mCamera.takePicture();
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCountTextView.setText(count.toString());
                }});
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mThread.start();

        if (mTextureView.isAvailable()) {
            // Preview用のTextureViewの準備ができている
            try {
                openCamera(mTextureView.getWidth(), mTextureView.getHeight());
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        } else {
            // 準備完了通知を受け取るためにリスナーを登録する
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    public void onPause() {
        closeCamera();
        mThread.stop();
        super.onPause();
    }

    private String setUpCameraOutputs(int width, int height) {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics
                        = manager.getCameraCharacteristics(cameraId);

                // フロントカメラを利用しない
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                // ストリーム制御をサポートしていない場合、セットアップを中断する
                StreamConfigurationMap map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }

                // 最大サイズでキャプチャする
                Size largest = Collections.max(
                        Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                        new CompareSizesByArea());

                setUpPreview(map.getOutputSizes(SurfaceTexture.class),
                        width, height, largest);
                configurePreviewTransform(width, height);

                mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
                        ImageFormat.JPEG, /*maxImages*/1);

//                final File saveDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Pic");
                mImageReader.setOnImageAvailableListener(
                        new ImageReader.OnImageAvailableListener() {

                            @Override
                            public void onImageAvailable(ImageReader reader) {
                                Log.e("Pic", "image is on available");
                                File file = /*new File(*/getExternalFilesDir(null)/*, "pic.jpg")*/;
                                mThread.getHandler().post(new ImageStore(reader.acquireNextImage(), file));
                            }

                        }, mThread.getHandler());

                return cameraId;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            // Camera2 API未サポート
            Log.e(TAG, "Camera Error:not support Camera2API");
        }

        return null;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void openCamera(int width, int height) throws CameraAccessException {
        if (checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
            return;
        }

        String cameraId = setUpCameraOutputs(width, height);
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!mCamera.isLocked()) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            manager.openCamera(cameraId, mCamera.stateCallback, mThread.getHandler());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
        setSeekBarRange(manager.getCameraCharacteristics(cameraId));
    }

    private void setSeekBarRange(CameraCharacteristics cameraCharacteristics) {
        sensitivityRange = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);
        exposeTimeRange = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);

        mSensorSensitivitySeekBar.setMax(sensitivityRange.getUpper());
        mSensorSensitivitySeekBarMinValue = sensitivityRange.getLower();
        mSensorExposeTimeSeekBar.setMax(Integer.valueOf(exposeTimeRange.getUpper().toString()));
        mSensorExposeTimeSeekBarMinValue = Integer.valueOf(exposeTimeRange.getLower().toString());
    }

    private void closeCamera() {
        mCamera.close();
        if (null != mImageReader) {
            mImageReader.close();
            mImageReader = null;
        }
    }

    //Texture Listener
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            // SurfaceTextureの準備が完了した
            try {
                openCamera(width, height);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            // Viewのサイズに変更があったためPreviewサイズを計算し直す
            configurePreviewTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }
    };

    private void setUpPreview(Size[] choices, int width, int height, Size aspectRatio) {
        // カメラ性能を超えたサイズを指定するとキャプチャデータにゴミがまじるため、注意

        // 表示するSurfaceより、高い解像度のプレビューサイズを抽出する
        List<Size> bigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * h / w &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }

        // プレビューを表示するSurfaceに最も近い（小さな）解像度を選択する
        if (bigEnough.size() > 0) {
            mPreviewSize = Collections.min(bigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            mPreviewSize = choices[0];
        }

        // プレビューが歪まないようにアスペクト比を調整する
//        int orientation = getResources().getConfiguration().orientation;
//        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
//            mTextureView.setAspectRatio(
//                    mPreviewSize.getWidth(), mPreviewSize.getHeight());
//        } else {
//            mTextureView.setAspectRatio(
//                    mPreviewSize.getHeight(), mPreviewSize.getWidth());
//        }
    }

    private void configurePreviewTransform(int viewWidth, int viewHeight) {
        if (null == mTextureView || null == mPreviewSize) {
            return;
        }
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

    // パーミッションの処理シーケンスはまだおかしい
    // Parmission handling for Android 6.0
    @TargetApi(Build.VERSION_CODES.M)
    private void requestCameraPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            // 権限チェックした結果、持っていない場合はダイアログを出す
            new AlertDialog.Builder(this)
                    .setMessage("Request Permission")
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            requestPermissions(new String[]{Manifest.permission.CAMERA},
                                    REQUEST_CODE_CAMERA_PERMISSION);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                }
                            })
                    .create();
            return;
        }

        // 権限を取得する
        requestPermissions(new String[]{Manifest.permission.CAMERA},
                REQUEST_CODE_CAMERA_PERMISSION);
        return;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        if (requestCode == REQUEST_CODE_CAMERA_PERMISSION) {
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                new AlertDialog.Builder(this)
                        .setMessage("Need Camera Permission")
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                finish();
                            }
                        })
                        .create();
            }
            return;
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public SurfaceTexture getSurfaceTextureFromTextureView() {
        return mTextureView.getSurfaceTexture();
    }

    @Override
    public Size getPreviewSize() {
        return mPreviewSize;
    }

    @Override
    public Handler getBackgroundHandler() {
        return mThread.getHandler();
    }

    @Override
    public Surface getImageRenderSurface() {
        return mImageReader.getSurface();
    }

    @Override
    public int getRotation() {
        return getWindowManager().getDefaultDisplay().getRotation();
    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }

}

