package com.smartlist.budgetcalc

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.smartlist.budgetcalc.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var shopAdapter: ShopAdapter
    private var shopItems = mutableListOf<ShopItem>()
    private val gson = Gson()

    private var currencySymbol = "$"
    private var budgetLimit = 0.0
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

        // [FIX] Hooking up the Settings Button
        binding.buttonSettings.setOnClickListener {
            showSettingsDialog()
        }
    }

    private fun setupRecyclerView() {
        shopAdapter = ShopAdapter(
            items = shopItems,
            currencySymbol = currencySymbol, // [FIX] Passing currency here
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
        val builder = AlertDialog.Builder(this)
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
        val builder = AlertDialog.Builder(this)
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

        binding.textViewTotalBudget.text = getString(R.string.total_budget_format, currencySymbol, totalBudget)
        binding.textViewActualSpent.text = getString(R.string.actually_spent_format, currencySymbol, actualSpent)

        if (budgetLimit > 0) {
            binding.textViewBudgetLimit.visibility = View.VISIBLE
            binding.textViewBudgetLimit.text = getString(R.string.budget_limit_format, currencySymbol, budgetLimit)

            if (totalBudget > budgetLimit) {
                binding.textViewTotalBudget.setTextColor(getColor(android.R.color.holo_red_dark))
            } else {
                binding.textViewTotalBudget.setTextColor(getColor(android.R.color.black))
            }
        } else {
            binding.textViewBudgetLimit.visibility = View.GONE
        }
    }

    private fun saveData() {
        val json = gson.toJson(shopItems)
        prefs.edit().putString("shop_items", json).apply()
    }

    private fun loadData() {
        val json = prefs.getString("shop_items", null)
        val type = object : TypeToken<MutableList<ShopItem>>() {}.type
        shopItems = gson.fromJson(json, type) ?: mutableListOf()

        currencySymbol = prefs.getString("currency_symbol", "$") ?: "$"
        budgetLimit = prefs.getFloat("budget_limit", 0.0f).toDouble()
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

    private fun showSettingsDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("App Settings")

        val view = layoutInflater.inflate(R.layout.dialog_settings, null)
        // [FIX] These IDs now exist in the new layout
        val limitET = view.findViewById<EditText>(R.id.editTextLimit)
        val currencySpinner = view.findViewById<Spinner>(R.id.spinnerCurrency)

        limitET.setText(budgetLimit.toString())

        // Select the current currency in the spinner
        val currencyOptions = resources.getStringArray(R.array.currency_options)
        val index = currencyOptions.indexOf(currencySymbol)
        if (index >= 0) {
            currencySpinner.setSelection(index)
        }

        builder.setView(view)
        builder.setPositiveButton("Save") { _, _ ->
            budgetLimit = limitET.text.toString().toDoubleOrNull() ?: 0.0
            currencySymbol = currencySpinner.selectedItem.toString()

            prefs.edit().apply {
                putFloat("budget_limit", budgetLimit.toFloat())
                putString("currency_symbol", currencySymbol)
                apply()
            }

            // [FIX] Update adapter's currency property
            shopAdapter.currencySymbol = currencySymbol

            updateTotals()
            shopAdapter.notifyDataSetChanged()
        }
        builder.show()
    }
}