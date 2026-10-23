package com.cie.bth2.customer

import android.content.Intent
import android.os.Bundle
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cie.bth2.R
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

/** Màn hình Customer: danh sách trái cây, tìm kiếm, lọc danh mục. */
class CustomerFruitListActivity : AppCompatActivity() {

    private lateinit var repo: CustomerShopRepository
    private lateinit var adapter: CustomerFruitAdapter
    private lateinit var etSearch: EditText
    private lateinit var chipGroup: ChipGroup
    private lateinit var cbExpired: CheckBox
    private lateinit var tvEmpty: TextView
    private var selectedCategory: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_fruit_list)
        title = getString(R.string.cs_title_list)
        repo = CustomerShopRepository(this)

        etSearch = findViewById(R.id.etSearch)
        chipGroup = findViewById(R.id.chipCategories)
        cbExpired = findViewById(R.id.cbShowExpired)
        tvEmpty = findViewById(R.id.tvEmpty)

        adapter = CustomerFruitAdapter { fruit ->
            startActivity(
                Intent(this, CustomerFruitDetailActivity::class.java)
                    .putExtra(CustomerFruitDetailActivity.EXTRA_FRUIT_ID, fruit.id)
            )
        }
        findViewById<RecyclerView>(R.id.rvFruits).apply {
            layoutManager = LinearLayoutManager(this@CustomerFruitListActivity)
            adapter = this@CustomerFruitListActivity.adapter
        }

        etSearch.doAfterTextChanged { reload() }
        cbExpired.setOnCheckedChangeListener { _, _ -> reload() }
        chipGroup.setOnCheckedStateChangeListener { group, ids ->
            selectedCategory = ids.firstOrNull()
                ?.let { group.findViewById<Chip>(it) }
                ?.takeIf { it.id != R.id.chipAll }
                ?.text?.toString()
            reload()
        }
    }

    override fun onResume() {
        super.onResume()
        // Tồn kho thay đổi sau khi đặt hàng -> nạp lại khi quay về.
        bindCategories()
        reload()
    }

    override fun onDestroy() {
        if (::repo.isInitialized) repo.close()
        super.onDestroy()
    }

    private fun bindCategories() {
        val keep = selectedCategory
        chipGroup.removeAllViews()
        chipGroup.addView(newChip(getString(R.string.cs_all)).apply { id = R.id.chipAll })
        var restored = false
        for (cat in repo.getCategories()) {
            val chip = newChip(cat)
            chipGroup.addView(chip)
            if (cat == keep) { chip.isChecked = true; restored = true }
        }
        if (!restored) {
            selectedCategory = null
            chipGroup.check(R.id.chipAll)
        }
    }

    private fun newChip(text: String) = Chip(this).apply {
        this.text = text
        isCheckable = true
        if (id == android.view.View.NO_ID) id = android.view.View.generateViewId()
    }

    private fun reload() {
        val list = repo.searchFruits(etSearch.text.toString(), selectedCategory, cbExpired.isChecked)
        adapter.submit(list)
        tvEmpty.isVisible = list.isEmpty()
    }
}
