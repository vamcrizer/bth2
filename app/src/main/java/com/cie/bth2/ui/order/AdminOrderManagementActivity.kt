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
import com.google.android.material.chip.ChipGroup

/** Admin screen: order statistics, status filter and status updates. */
class AdminOrderManagementActivity : com.cie.bth2.ui.common.BackActivity() {

    private lateinit var orderRepository: OrderRepository
    private lateinit var adapter: OrderAdapter
    private var statusFilter: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_order_management)
        title = getString(R.string.admin_orders_title)

        orderRepository = OrderRepository(DatabaseHelper(this))
        adapter = OrderAdapter(showCustomer = true, onAdvance = ::advanceOrder)
        findViewById<RecyclerView>(R.id.recyclerOrders).apply {
            layoutManager = LinearLayoutManager(this@AdminOrderManagementActivity)
            adapter = this@AdminOrderManagementActivity.adapter
        }

        findViewById<ChipGroup>(R.id.chipGroupStatus).setOnCheckedStateChangeListener { _, checkedIds ->
            statusFilter = when (checkedIds.firstOrNull()) {
                R.id.chipPending -> DatabaseHelper.STATUS_PENDING
                R.id.chipConfirmed -> DatabaseHelper.STATUS_CONFIRMED
                R.id.chipCompleted -> DatabaseHelper.STATUS_COMPLETED
                else -> null
            }
            refresh()
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun advanceOrder(order: OrderRepository.Order) {
        val newStatus = OrderStats.nextStatus(order.status)
        val updated = newStatus != null && try {
            orderRepository.updateStatus(order.id, newStatus)
        } catch (_: IllegalArgumentException) {
            // Status changed elsewhere since the list was loaded.
            false
        }
        val message = if (updated && newStatus != null) {
            getString(R.string.order_advance_done, order.id, OrderAdapter.statusLabel(this, newStatus))
        } else {
            getString(R.string.order_advance_failed)
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        refresh()
    }

    private fun refresh() {
        val allOrders = orderRepository.getAll()
        val stats = OrderStats.from(allOrders)
        findViewById<TextView>(R.id.textStatTotal).text = stats.total.toString()
        findViewById<TextView>(R.id.textStatPending).text = stats.pending.toString()
        findViewById<TextView>(R.id.textStatConfirmed).text = stats.confirmed.toString()
        findViewById<TextView>(R.id.textStatCompleted).text = stats.completed.toString()
        findViewById<TextView>(R.id.textStatRevenue).text = getString(
            R.string.admin_orders_revenue,
            OrderAdapter.formatCurrency(stats.completedRevenue)
        )

        val orders = statusFilter?.let { status -> allOrders.filter { it.status == status } } ?: allOrders
        adapter.submit(orders)
        findViewById<TextView>(R.id.textEmpty).visibility =
            if (orders.isEmpty()) View.VISIBLE else View.GONE
    }

    companion object {
        fun newIntent(context: Context): Intent =
            Intent(context, AdminOrderManagementActivity::class.java)
    }
}
