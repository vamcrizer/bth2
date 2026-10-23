package com.cie.bth2.customer

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.cie.bth2.R
import com.cie.bth2.data.Fruit

/** Chi tiết trái cây + chọn số lượng + đặt hàng. Khoá đặt hàng nếu hết hạn / hết hàng. */
class CustomerFruitDetailActivity : com.cie.bth2.ui.common.BackActivity() {

    private lateinit var repo: CustomerShopRepository
    private lateinit var fruit: Fruit
    private var amount = 1

    private lateinit var tvAmount: TextView
    private lateinit var tvTotal: TextView
    private lateinit var tvBlocked: TextView
    private lateinit var btnMinus: Button
    private lateinit var btnPlus: Button
    private lateinit var btnOrder: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_fruit_detail)
        repo = CustomerShopRepository(this)

        tvAmount = findViewById(R.id.tvAmount)
        tvTotal = findViewById(R.id.tvTotal)
        tvBlocked = findViewById(R.id.tvBlocked)
        btnMinus = findViewById(R.id.btnMinus)
        btnPlus = findViewById(R.id.btnPlus)
        btnOrder = findViewById(R.id.btnOrder)

        btnMinus.setOnClickListener { if (amount > 1) { amount--; render() } }
        btnPlus.setOnClickListener { if (amount < fruit.quantity) { amount++; render() } }
        btnOrder.setOnClickListener { placeOrder() }

        loadFruit()
    }

    /** Đóng màn hình nếu không tìm thấy fruit. */
    private fun loadFruit(): Boolean {
        val id = intent.getLongExtra(EXTRA_FRUIT_ID, -1L)
        val found = repo.getFruit(id)
        if (found == null) {
            Toast.makeText(this, R.string.cs_not_found, Toast.LENGTH_SHORT).show()
            finish()
            return false
        }
        fruit = found
        findViewById<android.widget.ImageView>(R.id.fruitPhoto).setImageResource(com.cie.bth2.ui.common.FruitImages.forFruit(fruit.name, fruit.category))
        amount = amount.coerceIn(1, maxOf(1, fruit.quantity))
        title = fruit.name
        findViewById<TextView>(R.id.tvName).text = fruit.name
        findViewById<TextView>(R.id.tvCategory).text = fruit.category
        findViewById<TextView>(R.id.tvPrice).text = formatVnd(fruit.price)
        findViewById<TextView>(R.id.tvQuantity).text = getString(R.string.cs_stock, fruit.quantity)
        findViewById<TextView>(R.id.tvExpiry).text = getString(R.string.cs_expiry, fruit.expiryDate)
        findViewById<TextView>(R.id.tvDescription).text = fruit.description
        render()
        return true
    }

    private fun render() {
        val blockedReason = when {
            OrderRules.isExpired(fruit) -> R.string.cs_blocked_expired
            fruit.quantity <= 0 -> R.string.cs_blocked_out_of_stock
            else -> null
        }
        val canBuy = blockedReason == null
        tvBlocked.isVisible = !canBuy
        blockedReason?.let { tvBlocked.setText(it) }
        btnOrder.isEnabled = canBuy
        btnMinus.isEnabled = canBuy && amount > 1
        btnPlus.isEnabled = canBuy && amount < fruit.quantity
        tvAmount.text = amount.toString()
        tvTotal.text = getString(R.string.cs_total, formatVnd(fruit.price * amount))
    }

    private fun placeOrder() {
        val customerId = CustomerSession.customerId(this)
        if (customerId == null) {
            Toast.makeText(this, R.string.cs_customer_missing, Toast.LENGTH_LONG).show()
            finish()
            return
        }
        when (val r = repo.placeOrder(customerId, fruit.id, amount)) {
            is CustomerShopRepository.PlaceOrderResult.Success -> {
                AlertDialog.Builder(this)
                    .setTitle(R.string.cs_order_success_title)
                    .setMessage(getString(R.string.cs_order_success_msg, r.orderId, formatVnd(r.total)))
                    .setPositiveButton(android.R.string.ok) { _, _ -> finish() }
                    .setCancelable(false)
                    .show()
            }
            is CustomerShopRepository.PlaceOrderResult.Rejected -> {
                val msg = when (r.reason) {
                    OrderRules.Result.EXPIRED -> R.string.cs_blocked_expired
                    OrderRules.Result.OUT_OF_STOCK -> R.string.cs_blocked_out_of_stock
                    else -> R.string.cs_not_enough_stock
                }
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                loadFruit() // dữ liệu có thể đã đổi -> nạp lại
            }
            CustomerShopRepository.PlaceOrderResult.FruitNotFound -> {
                Toast.makeText(this, R.string.cs_not_found, Toast.LENGTH_SHORT).show()
                finish()
            }
            CustomerShopRepository.PlaceOrderResult.CustomerNotFound -> {
                Toast.makeText(this, R.string.cs_customer_missing, Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        if (::repo.isInitialized) repo.close()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_FRUIT_ID = "extra_fruit_id"
    }
}
