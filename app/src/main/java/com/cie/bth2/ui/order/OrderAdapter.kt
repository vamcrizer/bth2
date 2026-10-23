package com.cie.bth2.ui.order

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.cie.bth2.R
import com.cie.bth2.data.DatabaseHelper
import com.cie.bth2.data.OrderRepository
import java.text.NumberFormat
import java.util.Locale

/**
 * Order list shared by customer history and admin management.
 * Pass [onAdvance] to show the "move to next status" button (admin only).
 */
class OrderAdapter(
    private val showCustomer: Boolean,
    private val onAdvance: ((OrderRepository.Order) -> Unit)? = null
) : RecyclerView.Adapter<OrderAdapter.ViewHolder>() {

    private var orders: List<OrderRepository.Order> = emptyList()

    fun submit(newOrders: List<OrderRepository.Order>) {
        orders = newOrders
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_order, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(orders[position])

    override fun getItemCount(): Int = orders.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title: TextView = view.findViewById(R.id.textOrderTitle)
        private val customer: TextView = view.findViewById(R.id.textOrderCustomer)
        private val date: TextView = view.findViewById(R.id.textOrderDate)
        private val total: TextView = view.findViewById(R.id.textOrderTotal)
        private val status: TextView = view.findViewById(R.id.textOrderStatus)
        private val advance: Button = view.findViewById(R.id.buttonAdvanceStatus)

        fun bind(order: OrderRepository.Order) {
            val context = itemView.context
            title.text = context.getString(R.string.order_item_title, order.id)
            customer.visibility = if (showCustomer) View.VISIBLE else View.GONE
            customer.text = context.getString(R.string.order_item_customer, order.customerName)
            date.text = context.getString(R.string.order_item_date, order.orderDate)
            total.text = context.getString(R.string.order_item_total, formatCurrency(order.totalAmount))
            status.text = statusLabel(context, order.status)
            status.setBackgroundColor(ContextCompat.getColor(context, statusColor(order.status)))

            val next = OrderStats.nextStatus(order.status)
            if (onAdvance != null && next != null) {
                advance.visibility = View.VISIBLE
                advance.text = context.getString(R.string.order_advance_to, statusLabel(context, next))
                advance.setOnClickListener { onAdvance.invoke(order) }
            } else {
                advance.visibility = View.GONE
                advance.setOnClickListener(null)
            }
        }
    }

    companion object {
        private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))

        fun formatCurrency(amount: Double): String = currencyFormat.format(amount)

        fun statusLabel(context: android.content.Context, status: String): String = when (status) {
            DatabaseHelper.STATUS_PENDING -> context.getString(R.string.order_status_pending)
            DatabaseHelper.STATUS_CONFIRMED -> context.getString(R.string.order_status_confirmed)
            DatabaseHelper.STATUS_COMPLETED -> context.getString(R.string.order_status_completed)
            else -> status
        }

        private fun statusColor(status: String): Int = when (status) {
            DatabaseHelper.STATUS_PENDING -> R.color.order_status_pending
            DatabaseHelper.STATUS_CONFIRMED -> R.color.order_status_confirmed
            else -> R.color.order_status_completed
        }
    }
}
