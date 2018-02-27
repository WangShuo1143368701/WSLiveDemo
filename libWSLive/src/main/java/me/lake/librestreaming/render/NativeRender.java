package me.lake.librestreaming.render;

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.view.Surface;

import me.lake.librestreaming.tools.LogTools;


public class NativeRender implements IRender {
    Surface mVisualSurface;
    int mPixelWidth;
    int mPixelHeight;
    int mPixelSize;

    @Override
    public void create(SurfaceTexture visualSurfaceTexture, int pixelFormat, int pixelWidth, int pixelHeight, int visualWidth, int visualHeight) {
        if (pixelFormat != ImageFormat.NV21) {
            throw new IllegalArgumentException("NativeRender,pixelFormat only support NV21");
        }
        mVisualSurface = new Surface(visualSurfaceTexture);
        mPixelWidth = pixelWidth;
        mPixelHeight = pixelHeight;
        mPixelSize = (3 * pixelWidth * pixelHeight) / 2;
    }

    @Override
    public void update(int visualWidth, int visualHeight) {

    }

    @Override
    public void rendering(byte[] pixel) {
        if (mVisualSurface != null && mVisualSurface.isValid()) {
            renderingSurface(mVisualSurface, pixel, mPixelWidth, mPixelHeight, mPixelSize);
        } else {
            LogTools.d("NativeRender,rendering()invalid Surface");
        }
    }

    @Override
    public void destroy(boolean releaseTexture) {
        if(releaseTexture) {
            mVisualSurface.release();
        }
    }

    @SuppressWarnings("all")
    private native void renderingSurface(Surface surface, byte[] pixels, int w, int h, int s);
}