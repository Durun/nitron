package io.github.durun.nitron.analyze.query

import io.github.durun.nitron.analyze.Pattern


interface AnalyzeQuery<out R> {
    fun analyze(pattern: Pattern): R
}