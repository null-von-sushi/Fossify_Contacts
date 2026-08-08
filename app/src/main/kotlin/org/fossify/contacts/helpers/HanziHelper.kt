@file:SuppressLint("NewApi")
package org.fossify.contacts.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.icu.text.Transliterator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.fossify.contacts.databases.HanziPinyin
import org.fossify.contacts.databases.HanziPinyinDatabase
import org.fossify.contacts.extensions.containsHanzi
import java.util.concurrent.ConcurrentHashMap

object HanziHelper {
    private val memoryCache = ConcurrentHashMap<String, String>()
    @Volatile
    private var isCacheLoaded = false

    private val transliterator: Transliterator? by lazy {
        try {
            Transliterator.getInstance("Any-Latin; Latin-ASCII")
        } catch (e: Exception) {
            null
        }
    }

    suspend fun initCache(context: Context) {
        if (isCacheLoaded) return
        withContext(Dispatchers.IO) {
            synchronized(this@HanziHelper) {
                if (isCacheLoaded) return@synchronized
                val db = HanziPinyinDatabase.getDatabase(context)
                val all = db.hanziPinyinDao().getAll()
                all.forEach {
                    memoryCache[it.hanzi] = it.pinyin
                }
                isCacheLoaded = true
            }
        }
    }

    fun isHanziReady(text: String): Boolean {
        if (text.isEmpty()) return true
        val firstChar = text.take(1)
        if (!firstChar.containsHanzi()) return true
        return memoryCache.containsKey(firstChar)
    }

    fun getPinyin(char: String): String? {
        return memoryCache[char]
    }

    suspend fun processHanzi(text: String, context: Context) {
        if (text.isEmpty()) return
        val firstChar = text.take(1)
        if (!firstChar.containsHanzi() || memoryCache.containsKey(firstChar)) return

        withContext(Dispatchers.IO) {
            val pinyin = try {
                transliterator?.transliterate(firstChar) ?: firstChar
            } catch (e: Exception) {
                firstChar
            }
            
            memoryCache[firstChar] = pinyin
            val db = HanziPinyinDatabase.getDatabase(context)
            db.hanziPinyinDao().insert(HanziPinyin(firstChar, pinyin))
        }
    }
    
    fun getFullSortKey(text: String): String {
        if (text.isEmpty()) return ""
        val firstChar = text.take(1)
        return if (firstChar.containsHanzi()) {
            val pinyin = memoryCache[firstChar] ?: firstChar
            pinyin + text.drop(1)
        } else {
            text
        }
    }
}
