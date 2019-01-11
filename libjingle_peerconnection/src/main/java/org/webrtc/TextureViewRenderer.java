/*
 *  Copyright 2015 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package org.webrtc;

import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;

import java.util.concurrent.CountDownLatch;

/**
 * Display the video stream on a SurfaceView.
 */
public class TextureViewRenderer extends TextureView
        implements TextureView.SurfaceTextureListener, VideoSink, RendererCommon.RendererEvents {
    private static final String TAG = "TextureViewRenderer";

    // Cached resource name.
    private final String resourceName;
    private final RendererCommon.VideoLayoutMeasure videoLayoutMeasure =
            new RendererCommon.VideoLayoutMeasure();
    private final TextureEglRenderer eglRenderer;

    // Callback for reporting renderer events. Read-only after initilization so no lock required.
    private RendererCommon.RendererEvents rendererEvents;

    // Accessed only on the main thread.
    private int rotatedFrameWidth;
    private int rotatedFrameHeight;
    private boolean enableFixedSize;
    private int surfaceWidth;
    private int surfaceHeight;

    /**
     * Standard View constructor. In order to render something, you must first call init().
     */
    public TextureViewRenderer(Context context) {
        super(context);
        this.resourceName = getResourceName();
        eglRenderer = new TextureEglRenderer(resourceName);
        setSurfaceTextureListener(this);
    }

    /**
     * Standard View constructor. In order to render something, you must first call init().
     */
    public TextureViewRenderer(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.resourceName = getResourceName();
        eglRenderer = new TextureEglRenderer(resourceName);
        setSurfaceTextureListener(this);
    }

    /**
     * Initialize this class, sharing resources with |sharedContext|. It is allowed to call init() to
     * reinitialize the renderer after a previous init()/release() cycle.
     */
    public void init(EglBase.Context sharedContext, RendererCommon.RendererEvents rendererEvents) {
        init(sharedContext, rendererEvents, EglBase.CONFIG_PLAIN, new GlRectDrawer());
    }

    /**
     * Initialize this class, sharing resources with |sharedContext|. The custom |drawer| will be used
     * for drawing frames on the EGLSurface. This class is responsible for calling release() on
     * |drawer|. It is allowed to call init() to reinitialize the renderer after a previous
     * init()/release() cycle.
     */
    public void init(final EglBase.Context sharedContext,
                     RendererCommon.RendererEvents rendererEvents, final int[] configAttributes,
                     RendererCommon.GlDrawer drawer) {
        ThreadUtils.checkIsOnMainThread();
        this.rendererEvents = rendererEvents;
        rotatedFrameWidth = 0;
        rotatedFrameHeight = 0;
        eglRenderer.init(sharedContext, this /* rendererEvents */, configAttributes, drawer);
    }

    /**
     * Block until any pending frame is returned and all GL resources released, even if an interrupt
     * occurs. If an interrupt occurs during release(), the interrupt flag will be set. This function
     * should be called before the Activity is destroyed and the EGLContext is still valid. If you
     * don't call this function, the GL resources might leak.
     */
    public void release() {
        eglRenderer.release();
    }

    /**
     * Register a callback to be invoked when a new video frame has been received.
     *
     * @param listener The callback to be invoked. The callback will be invoked on the render thread.
     *                 It should be lightweight and must not call removeFrameListener.
     * @param scale    The scale of the Bitmap passed to the callback, or 0 if no Bitmap is
     *                 required.
     */
    public void addFrameListener(
            EglRenderer.FrameListener listener, float scale, RendererCommon.GlDrawer drawerParam) {
        eglRenderer.addFrameListener(listener, scale, drawerParam);
    }

    /**
     * Register a callback to be invoked when a new video frame has been received. This version uses
     * the drawer of the EglRenderer that was passed in init.
     *
     * @param listener The callback to be invoked. The callback will be invoked on the render thread.
     *                 It should be lightweight and must not call removeFrameListener.
     * @param scale    The scale of the Bitmap passed to the callback, or 0 if no Bitmap is
     *                 required.
     */
    public void addFrameListener(EglRenderer.FrameListener listener, float scale) {
        eglRenderer.addFrameListener(listener, scale);
    }

    public void removeFrameListener(EglRenderer.FrameListener listener) {
        eglRenderer.removeFrameListener(listener);
    }

    /**
     * Enables fixed size for the surface. This provides better performance but might be buggy on some
     * devices. By default this is turned off.
     */
    public void setEnableHardwareScaler(boolean enabled) {
        ThreadUtils.checkIsOnMainThread();
        enableFixedSize = enabled;
        updateSurfaceSize();
    }

    /**
     * Set if the video stream should be mirrored or not.
     */
    public void setMirror(final boolean mirror) {
        eglRenderer.setMirror(mirror);
    }

    /**
     * Set how the video will fill the allowed layout area.
     */
    public void setScalingType(RendererCommon.ScalingType scalingType) {
        ThreadUtils.checkIsOnMainThread();
        videoLayoutMeasure.setScalingType(scalingType);
        requestLayout();
    }

    public void setScalingType(RendererCommon.ScalingType scalingTypeMatchOrientation,
                               RendererCommon.ScalingType scalingTypeMismatchOrientation) {
        ThreadUtils.checkIsOnMainThread();
        videoLayoutMeasure.setScalingType(scalingTypeMatchOrientation, scalingTypeMismatchOrientation);
        requestLayout();
    }

    /**
     * Limit render framerate.
     *
     * @param fps Limit render framerate to this value, or use Float.POSITIVE_INFINITY to disable fps
     *            reduction.
     */
    public void setFpsReduction(float fps) {
        eglRenderer.setFpsReduction(fps);
    }

    public void disableFpsReduction() {
        eglRenderer.disableFpsReduction();
    }

    public void pauseVideo() {
        eglRenderer.pauseVideo();
    }

    // VideoSink interface.
    @Override
    public void onFrame(VideoFrame frame) {
        eglRenderer.onFrame(frame);
    }

    // View layout interface.
    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        ThreadUtils.checkIsOnMainThread();
        Point size =
                videoLayoutMeasure.measure(widthSpec, heightSpec, rotatedFrameWidth, rotatedFrameHeight);
        setMeasuredDimension(size.x, size.y);
        logD("onMeasure(). New size: " + size.x + "x" + size.y);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        ThreadUtils.checkIsOnMainThread();
        eglRenderer.setLayoutAspectRatio((right - left) / (float) (bottom - top));
        updateSurfaceSize();
    }

    private void updateSurfaceSize() {
        ThreadUtils.checkIsOnMainThread();
        if (enableFixedSize && rotatedFrameWidth != 0 && rotatedFrameHeight != 0 && getWidth() != 0
                && getHeight() != 0) {
            final float layoutAspectRatio = getWidth() / (float) getHeight();
            final float frameAspectRatio = rotatedFrameWidth / (float) rotatedFrameHeight;
            final int drawnFrameWidth;
            final int drawnFrameHeight;
            if (frameAspectRatio > layoutAspectRatio) {
                drawnFrameWidth = (int) (rotatedFrameHeight * layoutAspectRatio);
                drawnFrameHeight = rotatedFrameHeight;
            } else {
                drawnFrameWidth = rotatedFrameWidth;
                drawnFrameHeight = (int) (rotatedFrameWidth / layoutAspectRatio);
            }
            // Aspect ratio of the drawn frame and the view is the same.
            final int width = Math.min(getWidth(), drawnFrameWidth);
            final int height = Math.min(getHeight(), drawnFrameHeight);
            logD("updateSurfaceSize. Layout size: " + getWidth() + "x" + getHeight() + ", frame size: "
                    + rotatedFrameWidth + "x" + rotatedFrameHeight + ", requested surface size: " + width
                    + "x" + height + ", old surface size: " + surfaceWidth + "x" + surfaceHeight);
            if (width != surfaceWidth || height != surfaceHeight) {
                surfaceWidth = width;
                surfaceHeight = height;
//        getHolder().setFixedSize(width, height);
            }
        } else {
            surfaceWidth = surfaceHeight = 0;
//      getHolder().setSizeFromLayout();
        }
    }

    private String getResourceName() {
        try {
            return getResources().getResourceEntryName(getId());
        } catch (NotFoundException e) {
            return "";
        }
    }

    /**
     * Post a task to clear the SurfaceView to a transparent uniform color.
     */
    public void clearImage() {
        eglRenderer.clearImage();
    }

    @Override
    public void onFirstFrameRendered() {
        if (rendererEvents != null) {
            rendererEvents.onFirstFrameRendered();
        }
    }

    @Override
    public void onFrameResolutionChanged(int videoWidth_, int videoHeight_, int rotation_) {
        if (rendererEvents != null) {
            rendererEvents.onFrameResolutionChanged(videoWidth_, videoHeight_, rotation_);
        }
        final int rotatedWidth = rotation_ == 0 || rotation_ == 180 ? videoWidth_ : videoHeight_;
        final int rotatedHeight = rotation_ == 0 || rotation_ == 180 ? videoHeight_ : videoWidth_;
        // run immediately if possible for ui thread tests
        postOrRun(new Runnable() {
            @Override
            public void run() {
                rotatedFrameWidth = rotatedWidth;
                rotatedFrameHeight = rotatedHeight;
                updateSurfaceSize();
                requestLayout();
            }
        });
    }

    private void postOrRun(Runnable r) {
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            r.run();
        } else {
            post(r);
        }
    }

    private void logD(String string) {
        Logging.d(TAG, resourceName + ": " + string);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        ThreadUtils.checkIsOnMainThread();
        logD("onSurfaceTextureAvailable: size: " + width + "x" + height);
        eglRenderer.createEglSurface(surfaceTexture);
        surfaceWidth = width;
        surfaceHeight = height;
        updateSurfaceSize();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
        ThreadUtils.checkIsOnMainThread();
        logD( "surfaceChanged: size: " + width + "x" + height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        ThreadUtils.checkIsOnMainThread();
        final CountDownLatch completionLatch = new CountDownLatch(1);
        eglRenderer.releaseEglSurface(new Runnable() {
            @Override
            public void run() {
                completionLatch.countDown();
            }
        });
        ThreadUtils.awaitUninterruptibly(completionLatch);
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        ThreadUtils.checkIsOnMainThread();
        logD("onSurfaceTextureUpdated");
    }

    // SurfaceHolder.Callback interface.
  /*@Override
  public void surfaceCreated(final SurfaceHolder holder) {
    ThreadUtils.checkIsOnMainThread();
    surfaceWidth = surfaceHeight = 0;
    updateSurfaceSize();
  }

  @Override
  public void surfaceDestroyed(SurfaceHolder holder) {}

  @Override
  public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}*/
}
