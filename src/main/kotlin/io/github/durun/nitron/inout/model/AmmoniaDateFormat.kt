package io.github.durun.nitron.inout.model

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

val ammoniaDateFormat: DateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
val ammoniaDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")