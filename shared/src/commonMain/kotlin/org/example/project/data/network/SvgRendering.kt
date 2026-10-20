package org.example.project.data.network

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Draws an SVG document into a new [ImageBitmap] whose longer side is [maxSidePx], keeping the
 * document's aspect ratio (its `viewBox`, else its own width and height). The sticker library is
 * served as SVG, and Compose Multiplatform's own SVG decoding is not available on Android, so each
 * platform renders it itself: AndroidSVG on Android, Skia's SVG module on iOS.
 *
 * @throws Exception if the bytes are not a renderable SVG; callers treat that as a load failure.
 */
expect fun rasterizeSvg(bytes: ByteArray, maxSidePx: Int): ImageBitmap

/** Whether [url] names an SVG file, ignoring any query or fragment. */
internal fun isSvgUrl(url: String): Boolean =
    url.substringBefore('?').substringBefore('#').endsWith(".svg", ignoreCase = true)

private val StyleBlock = Regex("""<style[^>]*>(.*?)</style>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
private val CssRule = Regex("""([^{}]+)\{([^{}]*)\}""")
private val CssComment = Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL)
private val CData = Regex("""<!\[CDATA\[|]]>""")
private val ClassSelector = Regex("""^\.([A-Za-z0-9_-]+)$""")
private val OpeningTag = Regex("""<([A-Za-z][\w:.-]*)(\s[^<>]*?)?(/?)>""")
private val ClassAttribute = Regex("""\sclass\s*=\s*"([^"]*)"""")
private val StyleAttribute = Regex("""\sstyle\s*=\s*"([^"]*)"""")

/**
 * Copies the SVG's `<style>` class rules onto the elements that use them, as `style` attributes.
 *
 * Illustrator exports (the sticker library) put every fill and opacity in a stylesheet of class
 * rules (`.cls-1, .cls-4 { fill: … }`) and give the shapes only a `class`. Skia's SVG module reads
 * `style` attributes but not stylesheets, so on iOS every shape would draw in default black. Only
 * plain class selectors are handled, which is all those exports use; rules apply in stylesheet
 * order, so a later rule wins as it would in CSS, and an element's own `style` still wins over all.
 */
internal fun inlineSvgClassStyles(svg: String): String {
    val rules = StyleBlock.findAll(svg)
        .flatMap { block -> CssRule.findAll(CssComment.replace(CData.replace(block.groupValues[1], ""), "")) }
        .mapNotNull { rule ->
            val classes = rule.groupValues[1].split(',')
                .mapNotNull { ClassSelector.matchEntire(it.trim())?.groupValues?.get(1) }
                .toSet()
            val declarations = rule.groupValues[2].split(';').map { it.trim() }.filter { ':' in it }
            if (classes.isEmpty() || declarations.isEmpty()) null else classes to declarations
        }
        .toList()
    if (rules.isEmpty()) return svg

    return OpeningTag.replace(svg) { tag ->
        val attributes = tag.groupValues[2]
        val classAttribute = ClassAttribute.find(attributes) ?: return@replace tag.value
        val classes = classAttribute.groupValues[1].split(Regex("\\s+")).filter { it.isNotEmpty() }.toSet()
        val declarations = rules.filter { (ruleClasses, _) -> ruleClasses.any { it in classes } }.flatMap { it.second }
        if (declarations.isEmpty()) return@replace tag.value

        val ownStyle = StyleAttribute.find(attributes)
        val style = (declarations + listOfNotNull(ownStyle?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }))
            .joinToString(";")
        val newAttributes = if (ownStyle != null) {
            attributes.replaceRange(ownStyle.range, " style=\"$style\"")
        } else {
            "$attributes style=\"$style\""
        }
        "<${tag.groupValues[1]}$newAttributes${tag.groupValues[3]}>"
    }
}
