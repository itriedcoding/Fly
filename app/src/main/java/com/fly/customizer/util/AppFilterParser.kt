package com.fly.customizer.util

/**
 * Parser for the `appfilter.xml` file that icon packs ship.
 *
 * Icon packs declare entries as
 * `<item component="ComponentInfo{com.app/com.app.Main}" drawable="app_icon"/>`
 * and may use the short class form `com.app/.Main`.
 */
object AppFilterParser {

    private val itemPattern = Regex("""<item\b[^>]*>""", RegexOption.IGNORE_CASE)
    private val componentAttribute = Regex("""component\s*=\s*"([^"]*)"""", RegexOption.IGNORE_CASE)
    private val drawableAttribute = Regex("""drawable\s*=\s*"([^"]*)"""", RegexOption.IGNORE_CASE)

    /**
     * Returns a map of normalised `package/class` component keys to drawable names.
     */
    fun parse(xml: String): Map<String, String> {
        if (xml.isBlank()) return emptyMap()
        val result = LinkedHashMap<String, String>()
        for (match in itemPattern.findAll(xml)) {
            val tag = match.value
            val rawComponent = componentAttribute.find(tag)?.groupValues?.get(1) ?: continue
            val drawable = drawableAttribute.find(tag)?.groupValues?.get(1) ?: continue
            val key = normaliseComponent(rawComponent) ?: continue
            result[key] = drawable
        }
        return result
    }

    /**
     * Accepts `ComponentInfo{pkg/cls}`, `pkg/cls` and `pkg` and returns `pkg/cls`.
     */
    fun normaliseComponent(rawValue: String): String? {
        var value = rawValue.trim()
        if (value.isEmpty()) return null
        if (value.startsWith("ComponentInfo{", ignoreCase = true)) {
            value = value.substring("ComponentInfo{".length)
        }
        if (value.endsWith("}")) {
            value = value.dropLast(1)
        }
        value = value.trim()
        if (value.isEmpty()) return null
        val slash = value.indexOf('/')
        if (slash <= 0 || slash == value.length - 1) return null
        val packageName = value.substring(0, slash)
        var className = value.substring(slash + 1)
        if (className.startsWith(".")) {
            className = packageName + className
        }
        return "$packageName/$className"
    }

    /** Normalised key for a concrete component. */
    fun keyOf(packageName: String, className: String): String = "$packageName/$className"
}
