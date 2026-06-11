@file:SuppressLint("NewApi")
package org.fossify.contacts.extensions

import android.annotation.SuppressLint
import android.content.Context
import android.icu.text.Transliterator
import org.fossify.commons.extensions.normalizeString
import org.fossify.commons.models.contacts.Contact
import org.fossify.contacts.helpers.Config
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

fun String.containsHanzi() = any { it.code in 0x4E00..0x9FFF || it.code in 0x3400..0x4DBF }

private fun Char.isLatin() = this.code in 65..122

fun Contact.getProperName(config: Config, useNickname: Boolean = true): String {
    val name = if (useNickname && config.showNicknameInstead && nickname.isNotEmpty()) {
        nickname
    } else {
        getNameToDisplay()
    }

    if (config.startNameWithSurname && name.contains(", ")) {
        val hasHanzi = surname.containsHanzi() || firstName.containsHanzi()
        if (hasHanzi) {
            val needsSpace = (surname.isNotEmpty() && surname.last().isLatin()) ||
                             (firstName.isNotEmpty() && firstName.first().isLatin())
            return if (needsSpace) name.replace(", ", " ") else name.replace(", ", "")
        }
    }
    return name
}
