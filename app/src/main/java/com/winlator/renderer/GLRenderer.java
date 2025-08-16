package com.winlator.renderer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import com.winlator.R;
import com.winlator.XrActivity;
import com.winlator.math.Mathf;
import com.winlator.math.XForm;
import com.winlator.renderer.material.CursorMaterial;
import com.winlator.renderer.material.ShaderMaterial;
import com.winlator.renderer.material.WindowMaterial;
import com.winlator.widget.XServerView;
import com.winlator.xserver.Bitmask;
import com.winlator.xserver.Cursor;
import com.winlator.xserver.Drawable;
import com.winlator.xserver.Pointer;
import com.winlator.xserver.Window;
import com.winlator.xserver.WindowAttributes;
import com.winlator.xserver.WindowManager;
import com.winlator.xserver.XLock;
import com.winlator.xserver.XServer;

import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

    public class GLRenderer implements GLSurfaceView.Renderer {
    // existing fields...
    private final XServer xServer;
    private final XServerView xServerView;
    private final List<RenderableWindow> renderableWindows = new ArrayList<>();
    private boolean cursorVisible;
    private boolean screenOffsetYRelativeToCursor;
    private String forceFullscreenWMClass;
    private String[] unviewableWMClasses;
    private boolean fullscreen;
    private float magnifierZoom;

    // GL resources
    private int cursorTextureId = 0;
    private boolean glResourcesReady = false;

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0f, 0f, 0f, 1f);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // Context recreate safety: rebuild textures, FBOs, shaders
        initGLResources();
        glResourcesReady = true;
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        if (!glResourcesReady) {
            initGLResources();
            glResourcesReady = true;
        }

        // ... existing draw logic
    }

    private void initGLResources() {
        destroyGLResources();

        // Example: create cursor texture
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeResource(
                xServerView.getResources(),
                R.drawable.cursor,
                options
        );
        cursorTextureId = GLUtil.loadTexture(bitmap);
        bitmap.recycle();

        // Init other GL assets like shaders, VBOs, FBOs here
    }

    private void destroyGLResources() {
        if (cursorTextureId != 0) {
            int[] tex = {cursorTextureId};
            GLES20.glDeleteTextures(1, tex, 0);
            cursorTextureId = 0;
        }
        // Delete other GL resources here
    }

    public void onPause() {
        // Called from GLSurfaceView.Renderer hosting view's onPause
        destroyGLResources();
        glResourcesReady = false;
    }

    public void onResume() {
        // Surface will recreate resources in onSurfaceCreated
    }

    private Drawable createRootCursorDrawable() {
        Context context = xServerView.getContext().getApplicationContext();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.cursor, options);
        try {
            return Drawable.fromBitmap(bitmap);
        } finally {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
    }

    private void updateScene() {
        renderableWindows.clear();
        try (XLock lock = xServer.lock(
                XServer.Lockable.WINDOW_MANAGER,
                XServer.Lockable.DRAWABLE_MANAGER)) {
            collectRenderableWindows(
                xServer.windowManager.rootWindow,
                xServer.windowManager.rootWindow.getX(),
                xServer.windowManager.rootWindow.getY()
            );
        }
    }

    private void collectRenderableWindows(Window window, int x, int y) {
        if (!window.attributes.isMapped()) return;

        if (window == xServer.windowManager.rootWindow) {
            for (Window child : window.getChildren()) {
                collectRenderableWindows(child, child.getX() + x, child.getY() + y);
            }
            return;
        }

        if (unviewableWMClasses != null) {
            String wmClass = window.getClassName();
            for (String unviewableWMClass : unviewableWMClasses) {
                if (wmClass.contains(unviewableWMClass)) {
                    if (window.attributes.isEnabled()) {
                        window.disableAllDescendants();
                    }
                    return;
                }
            }
        }

        boolean forceFullscreen = false;
        if (forceFullscreenWMClass != null) {
            short width = window.getWidth();
            short height = window.getHeight();
            if (width >= 320 && height >= 200 &&
                width < xServer.screenInfo.width && height < xServer.screenInfo.height) {

                Window parent = window.getParent();
                boolean parentHasWMClass = parent.getClassName().contains(forceFullscreenWMClass);
                boolean hasWMClass = window.getClassName().contains(forceFullscreenWMClass);

                if (hasWMClass) {
                    forceFullscreen = !parentHasWMClass && window.getChildCount() == 0;
                } else {
                    short borderX = (short)(parent.getWidth() - width);
                    short borderY = (short)(parent.getHeight() - height);
                    if (parent.getChildCount() == 1 &&
                        borderX > 0 && borderY > 0 && borderX <= 12) {
                        forceFullscreen = true;
                        removeRenderableWindow(parent);
                    }
                }
            }
        }

        renderableWindows.add(
            new RenderableWindow(window.getContent(), x, y, forceFullscreen)
        );

        for (Window child : window.getChildren()) {
            collectRenderableWindows(child, child.getX() + x, child.getY() + y);
        }
    }

    private void removeRenderableWindow(Window window) {
        Drawable content = window.getContent();
        for (int i = 0, size = renderableWindows.size(); i < size; i++) {
            if (renderableWindows.get(i).content == content) {
                renderableWindows.remove(i);
                return;
            }
        }
    }

    private void updateWindowPosition(Window window) {
        for (RenderableWindow renderableWindow : renderableWindows) {
            if (renderableWindow.content == window.getContent()) {
                renderableWindow.rootX = window.getRootX();
                renderableWindow.rootY = window.getRootY();
                break;
            }
        }
    }

    public void setCursorVisible(boolean cursorVisible) {
        if (this.cursorVisible != cursorVisible) {
            this.cursorVisible = cursorVisible;
            xServerView.requestRender();
        }
    }

    public boolean isCursorVisible() {
        return cursorVisible;
    }

    public boolean isScreenOffsetYRelativeToCursor() {
        return screenOffsetYRelativeToCursor;
    }

    public void setScreenOffsetYRelativeToCursor(boolean value) {
        if (this.screenOffsetYRelativeToCursor != value) {
            this.screenOffsetYRelativeToCursor = value;
            xServerView.requestRender();
        }
    }

    public String getForceFullscreenWMClass() {
        return forceFullscreenWMClass;
    }

    public void setForceFullscreenWMClass(String forceFullscreenWMClass) {
        this.forceFullscreenWMClass = forceFullscreenWMClass;
    }

    public String[] getUnviewableWMClasses() {
        return unviewableWMClasses;
    }

    public void setUnviewableWMClasses(String... unviewableWMNames) {
        this.unviewableWMClasses = unviewableWMNames;
    }

    public boolean isFullscreen() {
        return fullscreen;
    }

    public float getMagnifierZoom() {
        return magnifierZoom;
    }

    public void setMagnifierZoom(float zoom) {
        if (this.magnifierZoom != zoom) {
            this.magnifierZoom = zoom;
            xServerView.requestRender();
        }
    }
}
