package com.winlator.renderer;

import androidx.annotation.Keep;

import com.winlator.xserver.Drawable;

import java.nio.ByteBuffer;

public class GPUImage extends Texture {
    private long hardwareBufferPtr;
    private long imageKHRPtr;
    private ByteBuffer virtualData;
    private short stride;
    private static boolean supported = false;
    private boolean locked = false;
    private int nativeHandle;

    static {
        System.loadLibrary("winlator7");
    }

    public GPUImage(short width, short height) {
        this(width, height, true, true);
    }

    public GPUImage(short width, short height, boolean cpuAccess, boolean format) {
        hardwareBufferPtr = createHardwareBuffer(width, height, cpuAccess, format);
        if (cpuAccess && hardwareBufferPtr != 0) {
            lockHardwareBuffer(hardwareBufferPtr);
            locked = true;
        }
    }

    public long getHardwareBufferPtr() {
        return hardwareBufferPtr;
    }

    @Override
    public void allocateTexture(short width, short height, ByteBuffer data) {
        if (isAllocated()) return;
        super.allocateTexture(width, height, null);
        imageKHRPtr = createImageKHR(hardwareBufferPtr, textureId);
    }

    @Override
    public void updateFromDrawable(Drawable drawable) {
        if (!isAllocated()) allocateTexture(drawable.width, drawable.height, null);
        needsUpdate = false;
    }

    public short getStride() {
        return stride;
    }

    @Keep
    private void setStride(short stride) {
        this.stride = stride;
    }

    public ByteBuffer getVirtualData() {
        return virtualData;
    }

    @Override
    public void destroy() {
        destroyImageKHR(imageKHRPtr);
        destroyHardwareBuffer(hardwareBufferPtr, locked);
        virtualData = null;
        imageKHRPtr = 0;
        hardwareBufferPtr = 0;
        super.destroy();
    }

    public static boolean isSupported() {
        return supported;
    }

    public static void checkIsSupported() {
        final short size = 8;
        GPUImage gpuImage = new GPUImage(size, size);
        gpuImage.allocateTexture(size, size, null);
        supported = gpuImage.hardwareBufferPtr != 0 && gpuImage.imageKHRPtr != 0 && gpuImage.virtualData != null;
        gpuImage.destroy();
    }

    @Keep
    private void setNativeHandle(int nativeHandle) {
        this.nativeHandle = nativeHandle;
    }

    public int getNativeHandle() {
        return nativeHandle;
    }

    private native long createHardwareBuffer(short width, short height, boolean cpuAccess, boolean format);

    private native void destroyHardwareBuffer(long hardwareBufferPtr, boolean locked);

    private native ByteBuffer lockHardwareBuffer(long hardwareBufferPtr);

    private native long createImageKHR(long hardwareBufferPtr, int textureId);

    private native void destroyImageKHR(long imageKHRPtr);
}