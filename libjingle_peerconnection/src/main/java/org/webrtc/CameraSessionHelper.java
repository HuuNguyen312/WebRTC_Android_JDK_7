package org.webrtc;

import android.content.Context;
import android.graphics.Matrix;
import android.view.Surface;
import android.view.WindowManager;

public class CameraSessionHelper {
    static int getDeviceOrientation(Context context) {
        final WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        switch (wm.getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 270;
            case Surface.ROTATION_0:
            default:
                return 0;
        }
    }

    static VideoFrame.TextureBuffer createTextureBufferWithModifiedTransformMatrix(
            TextureBufferImpl buffer, boolean mirror, int rotation) {
        final Matrix transformMatrix = new Matrix();
        // Perform mirror and rotation around (0.5, 0.5) since that is the center of the texture.
        transformMatrix.preTranslate(/* dx= */ 0.5f, /* dy= */ 0.5f);
        if (mirror) {
            transformMatrix.preScale(/* sx= */ -1f, /* sy= */ 1f);
        }
        transformMatrix.preRotate(rotation);
        transformMatrix.preTranslate(/* dx= */ -0.5f, /* dy= */ -0.5f);

        // The width and height are not affected by rotation since Camera2Session has set them to the
        // value they should be after undoing the rotation.
        return buffer.applyTransformMatrix(transformMatrix, buffer.getWidth(), buffer.getHeight());
    }
}
