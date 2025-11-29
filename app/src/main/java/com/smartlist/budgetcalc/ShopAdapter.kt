package com.smartlist.budgetcalc

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.smartlist.budgetcalc.databinding.ItemShopBinding

class ShopAdapter(
    private var items: MutableList<ShopItem>,
    private val onEdit: (ShopItem) -> Unit,
    private val onDelete: (ShopItem) -> Unit,
    private val onBoughtToggle: (ShopItem) -> Unit
) : RecyclerView.Adapter<ShopAdapter.ShopViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShopViewHolder {
        val binding = ItemShopBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ShopViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ShopViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newList: List<ShopItem>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }

    inner class ShopViewHolder(private val binding: ItemShopBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ShopItem) {
            binding.textViewName.text = item.name
            binding.textViewPrice.text = String.format("$%.2f", item.price)
            binding.checkBoxBought.isChecked = item.isBought

            updateStrikeThrough(item.isBought)

            binding.checkBoxBought.setOnCheckedChangeListener { _, isChecked ->
                item.isBought = isChecked
                updateStrikeThrough(isChecked)
                onBoughtToggle(item)
            }

            binding.buttonDelete.setOnClickListener {
                onDelete(item)
            }

            itemView.setOnClickListener {
                onEdit(item)
            }
        }

        private fun updateStrikeThrough(isBought: Boolean) {
            if (isBought) {
                binding.textViewName.paintFlags = binding.textViewName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                binding.textViewName.paintFlags = binding.textViewName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
        }
    }
}
