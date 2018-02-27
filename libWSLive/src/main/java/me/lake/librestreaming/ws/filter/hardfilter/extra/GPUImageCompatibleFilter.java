package me.lake.librestreaming.ws.filter.hardfilter.extra;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import jp.co.cyberagent.android.gpuimage.GPUImageFilter;
import me.lake.librestreaming.filter.hardvideofilter.BaseHardVideoFilter;
import me.lake.librestreaming.model.RESCoreParameters;
import me.lake.librestreaming.tools.GLESTools;


public class GPUImageCompatibleFilter<T extends GPUImageFilter> extends BaseHardVideoFilter {
    private T innerGPUImageFilter;

    private FloatBuffer innerShapeBuffer;
    private FloatBuffer innerTextureBuffer;

    public GPUImageCompatibleFilter(T filter) {
        innerGPUImageFilter = filter;
    }

    public T getGPUImageFilter() {
        return innerGPUImageFilter;
    }

    @Override
    public void onInit(int VWidth, int VHeight) {
        super.onInit(VWidth, VHeight);
        innerGPUImageFilter.init();
        innerGPUImageFilter.onOutputSizeChanged(VWidth, VHeight);
    }

    @Override
    public void onDraw(int cameraTexture, int targetFrameBuffer, FloatBuffer shapeBuffer, FloatBuffer textrueBuffer) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, targetFrameBuffer);
        innerGPUImageFilter.onDraw(cameraTexture, innerShapeBuffer, innerTextureBuffer);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        innerGPUImageFilter.destroy();
    }

    @Override
    public void onDirectionUpdate(int _directionFlag) {
        if (directionFlag != _directionFlag) {
            innerShapeBuffer = getGPUImageCompatShapeVerticesBuffer();
            innerTextureBuffer = getGPUImageCompatTextureVerticesBuffer(directionFlag);
        }
    }

    public static final float TEXTURE_NO_ROTATION[] = {
            1.0f, 1.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            0.0f, 0.0f,
    };

    public static final float TEXTURE_ROTATED_90[] = {
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 1.0f,
            1.0f, 0.0f,
    };
    public static final float TEXTURE_ROTATED_180[] = {
            0.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f,
    };
    public static final float TEXTURE_ROTATED_270[] = {
            1.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            0.0f, 1.0f,
    };
    static final float CUBE[] = {
            -1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f, 1.0f,
            1.0f, 1.0f,
    };

    public static FloatBuffer getGPUImageCompatShapeVerticesBuffer() {
        FloatBuffer result = ByteBuffer.allocateDirect(GLESTools.FLOAT_SIZE_BYTES * CUBE.length).
                order(ByteOrder.nativeOrder()).
                asFloatBuffer();
        result.put(CUBE);
        result.position(0);
        return result;
    }

    public static FloatBuffer getGPUImageCompatTextureVerticesBuffer(final int directionFlag) {
        float[] buffer;
        switch (directionFlag & 0xF0) {
            case RESCoreParameters.FLAG_DIRECTION_ROATATION_90:
                buffer = TEXTURE_ROTATED_90.clone();
                break;
            case RESCoreParameters.FLAG_DIRECTION_ROATATION_180:
                buffer = TEXTURE_ROTATED_180.clone();
                break;
            case RESCoreParameters.FLAG_DIRECTION_ROATATION_270:
                buffer = TEXTURE_ROTATED_270.clone();
                break;
            default:
                buffer = TEXTURE_NO_ROTATION.clone();
        }
        if ((directionFlag & RESCoreParameters.FLAG_DIRECTION_FLIP_HORIZONTAL) != 0) {
            buffer[0] = flip(buffer[0]);
            buffer[2] = flip(buffer[2]);
            buffer[4] = flip(buffer[4]);
            buffer[6] = flip(buffer[6]);
        }
        if ((directionFlag & RESCoreParameters.FLAG_DIRECTION_FLIP_VERTICAL) != 0) {
            buffer[1] = flip(buffer[1]);
            buffer[3] = flip(buffer[3]);
            buffer[5] = flip(buffer[5]);
            buffer[7] = flip(buffer[7]);
        }
        FloatBuffer result = ByteBuffer.allocateDirect(GLESTools.FLOAT_SIZE_BYTES * buffer.length).
                order(ByteOrder.nativeOrder()).
                asFloatBuffer();
        result.put(buffer);
        result.position(0);
        return result;
    }

    private static float flip(final float i) {
        return i == 0.0f ? 1.0f : 0.0f;
    }
}
