package org.webrtc;

import javax.annotation.Nullable;

import static org.webrtc.EglBase.CONFIG_PLAIN;

public class EglBaseHelper {
    /**
     * Create a new context with the specified config attributes, sharing data with |sharedContext|.
     * If |sharedContext| is null, a root context is created. This function will try to create an EGL
     * 1.4 context if possible, and an EGL 1.0 context otherwise.
     */
    public static EglBase create(@Nullable EglBase.Context sharedContext, int[] configAttributes) {
        return (EglBase14.isEGL14Supported()
                && (sharedContext == null || sharedContext instanceof EglBase14.Context))
                ? new EglBase14((EglBase14.Context) sharedContext, configAttributes)
                : new EglBase10((EglBase10.Context) sharedContext, configAttributes);
    }

    /**
     * Helper function for creating a plain root context. This function will try to create an EGL 1.4
     * context if possible, and an EGL 1.0 context otherwise.
     */
    public static EglBase create() {
        return create(null /* shaderContext */, CONFIG_PLAIN);
    }

    /**
     * Helper function for creating a plain context, sharing data with |sharedContext|. This function
     * will try to create an EGL 1.4 context if possible, and an EGL 1.0 context otherwise.
     */
    public static EglBase create(EglBase.Context sharedContext) {
        return create(sharedContext, CONFIG_PLAIN);
    }

    /**
     * Explicitly create a root EGl 1.0 context with the specified config attributes.
     */
    public static EglBase createEgl10(int[] configAttributes) {
        return new EglBase10(null /* shaderContext */, configAttributes);
    }

    /**
     * Explicitly create a root EGl 1.0 context with the specified config attributes
     * and shared context.
     */
    public static EglBase createEgl10(
            javax.microedition.khronos.egl.EGLContext sharedContext, int[] configAttributes) {
        return new EglBase10(new EglBase10.Context(sharedContext), configAttributes);
    }

    /**
     * Explicitly create a root EGl 1.4 context with the specified config attributes.
     */
    public static EglBase createEgl14(int[] configAttributes) {
        return new EglBase14(null /* shaderContext */, configAttributes);
    }

    /**
     * Explicitly create a root EGl 1.4 context with the specified config attributes
     * and shared context.
     */
    public static EglBase createEgl14(
            android.opengl.EGLContext sharedContext, int[] configAttributes) {
        return new EglBase14(new EglBase14.Context(sharedContext), configAttributes);
    }
}
