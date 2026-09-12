package com.example.utils

import android.content.Context
import android.content.Intent
import com.example.data.local.ItemEntity

object ShareHelper {
    fun shareItem(context: Context, item: ItemEntity, boxName: String? = null) {
        val locationText = item.formatLocationHierarchy()
        val textBuilder = StringBuilder()
        textBuilder.append("Find My Things\n\n")
        textBuilder.append("${item.name}\n")
        textBuilder.append("Category: ${item.category}\n\n")
        textBuilder.append("📍 Location:\n$locationText\n")

        if (!boxName.isNullOrBlank()) {
            textBuilder.append("\n📦 Inside Box: $boxName\n")
        }

        if (!item.notes.isNullOrBlank()) {
            textBuilder.append("\n📝 Notes:\n${item.notes}\n")
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Where is ${item.name}?")
            putExtra(Intent.EXTRA_TEXT, textBuilder.toString())
        }

        val chooser = Intent.createChooser(shareIntent, "Share item location")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
