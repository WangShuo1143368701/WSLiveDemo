package me.lake.librestreaming.render;

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

import me.lake.librestreaming.tools.GLESTools;


public class GLESRender implements IRender {
    private final Object syncRenderThread = new Object();
    GLESRenderThread glesRenderThread;

    @Override
    public void create(SurfaceTexture visualSurfaceTexture, int pixelFormat, int pixelWidth, int pixelHeight, int visualWidth, int visualHeight) {
        if (pixelFormat != ImageFormat.NV21) {
            throw new IllegalArgumentException("GLESRender,pixelFormat only support NV21");
        }
        synchronized (syncRenderThread) {
            glesRenderThread = new GLESRenderThread(visualSurfaceTexture,
                    pixelFormat,
                    pixelWidth,
                    pixelHeight,
                    visualWidth,
                    visualHeight);
            glesRenderThread.start();
        }
    }

    @Override
    public void update(int visualWidth, int visualHeight) {
        synchronized (syncRenderThread) {
            glesRenderThread.updateVisualWH(visualWidth, visualHeight);
        }
    }

    @Override
    public void rendering(byte[] pixel) {
        synchronized (syncRenderThread) {
            glesRenderThread.updatePixel(pixel);
        }
    }

    @Override
    public void destroy(boolean releaseTexture) {
        synchronized (syncRenderThread) {
            glesRenderThread.quit(releaseTexture);
            try {
                glesRenderThread.join();
            } catch (InterruptedException ignored) {
            }
        }
    }

    private static class GLESRenderThread extends Thread {
        int mPixelWidth;
        int mPixelHeight;
        int mySize;
        int mVisualWidth;
        int mVisualHeight;
        byte[] yTemp, uTemp, vTemp;
        SurfaceTexture mVisualSurfaceTexture;
        private final Object syncThread = new Object();
        boolean quit = false;
        boolean releaseTexture=true;

        EGL10 mEgl;
        EGLDisplay mEglDisplay;
        EGLConfig mEglConfig;
        EGLSurface mEglSurface;
        EGLContext mEglContext;
        int mProgram;

        public GLESRenderThread(SurfaceTexture visualSurfaceTexture, int pixelFormat, int pixelWidth, int pixelHeight, int visualWidth, int visualHeight) {
            quit = false;
            mVisualSurfaceTexture = visualSurfaceTexture;
            mPixelWidth = pixelWidth;
            mPixelHeight = pixelHeight;
            mySize = mPixelWidth * mPixelHeight;
            mVisualWidth = visualWidth;
            mVisualHeight = visualHeight;
            yBuf = ByteBuffer.allocateDirect(mySize);
            uBuf = ByteBuffer.allocateDirect(mySize >> 2);
            vBuf = ByteBuffer.allocateDirect(mySize >> 2);
            yTemp = new byte[mySize];
            uTemp = new byte[mySize >> 2];
            vTemp = new byte[mySize >> 2];
            Arrays.fill(uTemp, (byte) 0x7F);
            Arrays.fill(vTemp, (byte) 0x7F);
            uBuf.position(0);
            uBuf.put(uTemp).position(0);
            vBuf.position(0);
            vBuf.put(vTemp).position(0);
        }

        public void quit(boolean releaseTexture) {
            synchronized (syncThread) {
                this.releaseTexture = releaseTexture;
                quit = true;
                syncThread.notify();
            }
        }

        public void updatePixel(byte[] pixel) {
            synchronized (syncBuff) {
                NV21TOYUV(pixel, yTemp, uTemp, vTemp, mPixelWidth, mPixelHeight);
                yBuf.position(0);
                yBuf.put(yTemp).position(0);
                uBuf.position(0);
                uBuf.put(uTemp).position(0);
                vBuf.position(0);
                vBuf.put(vTemp).position(0);
            }
            synchronized (syncThread) {
                syncThread.notify();
            }
        }

        public void updateVisualWH(int visualWidth, int visualHeight) {
            mVisualWidth = visualWidth;
            mVisualHeight = visualHeight;
        }

        @Override
        public void run() {
            initGLES();
            mProgram = GLESTools.createProgram(vertexShaderCode, fragmentshaderCode);
            initVertex();
            initTexture();
            while (!quit) {
                drawFrame();
                if (!mEgl.eglSwapBuffers(mEglDisplay, mEglSurface)) {
                    throw new RuntimeException("eglSwapBuffers,failed!");
                }
                synchronized (syncThread) {
                    try {
                        if(!quit) {
                            syncThread.wait();
                        }
                    } catch (InterruptedException ignored) {
                    }
                }
            }
            releaseGLES();
            if (releaseTexture) {
                mVisualSurfaceTexture.release();
            }
        }

