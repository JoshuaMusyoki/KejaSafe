package com.rotd.kejasafe.chat

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import com.rotd.kejasafe.theme.LocalEmojiFontFamily

@Composable
fun ChatMessageText(
    text: String,
    color: Color,
    style: TextStyle,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val emojiFamily = LocalEmojiFontFamily.current
    val annotated = if (emojiFamily != null) text.withEmojiFont(emojiFamily) else null
    if (annotated == null) {
        Text(
            text = text,
            color = color,
            style = style,
            modifier = modifier,
            fontWeight = fontWeight,
            maxLines = maxLines,
            overflow = overflow,
        )
    } else {
        Text(
            text = annotated,
            color = color,
            style = style,
            modifier = modifier,
            fontWeight = fontWeight,
            maxLines = maxLines,
            overflow = overflow,
        )
    }
}

fun emojiVisualTransformation(emojiFamily: FontFamily?): VisualTransformation {
    if (emojiFamily == null) return VisualTransformation.None
    return VisualTransformation { text ->
        TransformedText(text.text.withEmojiFont(emojiFamily) ?: text, OffsetMapping.Identity)
    }
}

private fun String.withEmojiFont(emojiFamily: FontFamily): AnnotatedString? {
    if (!containsEmojiCluster()) return null
    val runs = splitEmojiRuns(this)
    return buildAnnotatedString {
        for ((segment, isEmoji) in runs) {
            if (isEmoji) {
                pushStyle(SpanStyle(fontFamily = emojiFamily))
                append(segment)
                pop()
            } else {
                append(segment)
            }
        }
    }
}

private fun String.containsEmojiCluster(): Boolean {
    var index = 0
    while (index < length) {
        if (emojiClusterLength(this, index) > 0) return true
        index += codePointSizeAt(this, index)
    }
    return false
}

internal fun splitEmojiRuns(text: String): List<Pair<String, Boolean>> {
    if (text.isEmpty()) return emptyList()
    val runs = mutableListOf<Pair<String, Boolean>>()
    val buffer = StringBuilder()
    var emojiRun = false
    var index = 0
    while (index < text.length) {
        val cluster = emojiClusterLength(text, index)
        if (cluster > 0) {
            if (buffer.isNotEmpty() && !emojiRun) {
                runs += buffer.toString() to false
                buffer.clear()
            }
            emojiRun = true
            buffer.append(text, index, index + cluster)
            index += cluster
        } else {
            if (buffer.isNotEmpty() && emojiRun) {
                runs += buffer.toString() to true
                buffer.clear()
            }
            emojiRun = false
            val size = codePointSizeAt(text, index)
            buffer.append(text, index, index + size)
            index += size
        }
    }
    if (buffer.isNotEmpty()) {
        runs += buffer.toString() to emojiRun
    }
    return runs
}

private fun emojiClusterLength(text: String, start: Int): Int {
    if (start >= text.length) return 0
    val first = codePointAt(text, start)
    val firstSize = codePointCharCount(first)
    if (isKeycapBase(first)) {
        var cursor = start + firstSize
        var sawKeycap = false
        while (cursor < text.length) {
            val cp = codePointAt(text, cursor)
            val size = codePointCharCount(cp)
            when (cp) {
                0xFE0F, 0xFE0E -> cursor += size
                0x20E3 -> {
                    sawKeycap = true
                    cursor += size
                    return if (sawKeycap) cursor - start else 0
                }
                else -> return 0
            }
        }
        return 0
    }
    if (!isEmojiBase(first)) return 0
    var cursor = start + firstSize
    if (first in 0x1F1E6..0x1F1FF && cursor < text.length) {
        val second = codePointAt(text, cursor)
        if (second in 0x1F1E6..0x1F1FF) {
            return cursor + codePointCharCount(second) - start
        }
    }
    while (cursor < text.length) {
        val cp = codePointAt(text, cursor)
        val size = codePointCharCount(cp)
        when {
            cp == 0xFE0F || cp == 0xFE0E -> cursor += size
            cp in 0x1F3FB..0x1F3FF -> cursor += size
            cp == 0x20E3 -> cursor += size
            cp == 0x200D -> {
                val after = cursor + size
                if (after >= text.length || !isEmojiBase(codePointAt(text, after))) break
                cursor = after + codePointCharCount(codePointAt(text, after))
            }
            else -> break
        }
    }
    return cursor - start
}

