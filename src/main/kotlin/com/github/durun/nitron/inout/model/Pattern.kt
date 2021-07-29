package com.github.durun.nitron.inout.model

import com.github.durun.nitron.core.MD5
import java.time.LocalDateTime

data class Pattern(
    val beforeHash: MD5?,
    val afterHash: MD5?,
    val changeType: ChangeType,
    val diffType: DiffType,
    val support: Int,
    val confidence: Double,
    val authors: Int,
    val files: Int,
    val nos: Int,
    val dateRange: ClosedRange<LocalDateTime>,
    var id: Int? = null
)