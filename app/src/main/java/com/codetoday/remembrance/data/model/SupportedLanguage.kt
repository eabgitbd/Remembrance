package com.codetoday.remembrance.data.model

enum class SupportedLanguage(
    val displayName: String,
    val code: String,
    val defaultFont: String
) {
    BENGALI("বাংলা", "bn", "Noto Serif Bengali"),
    ENGLISH("English", "en", "Roboto"),
    ARABIC("العربية", "ar", "Amiri");

    val isRtl: Boolean
        get() = this == ARABIC
}
