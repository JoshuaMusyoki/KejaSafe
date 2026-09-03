package com.rotd.kejasafe.domain.chat

/** UTC calendar day from epoch millis (matches existing chat time display). */
fun chatDayKey(epochMillis: Long): Long = epochMillis / 86_400_000L

fun formatUtcCalendarDate(epochMillis: Long): String {
    if (epochMillis <= 0L) return ""
    val days = epochMillis / 86_400_000L
    var z = days + 719_468L
    val era = if (z >= 0) z / 146_097L else (z - 146_096L) / 146_097L
    val doe = z - era * 146_097L
    val yoe = (doe - doe / 1460L + doe / 36524L - doe / 146_096L) / 365L
    var year = (yoe + era * 400L).toInt()
    val doy = (doe - (365L * yoe + yoe / 4L - yoe / 100L)).toInt()
    val mp = (5 * doy + 2) / 153
    val day = doy - (153 * mp + 2) / 5 + 1
    val month = if (mp < 10) mp + 3 else mp - 9
    if (month <= 2) year += 1
    val monthName = when (month) {
        1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"
        5 -> "May"; 6 -> "Jun"; 7 -> "Jul"; 8 -> "Aug"
        9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; else -> "Dec"
    }
    return "$day $monthName $year"
}

fun chatDateHeaderLabel(epochMillis: Long, nowMillis: Long): String {
    val day = chatDayKey(epochMillis)
    val today = chatDayKey(nowMillis)
    return when (today - day) {
        0L -> "Today"
        1L -> "Yesterday"
        else -> formatUtcCalendarDate(epochMillis)
    }
}

sealed class ChatTimelineItem {
    data class DayHeader(val dayKey: Long, val label: String) : ChatTimelineItem()
    data class Bubble(val message: ApartmentChatMessage) : ChatTimelineItem()
}

fun buildChatTimeline(
    messages: List<ApartmentChatMessage>,
    nowMillis: Long,
): List<ChatTimelineItem> {
    val items = mutableListOf<ChatTimelineItem>()
    var lastDay: Long? = null
    messages.sortedBy { it.timestamp }.forEach { message ->
        val day = chatDayKey(message.timestamp)
        if (lastDay != day) {
            items += ChatTimelineItem.DayHeader(
                dayKey = day,
                label = chatDateHeaderLabel(message.timestamp, nowMillis),
            )
            lastDay = day
        }
        items += ChatTimelineItem.Bubble(message)
    }
    return items
}

fun MessageStatus.receiptRank(): Int = when (this) {
    MessageStatus.FAILED -> -1
    MessageStatus.SENDING -> 0
    MessageStatus.SENT -> 1
    MessageStatus.DELIVERED -> 2
    MessageStatus.READ -> 3
}

fun MessageStatus.canUpgradeTo(target: MessageStatus): Boolean {
    if (this == MessageStatus.FAILED) return false
    if (target == MessageStatus.FAILED) return true
    return target.receiptRank() > receiptRank()
}
