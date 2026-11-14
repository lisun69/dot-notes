package com.abidur.dotnotes

/** 5x7 dot-matrix font. No font file needed. */
object DotFont {
    private val glyphs: Map<Char, Array<String>> = mapOf(
        '0' to arrayOf(" ### ", "#   #", "#  ##", "# # #", "##  #", "#   #", " ### "),
        '1' to arrayOf("  #  ", " ##  ", "  #  ", "  #  ", "  #  ", "  #  ", " ### "),
        '2' to arrayOf(" ### ", "#   #", "    #", "   # ", "  #  ", " #   ", "#####"),
        '3' to arrayOf("#####", "   # ", "  #  ", "   # ", "    #", "#   #", " ### "),
        '4' to arrayOf("   # ", "  ## ", " # # ", "#  # ", "#####", "   # ", "   # "),
        '5' to arrayOf("#####", "#    ", "#### ", "    #", "    #", "#   #", " ### "),
        '6' to arrayOf("  ## ", " #   ", "#    ", "#### ", "#   #", "#   #", " ### "),
        '7' to arrayOf("#####", "    #", "   # ", "  #  ", " #   ", " #   ", " #   "),
        '8' to arrayOf(" ### ", "#   #", "#   #", " ### ", "#   #", "#   #", " ### "),
        '9' to arrayOf(" ### ", "#   #", "#   #", " ####", "    #", "   # ", " ##  "),
        'A' to arrayOf(" ### ", "#   #", "#   #", "#####", "#   #", "#   #", "#   #"),
        'B' to arrayOf("#### ", "#   #", "#   #", "#### ", "#   #", "#   #", "#### "),
        'C' to arrayOf(" ### ", "#   #", "#    ", "#    ", "#    ", "#   #", " ### "),
        'D' to arrayOf("#### ", "#   #", "#   #", "#   #", "#   #", "#   #", "#### "),
        'E' to arrayOf("#####", "#    ", "#    ", "#### ", "#    ", "#    ", "#####"),
        'F' to arrayOf("#####", "#    ", "#    ", "#### ", "#    ", "#    ", "#    "),
        'G' to arrayOf(" ### ", "#   #", "#    ", "# ###", "#   #", "#   #", " ####"),
        'H' to arrayOf("#   #", "#   #", "#   #", "#####", "#   #", "#   #", "#   #"),
        'I' to arrayOf(" ### ", "  #  ", "  #  ", "  #  ", "  #  ", "  #  ", " ### "),
        'J' to arrayOf("  ###", "   # ", "   # ", "   # ", "   # ", "#  # ", " ##  "),
        'K' to arrayOf("#   #", "#  # ", "# #  ", "##   ", "# #  ", "#  # ", "#   #"),
        'L' to arrayOf("#    ", "#    ", "#    ", "#    ", "#    ", "#    ", "#####"),
        'M' to arrayOf("#   #", "## ##", "# # #", "# # #", "#   #", "#   #", "#   #"),
        'N' to arrayOf("#   #", "#   #", "##  #", "# # #", "#  ##", "#   #", "#   #"),
        'O' to arrayOf(" ### ", "#   #", "#   #", "#   #", "#   #", "#   #", " ### "),
        'P' to arrayOf("#### ", "#   #", "#   #", "#### ", "#    ", "#    ", "#    "),
        'Q' to arrayOf(" ### ", "#   #", "#   #", "#   #", "# # #", "#  # ", " ## #"),
        'R' to arrayOf("#### ", "#   #", "#   #", "#### ", "# #  ", "#  # ", "#   #"),
        'S' to arrayOf(" ####", "#    ", "#    ", " ### ", "    #", "    #", "#### "),
        'T' to arrayOf("#####", "  #  ", "  #  ", "  #  ", "  #  ", "  #  ", "  #  "),
        'U' to arrayOf("#   #", "#   #", "#   #", "#   #", "#   #", "#   #", " ### "),
        'V' to arrayOf("#   #", "#   #", "#   #", "#   #", "#   #", " # # ", "  #  "),
        'W' to arrayOf("#   #", "#   #", "#   #", "# # #", "# # #", "# # #", " # # "),
        'X' to arrayOf("#   #", "#   #", " # # ", "  #  ", " # # ", "#   #", "#   #"),
        'Y' to arrayOf("#   #", "#   #", " # # ", "  #  ", "  #  ", "  #  ", "  #  "),
        'Z' to arrayOf("#####", "    #", "   # ", "  #  ", " #   ", "#    ", "#####"),
        '/' to arrayOf("    #", "    #", "   # ", "  #  ", " #   ", "#    ", "#    "),
        '+' to arrayOf("     ", "  #  ", "  #  ", "#####", "  #  ", "  #  ", "     "),
        '-' to arrayOf("    ", "    ", "    ", "####", "    ", "    ", "    "),
        '.' to arrayOf("  ", "  ", "  ", "  ", "  ", "##", "##"),
        ' ' to arrayOf("   ", "   ", "   ", "   ", "   ", "   ", "   "),
    )

    private fun glyph(c: Char): Array<String> = glyphs[c.uppercaseChar()] ?: glyphs.getValue(' ')

    fun columns(text: String): Int {
        if (text.isEmpty()) return 0
        return text.sumOf { glyph(it)[0].length } + (text.length - 1)
    }

    fun forEachDot(text: String, action: (Int, Int, Boolean) -> Unit) {
        var x = 0
        for (ch in text) {
            val g = glyph(ch)
            val w = g[0].length
            for (r in 0 until 7) {
                for (c in 0 until w) action(x + c, r, g[r][c] == '#')
            }
            x += w + 1
        }
    }
}
