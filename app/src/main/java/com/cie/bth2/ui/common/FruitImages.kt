package com.cie.bth2.ui.common

import com.cie.bth2.R
import java.text.Normalizer
import java.util.Locale

/** Bundled illustrative photography; no network is needed to display products. */
object FruitImages {
    fun forFruit(name: String, category: String): Int {
        val key = Normalizer.normalize("$name $category", Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT)
        return when {
            key.contains("tao") || key.contains("apple") -> R.drawable.fruit_apple
            key.contains("cam") || key.contains("quyt") || key.contains("orange") -> R.drawable.fruit_orange
            key.contains("chuoi") || key.contains("banana") -> R.drawable.fruit_banana
            else -> R.drawable.fruit_hero
        }
    }
}
