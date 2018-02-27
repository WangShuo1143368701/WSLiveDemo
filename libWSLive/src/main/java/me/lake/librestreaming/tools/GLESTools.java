package me.lake.librestreaming.tools;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;


public class GLESTools {
    public static int FLOAT_SIZE_BYTES = 4;
    public static int SHORT_SIZE_BYTES = 2;

    public static String readTextFile(Resources res, int resId) {
        InputStream inputStream = res.openRawResource(resId);
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        StringBuilder result = new StringBuilder();
        try {
            while ((line = br.readLine()) != null) {
                result.append(line);
                result.append("\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return result.toString();
    }

    public static int createProgram(Resources res, int vertexShaderResId, int fragmentShaderResId) {
        String vertexShaderCode = readTextFile(res, vertexShaderResId);
        String fragmentShaderCode = readTextFile(res, fragmentShaderResId);
        return createProgram(vertexShaderCode, fragmentShaderCode);
    }

    public static int createProgram(String vertexShaderCode, String fragmentShaderCode) {
        if (vertexShaderCode == null || fragmentShaderCode == null) {
            throw new RuntimeException("invalid shader code");
        }
        int vertexShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        int fragmentShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);

        GLES20.glShaderSource(vertexShader, vertexShaderCode);
        GLES20.glShaderSource(fragmentShader, fragmentShaderCode);
        int[] status = new int[1];
        GLES20.glCompileShader(vertexShader);
        GLES20.glGetShaderiv(vertexShader, GLES20.GL_COMPILE_STATUS, status, 0);
        if (GLES20.GL_FALSE == status[0]) {
            throw new RuntimeException("vertext shader compile,failed:" + GLES20.glGetShaderInfoLog(vertexShader));
        }
        GLES20.glCompileShader(fragmentShader);
        GLES20.glGetShaderiv(fragmentShader, GLES20.GL_COMPILE_STATUS, status, 0);
        if (GLES20.GL_FALSE == status[0]) {
            throw new RuntimeException("fragment shader compile,failed:" + GLES20.glGetShaderInfoLog(fragmentShader));
        }
        int program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0);
        if (GLES20.GL_FALSE == status[0]) {
            throw new RuntimeException("link program,failed:" + GLES20.glGetProgramInfoLog(program));
        }
        return program;
    }

    public static void checkGlError(String op) {
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            String msg = op + ": glError 0x" + Integer.toHexString(error);
            LogTools.d(msg);
            throw new RuntimeException(msg);
        }
    }

    public static final int NO_TEXTURE = -1;

    public static int loadTexture(final Bitmap image, final int reUseTexture) {
        int[] texture = new int[1];
        if (reUseTexture == NO_TEXTURE) {
            GLES20.glGenTextures(1, texture, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, image, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        } else {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, reUseTexture);
            GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, image);
            texture[0] = reUseTexture;
        }
        return texture[0];
    }

    public static void createFrameBuff(int[] frameBuffer, int[] frameBufferTex, int width, int height) {
        GLES20.glGenFramebuffers(1, frameBuffer, 0);
        GLES20.glGenTextures(1, frameBufferTex, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, frameBufferTex[0]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLESTools.checkGlError("createCamFrameBuff");
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
}
