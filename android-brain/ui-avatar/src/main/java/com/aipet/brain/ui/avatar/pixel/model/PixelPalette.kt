package com.aipet.brain.ui.avatar.pixel.model

data class PixelPaletteEntry(
    val key: String,
    val color: PixelColor
) {
    init {
        require(key.isNotBlank()) { "Palette entry key must not be blank." }
    }
}

data class PixelPalette(
    val entries: List<PixelPaletteEntry>
) {
    init {
        require(entries.isNotEmpty()) { "Pixel palette must include at least one entry." }
        require(entries.map(PixelPaletteEntry::key).distinct().size == entries.size) {
            "Pixel palette entry keys must be unique."
        }
    }

    val size: Int = entries.size

    operator fun get(index: Int): PixelPaletteEntry = entries[index]

    fun containsKey(key: String): Boolean = entries.any { it.key == key }

    fun indexOf(key: String): Int {
        val index = entries.indexOfFirst { it.key == key }
        require(index >= 0) { "Pixel palette does not contain key '$key'." }
        return index
    }
}

object PixelPetDefaultPalette {
    const val TransparentKey: String = "transparent"
    const val EyeBaseKey: String = "eye_base"
    const val PupilKey: String = "pupil"
    const val HighlightKey: String = "highlight"
    const val AccentKey: String = "accent"

    val palette: PixelPalette = PixelPalette(
        entries = listOf(
            PixelPaletteEntry(key = TransparentKey, color = PixelColor.Transparent),
            PixelPaletteEntry(key = EyeBaseKey, color = PixelColor(red = 230, green = 240, blue = 255)),
            PixelPaletteEntry(key = PupilKey, color = PixelColor(red = 42, green = 52, blue = 74)),
            PixelPaletteEntry(key = HighlightKey, color = PixelColor(red = 255, green = 255, blue = 255)),
            PixelPaletteEntry(key = AccentKey, color = PixelColor(red = 255, green = 196, blue = 107))
        )
    )
}
