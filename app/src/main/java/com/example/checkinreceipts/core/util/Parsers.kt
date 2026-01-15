package com.example.checkinreceipts.core.util

fun extractHeader(text: String): String? =
    text.lineSequence()
        .map { it.trim() }
        .firstOrNull { it.length in 3..40 }

private val dateRegex = Regex("""(\d{4}[-/.]\d{1,2}[-/.]\d{1,2}|\d{1,2}[-/.]\d{1,2}[-/.]\d{2,4})""")
fun extractDate(text: String): String? = dateRegex.find(text)?.value

private val totalRegex = Regex("""(?i)(total|amount|sum)\s*[:]?\s*([¥$€£]?\s?\d+(?:[.,]\d{1,2})?)""")
fun extractTotal(text: String): String? = totalRegex.find(text)?.groupValues?.getOrNull(2)