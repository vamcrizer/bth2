package com.cie.bth2.ui.order

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cie.bth2.R
import com.cie.bth2.data.DatabaseHelper
import com.cie.bth2.data.OrderRepository

/** Order history of the signed-in customer. Start with [newIntent]. */
class CustomerOrderHistoryActivity : AppCompatActivity() {

    private lateinit var orderRepository: OrderRepository
    private lateinit var adapter: OrderAdapter
    private var customerId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_order_history)
        title = getString(R.string.order_history_title)

        orderRepository = OrderRepository(DatabaseHelper(this))
        customerId = intent.getLongExtra(EXTRA_CUSTOMER_ID, -1)
        if (customerId <= 0) {
            Toast.makeText(this, R.string.customer_not_found, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        adapter = OrderAdapter(showCustomer = false)
        findViewById<RecyclerView>(R.id.recyclerOrders).apply {
            layoutManager = LinearLayoutManager(this@CustomerOrderHistoryActivity)
            adapter = this@CustomerOrderHistoryActivity.adapter
        }
    }

    override fun onResume() {
        super.onResume()
        if (customerId <= 0) return
        val orders = orderRepository.getForCustomer(customerId)
        adapter.submit(orders)
        findViewById<TextView>(R.id.textEmpty).visibility =
            if (orders.isEmpty()) View.VISIBLE else View.GONE
    }

    companion object {
        /** Customer.id (not Account.id), matching the customer_id session key. */
        const val EXTRA_CUSTOMER_ID = "extra_customer_id"

        fun newIntent(context: Context, customerId: Long): Intent =
            Intent(context, CustomerOrderHistoryActivity::class.java)
                .putExtra(EXTRA_CUSTOMER_ID, customerId)
    }
}
