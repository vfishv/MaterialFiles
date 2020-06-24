/*
 * Copyright (c) 2018 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.navigation

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.RippleDrawable
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import me.zhanghai.android.files.R
import me.zhanghai.android.files.compat.getColorStateListCompat
import me.zhanghai.android.files.compat.getDrawableCompat
import me.zhanghai.android.files.databinding.NavigationDividerItemBinding
import me.zhanghai.android.files.databinding.NavigationItemBinding
import me.zhanghai.android.files.settings.Settings
import me.zhanghai.android.files.ui.SimpleAdapter
import me.zhanghai.android.files.util.dpToDimensionPixelSize
import me.zhanghai.android.files.util.getColorByAttr
import me.zhanghai.android.files.util.layoutInflater
import me.zhanghai.android.files.util.valueCompat
import me.zhanghai.android.foregroundcompat.ForegroundCompat

class NavigationListAdapter(
    private val listener: NavigationItem.Listener
) : SimpleAdapter<NavigationItem?, RecyclerView.ViewHolder>() {
    fun notifyCheckedChanged() {
        notifyItemRangeChanged(0, itemCount, PAYLOAD_CHECKED_CHANGED)
    }

    override val hasStableIds: Boolean
        get() = true

    override fun getItemId(position: Int): Long =
        getItem(position)?.id ?: list.subList(0, position).count { it == null }.toLong()

    override fun getItemViewType(position: Int): Int {
        val viewType = if (getItem(position) != null) ViewType.ITEM else ViewType.DIVIDER
        return viewType.ordinal
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (ViewType.values()[viewType]) {
            ViewType.ITEM ->
                ItemHolder(
                    NavigationItemBinding.inflate(parent.context.layoutInflater, parent, false)
                ).apply {
                    if (Settings.MATERIAL_DESIGN_2.valueCompat) {
                        val context = binding.itemLayout.context
                        binding.itemLayout.background = createItemBackgroundMd2(context)
                        ForegroundCompat.setForeground(
                            binding.itemLayout, createItemForegroundMd2(context)
                        )
                    } else {
                        binding.itemLayout.background =
                            binding.itemLayout.context.getDrawableCompat(
                                R.drawable.navigation_item_background
                            )
                    }
                    binding.iconImage.imageTintList = NavigationItemColor.create(
                        binding.iconImage.imageTintList!!, binding.iconImage.context
                    )
                    binding.titleText.setTextColor(
                        NavigationItemColor.create(
                            binding.titleText.textColors, binding.titleText.context
                        )
                    )
                    binding.subtitleText.setTextColor(
                        NavigationItemColor.create(
                            binding.subtitleText.textColors, binding.subtitleText.context
                        )
                    )
                }
            ViewType.DIVIDER ->
                DividerHolder(
                    NavigationDividerItemBinding.inflate(
                        parent.context.layoutInflater, parent, false
                    )
                )
        }
    }

    // @see com.google.android.material.navigation.NavigationView#createDefaultItemBackground
    private fun createItemBackgroundMd2(context: Context): Drawable =
        createItemShapeDrawableMd2(
            context.getColorStateListCompat(R.color.mtrl_navigation_item_background_color),
            context
        )

    private fun createItemForegroundMd2(context: Context): Drawable {
        val mask = createItemShapeDrawableMd2(ColorStateList.valueOf(Color.WHITE), context)
        val controlHighlightColor = context.getColorByAttr(R.attr.colorControlHighlight)
        return RippleDrawable(ColorStateList.valueOf(controlHighlightColor), null, mask)
    }

    // @see com.google.android.material.navigation.NavigationView#createDefaultItemBackground
    private fun createItemShapeDrawableMd2(
        fillColor: ColorStateList,
        context: Context
    ): Drawable {
        val materialShapeDrawable = MaterialShapeDrawable(
            ShapeAppearanceModel.builder(context, R.style.ShapeAppearance_Google_Navigation, 0)
                .build()
        ).apply { this.fillColor = fillColor }
        val insetRight = context.dpToDimensionPixelSize(8)
        return InsetDrawable(materialShapeDrawable, 0, 0, insetRight, 0)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        throw UnsupportedOperationException()
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: List<Any>
    ) {
        when (ViewType.values()[getItemViewType(position)]) {
            ViewType.ITEM -> {
                val item = getItem(position)!!
                val binding = (holder as ItemHolder).binding
                binding.itemLayout.isChecked = item.isChecked(listener)
                if (payloads.isNotEmpty()) {
                    return
                }
                binding.itemLayout.setOnClickListener { item.onClick(listener) }
                binding.itemLayout.setOnLongClickListener { item.onLongClick(listener) }
                binding.iconImage.setImageDrawable(item.getIcon(binding.iconImage.context))
                binding.titleText.text = item.getTitle(binding.titleText.context)
                binding.subtitleText.text = item.getSubtitle(binding.subtitleText.context)
            }
            ViewType.DIVIDER -> {}
        }
    }

    companion object {
        private val PAYLOAD_CHECKED_CHANGED = Any()
    }

    private enum class ViewType {
        ITEM,
        DIVIDER
    }

    private class ItemHolder(val binding: NavigationItemBinding) : RecyclerView.ViewHolder(
        binding.root
    )

    private class DividerHolder(
        val binding: NavigationDividerItemBinding
    ) : RecyclerView.ViewHolder(binding.root)
}
