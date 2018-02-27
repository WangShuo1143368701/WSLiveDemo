package me.lake.librestreaming.encoder.utils;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Helper class to draw to whole view using specific texture and texture matrix
 */
public class GLDrawer2D {
	private static final boolean DEBUG = false; // TODO set false on release
	private static final String TAG = "GLDrawer2D";

	private static final String vss
		= "uniform mat4 uMVPMatrix;\n"
		+ "uniform mat4 uTexMatrix;\n"
		+ "attribute highp vec4 aPosition;\n"
		+ "attribute highp vec4 aTextureCoord;\n"
		+ "varying highp vec2 vTextureCoord;\n"
		+ "\n"
		+ "void main() {\n"
		+ "	gl_Position = uMVPMatrix * aPosition;\n"
		+ "	vTextureCoord = (uTexMatrix * aTextureCoord).xy;\n"
		+ "}\n";
	private static final String fss
		= "precision mediump float;\n"
		+ "uniform sampler2D sTexture;\n"
		+ "varying highp vec2 vTextureCoord;\n"
		+ "void main() {\n"
		+ "  gl_FragColor = texture2D(sTexture, vTextureCoord);\n"
		+ "}";
	private static final float[] VERTICES = { 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, -1.0f };
	//private static final float[] TEXCOORD = { 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f };
	private static final float[] TEXCOORD = { 1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f };

	private final FloatBuffer pVertex;
	private final FloatBuffer pTexCoord;
	private int hProgram;
    int maPositionLoc;
    int maTextureCoordLoc;
    int muMVPMatrixLoc;
    int muTexMatrixLoc;
	private final float[] mMvpMatrix = new float[16];

	private static final int FLOAT_SZ = Float.SIZE / 8;
	private static final int VERTEX_NUM = 4;
	private static final int VERTEX_SZ = VERTEX_NUM * 2;
	/**
	 * Constructor
	 * this should be called in GL context
	 */
	public GLDrawer2D() {
		pVertex = ByteBuffer.allocateDirect(VERTEX_SZ * FLOAT_SZ)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		pVertex.put(VERTICES);
		pVertex.flip();
		pTexCoord = ByteBuffer.allocateDirect(VERTEX_SZ * FLOAT_SZ)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		pTexCoord.put(TEXCOORD);
		pTexCoord.flip();

		hProgram = loadShader(vss, fss);
		GLES20.glUseProgram(hProgram);
        maPositionLoc = GLES20.glGetAttribLocation(hProgram, "aPosition");
        maTextureCoordLoc = GLES20.glGetAttribLocation(hProgram, "aTextureCoord");
        muMVPMatrixLoc = GLES20.glGetUniformLocation(hProgram, "uMVPMatrix");
        muTexMatrixLoc = GLES20.glGetUniformLocation(hProgram, "uTexMatrix");

		Matrix.setIdentityM(mMvpMatrix, 0);
        GLES20.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mMvpMatrix, 0);
        GLES20.glUniformMatrix4fv(muTexMatrixLoc, 1, false, mMvpMatrix, 0);
		GLES20.glVertexAttribPointer(maPositionLoc, 2, GLES20.GL_FLOAT, false, VERTEX_SZ, pVertex);
		GLES20.glVertexAttribPointer(maTextureCoordLoc, 2, GLES20.GL_FLOAT, false, VERTEX_SZ, pTexCoord);
		GLES20.glEnableVertexAttribArray(maPositionLoc);
		GLES20.glEnableVertexAttribArray(maTextureCoordLoc);
	}

	/**
	 * terminatinng, this should be called in GL context
	 */
	public void release() {
		if (hProgram >= 0)
			GLES20.glDeleteProgram(hProgram);
		hProgram = -1;
	}

	/**
	 * draw specific texture with specific texture matrix
	 * @param tex_id texture ID
	 * @param tex_matrix texture matrix、if this is null, the last one use(we don't check size of this array and needs at least 16 of float)
	 */
	public void draw(final int tex_id, final float[] tex_matrix) {
		GLES20.glUseProgram(hProgram);
		if (tex_matrix != null)
			GLES20.glUniformMatrix4fv(muTexMatrixLoc, 1, false, tex_matrix, 0);
        GLES20.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mMvpMatrix, 0);
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex_id);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, VERTEX_NUM);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glUseProgram(0);
	}

	/**
	 * Set model/view/projection transform matrix
	 * @param matrix
	 * @param offset
	 */
	public void setMatrix(final float[] matrix, final int offset) {
		if ((matrix != null) && (matrix.length >= offset + 16)) {
			System.arraycopy(matrix, offset, mMvpMatrix, 0, 16);
		} else {
			Matrix.setIdentityM(mMvpMatrix, 0);
		}
	}
	/**
	 * create external texture
	 * @return texture ID
	 */
	public static int initTex() {
		if (DEBUG) Log.v(TAG, "initTex:");
		final int[] tex = new int[1];
		GLES20.glGenTextures(1, tex, 0);
		GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, tex[0]);
		GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
				GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
				GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
				GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
		GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
				GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
		return tex[0];
	}

	/**
	 * delete specific texture
	 */
	public static void deleteTex(final int hTex) {
		if (DEBUG) Log.v(TAG, "deleteTex:");
		final int[] tex = new int[] {hTex};
		GLES20.glDeleteTextures(1, tex, 0);
	}

	/**
	 * load, compile and link shader
	 * @param vss source of vertex shader
	 * @param fss source of fragment shader
	 * @return
	 */
	public static int loadShader(final String vss, final String fss) {
		if (DEBUG) Log.v(TAG, "loadShader:");
		int vs = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
		GLES20.glShaderSource(vs, vss);
		GLES20.glCompileShader(vs);
		final int[] compiled = new int[1];
		GLES20.glGetShaderiv(vs, GLES20.GL_COMPILE_STATUS, compiled, 0);
		if (compiled[0] == 0) {
			if (DEBUG) Log.e(TAG, "Failed to compile vertex shader:"
					+ GLES20.glGetShaderInfoLog(vs));
			GLES20.glDeleteShader(vs);
			vs = 0;
		}

		int fs = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
		GLES20.glShaderSource(fs, fss);
		GLES20.glCompileShader(fs);
		GLES20.glGetShaderiv(fs, GLES20.GL_COMPILE_STATUS, compiled, 0);
		if (compiled[0] == 0) {
			if (DEBUG) Log.w(TAG, "Failed to compile fragment shader:"
				+ GLES20.glGetShaderInfoLog(fs));
			GLES20.glDeleteShader(fs);
			fs = 0;
		}

		final int program = GLES20.glCreateProgram();
		GLES20.glAttachShader(program, vs);
		GLES20.glAttachShader(program, fs);
		GLES20.glLinkProgram(program);

		return program;
	}

}
