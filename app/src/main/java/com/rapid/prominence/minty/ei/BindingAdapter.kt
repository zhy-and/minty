package com.rapid.prominence.minty.ei
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.rapid.prominence.minty.CustomViewsInfo
import com.rapid.prominence.minty.R
import com.rapid.prominence.minty.databinding.ItemSlideBinding
import com.rapid.prominence.minty.randomBitmap
import com.zhpan.bannerview.BaseBannerAdapter
import com.zhpan.bannerview.BaseViewHolder

//

class BindingAdapter(private val mRoundCorner: Int) : BaseBannerAdapter<CustomViewsInfo>() {

  override fun createViewHolder(
    parent: ViewGroup,
    itemView: View,
    viewType: Int
  ): BaseViewHolder<CustomViewsInfo> {
    return BindingViewHolder(ItemSlideBinding.bind(itemView))
  }

    override fun bindData(
        holder: BaseViewHolder<CustomViewsInfo>?,
        data: CustomViewsInfo?,
        position: Int,
        pageSize: Int
    ) {
        if (holder is BindingViewHolder) {
            holder.viewBinding.bannerImage.setRoundCorner(mRoundCorner)
//            holder.viewBinding.bannerImage.setImageResource(data)
            Glide.with(holder.itemView.context)
                .load(data?.info)
                .thumbnail(
                        Glide.with(holder.itemView.context)
                            .load(randomBitmap())
                    )
                .into(holder.viewBinding.bannerImage)
        }
    }


  override fun getLayoutId(viewType: Int): Int {
    return R.layout.item_slide
  }
}

internal class BindingViewHolder(var viewBinding: ItemSlideBinding) :
  BaseViewHolder<CustomViewsInfo>(viewBinding.root)



