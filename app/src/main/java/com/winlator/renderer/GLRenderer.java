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

public class GLRenderer implements GLSurfaceView.Renderer, WindowManager.OnWindowModificationListener, Pointer.OnPointerMotionListener {
    public final XServerView xServerView;
    private final XServer xServer;
    private final VertexAttribute quadVertices = new VertexAttribute("position", 2);
    private final float[] tmpXForm1 = XForm.getInstance();
    private final float[] tmpXForm2 = XForm.getInstance();
    private final CursorMaterial cursorMaterial = new CursorMaterial();
    private final WindowMaterial windowMaterial = new WindowMaterial();
    public final ViewTransformation viewTransformation = new ViewTransformation();
    private final Drawable rootCursorDrawable;
    private final ArrayList<RenderableWindow> renderableWindows = new ArrayList<>();
    private String forceFullscreenWMClass = null;
    private boolean fullscreen = false;
    private boolean toggleFullscreen = false;
    private boolean viewportNeedsUpdate = true;
    private boolean cursorVisible = true;
    private boolean rootWindowDownsized = false;
    private boolean screenOffsetYRelativeToCursor = false;
    private String[] unviewableWMClasses = null;
    private float magnifierZoom = 1.0f;
    private boolean magnifierEnabled = true;
    private int surfaceWidth;
    private int surfaceHeight;

    public GLRenderer(XServerView xServerView, XServer xServer) {
        this.xServerView = xServerView;
        this.xServer = xServer;
        rootCursorDrawable = createRootCursorDrawable();

        quadVertices.put(new float[]{
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            1.0f, 1.0f
        });

        xServer.windowManager.addOnWindowModificationListener(this);
        xServer.pointer.addOnPointerMotionListener(this);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glDisable(GLES20.GL_DITHER);
        GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);

        GLES20.glFrontFace(GLES20.GL_CCW);
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthMask(false);

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        int w = width;
        int h = height;

        if (XrActivity.isEnabled(null)) {
            XrActivity activity = XrActivity.getInstance();
            activity.init();
            w = activity.getWidth();
            h = activity.getHeight();
            magnifierEnabled = false;
        }

        surfaceWidth = w;
        surfaceHeight = h;
        GLES20.glViewport(0, 0, w, h);
        viewTransformation.update(w, h, xServer.screenInfo.width, xServer.screenInfo.height);
        viewportNeedsUpdate = true;
    }

@Override
    public void onDrawFrame(GL10 gl) {
        if (toggleFullscreen) {
            fullscreen = !fullscreen;
            toggleFullscreen = false;
            viewportNeedsUpdate = true;
        }

        drawFrame();
    }

    private void drawFrame() {
        boolean xrFrame = false;
        boolean xrImmersive = false;
        if (XrActivity.isEnabled(null)) {
            xrImmersive = XrActivity.getImmersive();
            xrFrame = XrActivity.getInstance().beginFrame(xrImmersive, XrActivity.getSBS());
        }

        if (viewportNeedsUpdate && magnifierEnabled) {
            if (fullscreen) {
                GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight);
            } else {
                GLES20.glViewport(viewTransformation.viewOffsetX, viewTransformation.viewOffsetY,
                        viewTransformation.viewWidth, viewTransformation.viewHeight);
            }
            viewportNeedsUpdate = false;
        }

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        if (magnifierEnabled) {
            float pointerX = 0;
            float pointerY = 0;
            float zoom = !screenOffsetYRelativeToCursor ? magnifierZoom : 1.0f;

            if (zoom != 1.0f) {
                pointerX = Mathf.clamp(xServer.pointer.getX() * zoom - xServer.screenInfo.width * 0.5f,
                        0, xServer.screenInfo.width * Math.abs(1.0f - zoom));
            }

            if (screenOffsetYRelativeToCursor || zoom != 1.0f) {
                float scaleY = zoom != 1.0f ? Math.abs(1.0f - zoom) : 0.5f;
                float offsetY = xServer.screenInfo.height * (screenOffsetYRelativeToCursor ? 0.25f : 0.5f);
                pointerY = Mathf.clamp(xServer.pointer.getY() * zoom - offsetY,
                        0, xServer.screenInfo.height * scaleY);
            }

            XForm.makeTransform(tmpXForm2, -pointerX, -pointerY, zoom, zoom, 0);
        } else {
            if (!fullscreen) {
                int pointerY = 0;
                if (screenOffsetYRelativeToCursor) {
                    short halfScreenHeight = (short)(xServer.screenInfo.height / 2);
                    pointerY = Mathf.clamp(xServer.pointer.getY() - halfScreenHeight / 2,
                            0, halfScreenHeight);
                }

                XForm.makeTransform(tmpXForm2,
                        viewTransformation.sceneOffsetX,
                        viewTransformation.sceneOffsetY - pointerY,
                        viewTransformation.sceneScaleX,
                        viewTransformation.sceneScaleY, 0);

                GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
                GLES20.glScissor(viewTransformation.viewOffsetX, viewTransformation.viewOffsetY,
                        viewTransformation.viewWidth, viewTransformation.viewHeight);
            } else {
                XForm.identity(tmpXForm2);
            }
        }

        renderWindows(xrImmersive);
        if (cursorVisible && !rootWindowDownsized) {
            renderCursor();
        }

        if (!magnifierEnabled && !fullscreen) {
            GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
        }

        if (xrFrame) {
            XrActivity.getInstance().endFrame();
            XrActivity.updateControllers();
            xServerView.requestRender();
        }
    }

