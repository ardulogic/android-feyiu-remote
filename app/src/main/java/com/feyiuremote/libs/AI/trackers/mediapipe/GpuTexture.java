package com.feyiuremote.libs.AI.trackers.mediapipe;

import android.graphics.Bitmap;

import com.google.mediapipe.framework.AppTextureFrame;
import com.google.mediapipe.framework.TextureFrame;
import com.google.mediapipe.glutil.ShaderUtil;

public class GpuTexture {

    public static TextureFrame bitmapToGpuTexture(Bitmap bmp, long tsUs) {
        // Upload the bitmap into an RGBA texture.
        int texName = ShaderUtil.createRgbaTexture(bmp);

        // 3-argument constructor: (textureName, width, height)
        AppTextureFrame f =
                new AppTextureFrame(texName, bmp.getWidth(), bmp.getHeight());

        // Now attach the graph timestamp (strongly recommended!)
        f.setTimestamp(tsUs);          // ← same value you’ll pass to the graph
        return f;
    }
}