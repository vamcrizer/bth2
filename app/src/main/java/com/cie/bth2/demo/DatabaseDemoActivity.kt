package com.cie.bth2.demo

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import com.cie.bth2.data.DatabaseHelper

/** Interactive screen for presenting the SQLite schema and table operations. */
class DatabaseDemoActivity : Activity() {
    private lateinit var database: DatabaseHelper
    private lateinit var tablePicker: Spinner
    private lateinit var output: TextView
    private lateinit var status: TextView
    private val tableNames = DatabaseHelper.DEMO_TABLES

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = DatabaseHelper(applicationContext, "fruit_shop_showcase.db")
        buildScreen()
        refresh("Database đã sẵn sàng")
    }

    override fun onDestroy() {
        database.close()
        super.onDestroy()
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(16))
            setBackgroundColor(0xFFF5F7FA.toInt())
        }
        root.addView(actionButton("‹  " + getString(com.cie.bth2.R.string.navigate_back)) { finish() })
        root.addView(TextView(this).apply {
            text = "DATABASE SHOWCASE"
            textSize = 24f
            setTextColor(0xFF16324F.toInt())
            setPadding(0, 0, 0, dp(4))
        })
        root.addView(TextView(this).apply {
            text = "Fruit Shop Management • SQLite • 4 bảng quan hệ"
            textSize = 14f
            setTextColor(0xFF607080.toInt())
            setPadding(0, 0, 0, dp(14))
        })

        tablePicker = Spinner(this).apply {
            adapter = ArrayAdapter(this@DatabaseDemoActivity, android.R.layout.simple_spinner_dropdown_item, tableNames)
            setOnItemSelectedListener(SimpleItemSelectedListener { refresh("Đang xem bảng ${selectedTable()}") })
        }
        root.addView(tablePicker, matchWidth())

        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(actionButton("Xem bảng") { refresh("Đã tải dữ liệu bảng ${selectedTable()}") }, weightedWidth())
        actions.addView(actionButton("SQL CREATE") { showSql(database.createSqlFor(selectedTable())) }, weightedWidth())
        root.addView(actions)

        val destructive = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        destructive.addView(actionButton("DROP bảng") { confirmDrop() }, weightedWidth())
        destructive.addView(actionButton("Reset database") { confirmReset() }, weightedWidth())
        root.addView(destructive)

        status = TextView(this).apply {
            textSize = 13f
            setTextColor(0xFF18794E.toInt())
            setPadding(0, dp(8), 0, dp(8))
        }
        root.addView(status)

        val scroll = ScrollView(this)
        output = TextView(this).apply {
            textSize = 13f
            typeface = android.graphics.Typeface.MONOSPACE
            setTextColor(0xFF1B2733.toInt())
            setBackgroundColor(0xFFFFFFFF.toInt())
            setPadding(dp(14), dp(14), dp(14), dp(14))
        }
        scroll.addView(output)
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
    }

    private fun refresh(message: String) {
        val table = selectedTable()
        status.text = message
        output.text = buildString {
            appendLine("DATABASE: fruit_shop_showcase.db")
            appendLine("VERSION: ${DatabaseHelper.DATABASE_VERSION}")
            appendLine("TABLE: $table")
            appendLine("─".repeat(42))
            appendLine("SCHEMA")
            appendLine(schemaFor(table))
            appendLine()
            appendLine("ROWS")
            appendLine(readRows(table))
            appendLine()
            appendLine("FOREIGN KEYS")
            append(readForeignKeys(table))
        }
    }

    private fun schemaFor(table: String): String {
        val dbName = table.removeSurrounding("\"", "\"")
        return database.readableDatabase.rawQuery(
            "SELECT sql FROM sqlite_master WHERE type = 'table' AND name = ?",
            arrayOf(dbName)
        ).use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else "(Bảng chưa tồn tại — hãy Reset database)" }
    }

    private fun readRows(table: String): String {
        val safeTable = table
        return try {
            database.readableDatabase.rawQuery("SELECT * FROM $safeTable LIMIT 50", null).use { cursor ->
                if (cursor.count == 0) return@use "(Không có dữ liệu)"
                val columns = (0 until cursor.columnCount).map(cursor::getColumnName)
                buildString {
                    appendLine(columns.joinToString(" | "))
                    appendLine("─".repeat(42))
                    while (cursor.moveToNext()) {
                        appendLine(columns.indices.joinToString(" | ") { index ->
                            cursor.getString(index) ?: "NULL"
                        })
                    }
                }.trimEnd()
            }
        } catch (_: Exception) {
            "(Không đọc được bảng — bảng có thể đã bị DROP)"
        }
    }

    private fun readForeignKeys(table: String): String {
        val dbName = table.removeSurrounding("\"", "\"")
        return try {
            database.readableDatabase.rawQuery("PRAGMA foreign_key_list('$dbName')", null).use { cursor ->
                if (cursor.count == 0) return@use "Không có khóa ngoại"
                buildString {
                    while (cursor.moveToNext()) {
                        appendLine("${cursor.getString(3)} → ${cursor.getString(2)}.${cursor.getString(4)} (ON DELETE ${cursor.getString(6)})")
                    }
                }.trimEnd()
            }
        } catch (_: Exception) {
            "Không đọc được khóa ngoại"
        }
    }

    private fun confirmDrop() {
        val table = selectedTable()
        AlertDialog.Builder(this)
            .setTitle("DROP TABLE")
            .setMessage("Xóa bảng $table khỏi database demo? Dữ liệu bảng này sẽ mất đến khi Reset database.")
            .setNegativeButton("Hủy", null)
            .setPositiveButton("DROP bảng") { _, _ ->
                database.dropTable(table)
                refresh("Đã chạy: ${database.dropSqlFor(table)}")
            }
            .show()
    }

    private fun confirmReset() {
        AlertDialog.Builder(this)
            .setTitle("Khôi phục database demo")
            .setMessage("Tạo lại 4 bảng và nạp dữ liệu mẫu? Dữ liệu demo hiện tại sẽ bị thay thế.")
            .setNegativeButton("Hủy", null)
            .setPositiveButton("Reset") {
                    _, _ ->
                database.resetDemoDatabase()
                refresh("Đã tạo lại 4 bảng và nạp seed data")
            }
            .show()
    }

    private fun showSql(sql: String) {
        AlertDialog.Builder(this)
            .setTitle("SQL CREATE — ${selectedTable()}")
            .setMessage(sql)
            .setPositiveButton("Đóng", null)
            .show()
    }

    private fun selectedTable(): String = tableNames[tablePicker.selectedItemPosition]

    private fun actionButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        textSize = 12f
        setOnClickListener { action() }
    }

    private fun matchWidth() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    private fun weightedWidth() = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
