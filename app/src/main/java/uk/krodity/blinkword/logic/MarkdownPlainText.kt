package uk.krodity.blinkword.logic

/**
 * Converts Markdown source into plain readable prose for RSVP display.
 * This is intentionally a light regex pass, not a full CommonMark parser --
 * good enough to keep formatting characters out of the flashed word stream.
 */
object MarkdownPlainText {

    private val CODE_FENCE = Regex("(?m)^```.*$")
    private val INLINE_CODE = Regex("`([^`]*)`")
    private val HEADING = Regex("(?m)^#{1,6}\\s*")
    private val BLOCKQUOTE = Regex("(?m)^>\\s?")
    private val UNORDERED_LIST = Regex("(?m)^\\s*[-*+]\\s+")
    private val ORDERED_LIST = Regex("(?m)^\\s*\\d+\\.\\s+")
    private val HORIZONTAL_RULE = Regex("(?m)^\\s*([-*_])(\\s*\\1){2,}\\s*$")
    private val IMAGE = Regex("!\\[([^]]*)]\\([^)]*\\)")
    private val LINK = Regex("\\[([^]]*)]\\([^)]*\\)")
    private val AUTOLINK = Regex("<((?:https?|mailto):[^>]+)>")
    private val BOLD_ITALIC = Regex("(\\*\\*\\*|___)(.+?)\\1")
    private val BOLD = Regex("(\\*\\*|__)(.+?)\\1")
    private val ITALIC = Regex("(\\*|_)(.+?)\\1")
    private val TABLE_SEPARATOR_ROW = Regex("(?m)^\\s*\\|?\\s*:?-{2,}:?\\s*(\\|\\s*:?-{2,}:?\\s*)*\\|?\\s*$")

    fun toPlainText(markdown: String): String {
        var text = markdown

        text = CODE_FENCE.replace(text, "")
        text = INLINE_CODE.replace(text) { it.groupValues[1] }
        text = IMAGE.replace(text) { it.groupValues[1] }
        text = LINK.replace(text) { it.groupValues[1] }
        text = AUTOLINK.replace(text) { it.groupValues[1] }
        text = TABLE_SEPARATOR_ROW.replace(text, "")
        text = text.replace('|', ' ')
        text = HORIZONTAL_RULE.replace(text, "")
        text = HEADING.replace(text, "")
        text = BLOCKQUOTE.replace(text, "")
        text = UNORDERED_LIST.replace(text, "")
        text = ORDERED_LIST.replace(text, "")
        text = BOLD_ITALIC.replace(text) { it.groupValues[2] }
        text = BOLD.replace(text) { it.groupValues[2] }
        text = ITALIC.replace(text) { it.groupValues[2] }

        return text
    }
}
