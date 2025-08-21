package com.rapid.prominence.minty

import androidx.viewpager2.widget.ViewPager2
import com.blankj.utilcode.util.SizeUtils
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions

import jp.wasabeef.glide.transformations.CropCircleTransformation
fun ViewPager2.customFunction() {
    // 扩展功能


}
val songs_options: RequestOptions = RequestOptions()
    .placeholder(randomBitmap())
    .error(randomBitmap())
    .centerCrop()
    .transform(RoundedCorners(SizeUtils.dp2px(10f)))
val options: RequestOptions = RequestOptions()
    .placeholder(randomBitmap())
    .error(randomBitmap())
    .transform(RoundedCorners(SizeUtils.dp2px(10f)))


val artOptions1: RequestOptions = RequestOptions()
    .transform(RoundedCorners(SizeUtils.dp2px(12f)))
    .placeholder(randomBitmap())
    .error(randomBitmap())

val artOptions2: RequestOptions = RequestOptions()
    .transform(RoundedCorners(SizeUtils.dp2px(12f)))


val artOptions3: RequestOptions = RequestOptions()
    .centerCrop()
    .transform(RoundedCorners(SizeUtils.dp2px(12f)))



val roundOptions = RequestOptions().placeholder(randomBitmap())
    .error(randomBitmap()).transform(CropCircleTransformation())


val roundOptions2 = RequestOptions()
    .transform(CropCircleTransformation())
