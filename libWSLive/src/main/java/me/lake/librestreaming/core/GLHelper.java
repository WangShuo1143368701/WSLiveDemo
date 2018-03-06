package me.lake.librestreaming.core;

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.view.Surface;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGL10;

import me.lake.librestreaming.model.MediaCodecGLWapper;
import me.lake.librestreaming.model.OffScreenGLWapper;
import me.lake.librestreaming.model.RESCoreParameters;
import me.lake.librestreaming.model.ScreenGLWapper;
import me.lake.librestreaming.tools.GLESTools;


public class GLHelper {
    private static final int EGL_RECORDABLE_ANDROID = 0x3142;
    private static String VERTEXSHADER = "" +
            "attribute vec4 aPosition;\n" +
            "attribute vec2 aTextureCoord;\n" +
            "varying vec2 vTextureCoord;\n" +
            "void main(){\n" +
            "    gl_Position= aPosition;\n" +
            "    vTextureCoord = aTextureCoord;\n" +
            "}";
    private static final String VERTEXSHADER_CAMERA2D =
            "attribute vec4 aPosition;\n" +
            "attribute vec4 aTextureCoord;\n" +
            "uniform mat4 uTextureMatrix;\n" +
            "varying vec2 vTextureCoord;\n" +
            "void main(){\n" +
            "    gl_Position= aPosition;\n" +
            "    vTextureCoord = (uTextureMatrix * aTextureCoord).xy;\n" +
            "}";
    private static String FRAGMENTSHADER_CAMERA = "" +
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision highp float;\n" +
            "varying highp vec2 vTextureCoord;\n" +
            "uniform sampler2D uTexture;\n" +
            "void main(){\n" +
            "    vec4  color = texture2D(uTexture, vTextureCoord);\n" +
            "    gl_FragColor = color;\n" +
            "}";
    private static String FRAGMENTSHADER_CAMERA2D = "" +
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision highp float;\n" +
            "varying highp vec2 vTextureCoord;\n" +
            "uniform samplerExternalOES uTexture;\n" +
            "void main(){\n" +
            "    vec4  color = texture2D(uTexture, vTextureCoord);\n" +
            "    gl_FragColor = color;\n" +
            "}";
    private static String FRAGMENTSHADER_2D = "" +
            "precision highp float;\n" +
            "varying highp vec2 vTextureCoord;\n" +
            "uniform sampler2D uTexture;\n" +
            "void main(){\n" +
            "    vec4  color = texture2D(uTexture, vTextureCoord);\n" +
            "    gl_FragColor = color;\n" +
            "}";
    private static short drawIndices[] = {0, 1, 2, 0, 2, 3};
    private static float SquareVertices[] = {
            -1.0f, 1.0f,
            -1.0f, -1.0f,
            1.0f, -1.0f,
            1.0f, 1.0f};
    private static float CamTextureVertices[] = {
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f};
    private static float Cam2dTextureVertices[] = {
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f};
    private static float Cam2dTextureVertices_90[] = {
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f};
    private static float Cam2dTextureVertices_180[] = {
            1.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f,
            0.0f, 0.0f};
    private static float Cam2dTextureVertices_270[] = {
            1.0f, 1.0f,
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f};
    public static float MediaCodecTextureVertices[] = {
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f};
    //镜像后的
//    public static float MediaCodecTextureVertices[] = {
//            1.0f, 1.0f,
//            1.0f, 0.0f,
//            0.0f, 0.0f,
//            0.0f, 1.0f};

    private static float ScreenTextureVertices[] = {
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f};
//    private static float ScreenTextureVertices[] = {
//            1.0f, 1.0f,
//            1.0f, 0.0f,
//            0.0f, 0.0f,
//            0.0f, 1.0f};


    public static int FLOAT_SIZE_BYTES = 4;
    public static int SHORT_SIZE_BYTES = 2;
    public static int COORDS_PER_VERTEX = 2;
    public static int TEXTURE_COORDS_PER_VERTEX = 2;

