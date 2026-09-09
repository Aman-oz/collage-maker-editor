package org.example.project.ui.emoji

import androidx.compose.ui.geometry.Offset

internal enum class EmojiCategory(val label: String) {
    Smileys("Smileys"),
    Animals("Animals"),
    Food("Food"),
    Activities("Activities"),
    Travel("Travel"),
    Objects("Objects"),
    Symbols("Symbols"),
    Flags("Flags"),
}

internal val EmojisByCategory: Map<EmojiCategory, List<String>> = mapOf(
    EmojiCategory.Smileys to listOf(
        "😀", "😃", "😄", "😁", "😆", "😅", "🤣", "😂", "🙂", "😉",
        "😊", "😇", "🥰", "😍", "😘", "😋", "😛", "🤪", "🤨", "🧐",
        "😎", "🥳", "😏", "😴", "🤗", "🤔", "🤫", "🤯", "😳", "🥶",
        "😱", "😭", "😡", "🥺", "😢", "😜", "🤩", "😬", "🙃", "😪",
    ),
    EmojiCategory.Animals to listOf(
        "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼", "🐨", "🐯",
        "🦁", "🐮", "🐷", "🐸", "🐵", "🐔", "🐧", "🐦", "🐤", "🦆",
        "🦉", "🐺", "🐗", "🐴", "🦄", "🐝", "🐛", "🦋", "🐌", "🐞",
        "🐢", "🐍", "🦎", "🐙", "🦑", "🦀", "🐬", "🐳", "🦈", "🐘",
    ),
    EmojiCategory.Food to listOf(
        "🍏", "🍎", "🍊", "🍋", "🍌", "🍉", "🍇", "🍓", "🍒", "🍑",
        "🥭", "🍍", "🥝", "🍅", "🥑", "🥦", "🥕", "🌽", "🥐", "🍞",
        "🧀", "🍳", "🥞", "🥓", "🍔", "🍟", "🍕", "🌭", "🌮", "🌯",
        "🍜", "🍣", "🍱", "🍤", "🍦", "🍩", "🍪", "🎂", "🍫", "🍭",
    ),
    EmojiCategory.Activities to listOf(
        "⚽", "🏀", "🏈", "⚾", "🎾", "🏐", "🏉", "🎱", "🏓", "🏸",
        "🥊", "🥋", "⛳", "🎣", "🎽", "🛹", "🛷", "⛸️", "🎿", "🏆",
        "🥇", "🎖️", "🎗️", "🎫", "🎪", "🎭", "🎨", "🎬", "🎤", "🎧",
        "🎼", "🎹", "🥁", "🎷", "🎺", "🎸", "🎻", "🎲", "🎯", "🎮",
    ),
    EmojiCategory.Travel to listOf(
        "🚗", "🚕", "🚌", "🏎️", "🚓", "🚑", "🚒", "🚚", "🚲", "🏍️",
        "✈️", "🚀", "🚁", "⛵", "🚤", "🛳️", "⚓", "🗺️", "🗽", "🗼",
        "🏰", "🎡", "🎢", "🏖️", "🏝️", "🌋", "⛰️", "🏔️", "🏕️", "🏠",
        "🏢", "🏥", "🏦", "🏨", "⛪", "🕌", "🌅", "🌄", "🌉", "🌌",
    ),
    EmojiCategory.Objects to listOf(
        "⌚", "📱", "💻", "🖥️", "🖨️", "🖱️", "📷", "📹", "📺", "📻",
        "⏰", "⏳", "🔋", "💡", "🔦", "🕯️", "💰", "💳", "💎", "🔧",
        "🔨", "⚙️", "🔩", "🔫", "🔪", "🗡️", "🛡️", "🔮", "📿", "🔭",
        "💊", "🩺", "🧬", "🧹", "🧴", "🗑️", "🔑", "🔒", "🔓", "📌",
    ),
    EmojiCategory.Symbols to listOf(
        "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "💔", "❣️",
        "💕", "💞", "💓", "💗", "💖", "💘", "💝", "✨", "⭐", "🌟",
        "💫", "🔥", "💯", "✅", "❌", "❓", "❗", "⚠️", "♻️", "🔞",
        "📛", "🔰", "🆗", "🆕", "🔟", "🔢", "▶️", "⏸️", "🔀", "🔁",
    ),
    EmojiCategory.Flags to listOf(
        "🏳️", "🏴", "🏁", "🚩", "🏳️‍🌈", "🇺🇸", "🇬🇧", "🇨🇦", "🇦🇺", "🇮🇳",
        "🇫🇷", "🇩🇪", "🇮🇹", "🇪🇸", "🇵🇹", "🇯🇵", "🇰🇷", "🇨🇳", "🇧🇷", "🇲🇽",
        "🇷🇺", "🇿🇦", "🇪🇬", "🇳🇬", "🇰🇪", "🇸🇦", "🇦🇪", "🇹🇷", "🇬🇷", "🇳🇱",
        "🇸🇪", "🇳🇴", "🇩🇰", "🇫🇮", "🇨🇭", "🇮🇪", "🇵🇱", "🇦🇹", "🇧🇪", "🇳🇿",
    ),
)

/** A single emoji placed on the photo: unique [id], the glyph, its center as a 0f..1f fraction
 * of the image bounds, and a relative [scale] (1f = the base placed size). */
internal data class PlacedEmoji(
    val id: Long,
    val emoji: String,
    val offsetFraction: Offset = Offset(0.5f, 0.5f),
    val scale: Float = 1f,
)

internal const val EmojiBaseSizeSp = 40f
internal val EmojiScaleRange = 0.3f..4f
