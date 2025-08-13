package com.rapid.prominence.minty.ei;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapResource;

import java.security.MessageDigest;

public class CornerTransform implements Transformation<Bitmap> {

    private BitmapPool mBitmapPool;

    private float radius;

    private boolean isLeftTop, isRightTop, isLeftBottom, isRightBotoom;

    /**
     * 闇€瑕佽缃渾瑙掔殑閮ㄥ垎
     *
     * @param leftTop     宸︿笂瑙?
     * @param rightTop    鍙充笂瑙?
     * @param leftBottom  宸︿笅瑙?
     * @param rightBottom 鍙充笅瑙?
     */
    public void setNeedCorner(boolean leftTop, boolean rightTop, boolean leftBottom, boolean rightBottom) {
        isLeftTop = leftTop;
        isRightTop = rightTop;
        isLeftBottom = leftBottom;
        isRightBotoom = rightBottom;
    }

    /**
     * @param context 涓婁笅鏂?
     * @param radius  鍦嗚骞呭害
     */
    public CornerTransform(Context context, float radius) {
        this.mBitmapPool = Glide.get(context).getBitmapPool();
        this.radius = radius;
    }

    @NonNull
    @Override
    public Resource<Bitmap> transform(@NonNull Context context, @NonNull Resource<Bitmap> resource, int outWidth, int outHeight) {

        Bitmap source = resource.get();
        int finalWidth, finalHeight;
        //杈撳嚭鐩爣鐨勫楂樻垨楂樺姣斾緥
        float scale;
        if (outWidth > outHeight) {
            //濡傛灉 杈撳嚭瀹藉害 > 杈撳嚭楂樺害 姹傞珮瀹芥瘮

            scale = (float) outHeight / (float) outWidth;
            finalWidth = source.getWidth();
            //鍥哄畾鍘熷浘瀹藉害,姹傛渶缁堥珮搴?
            finalHeight = (int) ((float) source.getWidth() * scale);
            if (finalHeight > source.getHeight()) {
                //濡傛灉 姹傚嚭鐨勬渶缁堥珮搴?> 鍘熷浘楂樺害 姹傚楂樻瘮

                scale = (float) outWidth / (float) outHeight;
                finalHeight = source.getHeight();
                //鍥哄畾鍘熷浘楂樺害,姹傛渶缁堝搴?
                finalWidth = (int) ((float) source.getHeight() * scale);
            }
        } else if (outWidth < outHeight) {
            //濡傛灉 杈撳嚭瀹藉害 < 杈撳嚭楂樺害 姹傚楂樻瘮

            scale = (float) outWidth / (float) outHeight;
            finalHeight = source.getHeight();
            //鍥哄畾鍘熷浘楂樺害,姹傛渶缁堝搴?
            finalWidth = (int) ((float) source.getHeight() * scale);
            if (finalWidth > source.getWidth()) {
                //濡傛灉 姹傚嚭鐨勬渶缁堝搴?> 鍘熷浘瀹藉害 姹傞珮瀹芥瘮

                scale = (float) outHeight / (float) outWidth;
                finalWidth = source.getWidth();
                finalHeight = (int) ((float) source.getWidth() * scale);
            }
        } else {
            //濡傛灉 杈撳嚭瀹藉害=杈撳嚭楂樺害
            finalHeight = source.getHeight();
            finalWidth = finalHeight;
        }

        //淇鍦嗚
        this.radius *= (float) finalHeight / (float) outHeight;
        Bitmap outBitmap = this.mBitmapPool.get(finalWidth, finalHeight, Bitmap.Config.ARGB_8888);
        if (outBitmap == null) {
            outBitmap = Bitmap.createBitmap(finalWidth, finalHeight, Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(outBitmap);
        Paint paint = new Paint();
        //鍏宠仈鐢荤瑪缁樺埗鐨勫師鍥綽itmap
        BitmapShader shader = new BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        //璁＄畻涓績浣嶇疆,杩涜鍋忕Щ
        int width = (source.getWidth() - finalWidth) / 2;
        int height = (source.getHeight() - finalHeight) / 2;
        if (width != 0 || height != 0) {
            Matrix matrix = new Matrix();
            matrix.setTranslate((float) (-width), (float) (-height));
            shader.setLocalMatrix(matrix);
        }

        paint.setShader(shader);
        paint.setAntiAlias(true);
        RectF rectF = new RectF(0.0F, 0.0F, (float) canvas.getWidth(), (float) canvas.getHeight());
        //鍏堢粯鍒跺渾瑙掔煩褰?
        canvas.drawRoundRect(rectF, this.radius, this.radius, paint);

        //宸︿笂瑙掑渾瑙?
        if (!isLeftTop) {
            canvas.drawRect(0, 0, radius, radius, paint);
        }
        //鍙充笂瑙掑渾瑙?
        if (!isRightTop) {
            canvas.drawRect(canvas.getWidth() - radius, 0, canvas.getWidth(), radius, paint);
        }
        //宸︿笅瑙掑渾瑙?
        if (!isLeftBottom) {
            canvas.drawRect(0, canvas.getHeight() - radius, radius, canvas.getHeight(), paint);
        }
        //鍙充笅瑙掑渾瑙?
        if (!isRightBotoom) {
            canvas.drawRect(canvas.getWidth() - radius, canvas.getHeight() - radius, canvas.getWidth(), canvas.getHeight(), paint);
        }

        return BitmapResource.obtain(outBitmap, this.mBitmapPool);
    }


    @Override
    public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
    }
}
