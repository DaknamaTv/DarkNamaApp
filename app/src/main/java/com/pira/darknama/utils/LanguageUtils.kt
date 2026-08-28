package com.pira.darknama.utils

object LanguageUtils {
    /**
     * Checks if the given text contains Farsi (Persian) characters.
     * Farsi characters have Unicode values in the range 0x0600 to 0x06FF and 0xFB50 to 0xFDFF.
     */
    fun isFarsiText(text: String): Boolean {
        // Check if the text contains any Farsi characters
        for (char in text) {
            val codePoint = char.toInt()
            // Check for Arabic/Persian script range
            if ((codePoint >= 0x0600 && codePoint <= 0x06FF) || 
                (codePoint >= 0xFB50 && codePoint <= 0xFDFF) ||
                (codePoint >= 0xFE70 && codePoint <= 0xFEFF)) {
                return true
            }
        }
        return false
    }
    
    /**
     * Checks if the given text is primarily in English (Latin characters).
     * This is a simple check that considers text as English if it contains
     * mostly Latin characters (A-Z, a-z) and common punctuation/spaces.
     */
    fun isEnglishText(text: String): Boolean {
        if (text.isEmpty()) return true
        
        var latinCharCount = 0
        var totalCharCount = 0
        
        for (char in text) {
            // Skip spaces, numbers, and common punctuation for this check
            if (char.isLetter()) {
                totalCharCount++
                // Check if it's a Latin character (English alphabet)
                if (char.toLowerCase() in 'a'..'z') {
                    latinCharCount++
                }
            }
        }
        
        // If we have no letters, consider it English
        if (totalCharCount == 0) return true
        
        // Consider it English if more than 50% of letters are Latin
        return (latinCharCount.toDouble() / totalCharCount) > 0.5
    }
    
    /**
     * Checks if the title should be displayed (is in English or other supported languages).
     * Returns true if the title is not in Farsi.
     */
    fun shouldDisplayTitle(title: String): Boolean {
        // If it's Farsi text, don't display
        return !isFarsiText(title)
    }
}