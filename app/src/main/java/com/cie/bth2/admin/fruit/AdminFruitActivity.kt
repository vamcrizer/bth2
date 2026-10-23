package com.cie.bth2.admin.fruit

import android.app.DatePickerDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import com.cie.bth2.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.TextInputEditText
import java.text.NumberFormat
import java.util.Locale

class AdminFruitActivity : AppCompatActivity(), FruitPresenter.View {
    private lateinit var presenter: FruitPresenter
    private lateinit var summary: TextView
    private lateinit var search: EditText
    private lateinit var categories: Spinner
    private lateinit var expiry: Spinner
    private lateinit var add: Button
    private lateinit var list: ListView
    private lateinit var adapter: FruitAdapter
    private lateinit var totalStat: TextView
    private lateinit var expiredStat: TextView
    private lateinit var todayStat: TextView
    private lateinit var emptyPanel: LinearLayout
    private lateinit var emptyTitle: TextView
    private lateinit var emptyMessage: TextView
    private lateinit var clearFilters: Button
    private lateinit var progress: ProgressBar
    private lateinit var warning: TextView
    private var pendingFeedback: String? = null
    private var savingDialog: AlertDialog? = null
    private var savingError: TextView? = null
    private var items = emptyList<Fruit>()
    private var shown = emptyList<Fruit>()
    private var category: String? = null
    private var busy = false
    private var captureDraft: (() -> Bundle)? = null
    private val handler = Handler(Looper.getMainLooper())
    private val dateRefresh = object : Runnable {
        override fun run() { render(); handler.postDelayed(this, 30_000) }
    }
    private val money = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Quản lý trái cây"
        category = savedInstanceState?.getString("category")
        val root = column().apply { setBackgroundColor(getColor(R.color.admin_background)) }
        setContentView(root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val keyboard = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, maxOf(bars.bottom, keyboard.bottom))
            insets
        }
        val header = column().apply { setPadding(dp(20), dp(24), dp(20), dp(8)) }
        header.addView(MaterialButton(this).apply {
            text = "‹  " + getString(R.string.navigate_back)
            contentDescription = getString(R.string.navigate_back)
            isAllCaps = false
            setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        }, LinearLayout.LayoutParams(-2, dp(48)))
        header.addView(label("FRUIT SHOP  /  ADMIN", 12, R.color.admin_primary, true).apply { letterSpacing = .12f })
        header.addView(label("Kho trái cây", 30, bold = true).apply { setPadding(0, dp(8), 0, dp(4)) })
        header.addView(label("Quản lý hàng tươi, theo dõi từng ngày.", 14, R.color.admin_muted))
        val stats = LinearLayout(this).apply { setPadding(0, dp(22), 0, dp(18)) }
        fun stat(title: String, onClick: () -> Unit): TextView {
            val box = column().apply {
                background = rounded(getColor(R.color.admin_surface), 16, getColor(R.color.admin_border))
                setPadding(dp(12), dp(14), dp(8), dp(14))
                setOnClickListener { onClick() }
                isFocusable = true
                contentDescription = "Lọc: $title"
            }
            val value = label("0", 26, bold = true)
            box.addView(value)
            box.addView(label(title, 12, R.color.admin_muted))
            stats.addView(box, LinearLayout.LayoutParams(0, -1, 1f).apply { marginEnd = dp(6) })
            return value
        }
        totalStat = stat("Mặt hàng") { expiry.setSelection(0) }
        expiredStat = stat("Đã hết hạn") { expiry.setSelection(1) }
        todayStat = stat("Hạn hôm nay") { expiry.setSelection(2) }
        header.addView(stats)
        warning = label("", 14).apply {
            background = rounded(0xFFFFE9CF.toInt(), 12)
            setTextColor(0xFF774411.toInt())
            setPadding(dp(14), dp(12), dp(14), dp(12))
            visibility = View.GONE
            setOnClickListener { expiry.setSelection(1) }
        }
        header.addView(warning)
        val searchBox = TextInputLayout(this).apply {
            hint = "Tìm trái cây"
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            setBoxCornerRadii(dp(14).toFloat(), dp(14).toFloat(), dp(14).toFloat(), dp(14).toFloat())
            endIconMode = TextInputLayout.END_ICON_CLEAR_TEXT
        }
        search = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            setSingleLine()
            textSize = 16f
            setText(savedInstanceState?.getString("query", ""))
        }
        searchBox.addView(search)
        header.addView(searchBox, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16) })
        header.addView(label("Tìm theo tên hoặc mô tả", 12, R.color.admin_muted).apply { setPadding(dp(4), dp(4), 0, dp(12)) })
        val filters = LinearLayout(this)
        fun filterBox(title: String, spinner: Spinner) {
            val box = column()
            box.addView(label(title, 12, R.color.admin_muted).apply { setPadding(dp(4), 0, 0, dp(6)) })
            spinner.minimumHeight = dp(48)
            spinner.background = rounded(getColor(R.color.admin_surface), 12, getColor(R.color.admin_border))
            spinner.contentDescription = title
            box.addView(spinner, LinearLayout.LayoutParams(-1, -2))
            filters.addView(box, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(6) })
        }
        categories = Spinner(this)
        categories.adapter = spinnerAdapter(listOf("Tất cả danh mục"))
        filterBox("DANH MỤC ▾", categories)
        expiry = Spinner(this).apply {
            adapter = spinnerAdapter(listOf("Tất cả hạn sử dụng", "Đã hết hạn", "Hết hạn hôm nay", "Còn hạn"))
            setSelection(savedInstanceState?.getInt("expiry", 0) ?: 0)
        }
        filterBox("HẠN SỬ DỤNG ▾", expiry)
        header.addView(filters)
        clearFilters = MaterialButton(this).apply {
            text = "Đặt lại bộ lọc"; isAllCaps = false
            setOnClickListener { search.setText(""); category = null; categories.setSelection(0); expiry.setSelection(0); render() }
        }
        header.addView(clearFilters)
        summary = label("Đang tải kho trái cây…", 14, R.color.admin_muted).apply {
            setPadding(0, dp(6), 0, dp(10))
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        header.addView(summary)
        list = ListView(this).apply {
            divider = ColorDrawable(Color.TRANSPARENT)
            dividerHeight = dp(10)
            clipToPadding = false
            setPadding(dp(4), 0, dp(4), dp(16))
            addHeaderView(header, null, false)
        }
        emptyPanel = column().apply {
            gravity = Gravity.CENTER
            setPadding(dp(32), dp(32), dp(32), dp(40))
        }
        emptyPanel.addView(ImageView(this).apply {
            setImageResource(R.drawable.ic_admin_leaf)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LinearLayout.LayoutParams(dp(64), dp(64)).apply { bottomMargin = dp(16) })
        emptyTitle = label("Kho đang trống", 20, bold = true).apply { gravity = Gravity.CENTER }
        emptyMessage = label("", 14, R.color.admin_muted).apply { gravity = Gravity.CENTER; setPadding(0, dp(8), 0, 0) }
        emptyPanel.addView(emptyTitle)
        emptyPanel.addView(emptyMessage)
        list.addFooterView(emptyPanel, null, false)
        adapter = FruitAdapter()
        list.adapter = adapter
        root.addView(list, LinearLayout.LayoutParams(-1, 0, 1f))
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = true; visibility = View.INVISIBLE
        }
        root.addView(progress, LinearLayout.LayoutParams(-1, dp(3)))
        val bottom = column().apply {
            setPadding(dp(20), dp(8), dp(20), dp(12))
            setBackgroundColor(getColor(R.color.admin_surface))
        }
        add = MaterialButton(this).apply {
            text = "+  Thêm trái cây"; isAllCaps = false; textSize = 16f
            cornerRadius = dp(14)
            setOnClickListener { edit(null) }
        }
        bottom.addView(add, LinearLayout.LayoutParams(-1, dp(56)))
        root.addView(bottom)
        list.setOnItemClickListener { _, _, position, _ ->
            val index = position - list.headerViewsCount
            if (!busy && index in shown.indices) edit(shown[index])
        }
        search.addTextChangedListener { render() }
        categories.onItemSelectedListener = selection {
            category = if (categories.selectedItemPosition <= 0) null else categories.selectedItem as String
            render()
        }
        expiry.onItemSelectedListener = selection { render() }
        presenter = FruitPresenter(SqliteFruitRepository(this), this)
        savedInstanceState?.getBundle("draft")?.let { draft ->
            val id = draft.getLong("id")
            edit(if (id == 0L) null else Fruit(id, "", "", 0.0, 0, FruitRules.today(), ""), draft)
        }
    }

    override fun onResume() { super.onResume(); presenter.refresh(); handler.post(dateRefresh) }
    override fun onPause() { handler.removeCallbacks(dateRefresh); super.onPause() }
    override fun onDestroy() { presenter.close(); super.onDestroy() }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("query", search.text.toString())
        outState.putString("category", category)
        outState.putInt("expiry", expiry.selectedItemPosition)
        captureDraft?.let { outState.putBundle("draft", it()) }
        super.onSaveInstanceState(outState)
    }
    override fun dispatch(action: () -> Unit) { runOnUiThread(action) }
    override fun setBusy(busy: Boolean) {
        this.busy = busy
        add.isEnabled = !busy
        progress.visibility = if (busy) View.VISIBLE else View.INVISIBLE
        savingDialog?.let { dialog ->
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = !busy
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).isEnabled = !busy
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.isEnabled = !busy
            dialog.setCancelable(!busy)
        }
    }
    override fun showError() {
        pendingFeedback = null
        if (savingDialog?.isShowing == true) {
            savingError?.text = "Chưa lưu được dữ liệu. Thông tin vẫn được giữ lại; vui lòng thử lại."
            return
        }
        AlertDialog.Builder(this).setTitle("Không thể cập nhật dữ liệu")
            .setMessage("Thao tác chưa hoàn tất. Vui lòng thử lại.")
            .setPositiveButton("Tải lại") { _, _ -> presenter.refresh() }.setNegativeButton("Đóng", null).show()
    }
    override fun showFruits(items: List<Fruit>) {
        pendingFeedback?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
        if (pendingFeedback != null) savingDialog?.dismiss()
        savingDialog = null
        savingError = null
        pendingFeedback = null
        this.items = items
        val actualCategories = items.map { it.category }.distinct().sorted()
        val names = listOf("Tất cả danh mục") + actualCategories
        val selected = actualCategories.indexOf(category) + 1
        categories.adapter = spinnerAdapter(names)
        categories.setSelection(selected)
        if (selected == 0) category = null
        render()
    }

    private fun render() {
        if (!::adapter.isInitialized) return
        val today = FruitRules.today()
        shown = FruitRules.filter(items, search.text.toString(), category,
            ExpiryFilter.entries[expiry.selectedItemPosition.coerceAtLeast(0)], today)
        val expired = items.count { FruitRules.expired(it, today) }
        totalStat.text = items.size.toString()
        expiredStat.text = expired.toString()
        todayStat.text = items.count { it.expiryDate == today }.toString()
        warning.visibility = if (expired > 0) View.VISIBLE else View.GONE
        warning.text = "⚠  $expired mặt hàng đã hết hạn. Chạm để xem và xử lý."
        summary.text = "${shown.size} mặt hàng" + if (shown.isNotEmpty()) "  •  Chạm để chỉnh sửa" else ""
        clearFilters.isEnabled = search.text.isNotBlank() || category != null || expiry.selectedItemPosition != 0
        emptyPanel.visibility = if (shown.isEmpty()) View.VISIBLE else View.GONE
        emptyTitle.text = if (items.isEmpty()) "Bắt đầu với trái cây đầu tiên" else "Chưa tìm thấy trái cây"
        emptyMessage.text = if (items.isEmpty()) "Thêm mặt hàng để quản lý số lượng, giá bán và hạn sử dụng tại đây."
            else "Thử một tên khác hoặc đặt lại bộ lọc để xem toàn bộ kho."
        adapter.notifyDataSetChanged()
    }

    private fun edit(fruit: Fruit?, draft: Bundle? = null) {
        val form = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(8), dp(20), dp(8)) }
        form.addView(label("Thông tin rõ ràng giúp bạn theo dõi kho dễ dàng hơn. Các ô có * là bắt buộc.", 13, R.color.admin_muted).apply {
            setPadding(0, 0, 0, dp(12))
        })
        fun field(label: String, value: String, type: Int = InputType.TYPE_CLASS_TEXT): EditText {
            val container = TextInputLayout(this).apply {
                hint = label
                boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
                setBoxCornerRadii(dp(12).toFloat(), dp(12).toFloat(), dp(12).toFloat(), dp(12).toFloat())
            }
            val input = TextInputEditText(container.context).apply {
                inputType = type
                textSize = 16f
                setText(value)
                if (type and InputType.TYPE_TEXT_FLAG_MULTI_LINE == 0) setSingleLine()
                setPadding(dp(14), dp(16), dp(14), dp(16))
            }
            container.addView(input)
            form.addView(container, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8); bottomMargin = dp(4) })
            return input
        }
        val name = field("Tên trái cây *", fruit?.name.orEmpty())
        val group = field("Danh mục *", fruit?.category.orEmpty())
        val price = field("Giá (VNĐ) *", fruit?.price?.toString().orEmpty(), InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
        val quantity = field("Số lượng *", fruit?.quantity?.toString().orEmpty(), InputType.TYPE_CLASS_NUMBER)
        val date = field("Hạn sử dụng (yyyy-MM-dd) *", fruit?.expiryDate ?: FruitRules.today())
        date.isFocusable = false
        date.contentDescription = "Chọn hạn sử dụng"
        form.addView(label("Chạm vào ngày để mở lịch. Hàng còn hạn đến hết ngày đã chọn.", 12, R.color.admin_muted).apply {
            setPadding(dp(4), 0, dp(4), dp(8))
        })
        date.setOnClickListener {
            val parts = date.text.toString().split("-").map { it.toInt() }
            DatePickerDialog(this, { _, year, month, day ->
                date.setText(String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day))
            }, parts[0], parts[1] - 1, parts[2]).show()
        }
        val description = field("Mô tả", fruit?.description.orEmpty(), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE)
        description.minLines = 2
        val fields = mapOf("name" to name, "category" to group, "price" to price,
            "quantity" to quantity, "date" to date, "description" to description)
        draft?.let { saved -> fields.forEach { (key, input) -> input.setText(saved.getString(key, "")) } }
        captureDraft = {
            Bundle().apply {
                putLong("id", fruit?.id ?: 0)
                fields.forEach { (key, input) -> putString(key, input.text.toString()) }
            }
        }
        val error = TextView(this).apply {
            setTextColor(getColor(R.color.admin_error))
            setPadding(0, dp(8), 0, 0)
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        form.addView(error)
        val scroll = ScrollView(this).apply { addView(form) }
        val dialog = AlertDialog.Builder(this).setTitle(if (fruit == null) "Thêm trái cây" else "Sửa trái cây")
            .setView(scroll).setPositiveButton("Lưu trái cây", null).setNegativeButton("Hủy", null)
            .apply { if (fruit != null) setNeutralButton("Xóa", null) }.create()
        dialog.setOnDismissListener {
            captureDraft = null
            if (savingDialog == dialog) { savingDialog = null; savingError = null }
        }
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val updated = Fruit(fruit?.id ?: 0, name.text.toString().trim(), group.text.toString().trim(),
                    price.text.toString().replace(',', '.').toDoubleOrNull() ?: Double.NaN,
                    quantity.text.toString().toIntOrNull() ?: -1, date.text.toString(), description.text.toString().trim())
                val message = FruitRules.validate(updated)
                if (message != null) {
                    error.text = message
                    val invalid = when {
                        updated.name.isBlank() -> name
                        updated.category.isBlank() -> group
                        !updated.price.isFinite() || updated.price < 0 -> price
                        updated.quantity < 0 -> quantity
                        else -> date
                    }
                    invalid.error = message
                    invalid.requestFocus()
                } else {
                    error.text = "Đang lưu…"
                    savingDialog = dialog
                    savingError = error
                    pendingFeedback = if (fruit == null) "Đã thêm trái cây vào kho" else "Đã lưu thay đổi"
                    presenter.save(updated)
                }
            }
            if (fruit != null) dialog.getButton(AlertDialog.BUTTON_NEUTRAL).apply {
                setTextColor(getColor(R.color.admin_error))
                setOnClickListener {
                AlertDialog.Builder(this@AdminFruitActivity).setTitle("Xóa trái cây?")
                    .setMessage("Xóa “${name.text}”? Thao tác này không thể hoàn tác.")
                    .setNegativeButton("Hủy", null).setPositiveButton("Xóa") { _, _ ->
                        pendingFeedback = "Đã xóa trái cây khỏi kho"
                        presenter.delete(fruit.id); dialog.dismiss()
                    }.show()
                }
            }
        }
        dialog.show()
    }

    private inner class FruitAdapter : BaseAdapter() {
        override fun getCount() = shown.size
        override fun getItem(position: Int) = shown[position]
        override fun getItemId(position: Int) = shown[position].id
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val row = (convertView as? LinearLayout) ?: LinearLayout(this@AdminFruitActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(20), 0, dp(20), 0)
                val card = column().apply {
                    background = rounded(getColor(R.color.admin_surface), 18, getColor(R.color.admin_border))
                    setPadding(dp(16), dp(16), dp(16), dp(16))
                }
                val heading = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
                heading.addView(label("", 20, bold = true), LinearLayout.LayoutParams(0, -2, 1f))
                heading.addView(label("Sửa  ›", 13, R.color.admin_primary).apply { setPadding(dp(12), 0, 0, 0) })
                card.addView(heading)
                card.addView(label("", 13, R.color.admin_muted).apply { setPadding(0, dp(4), 0, dp(12)) })
                val details = LinearLayout(context)
                details.addView(label("", 18, R.color.admin_primary, true), LinearLayout.LayoutParams(0, -2, 1f))
                details.addView(label("", 13, R.color.admin_muted).apply { gravity = Gravity.END })
                card.addView(details)
                card.addView(label("", 13).apply { setPadding(dp(10), dp(7), dp(10), dp(7)) },
                    LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) })
                card.addView(ImageView(context).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    contentDescription = getString(R.string.fruit_photo_description)
                    background = rounded(getColor(R.color.admin_surface), 12)
                    clipToOutline = true
                }, LinearLayout.LayoutParams(-1, dp(140)).apply { topMargin = dp(12) })
                addView(card)
            }
            val fruit = shown[position]
            val card = row.getChildAt(0) as LinearLayout
            (card.getChildAt(4) as ImageView).setImageResource(com.cie.bth2.ui.common.FruitImages.forFruit(fruit.name, fruit.category))
            ((card.getChildAt(0) as LinearLayout).getChildAt(0) as TextView).text = fruit.name
            (card.getChildAt(1) as TextView).text = fruit.category
            val details = card.getChildAt(2) as LinearLayout
            (details.getChildAt(0) as TextView).text = money.format(fruit.price)
            (details.getChildAt(1) as TextView).apply {
                text = if (fruit.quantity == 0) "Hết hàng" else "Tồn kho\n${fruit.quantity}"
                setTextColor(getColor(if (fruit.quantity == 0) R.color.admin_error else R.color.admin_muted))
            }
            val expired = FruitRules.expired(fruit)
            val today = fruit.expiryDate == FruitRules.today()
            (card.getChildAt(3) as TextView).apply {
                text = (if (expired) "⚠ Đã hết hạn" else if (today) "◷ Hết hạn hôm nay" else "✓ Còn hạn") + "  •  ${displayDate(fruit.expiryDate)}"
                setTextColor(if (expired) 0xFF9C2933.toInt() else if (today) 0xFF774411.toInt() else 0xFF246B45.toInt())
                background = rounded(if (expired) 0xFFFFE4E5.toInt() else if (today) 0xFFFFE9CF.toInt() else 0xFFE6F1E8.toInt(), 8)
            }
            return row
        }
    }
    private fun spinnerAdapter(values: List<String>) = ArrayAdapter(this, android.R.layout.simple_spinner_item, values).apply {
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    }
    private fun selection(action: () -> Unit) = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) = action()
        override fun onNothingSelected(parent: AdapterView<*>?) = Unit
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    private fun label(value: String, size: Int, color: Int = R.color.admin_ink, bold: Boolean = false) = TextView(this).apply {
        text = value
        textSize = size.toFloat()
        setTextColor(getColor(color))
        if (bold) setTypeface(null, Typeface.BOLD)
    }
    private fun rounded(color: Int, radius: Int, border: Int? = null) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
        border?.let { setStroke(dp(1), it) }
    }
    private fun displayDate(value: String): String = value.split("-").reversed().joinToString("/")
}