@Override
    public void onMapWindow(Window window) {
        xServerView.queueEvent(this::updateScene);
        xServerView.requestRender();
    }

    @Override
    public void onUnmapWindow(Window window) {
        xServerView.queueEvent(this::updateScene);
        xServerView.requestRender();
    }

    @Override
    public void onChangeWindowZOrder(Window window) {
        xServerView.queueEvent(this::updateScene);
        xServerView.requestRender();
    }

    @Override
    public void onUpdateWindowContent(Window window) {
        xServerView.requestRender();
    }

    @Override
    public void onUpdateWindowGeometry(final Window window, boolean resized) {
        if (resized) {
            xServerView.queueEvent(this::updateScene);
        } else {
            xServerView.queueEvent(() -> updateWindowPosition(window));
        }
        xServerView.requestRender();
    }

    @Override
    public void onUpdateWindowAttributes(Window window, Bitmask mask) {
        if (mask.isSet(WindowAttributes.FLAG_CURSOR)) {
            xServerView.requestRender();
        }
    }

    @Override
    public void onPointerMove(short x, short y) {
        xServerView.requestRender();
    }

    private void renderDrawable(Drawable drawable, int x, int y, ShaderMaterial material) {
        renderDrawable(drawable, x, y, material, false);
    }

    private void renderDrawable(Drawable drawable, int x, int y, ShaderMaterial material, boolean forceFullscreen) {
        synchronized (drawable.renderLock) {
            Texture texture = drawable.getTexture();
            if (drawable.isDirty()) {
                texture.updateFromDrawable(drawable);
                drawable.clearDirty();
            }

            if (forceFullscreen) {
                short newHeight = (short)Math.min(xServer.screenInfo.height,
                        ((float)xServer.screenInfo.width / drawable.width) * drawable.height);
                short newWidth = (short)(((float)newHeight / drawable.height) * drawable.width);
                XForm.set(tmpXForm1,
                        (xServer.screenInfo.width - newWidth) * 0.5f,
                        (xServer.screenInfo.height - newHeight) * 0.5f,
                        newWidth, newHeight);
            } else {
                XForm.set(tmpXForm1, x, y, drawable.width, drawable.height);
            }

            XForm.multiply(tmpXForm1, tmpXForm1, tmpXForm2);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture.getTextureId());
            GLES20.glUniform1i(material.getUniformLocation("texture"), 0);
            GLES20.glUniform1fv(material.getUniformLocation("xform"), tmpXForm1.length, tmpXForm1, 0);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, quadVertices.count());
        }
    }

    private void renderWindows(boolean forceFullscreen) {
        windowMaterial.use();
        GLES20.glUniform2f(windowMaterial.getUniformLocation("viewSize"),
                xServer.screenInfo.width, xServer.screenInfo.height);
        quadVertices.bind(windowMaterial.programId);

        boolean singleWindow = forceFullscreen;
        try (XLock lock = xServer.lock(XServer.Lockable.DRAWABLE_MANAGER)) {
            rootWindowDownsized = false;
            if (fullscreen && !renderableWindows.isEmpty()) {
                RenderableWindow root = renderableWindows.get(0);
                if ((root.content.width < xServer.screenInfo.width) ||
                    (root.content.height < xServer.screenInfo.height)) {
                    rootWindowDownsized = true;
                    singleWindow = true;
                }
            }

            if (!renderableWindows.isEmpty()) {
                if (singleWindow) {
                    RenderableWindow win = renderableWindows.get(renderableWindows.size() - 1);
                    renderDrawable(win.content, win.rootX, win.rootY, windowMaterial, true);
                } else {
                    for (RenderableWindow win : renderableWindows) {
                        renderDrawable(win.content, win.rootX, win.rootY, windowMaterial, win.forceFullscreen);
                    }
                }
            }
        }

        quadVertices.disable();
    }

    private void renderCursor() {
        cursorMaterial.use();
        GLES20.glUniform2f(cursorMaterial.getUniformLocation("viewSize"),
                xServer.screenInfo.width, xServer.screenInfo.height);
        quadVertices.bind(cursorMaterial.programId);

        try (XLock lock = xServer.lock(XServer.Lockable.DRAWABLE_MANAGER)) {
            Window pointWindow = xServer.inputDeviceManager.getPointWindow();
            Cursor cursor = pointWindow != null ? pointWindow.attributes.getCursor() : null;
            short x = xServer.pointer.getClampedX();
            short y = xServer.pointer.getClampedY();

            if (cursor != null && cursor.isVisible()) {
                renderDrawable(cursor.cursorImage, x - cursor.hotSpotX, y - cursor.hotSpotY, cursorMaterial);
            } else {
                renderDrawable(rootCursorDrawable, x, y, cursorMaterial);
            }
        }

        quadVertices.disable();
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
        try (XLock lock = xServer.lock(XServer.Lockable.WINDOW_MANAGER, XServer.Lockable.DRAWABLE_MANAGER)) {
            collectRenderableWindows(xServer.windowManager.rootWindow,
                    xServer.windowManager.rootWindow.getX(),
                    xServer.windowManager.rootWindow.getY());
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

        renderableWindows.add(new RenderableWindow(window.getContent(), x, y, forceFullscreen));

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

    public void toggleFullscreen() {
        toggleFullscreen = true;
        xServerView.requestRender();
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