private fun isKeycapBase(codePoint: Int): Boolean =
    codePoint in 0x30..0x39 || codePoint == '#'.code || codePoint == '*'.code

private fun isEmojiBase(codePoint: Int): Boolean = when (codePoint) {
    0x00A9, 0x00AE, 0x203C, 0x2049, 0x2122, 0x2139, 0x2194, 0x2195, 0x2196, 0x2197,
    0x2198, 0x2199, 0x21A9, 0x21AA, 0x231A, 0x231B, 0x2328, 0x23CF, 0x23E9, 0x23EA,
    0x23EB, 0x23EC, 0x23ED, 0x23EE, 0x23EF, 0x23F0, 0x23F1, 0x23F2, 0x23F3, 0x23F8,
    0x23F9, 0x23FA, 0x24C2, 0x25AA, 0x25AB, 0x25B6, 0x25C0, 0x25FB, 0x25FC, 0x25FD,
    0x25FE, 0x2600, 0x2601, 0x2602, 0x2603, 0x2604, 0x260E, 0x2611, 0x2614, 0x2615,
    0x2618, 0x261D, 0x2620, 0x2622, 0x2623, 0x2626, 0x262A, 0x262E, 0x262F, 0x2638,
    0x2639, 0x263A, 0x2640, 0x2642, 0x2648, 0x2649, 0x264A, 0x264B, 0x264C, 0x264D,
    0x264E, 0x264F, 0x2650, 0x2651, 0x2652, 0x2653, 0x265F, 0x2660, 0x2663, 0x2665,
    0x2666, 0x2668, 0x267B, 0x267E, 0x267F, 0x2692, 0x2693, 0x2694, 0x2695, 0x2696,
    0x2697, 0x2699, 0x269B, 0x269C, 0x26A0, 0x26A1, 0x26AA, 0x26AB, 0x26B0, 0x26B1,
    0x26BD, 0x26BE, 0x26C4, 0x26C5, 0x26C8, 0x26CE, 0x26CF, 0x26D1, 0x26D3, 0x26D4,
    0x26E9, 0x26EA, 0x26F0, 0x26F1, 0x26F2, 0x26F3, 0x26F4, 0x26F5, 0x26F7, 0x26F8,
    0x26F9, 0x26FA, 0x26FD, 0x2702, 0x2705, 0x2708, 0x2709, 0x270A, 0x270B, 0x270C,
    0x270D, 0x270F, 0x2712, 0x2714, 0x2716, 0x271D, 0x2721, 0x2728, 0x2733, 0x2734,
    0x2744, 0x2747, 0x274C, 0x274E, 0x2753, 0x2754, 0x2755, 0x2757, 0x2763, 0x2764,
    0x2795, 0x2796, 0x2797, 0x27A1, 0x27B0, 0x27BF, 0x2934, 0x2935, 0x2B05, 0x2B06,
    0x2B07, 0x2B1B, 0x2B1C, 0x2B50, 0x2B55, 0x3030, 0x303D, 0x3297, 0x3299,
    -> true
    else -> codePoint in 0x1F000..0x1FAFF || codePoint in 0x1F1E6..0x1F1FF
}

private fun codePointAt(text: String, index: Int): Int {
    val high = text[index]
    if (high in '\uD800'..'\uDBFF' && index + 1 < text.length) {
        val low = text[index + 1]
        if (low in '\uDC00'..'\uDFFF') {
            return 0x10000 + ((high.code - 0xD800) shl 10) + (low.code - 0xDC00)
        }
    }
    return high.code
}

private fun codePointCharCount(codePoint: Int): Int = if (codePoint > 0xFFFF) 2 else 1

private fun codePointSizeAt(text: String, index: Int): Int = codePointCharCount(codePointAt(text, index))
