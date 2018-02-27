package me.lake.librestreaming.ws;

import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;
import android.view.View;


public class AspectTextureView extends TextureView {
    public static final int MODE_FITXY = 0;
    public static final int MODE_INSIDE = 1;
    public static final int MODE_OUTSIDE = 2;
    private double targetAspect = -1;
    private int aspectMode = MODE_OUTSIDE;

    public AspectTextureView(Context context) {
        super(context);
    }

    public AspectTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AspectTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * @param mode        {@link #MODE_FITXY},{@link #MODE_INSIDE},{@link #MODE_OUTSIDE}
     * @param aspectRatio width/height
     */
    public void setAspectRatio(int mode, double aspectRatio) {
        if (mode != MODE_INSIDE && mode != MODE_OUTSIDE && mode != MODE_FITXY) {
            throw new IllegalArgumentException("illegal mode");
        }
        if (aspectRatio < 0) {
            throw new IllegalArgumentException("illegal aspect ratio");
        }
        if (targetAspect != aspectRatio || aspectMode != mode) {
            targetAspect = aspectRatio;
            aspectMode = mode;
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (targetAspect > 0) {
            int initialWidth = MeasureSpec.getSize(widthMeasureSpec);
            int initialHeight = MeasureSpec.getSize(heightMeasureSpec);

            double viewAspectRatio = (double) initialWidth / initialHeight;
            double aspectDiff = targetAspect / viewAspectRatio - 1;

            if (Math.abs(aspectDiff) > 0.01 && aspectMode != MODE_FITXY) {
                if (aspectMode == MODE_INSIDE) {
                    if (aspectDiff > 0) {
                        initialHeight = (int) (initialWidth / targetAspect);
                    } else {
                        initialWidth = (int) (initialHeight * targetAspect);
                    }
                } else if (aspectMode == MODE_OUTSIDE) {
                    if (aspectDiff > 0) {
                        initialWidth = (int) (initialHeight * targetAspect);
                    } else {
                        initialHeight = (int) (initialWidth / targetAspect);
                    }
                }
                widthMeasureSpec = MeasureSpec.makeMeasureSpec(initialWidth, MeasureSpec.EXACTLY);
                heightMeasureSpec = MeasureSpec.makeMeasureSpec(initialHeight, MeasureSpec.EXACTLY);
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public void layout(int l, int t, int r, int b) {
        View p = (View) getParent();
        if (p != null) {
            int pw = p.getMeasuredWidth();
            int ph = p.getMeasuredHeight();
            int w = getMeasuredWidth();
            int h = getMeasuredHeight();
            t = (ph - h) / 2;
            l = (pw - w) / 2;
            r += l;
            b += t;
        }
        super.layout(l, t, r, b);
    }
}
