package me.lake.librestreaming.model;

import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;


public class OffScreenGLWapper {
    public EGLDisplay eglDisplay;
    public EGLConfig eglConfig;
    public EGLSurface eglSurface;
    public EGLContext eglContext;

    public int cam2dProgram;
    public int cam2dTextureMatrix;
    public int cam2dTextureLoc;
    public int cam2dPostionLoc;
    public int cam2dTextureCoordLoc;

    public int camProgram;
    public int camTextureLoc;
    public int camPostionLoc;
    public int camTextureCoordLoc;
}
