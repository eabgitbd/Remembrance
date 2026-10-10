package com.codetoday.remembrance.domain.typography

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.codetoday.remembrance.R
import com.codetoday.remembrance.data.model.SupportedLanguage

object FontCatalog {

    val googleFontProvider: GoogleFont.Provider by lazy {
        GoogleFont.Provider(
            providerAuthority = "com.google.android.gms.fonts",
            providerPackage = "com.google.android.gms",
            certificates = R.array.com_google_android_gms_fonts_certs
        )
    }

    val BENGALI_FONTS = listOf(
        "Noto Serif Bengali",
        "Noto Sans Bengali",
        "Hind Siliguri",
        "Tiro Bangla",
        "Galada",
        "Mina",
        "Atma",
        "Anek Bangla",
        "Baloo Da 2",
        "Kalpurush"
    )

    val ENGLISH_FONTS = listOf(
        "Roboto",
        "Poppins",
        "Montserrat",
        "Open Sans",
        "Lato",
        "Inter",
        "Playfair Display",
        "Raleway",
        "Merriweather",
        "Oswald"
    )

    val ARABIC_FONTS = listOf(
        "Amiri",
        "Cairo",
        "Scheherazade New",
        "Noto Naskh Arabic",
        "Tajawal",
        "Almarai",
        "Readex Pro",
        "El Messiri",
        "Lateef",
        "Changa"
    )

    fun getFontsForLanguage(language: SupportedLanguage): List<String> {
        return when (language) {
            SupportedLanguage.BENGALI -> BENGALI_FONTS
            SupportedLanguage.ENGLISH -> ENGLISH_FONTS
            SupportedLanguage.ARABIC -> ARABIC_FONTS
        }
    }

    private val fontFamilyCache = mutableMapOf<String, FontFamily>()

    fun getFontFamily(fontName: String, language: SupportedLanguage): FontFamily {
        return fontFamilyCache.getOrPut(fontName) {
            try {
                val googleFont = GoogleFont(fontName)
                FontFamily(
                    Font(googleFont = googleFont, fontProvider = googleFontProvider)
                )
            } catch (e: Exception) {
                when (language) {
                    SupportedLanguage.BENGALI -> FontFamily.Serif
                    SupportedLanguage.ENGLISH -> FontFamily.Default
                    SupportedLanguage.ARABIC -> FontFamily.Serif
                }
            }
        }
    }
}