        private void drawFrame() {
            GLES20.glViewport(0, 0, mVisualWidth, mVisualHeight);
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

            GLES20.glUseProgram(mProgram);

            synchronized (syncBuff) {
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yTexture[0]);
                GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0,
                        mPixelWidth,
                        mPixelHeight,
                        GLES20.GL_LUMINANCE,
                        GLES20.GL_UNSIGNED_BYTE,
                        yBuf);
                GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, uTexture[0]);
                GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0,
                        mPixelWidth >> 1,
                        mPixelHeight >> 1,
                        GLES20.GL_LUMINANCE,
                        GLES20.GL_UNSIGNED_BYTE,
                        uBuf);
                GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, vTexture[0]);
                GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0,
                        mPixelWidth >> 1,
                        mPixelHeight >> 1,
                        GLES20.GL_LUMINANCE,
                        GLES20.GL_UNSIGNED_BYTE,
                        vBuf);
            }
            //=================================
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawIndices.length, GLES20.GL_UNSIGNED_SHORT, mDrawIndicesBuffer);
            GLES20.glFinish();
        }

        private void initGLES() {
            mEgl = (EGL10) EGLContext.getEGL();
            mEglDisplay = mEgl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
            if (EGL10.EGL_NO_DISPLAY == mEglDisplay) {
                throw new RuntimeException("GLESRender,eglGetDisplay,failed:" + GLUtils.getEGLErrorString(mEgl.eglGetError()));
            }
            int versions[] = new int[2];
            if (!mEgl.eglInitialize(mEglDisplay, versions)) {
                throw new RuntimeException("GLESRender,eglInitialize,failed:" + GLUtils.getEGLErrorString(mEgl.eglGetError()));
            }
            int configsCount[] = new int[1];
            EGLConfig configs[] = new EGLConfig[1];
            int configSpec[] = new int[]{
                    EGL10.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                    EGL10.EGL_RED_SIZE, 8,
                    EGL10.EGL_GREEN_SIZE, 8,
                    EGL10.EGL_BLUE_SIZE, 8,
                    EGL10.EGL_DEPTH_SIZE, 0,
                    EGL10.EGL_STENCIL_SIZE, 0,
                    EGL10.EGL_NONE
            };
            mEgl.eglChooseConfig(mEglDisplay, configSpec, configs, 1, configsCount);
            if (configsCount[0] <= 0) {
                throw new RuntimeException("GLESRender,eglChooseConfig,failed:" + GLUtils.getEGLErrorString(mEgl.eglGetError()));
            }
            mEglConfig = configs[0];
            mEglSurface = mEgl.eglCreateWindowSurface(mEglDisplay, mEglConfig, mVisualSurfaceTexture, null);
            if (null == mEglSurface || EGL10.EGL_NO_SURFACE == mEglSurface) {
                throw new RuntimeException("GLESRender,eglCreateWindowSurface,failed:" + GLUtils.getEGLErrorString(mEgl.eglGetError()));
            }
            int contextSpec[] = new int[]{
                    EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                    EGL14.EGL_NONE
            };
            mEglContext = mEgl.eglCreateContext(mEglDisplay, mEglConfig, EGL10.EGL_NO_CONTEXT, contextSpec);
            if (EGL10.EGL_NO_CONTEXT == mEglContext) {
                throw new RuntimeException("GLESRender,eglCreateContext,failed:" + GLUtils.getEGLErrorString(mEgl.eglGetError()));
            }
            if (!mEgl.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext)) {
                throw new RuntimeException("GLESRender,eglMakeCurrent,failed:" + GLUtils.getEGLErrorString(mEgl.eglGetError()));
            }
            GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        }

        private void initVertex() {
            mSquareVerticesBuffer = ByteBuffer.allocateDirect(FLOAT_SIZE_BYTES * squareVertices.length).
                    order(ByteOrder.nativeOrder()).
                    asFloatBuffer();
            mSquareVerticesBuffer.put(squareVertices);
            mSquareVerticesBuffer.position(0);
            mTextureCoordsBuffer = ByteBuffer.allocateDirect(FLOAT_SIZE_BYTES * textureVertices.length).
                    order(ByteOrder.nativeOrder()).
                    asFloatBuffer();
            mTextureCoordsBuffer.put(textureVertices);
            mTextureCoordsBuffer.position(0);
            mDrawIndicesBuffer = ByteBuffer.allocateDirect(SHORT_SIZE_BYTES * drawIndices.length).
                    order(ByteOrder.nativeOrder()).
                    asShortBuffer();
            mDrawIndicesBuffer.put(drawIndices);
            mDrawIndicesBuffer.position(0);
        }

        private void initTexture() {
            GLES20.glEnable(GLES20.GL_TEXTURE_2D);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE2);

            createTexture(mPixelWidth, mPixelHeight, GLES20.GL_LUMINANCE, yTexture);
            createTexture(mPixelWidth >> 1, mPixelHeight >> 1, GLES20.GL_LUMINANCE, uTexture);
            createTexture(mPixelWidth >> 1, mPixelHeight >> 1, GLES20.GL_LUMINANCE, vTexture);

            GLES20.glUseProgram(mProgram);
            sampleYLoaction = GLES20.glGetUniformLocation(mProgram, "samplerY");
            sampleULoaction = GLES20.glGetUniformLocation(mProgram, "samplerU");
            sampleVLoaction = GLES20.glGetUniformLocation(mProgram, "samplerV");
            GLES20.glUniform1i(sampleYLoaction, 0);
            GLES20.glUniform1i(sampleULoaction, 1);
            GLES20.glUniform1i(sampleVLoaction, 2);
            int aPostionLocation = GLES20.glGetAttribLocation(mProgram, "aPosition");
            int aTextureCoordLocation = GLES20.glGetAttribLocation(mProgram, "aTextureCoord");
            GLES20.glEnableVertexAttribArray(aPostionLocation);
            GLES20.glVertexAttribPointer(aPostionLocation, SHAPE_COORD_PER_VERTEX,
                    GLES20.GL_FLOAT, false,
                    SHAPE_COORD_PER_VERTEX * 4, mSquareVerticesBuffer);
            GLES20.glEnableVertexAttribArray(aTextureCoordLocation);
            GLES20.glVertexAttribPointer(aTextureCoordLocation, TEXTURE_COORD_PER_VERTEX,
                    GLES20.GL_FLOAT, false,
                    TEXTURE_COORD_PER_VERTEX * 4, mTextureCoordsBuffer);
        }

        private void createTexture(int width, int height, int format, int[] texture) {
            GLES20.glGenTextures(1, texture, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0]);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, format, width, height, 0, format, GLES20.GL_UNSIGNED_BYTE, null);
        }

        private void releaseGLES() {
            GLES20.glDeleteProgram(mProgram);
            GLES20.glDeleteTextures(1, yTexture, 0);
            GLES20.glDeleteTextures(1, uTexture, 0);
            GLES20.glDeleteTextures(1, vTexture, 0);
            mEgl.eglMakeCurrent(mEglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
            mEgl.eglDestroySurface(mEglDisplay, mEglSurface);
            mEgl.eglDestroyContext(mEglDisplay, mEglContext);
            mEgl.eglTerminate(mEglDisplay);
        }

        //Pixel Buff
        private final Object syncBuff = new Object();
        private ByteBuffer yBuf;
        private ByteBuffer uBuf;
        private ByteBuffer vBuf;

        //texture
        private int[] yTexture = new int[1];
        private int[] uTexture = new int[1];
        private int[] vTexture = new int[1];
        private int sampleYLoaction;
        private int sampleULoaction;
        private int sampleVLoaction;

        //shape vertices
        private FloatBuffer mSquareVerticesBuffer;
        private static float squareVertices[] = {
                -1.0f, -1.0f, 0.0f,
                1.0f, -1.0f, 0.0f,
                1.0f, 1.0f, 0.0f,
                -1.0f, 1.0f, 0.0f
        };
        //texture coordinate vertices
        private FloatBuffer mTextureCoordsBuffer;
        private static float textureVertices[] = {
                0.0f, 1.0f,
                1.0f, 1.0f,
                1.0f, 0.0f,
                0.0f, 0.0f
        };
        //gl draw order
        private ShortBuffer mDrawIndicesBuffer;
        private static short drawIndices[] = {0, 1, 2, 0, 2, 3};

        private static int FLOAT_SIZE_BYTES = 4;
        private static int SHORT_SIZE_BYTES = 2;
        private static final int SHAPE_COORD_PER_VERTEX = 3;
        private static final int TEXTURE_COORD_PER_VERTEX = 2;

        private static String vertexShaderCode =
                "attribute vec4 aPosition;\n" +
                        "attribute vec2 aTextureCoord;\n" +
                        "varying vec2 vTextureCoord;\n" +
                        "void main(){\n" +
                        "    gl_Position= aPosition;\n" +
                        "    vTextureCoord = aTextureCoord;\n" +
                        "}";
        private static String fragmentshaderCode =
                "varying lowp vec2 vTextureCoord;\n" +
                        "uniform sampler2D samplerY;\n" +
                        "uniform sampler2D samplerU;\n" +
                        "uniform sampler2D samplerV;\n" +
                        "const mediump mat3 yuv2rgb = mat3(1,1,1,0,-0.39465,2.03211,1.13983,-0.5806,0);\n" +
                        "void main(){\n" +
                        "    mediump vec3 yuv;\n" +
                        "    yuv.x = texture2D(samplerY,vTextureCoord).r;\n" +
                        "    yuv.y = texture2D(samplerU,vTextureCoord).r - 0.5;\n" +
                        "    yuv.z = texture2D(samplerV,vTextureCoord).r - 0.5;\n" +
                        "    gl_FragColor = vec4(yuv2rgb*yuv,1);\n" +
                        "}";
    }

    @SuppressWarnings("all")
    private static native void NV21TOYUV(byte[] src, byte[] dstY, byte[] dstU, byte[] dstV, int width, int height);
}
