@file:SuppressLint("NewApi")
package org.fossify.contacts.extensions

import android.annotation.SuppressLint
import android.content.Context
import android.icu.text.Transliterator
import org.fossify.commons.extensions.normalizeString
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

private val transliterator: Transliterator? by lazy {
    try {
        Transliterator.getInstance("Any-Latin; Latin-ASCII")
    } catch (e: Exception) {
        null
    }
}

private const val SORT_KEY_PREFS = "sort_key_cache_v1"
private val sortKeyCache = ConcurrentHashMap<String, String>()
private var isCacheLoaded = false

fun String.getSortKey(context: Context): String {
    if (this.isEmpty()) {
        return ""
    }

    if (!isCacheLoaded) {
        synchronized(sortKeyCache) {
            if (!isCacheLoaded) {
                val prefs = context.getSharedPreferences(SORT_KEY_PREFS, Context.MODE_PRIVATE)
                prefs.all.forEach { (key, value) ->
                    if (value is String) {
                        sortKeyCache[key] = value
                    }
                }
                isCacheLoaded = true
            }
        }
    }

    val cached = sortKeyCache[this]
    if (cached != null) {
        return cached
    }

    val result = try {
        val transliterated = transliterator?.transliterate(this) ?: this
        transliterated.lowercase(Locale.getDefault()).normalizeString()
    } catch (e: Exception) {
        this.lowercase(Locale.getDefault()).normalizeString()
    }

    sortKeyCache[this] = result
    context.getSharedPreferences(SORT_KEY_PREFS, Context.MODE_PRIVATE).edit().putString(this, result).apply()
    return result
}
