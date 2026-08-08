package org.fossify.contacts.helpers

import android.content.Context
import android.content.res.Configuration
import org.fossify.commons.models.contacts.Contact
import org.fossify.contacts.R
import java.util.Locale

object MeNicknameHelper {
    private var allMeRegexes: List<Regex>? = null

    @Synchronized
    fun init(context: Context) {
        if (allMeRegexes != null) return
        
        val regexes = mutableSetOf<String>()
        val locales = context.resources.assets.locales
        val currentConfig = context.resources.configuration
        
        // Add the current one first for speed
        regexes.add(context.getString(R.string.me_nickname_regex))

        locales.forEach { localeTag ->
            try {
                val locale = if (localeTag.isEmpty()) Locale.ROOT else Locale.forLanguageTag(localeTag)
                val config = Configuration(currentConfig)
                config.setLocale(locale)
                val localizedContext = context.createConfigurationContext(config)
                val regexString = localizedContext.getString(R.string.me_nickname_regex)
                if (regexString.isNotEmpty()) {
                    regexes.add(regexString)
                }
            } catch (ignored: Exception) {
            }
        }
        
        allMeRegexes = regexes.map { Regex(it, RegexOption.IGNORE_CASE) }
    }

    fun isMeNickname(nickname: String): Boolean {
        if (nickname.isEmpty()) return false
        val trimmed = nickname.trim()
        return allMeRegexes?.any { it.matches(trimmed) } ?: false
    }

    fun mergeMeContacts(context: Context, contacts: ArrayList<Contact>): ArrayList<Contact> {
        val (meContacts, others) = contacts.partition { isMeNickname(it.nickname) }
        if (meContacts.isEmpty()) return contacts

        val meCanonical = context.getString(R.string.me_canonical)
        if (meContacts.size == 1) {
            meContacts.first().nickname = meCanonical
            return contacts
        }

        val firstMe = meContacts.first().copy()
        firstMe.nickname = meCanonical

        for (i in 1 until meContacts.size) {
            val other = meContacts[i]

            other.phoneNumbers.forEach { num ->
                if (firstMe.phoneNumbers.none { it.value == num.value }) {
                    firstMe.phoneNumbers.add(num)
                }
            }

            other.emails.forEach { email ->
                if (firstMe.emails.none { it.value == email.value }) {
                    firstMe.emails.add(email)
                }
            }

            other.addresses.forEach { addr ->
                if (firstMe.addresses.none { it.value == addr.value }) {
                    firstMe.addresses.add(addr)
                }
            }

            if (other.notes.isNotEmpty()) {
                if (firstMe.notes.isEmpty()) {
                    firstMe.notes = other.notes
                } else if (!firstMe.notes.contains(other.notes)) {
                    firstMe.notes += "\n" + other.notes
                }
            }

            other.groups.forEach { group ->
                if (firstMe.groups.none { it.id == group.id }) {
                    firstMe.groups.add(group)
                }
            }

            other.websites.forEach { site ->
                if (firstMe.websites.none { it == site }) {
                    firstMe.websites.add(site)
                }
            }

            other.IMs.forEach { im ->
                if (firstMe.IMs.none { it.value == im.value }) {
                    firstMe.IMs.add(im)
                }
            }
        }

        val result = ArrayList<Contact>(others)
        result.add(0, firstMe)
        return result
    }
}
