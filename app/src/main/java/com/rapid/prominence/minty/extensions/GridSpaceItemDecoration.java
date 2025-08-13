package com.rapid.prominence.minty.extensions;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * 鎻忚堪 : RecyclerView GridLayoutManager 绛夐棿璺濄€?
 * <p>
 * 绛夐棿璺濋渶婊¤冻涓や釜鏉′欢锛?
 * 1.鍚勪釜妯″潡鐨勫ぇ灏忕浉绛夛紝鍗?鍚勫垪鐨刲eft+right 鍊肩浉绛夛紱
 * 2.鍚勫垪鐨勯棿璺濈浉绛夛紝鍗?鍓嶅垪鐨剅ight + 鍚庡垪鐨刲eft = 鍒楅棿璺濓紱
 * <p>
 * 鍦▄@link #( RecyclerView, RecyclerView.State)} 涓拡瀵?outRect 鐨刲eft 鍜宺ight 婊¤冻杩欎袱涓潯浠跺嵆鍙?
 * <p>
 * 浣滆€?: shiguotao
 * 鐗堟湰 : V1
 * 鍒涘缓鏃堕棿 : 2020/3/19 4:54 PM
 */
public class GridSpaceItemDecoration extends RecyclerView.ItemDecoration {

    private final String TAG = "GridSpaceItemDecoration";

    private int mSpanCount;//妯潯鐩暟閲?
    private int mRowSpacing;//琛岄棿璺?
    private int mColumnSpacing;// 鍒楅棿璺?

    /**
     * @param spanCount     鍒楁暟
     * @param rowSpacing    琛岄棿璺?
     * @param columnSpacing 鍒楅棿璺?
     */
    public GridSpaceItemDecoration(int spanCount, int rowSpacing, int columnSpacing) {
        this.mSpanCount = spanCount;
        this.mRowSpacing = rowSpacing;
        this.mColumnSpacing = columnSpacing;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view); // 鑾峰彇view 鍦╝dapter涓殑浣嶇疆銆?
        int column = position % mSpanCount; // view 鎵€鍦ㄧ殑鍒?

        outRect.left = column * mColumnSpacing / mSpanCount; // column * (鍒楅棿璺?* (1f / 鍒楁暟))
        outRect.right = mColumnSpacing - (column + 1) * mColumnSpacing / mSpanCount; // 鍒楅棿璺?- (column + 1) * (鍒楅棿璺?* (1f /鍒楁暟))

//        Log.e(TAG, "position:" + position
//                + "    columnIndex: " + column
//                + "    left,right ->" + outRect.left + "," + outRect.right);

        // 濡傛灉position > 琛屾暟锛岃鏄庝笉鏄湪绗竴琛岋紝鍒欎笉鎸囧畾琛岄珮锛屽叾浠栬鐨勪笂闂磋窛涓?top=mRowSpacing
        if (position >= mSpanCount) {
            outRect.top = mRowSpacing; // item top
        }
    }
}

