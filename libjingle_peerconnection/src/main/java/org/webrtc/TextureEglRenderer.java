/*
 *  Copyright 2017 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package org.webrtc;

/**
 * Display the video stream on a Surface.
 * renderFrame() is asynchronous to avoid blocking the calling thread.
 * This class is thread safe and handles access from potentially three different threads:
 * Interaction from the main app in init, release and setMirror.
 * Interaction from C++ rtc::VideoSinkInterface in renderFrame.
 * Interaction from SurfaceHolder lifecycle in surfaceCreated, surfaceChanged, and surfaceDestroyed.
 */
public class TextureEglRenderer extends EglRenderer {
    private static final String TAG = "TextureEglRenderer";

    // Callback for reporting renderer events. Read-only after initilization so no lock required.
    private RendererCommon.RendererEvents rendererEvents;

    private final Object layoutLock = new Object();
    private boolean isRenderingPaused;
    private boolean isFirstFrameRendered;
    private int rotatedFrameWidth;
    private int rotatedFrameHeight;
    private int frameRotation;

    /**
     * In order to render something, you must first call init().
     */
    public TextureEglRenderer(String name) {
        super(name);
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
        synchronized (layoutLock) {
            isFirstFrameRendered = false;
            rotatedFrameWidth = 0;
            rotatedFrameHeight = 0;
            frameRotation = 0;
        }
        super.init(sharedContext, configAttributes, drawer);
    }

    @Override
    public void init(final EglBase.Context sharedContext, final int[] configAttributes,
                     RendererCommon.GlDrawer drawer) {
        init(sharedContext, null /* rendererEvents */, configAttributes, drawer);
    }

    /**
     * Limit render framerate.
     *
     * @param fps Limit render framerate to this value, or use Float.POSITIVE_INFINITY to disable fps
     *            reduction.
     */
    @Override
    public void setFpsReduction(float fps) {
        synchronized (layoutLock) {
            isRenderingPaused = fps == 0f;
        }
        super.setFpsReduction(fps);
    }

    @Override
    public void disableFpsReduction() {
        synchronized (layoutLock) {
            isRenderingPaused = false;
        }
        super.disableFpsReduction();
    }

    @Override
    public void pauseVideo() {
        synchronized (layoutLock) {
            isRenderingPaused = true;
        }
        super.pauseVideo();
    }

    // VideoSink interface.
    @Override
    public void onFrame(VideoFrame frame) {
        updateFrameDimensionsAndReportEvents(frame);
        super.onFrame(frame);
    }

    // Update frame dimensions and report any changes to |rendererEvents|.
    private void updateFrameDimensionsAndReportEvents(VideoFrame frame) {
        synchronized (layoutLock) {
            if (isRenderingPaused) {
                return;
            }
            if (!isFirstFrameRendered) {
                isFirstFrameRendered = true;
                logD("Reporting first rendered frame.");
                if (rendererEvents != null) {
                    rendererEvents.onFirstFrameRendered();
                }
            }
            if (rotatedFrameWidth != frame.getRotatedWidth()
                    || rotatedFrameHeight != frame.getRotatedHeight()
                    || frameRotation != frame.getRotation()) {
                logD("Reporting frame resolution changed to " + frame.getBuffer().getWidth() + "x"
                        + frame.getBuffer().getHeight() + " with rotation " + frame.getRotation());
                if (rendererEvents != null) {
                    rendererEvents.onFrameResolutionChanged(
                            frame.getBuffer().getWidth(), frame.getBuffer().getHeight(), frame.getRotation());
                }
                rotatedFrameWidth = frame.getRotatedWidth();
                rotatedFrameHeight = frame.getRotatedHeight();
                frameRotation = frame.getRotation();
            }
        }
    }

    private void logD(String string) {
        Logging.d(TAG, name + ": " + string);
    }
}
