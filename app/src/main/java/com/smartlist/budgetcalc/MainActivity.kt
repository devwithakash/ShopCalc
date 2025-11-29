package com.smartlist.budgetcalc

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.smartlist.budgetcalc.databinding.ActivityMainBinding
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var shopAdapter: ShopAdapter
    private var shopItems = mutableListOf<ShopItem>()
    private val gson = Gson()
    private val prefs by lazy { getSharedPreferences("shop_prefs", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadData()
        setupRecyclerView()
        updateTotals()
        updateEmptyStateVisibility()

        binding.fab.setOnClickListener {
            showAddItemDialog()
        }
    }

    private fun setupRecyclerView() {
        shopAdapter = ShopAdapter(
            items = shopItems,
            onEdit = { item -> showEditItemDialog(item) },
            onDelete = { item ->
                shopItems.remove(item)
                shopAdapter.notifyDataSetChanged()
                saveData()
                updateTotals()
                updateEmptyStateVisibility()
            },
            onBoughtToggle = {
                saveData()
                updateTotals()
            }
        )
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = shopAdapter
        }
    }

    private fun showAddItemDialog() {
        val builder = AlertDialog.Builder(this, com.google.android.material.R.style.Theme_MaterialComponents_DayNight_Dialog_Alert)
        builder.setTitle("Add New Item")

        val view = layoutInflater.inflate(R.layout.dialog_add_item, null)
        val nameEditText = view.findViewById<EditText>(R.id.editTextName)
        val priceEditText = view.findViewById<EditText>(R.id.editTextPrice)
        builder.setView(view)

        builder.setPositiveButton("Add") { _, _ ->
            val name = nameEditText.text.toString()
            val price = priceEditText.text.toString().toDoubleOrNull() ?: 0.0

            if (name.isNotEmpty()) {
                val newItem = ShopItem(name = name, price = price)
                shopItems.add(newItem)
                shopAdapter.notifyItemInserted(shopItems.size - 1)
                saveData()
                updateTotals()
                updateEmptyStateVisibility()
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.create().show()
    }

    private fun showEditItemDialog(item: ShopItem) {
        val builder = AlertDialog.Builder(this, com.google.android.material.R.style.Theme_MaterialComponents_DayNight_Dialog_Alert)
        builder.setTitle("Edit Item")

        val view = layoutInflater.inflate(R.layout.dialog_add_item, null)
        val nameEditText = view.findViewById<EditText>(R.id.editTextName)
        val priceEditText = view.findViewById<EditText>(R.id.editTextPrice)

        nameEditText.setText(item.name)
        priceEditText.setText(item.price.toString())

        builder.setView(view)

        builder.setPositiveButton("Save") { _, _ ->
            val newName = nameEditText.text.toString()
            val newPrice = priceEditText.text.toString().toDoubleOrNull() ?: 0.0

            if (newName.isNotEmpty()) {
                item.name = newName
                item.price = newPrice
                shopAdapter.notifyDataSetChanged()
                saveData()
                updateTotals()
                updateEmptyStateVisibility()
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.create().show()
    }

    private fun updateTotals() {
        val totalBudget = shopItems.sumOf { it.price }
        val actualSpent = shopItems.filter { it.isBought }.sumOf { it.price }

        binding.textViewTotalBudget.text = String.format("Total Budget: $%.2f", totalBudget)
        binding.textViewActualSpent.text = String.format("Actually Spent: $%.2f", actualSpent)
    }

    private fun saveData() {
        val json = gson.toJson(shopItems)
        prefs.edit().putString("shop_items", json).apply()
    }

    private fun loadData() {
        val json = prefs.getString("shop_items", null)
        val type = object : TypeToken<MutableList<ShopItem>>() {}.type
        shopItems = gson.fromJson(json, type) ?: mutableListOf()
    }

    private fun updateEmptyStateVisibility() {
        if (shopItems.isEmpty()) {
            binding.textViewEmptyState.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        } else {
            binding.textViewEmptyState.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
        }
    }
}
