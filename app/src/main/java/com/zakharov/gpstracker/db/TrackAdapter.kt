package com.zakharov.gpstracker.db

import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.zakharov.gpstracker.R
import com.zakharov.gpstracker.databinding.TrackItemBinding

class TrackAdapter(private val listener: Listener) :
    ListAdapter<TrackItem, TrackAdapter.Holder>(Comparator()) {

    class Holder(view: View, private val listener: Listener) : RecyclerView.ViewHolder(view),
        OnClickListener {

        private val binding = TrackItemBinding.bind(view)
        private var trackTemp: TrackItem? = null

        init {
            binding.ibDelete.setOnClickListener(this)
            binding.item.setOnClickListener(this)
        }

        fun bind(trackItem: TrackItem) = with(binding) {
            trackTemp = trackItem
            val speed = "${trackItem.speed} km/h"
            val distance = "${trackItem.distance} km"
            tvDate.text = trackItem.date
            tvSpeed.text = speed
            tvTime.text = trackItem.time
            tvDistance.text = distance
        }

        override fun onClick(v: View) {
            val type = when (v.id) {
                R.id.ibDelete -> ClickType.DELETE
                R.id.item -> ClickType.OPEN
                else -> ClickType.OPEN
            }
            trackTemp?.let { listener.onclick(it, type) }
        }
    }

    class Comparator : DiffUtil.ItemCallback<TrackItem>() {
        override fun areItemsTheSame(oldItem: TrackItem, newItem: TrackItem): Boolean {
            return oldItem.id == newItem.id

        }

        override fun areContentsTheSame(oldItem: TrackItem, newItem: TrackItem): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.track_item, parent, false)
        return Holder(view, listener)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(getItem(position))
    }

    interface Listener {
        fun onclick(track: TrackItem, type: ClickType)
    }

    enum class ClickType {
        DELETE,
        OPEN
    }
}