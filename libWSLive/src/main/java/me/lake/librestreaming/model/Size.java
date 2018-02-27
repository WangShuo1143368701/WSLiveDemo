package me.lake.librestreaming.model;


public final class Size {
    /**
     * Create a new immutable Size instance.
     *
     * @param width  The width of the size, in pixels
     * @param height The height of the size, in pixels
     */
    public Size(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    /**
     * Get the width of the size (in pixels).
     *
     * @return width
     */
    public int getWidth() {
        return mWidth;
    }

    /**
     * Get the height of the size (in pixels).
     *
     * @return height
     */
    public int getHeight() {
        return mHeight;
    }

    /**
     * Check if this size is equal to another size.
     * <p>
     * Two sizes are equal if and only if both their widths and heights are
     * equal.
     * </p>
     * <p>
     * A size object is never equal to any other type of object.
     * </p>
     *
     * @return {@code true} if the objects were equal, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (obj instanceof Size) {
            Size other = (Size) obj;
            return mWidth == other.mWidth && mHeight == other.mHeight;
        }
        return false;
    }

    /**
     * Return the size represented as a string with the format {@code "WxH"}
     *
     * @return string representation of the size
     */
    @Override
    public String toString() {
        return mWidth + "x" + mHeight;
    }

    private static NumberFormatException invalidSize(String s) {
        throw new NumberFormatException("Invalid Size: \"" + s + "\"");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        // assuming most sizes are <2^16, doing a rotate will give us perfect hashing
        return mHeight ^ ((mWidth << (Integer.SIZE / 2)) | (mWidth >>> (Integer.SIZE / 2)));
    }

    private final int mWidth;
    private final int mHeight;
}
