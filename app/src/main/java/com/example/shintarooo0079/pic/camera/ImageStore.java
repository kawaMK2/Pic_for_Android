package com.example.shintarooo0079.pic.camera;

import android.media.Image;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by shintarooo0079 on 2017/04/10.
 */

public class ImageStore implements Runnable {

    private final Image mImage;
    private final File mFile;

    public ImageStore(Image image, File file) {
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
        String imageFileName = timeStamp + "_";
        mImage = image;
        mFile = new File(file, imageFileName+".jpg");
    }

    @Override
    public void run() {
        ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(mFile);
            output.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            mImage.close();
            if (null != output) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        Log.e("Pic", "output " + mFile.getAbsolutePath());
    }
}
