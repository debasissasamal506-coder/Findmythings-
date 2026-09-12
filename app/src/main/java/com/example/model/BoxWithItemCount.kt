package com.example.model

import com.example.data.local.BoxEntity

data class BoxWithItemCount(
    val box: BoxEntity,
    val itemCount: Int
)
