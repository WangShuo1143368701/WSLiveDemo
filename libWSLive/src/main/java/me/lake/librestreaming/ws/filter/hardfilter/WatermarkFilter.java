package me.lake.librestreaming.ws.filter.hardfilter;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import java.nio.FloatBuffer;

import me.lake.librestreaming.filter.hardvideofilter.BaseHardVideoFilter;
import me.lake.librestreaming.tools.GLESTools;


public class WatermarkFilter extends BaseHardVideoFilter {
    protected int glProgram;
    protected int glCamTextureLoc;
    protected int glCamPostionLoc;
    protected int glCamTextureCoordLoc;
    protected int glImageTextureLoc;
    protected int glImageRectLoc;
    protected String vertexShader_filter = "" +
            "attribute vec4 aCamPosition;\n" +
            "attribute vec2 aCamTextureCoord;\n" +
            "varying vec2 vCamTextureCoord;\n" +
            "void main(){\n" +
            "   gl_Position= aCamPosition;\n" +
            "   vCamTextureCoord = aCamTextureCoord;\n" +
            "}";
    protected String fragmentshader_filter = "" +
            "precision mediump float;\n" +
            "varying mediump vec2 vCamTextureCoord;\n" +
            "uniform sampler2D uCamTexture;\n" +
            "uniform sampler2D uImageTexture;\n" +
            "uniform vec4 imageRect;\n" +
            "void main(){\n" +
            "   lowp vec4 c1 = texture2D(uCamTexture, vCamTextureCoord);\n" +
            "   lowp vec2 vCamTextureCoord2 = vec2(vCamTextureCoord.x,1.0-vCamTextureCoord.y);\n" +
            "   if(vCamTextureCoord2.x>imageRect.r && vCamTextureCoord2.x<imageRect.b && vCamTextureCoord2.y>imageRect.g && vCamTextureCoord2.y<imageRect.a)\n" +
            "   {\n" +
            "        vec2 imagexy = vec2((vCamTextureCoord2.x-imageRect.r)/(imageRect.b-imageRect.r),(vCamTextureCoord2.y-imageRect.g)/(imageRect.a-imageRect.g));\n" +
            "        lowp vec4 c2 = texture2D(uImageTexture, imagexy);\n" +
            "        lowp vec4 outputColor = c2+c1*c1.a*(1.0-c2.a);\n" +
            "        outputColor.a = 1.0;\n" +
            "        gl_FragColor = outputColor;\n" +
            "   }else\n" +
            "   {\n" +
            "       gl_FragColor = c1;\n" +
            "   }\n" +
            "}";
    protected int imageTexture = GLESTools.NO_TEXTURE;

    protected final Object syncBitmap = new Object();
    protected Bitmap iconBitmap;
    protected boolean needUpdate;
    protected RectF iconRectF;
    protected Rect iconRect;

    public WatermarkFilter(Bitmap _bitmap, Rect _rect) {
        iconBitmap = _bitmap;
        needUpdate = true;
        iconRectF = new RectF();
        iconRect = _rect;
    }

    protected WatermarkFilter() {
        iconBitmap = null;
        needUpdate = false;
        iconRectF = new RectF(0,0,0,0);
    }

    public void updateIcon(Bitmap _bitmap, Rect _rect) {
        synchronized (syncBitmap) {
            if (_bitmap != null) {
                iconBitmap = _bitmap;
                needUpdate = true;
            }
            if (_rect != null) {
                iconRect = _rect;
            }
        }
    }


    @Override
    public void onInit(int VWidth, int VHeight) {
        super.onInit(VWidth, VHeight);
        glProgram = GLESTools.createProgram(vertexShader_filter, fragmentshader_filter);
        GLES20.glUseProgram(glProgram);
        glCamTextureLoc = GLES20.glGetUniformLocation(glProgram, "uCamTexture");
        glImageTextureLoc = GLES20.glGetUniformLocation(glProgram, "uImageTexture");
        glCamPostionLoc = GLES20.glGetAttribLocation(glProgram, "aCamPosition");
        glCamTextureCoordLoc = GLES20.glGetAttribLocation(glProgram, "aCamTextureCoord");
        glImageRectLoc = GLES20.glGetUniformLocation(glProgram, "imageRect");

    }

    @Override
    public void onDraw(int cameraTexture, int targetFrameBuffer, FloatBuffer shapeBuffer, FloatBuffer textrueBuffer) {
        synchronized (syncBitmap) {
            if (needUpdate) {
                if (imageTexture != GLESTools.NO_TEXTURE) {
                    GLES20.glDeleteTextures(1, new int[]{imageTexture}, 0);
                }
                imageTexture = GLESTools.loadTexture(iconBitmap, GLESTools.NO_TEXTURE);
            }
        }
        iconRectF.top = iconRect.top / (float) SIZE_HEIGHT;
        iconRectF.bottom = iconRect.bottom / (float) SIZE_HEIGHT;
        iconRectF.left = iconRect.left / (float) SIZE_WIDTH;
        iconRectF.right = iconRect.right / (float) SIZE_WIDTH;
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, targetFrameBuffer);
        GLES20.glUseProgram(glProgram);
        GLES20.glUniform4f(glImageRectLoc, iconRectF.left, iconRectF.top, iconRectF.right, iconRectF.bottom);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, cameraTexture);
        GLES20.glUniform1i(glCamTextureLoc, 0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, imageTexture);
        GLES20.glUniform1i(glImageTextureLoc, 1);
        GLES20.glEnableVertexAttribArray(glCamPostionLoc);
        GLES20.glEnableVertexAttribArray(glCamTextureCoordLoc);
        shapeBuffer.position(0);
        GLES20.glVertexAttribPointer(glCamPostionLoc, 2,
                GLES20.GL_FLOAT, false,
                2 * 4, shapeBuffer);
        textrueBuffer.position(0);
        GLES20.glVertexAttribPointer(glCamTextureCoordLoc, 2,
                GLES20.GL_FLOAT, false,
                2 * 4, textrueBuffer);
        GLES20.glViewport(0, 0, SIZE_WIDTH, SIZE_HEIGHT);
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawIndecesBuffer.limit(), GLES20.GL_UNSIGNED_SHORT, drawIndecesBuffer);
        GLES20.glFinish();
        GLES20.glDisableVertexAttribArray(glCamPostionLoc);
        GLES20.glDisableVertexAttribArray(glCamTextureCoordLoc);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        GLES20.glUseProgram(0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        GLES20.glDeleteProgram(glProgram);
        GLES20.glDeleteTextures(1, new int[]{imageTexture}, 0);
    }
}
