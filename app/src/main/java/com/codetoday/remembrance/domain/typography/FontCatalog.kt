package com.codetoday.remembrance.domain.typography

import android.graphics.Typeface
import androidx.compose.ui.text.font.FontFamily
import com.codetoday.remembrance.data.model.SupportedLanguage

object FontCatalog {

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

    private val typefaceCache = mutableMapOf<String, Typeface>()

    fun getTypeface(fontName: String, language: SupportedLanguage): Typeface {
        return typefaceCache.getOrPut(fontName) {
            when (fontName) {
                // English typography styles
                "Roboto" -> Typeface.create("sans-serif", Typeface.NORMAL)
                "Poppins" -> Typeface.create("sans-serif-medium", Typeface.NORMAL)
                "Montserrat" -> Typeface.create("sans-serif-black", Typeface.NORMAL)
                "Open Sans" -> Typeface.create("sans-serif", Typeface.NORMAL)
                "Lato" -> Typeface.create("sans-serif-light", Typeface.NORMAL)
                "Inter" -> Typeface.create("sans-serif", Typeface.NORMAL)
                "Playfair Display" -> Typeface.create("serif", Typeface.BOLD)
                "Raleway" -> Typeface.create("sans-serif-thin", Typeface.NORMAL)
                "Merriweather" -> Typeface.create("serif", Typeface.NORMAL)
                "Oswald" -> Typeface.create("sans-serif-condensed", Typeface.BOLD)

                // Bengali typography styles
                "Noto Serif Bengali" -> Typeface.create("serif", Typeface.NORMAL)
                "Noto Sans Bengali" -> Typeface.create("sans-serif", Typeface.NORMAL)
                "Hind Siliguri" -> Typeface.create("sans-serif-medium", Typeface.NORMAL)
                "Tiro Bangla" -> Typeface.create("serif", Typeface.ITALIC)
                "Galada" -> Typeface.create("cursive", Typeface.BOLD)
                "Mina" -> Typeface.create("sans-serif-light", Typeface.NORMAL)
                "Atma" -> Typeface.create("casual", Typeface.NORMAL)
                "Anek Bangla" -> Typeface.create("sans-serif-condensed", Typeface.NORMAL)
                "Baloo Da 2" -> Typeface.create("sans-serif-black", Typeface.NORMAL)
                "Kalpurush" -> Typeface.create("serif", Typeface.BOLD)

                // Arabic typography styles
                "Amiri" -> Typeface.create("serif", Typeface.NORMAL)
                "Cairo" -> Typeface.create("sans-serif-medium", Typeface.NORMAL)
                "Scheherazade New" -> Typeface.create("serif", Typeface.BOLD)
                "Noto Naskh Arabic" -> Typeface.create("serif", Typeface.NORMAL)
                "Tajawal" -> Typeface.create("sans-serif-light", Typeface.NORMAL)
                "Almarai" -> Typeface.create("sans-serif", Typeface.NORMAL)
                "Readex Pro" -> Typeface.create("sans-serif-medium", Typeface.NORMAL)
                "El Messiri" -> Typeface.create("casual", Typeface.NORMAL)
                "Lateef" -> Typeface.create("cursive", Typeface.NORMAL)
                "Changa" -> Typeface.create("sans-serif-condensed", Typeface.BOLD)

                else -> when (language) {
                    SupportedLanguage.BENGALI -> Typeface.create("serif", Typeface.NORMAL)
                    SupportedLanguage.ARABIC -> Typeface.create("serif", Typeface.NORMAL)
                    SupportedLanguage.ENGLISH -> Typeface.create("sans-serif", Typeface.NORMAL)
                }
            }
        }
    }

    private val fontFamilyCache = mutableMapOf<String, FontFamily>()

    fun getFontFamily(fontName: String, language: SupportedLanguage): FontFamily {
        return fontFamilyCache.getOrPut(fontName) {
            val typeface = getTypeface(fontName, language)
            FontFamily(typeface)
        }
    }
}