    public static void initOffScreenGL(OffScreenGLWapper wapper) {
        wapper.eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (EGL14.EGL_NO_DISPLAY == wapper.eglDisplay) {
            throw new RuntimeException("eglGetDisplay,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
        }
        int versions[] = new int[2];
        if (!EGL14.eglInitialize(wapper.eglDisplay, versions, 0, versions, 1)) {
            throw new RuntimeException("eglInitialize,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
        }
        int configsCount[] = new int[1];
        EGLConfig configs[] = new EGLConfig[1];
        int configSpec[] = new int[]{
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_DEPTH_SIZE, 0,
                EGL14.EGL_STENCIL_SIZE, 0,
                EGL14.EGL_NONE
        };
        EGL14.eglChooseConfig(wapper.eglDisplay, configSpec, 0, configs, 0, 1, configsCount, 0);
        if (configsCount[0] <= 0) {
            throw new RuntimeException("eglChooseConfig,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
        }
        wapper.eglConfig = configs[0];
        int[] surfaceAttribs = {
                EGL10.EGL_WIDTH, 1,
                EGL10.EGL_HEIGHT, 1,
                EGL14.EGL_NONE
        };
        int contextSpec[] = new int[]{
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
        };
        wapper.eglContext = EGL14.eglCreateContext(wapper.eglDisplay, wapper.eglConfig, EGL14.EGL_NO_CONTEXT, contextSpec, 0);
        if (EGL14.EGL_NO_CONTEXT == wapper.eglContext) {
            throw new RuntimeException("eglCreateContext,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
        }
        int[] values = new int[1];
        EGL14.eglQueryContext(wapper.eglDisplay, wapper.eglContext, EGL14.EGL_CONTEXT_CLIENT_VERSION, values, 0);
        wapper.eglSurface = EGL14.eglCreatePbufferSurface(wapper.eglDisplay, wapper.eglConfig, surfaceAttribs, 0);
        if (null == wapper.eglSurface || EGL14.EGL_NO_SURFACE == wapper.eglSurface) {
            throw new RuntimeException("eglCreateWindowSurface,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
        }
    }

    public static void initMediaCodecGL(MediaCodecGLWapper wapper, EGLContext sharedContext, Surface mediaInputSurface) {
        wapper.eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (EGL14.EGL_NO_DISPLAY == wapper.eglDisplay) {
            throw new RuntimeException("eglGetDisplay,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
        }
        int versions[] = new int[2];
        if (!EGL14.eglInitialize(wapper.eglDisplay, versions, 0, versions, 1)) {
            throw new RuntimeException("eglInitialize,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
        }
        int configsCount[] = new int[1];
        EGLConfig configs[] = new EGLConfig[1];
        int configSpec[] = new int[]{
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL_RECORDABLE_ANDROID, 1,
                EGL14.EGL_DEPTH_SIZE, 0,
                EGL14.EGL_STENCIL_SIZE, 0,
                EGL14.EGL_NONE
        };
        EGL14.eglChooseConfig(wapper.eglDisplay, configSpec, 0, configs, 0, 1, configsCount, 0);
        if (configsCount[0] <= 0) {
            throw new RuntimeException("eglChooseConfig,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
        }
        wapper.eglConfig = configs[0];
        int[] surfaceAttribs = {
                EGL14.EGL_NONE
        };
        int contextSpec[] = new int[]{
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
        };
        wapper.eglContext = EGL14.eglCreateContext(wapper.eglDisplay, wapper.eglConfig, sharedContext, contextSpec, 0);
        if (EGL14.EGL_NO_CONTEXT == wapper.eglContext) {
            throw new RuntimeException("eglCreateContext,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
        }
        int[] values = new int[1];
        EGL14.eglQueryContext(wapper.eglDisplay, wapper.eglContext, EGL14.EGL_CONTEXT_CLIENT_VERSION, values, 0);
        wapper.eglSurface = EGL14.eglCreateWindowSurface(wapper.eglDisplay, wapper.eglConfig, mediaInputSurface, surfaceAttribs, 0);
        if (null == wapper.eglSurface || EGL14.EGL_NO_SURFACE == wapper.eglSurface) {
            throw new RuntimeException("eglCreateWindowSurface,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
        }
    }

    public static void initScreenGL(ScreenGLWapper wapper, EGLContext sharedContext, SurfaceTexture screenSurface) {
        wapper.eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (EGL14.EGL_NO_DISPLAY == wapper.eglDisplay) {
            throw new RuntimeException("eglGetDisplay,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
        }
        int versions[] = new int[2];
        if (!EGL14.eglInitialize(wapper.eglDisplay, versions, 0, versions, 1)) {
            throw new RuntimeException("eglInitialize,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
        }
        int configsCount[] = new int[1];
        EGLConfig configs[] = new EGLConfig[1];
        int configSpec[] = new int[]{
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_DEPTH_SIZE, 0,
                EGL14.EGL_STENCIL_SIZE, 0,
                EGL14.EGL_NONE
        };
        EGL14.eglChooseConfig(wapper.eglDisplay, configSpec, 0, configs, 0, 1, configsCount, 0);
        if (configsCount[0] <= 0) {
            throw new RuntimeException("eglChooseConfig,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
        }
        wapper.eglConfig = configs[0];
        int[] surfaceAttribs = {
                EGL14.EGL_NONE
        };
        int contextSpec[] = new int[]{
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
        };
        wapper.eglContext = EGL14.eglCreateContext(wapper.eglDisplay, wapper.eglConfig, sharedContext, contextSpec, 0);
        if (EGL14.EGL_NO_CONTEXT == wapper.eglContext) {
            throw new RuntimeException("eglCreateContext,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
        }
        int[] values = new int[1];
        EGL14.eglQueryContext(wapper.eglDisplay, wapper.eglContext, EGL14.EGL_CONTEXT_CLIENT_VERSION, values, 0);
        wapper.eglSurface = EGL14.eglCreateWindowSurface(wapper.eglDisplay, wapper.eglConfig, screenSurface, surfaceAttribs, 0);
        if (null == wapper.eglSurface || EGL14.EGL_NO_SURFACE == wapper.eglSurface) {
            throw new RuntimeException("eglCreateWindowSurface,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
        }
    }

    public static void makeCurrent(OffScreenGLWapper wapper) {
        if (!EGL14.eglMakeCurrent(wapper.eglDisplay, wapper.eglSurface, wapper.eglSurface, wapper.eglContext)) {
            throw new RuntimeException("eglMakeCurrent,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
        }
    }

    public static void makeCurrent(MediaCodecGLWapper wapper) {
        if (!EGL14.eglMakeCurrent(wapper.eglDisplay, wapper.eglSurface, wapper.eglSurface, wapper.eglContext)) {
            throw new RuntimeException("eglMakeCurrent,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
        }
    }

    public static void makeCurrent(ScreenGLWapper wapper) {
        if (!EGL14.eglMakeCurrent(wapper.eglDisplay, wapper.eglSurface, wapper.eglSurface, wapper.eglContext)) {
            throw new RuntimeException("eglMakeCurrent,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
        }
    }

    public static void createCamFrameBuff(int[] frameBuffer, int[] frameBufferTex, int width, int height) {
        GLES20.glGenFramebuffers(1, frameBuffer, 0);
        GLES20.glGenTextures(1, frameBufferTex, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, frameBufferTex[0]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, frameBufferTex[0], 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLESTools.checkGlError("createCamFrameBuff");
    }

    public static void enableVertex(int posLoc, int texLoc, FloatBuffer shapeBuffer, FloatBuffer texBuffer) {
        GLES20.glEnableVertexAttribArray(posLoc);
        GLES20.glEnableVertexAttribArray(texLoc);
        GLES20.glVertexAttribPointer(posLoc, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                COORDS_PER_VERTEX * 4, shapeBuffer);
        GLES20.glVertexAttribPointer(texLoc, TEXTURE_COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                TEXTURE_COORDS_PER_VERTEX * 4, texBuffer);
    }

    public static void disableVertex(int posLoc, int texLoc) {
        GLES20.glDisableVertexAttribArray(posLoc);
        GLES20.glDisableVertexAttribArray(texLoc);
    }

    public static int createCamera2DProgram() {
        return GLESTools.createProgram(VERTEXSHADER_CAMERA2D, FRAGMENTSHADER_CAMERA2D);
    }

    public static int createCameraProgram() {
        return GLESTools.createProgram(VERTEXSHADER, FRAGMENTSHADER_CAMERA);
    }

    public static int createMediaCodecProgram() {
        return GLESTools.createProgram(VERTEXSHADER, FRAGMENTSHADER_2D);
    }

    public static int createScreenProgram() {
        return GLESTools.createProgram(VERTEXSHADER, FRAGMENTSHADER_2D);
    }

    public static ShortBuffer getDrawIndecesBuffer() {
        ShortBuffer result = ByteBuffer.allocateDirect(SHORT_SIZE_BYTES * drawIndices.length).
                order(ByteOrder.nativeOrder()).
                asShortBuffer();
        result.put(drawIndices);
        result.position(0);
        return result;
    }

    public static FloatBuffer getShapeVerticesBuffer() {
        FloatBuffer result = ByteBuffer.allocateDirect(FLOAT_SIZE_BYTES * SquareVertices.length).
                order(ByteOrder.nativeOrder()).
                asFloatBuffer();
        result.put(SquareVertices);
        result.position(0);
        return result;
    }

    public static FloatBuffer getMediaCodecTextureVerticesBuffer() {
        FloatBuffer result = ByteBuffer.allocateDirect(FLOAT_SIZE_BYTES * MediaCodecTextureVertices.length).
                order(ByteOrder.nativeOrder()).
                asFloatBuffer();
        result.put(MediaCodecTextureVertices);
        result.position(0);
        return result;
    }

    public static FloatBuffer getScreenTextureVerticesBuffer() {
        FloatBuffer result = ByteBuffer.allocateDirect(FLOAT_SIZE_BYTES * ScreenTextureVertices.length).
                order(ByteOrder.nativeOrder()).
                asFloatBuffer();
        result.put(ScreenTextureVertices);
        result.position(0);
        return result;
    }

    public static FloatBuffer getCamera2DTextureVerticesBuffer(final int directionFlag, final float cropRatio) {
        if (directionFlag == -1) {
            FloatBuffer result = ByteBuffer.allocateDirect(FLOAT_SIZE_BYTES * Cam2dTextureVertices.length).
                    order(ByteOrder.nativeOrder()).
                    asFloatBuffer();
            result.put(CamTextureVertices);
            result.position(0);
            return result;
        }
        float[] buffer;
        switch (directionFlag & 0xF0) {
            case RESCoreParameters.FLAG_DIRECTION_ROATATION_90:
                buffer = Cam2dTextureVertices_90.clone();
                break;
            case RESCoreParameters.FLAG_DIRECTION_ROATATION_180:
                buffer = Cam2dTextureVertices_180.clone();
                break;
            case RESCoreParameters.FLAG_DIRECTION_ROATATION_270:
                buffer = Cam2dTextureVertices_270.clone();
                break;
            default:
                buffer = Cam2dTextureVertices.clone();
        }
        if ((directionFlag & 0xF0) == RESCoreParameters.FLAG_DIRECTION_ROATATION_0 || (directionFlag & 0xF0) == RESCoreParameters.FLAG_DIRECTION_ROATATION_180) {
            if (cropRatio > 0) {
                buffer[1] = buffer[1] == 1.0f ? (1.0f - cropRatio) : cropRatio;
                buffer[3] = buffer[3] == 1.0f ? (1.0f - cropRatio) : cropRatio;
                buffer[5] = buffer[5] == 1.0f ? (1.0f - cropRatio) : cropRatio;
                buffer[7] = buffer[7] == 1.0f ? (1.0f - cropRatio) : cropRatio;
            } else {
                buffer[0] = buffer[0] == 1.0f ? (1.0f + cropRatio) : -cropRatio;
                buffer[2] = buffer[2] == 1.0f ? (1.0f + cropRatio) : -cropRatio;
                buffer[4] = buffer[4] == 1.0f ? (1.0f + cropRatio) : -cropRatio;
                buffer[6] = buffer[6] == 1.0f ? (1.0f + cropRatio) : -cropRatio;
            }
        } else {
            if (cropRatio > 0) {
                buffer[0] = buffer[0] == 1.0f ? (1.0f - cropRatio) : cropRatio;
                buffer[2] = buffer[2] == 1.0f ? (1.0f - cropRatio) : cropRatio;
                buffer[4] = buffer[4] == 1.0f ? (1.0f - cropRatio) : cropRatio;
                buffer[6] = buffer[6] == 1.0f ? (1.0f - cropRatio) : cropRatio;
            } else {
                buffer[1] = buffer[1] == 1.0f ? (1.0f + cropRatio) : -cropRatio;
                buffer[3] = buffer[3] == 1.0f ? (1.0f + cropRatio) : -cropRatio;
                buffer[5] = buffer[5] == 1.0f ? (1.0f + cropRatio) : -cropRatio;
                buffer[7] = buffer[7] == 1.0f ? (1.0f + cropRatio) : -cropRatio;
            }
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
        FloatBuffer result = ByteBuffer.allocateDirect(FLOAT_SIZE_BYTES * buffer.length).
                order(ByteOrder.nativeOrder()).
                asFloatBuffer();
        result.put(buffer);
        result.position(0);
        return result;
    }

    public static FloatBuffer getCameraTextureVerticesBuffer() {
        FloatBuffer result = ByteBuffer.allocateDirect(FLOAT_SIZE_BYTES * Cam2dTextureVertices.length).
                order(ByteOrder.nativeOrder()).
                asFloatBuffer();
        result.put(CamTextureVertices);
        result.position(0);
        return result;
    }

    private static float flip(final float i) {
        return (1.0f - i);
    }


    public static FloatBuffer adjustTextureFlip(boolean flipHorizontal) {
        float[] textureCords = getFlip(flipHorizontal, false);
        FloatBuffer mTextureBuffer = null;
        if (mTextureBuffer == null) {
            mTextureBuffer = ByteBuffer.allocateDirect(textureCords.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
        }
        mTextureBuffer.clear();
        mTextureBuffer.put(textureCords).position(0);

        return mTextureBuffer;
    }

    public static float[] getFlip(final boolean flipHorizontal,
                                      final boolean flipVertical) {
        float[] rotatedTex = Cam2dTextureVertices;

        if (flipHorizontal) {
            rotatedTex = new float[]{
                    flip2(rotatedTex[0]), rotatedTex[1],
                    flip2(rotatedTex[2]), rotatedTex[3],
                    flip2(rotatedTex[4]), rotatedTex[5],
                    flip2(rotatedTex[6]), rotatedTex[7],
            };
        }
        if (flipVertical) {
            rotatedTex = new float[]{
                    rotatedTex[0], flip2(rotatedTex[1]),
                    rotatedTex[2], flip2(rotatedTex[3]),
                    rotatedTex[4], flip2(rotatedTex[5]),
                    rotatedTex[6], flip2(rotatedTex[7]),
            };
        }
        return rotatedTex;
    }


    private static float flip2(final float i) {
        if (i == 0.0f) {
            return 1.0f;
        }
        return 0.0f;
    }
}