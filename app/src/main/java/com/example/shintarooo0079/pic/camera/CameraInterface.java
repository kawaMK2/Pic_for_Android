package com.example.shintarooo0079.pic.camera;

import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.util.Size;
import android.view.Surface;

/**
 * Created by shintarooo0079 on 2017/04/06.
 */

public interface CameraInterface {
    SurfaceTexture getSurfaceTextureFromTextureView();
    Size getPreviewSize();
    Handler getBackgroundHandler();
    Surface getImageRenderSurface();
    int getRotation();
}
