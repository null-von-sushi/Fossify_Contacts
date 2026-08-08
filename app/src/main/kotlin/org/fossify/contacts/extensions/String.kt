@file:SuppressLint("NewApi")
package org.fossify.contacts.extensions

import android.annotation.SuppressLint
import android.content.Context
import org.fossify.commons.extensions.normalizeString
import org.fossify.commons.models.contacts.Contact
import org.fossify.contacts.helpers.Config
import org.fossify.contacts.helpers.HanziHelper
import java.util.Locale

fun String.getSortKey(context: Context): String {
    if (this.isEmpty()) {
        return ""
    }

    val result = HanziHelper.getFullSortKey(this)
    return result.lowercase(Locale.getDefault()).normalizeString()
}

fun String.containsHanzi() = any { it.code in 0x4E00..0x9FFF || it.code in 0x3400..0x4DBF }

private fun Char.isLatin() = this.code in 65..122

fun Contact.isMeNickname() = nickname.trim().matches(Regex("[Mm][Ee]"))
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
