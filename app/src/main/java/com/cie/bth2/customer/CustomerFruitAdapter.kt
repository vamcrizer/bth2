package com.cie.bth2.customer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.cie.bth2.R
import com.cie.bth2.data.Fruit

class CustomerFruitAdapter(
    private val onClick: (Fruit) -> Unit
) : RecyclerView.Adapter<CustomerFruitAdapter.Holder>() {

    private var items: List<Fruit> = emptyList()
    private var today: String = OrderRules.today()

    fun submit(list: List<Fruit>) {
        items = list
        today = OrderRules.today()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_customer_fruit, parent, false)
    )

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val fruit = items[position]
        val ctx = holder.itemView.context
        holder.photo.setImageResource(com.cie.bth2.ui.common.FruitImages.forFruit(fruit.name, fruit.category))
        holder.name.text = fruit.name
        holder.category.text = fruit.category
        holder.price.text = formatVnd(fruit.price)
        holder.meta.text = ctx.getString(R.string.cs_stock_expiry, fruit.quantity, fruit.expiryDate)

        val badge = when {
            OrderRules.isExpired(fruit, today) -> R.string.cs_badge_expired
            fruit.quantity <= 0 -> R.string.cs_badge_out_of_stock
            else -> null
        }
        if (badge == null) {
            holder.badge.visibility = View.GONE
            holder.itemView.alpha = 1f
        } else {
            holder.badge.visibility = View.VISIBLE
            holder.badge.setText(badge)
            holder.badge.setBackgroundColor(ContextCompat.getColor(ctx, R.color.cs_badge_bg))
            holder.itemView.alpha = 0.6f
        }
        holder.itemView.setOnClickListener { onClick(fruit) }
    }

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val photo: android.widget.ImageView = view.findViewById(R.id.fruitPhoto)
        val name: TextView = view.findViewById(R.id.tvName)
        val category: TextView = view.findViewById(R.id.tvCategory)
        val price: TextView = view.findViewById(R.id.tvPrice)
        val meta: TextView = view.findViewById(R.id.tvMeta)
        val badge: TextView = view.findViewById(R.id.tvBadge)
    }
}
