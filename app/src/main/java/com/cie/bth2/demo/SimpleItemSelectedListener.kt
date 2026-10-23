package com.cie.bth2.demo

import android.view.View
import android.widget.AdapterView

internal class SimpleItemSelectedListener(
    private val onSelected: () -> Unit
) : AdapterView.OnItemSelectedListener {
    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) = onSelected()
    override fun onNothingSelected(parent: AdapterView<*>?) = Unit
}
