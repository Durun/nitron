package com.github.durun.nitron.app.metrics

import com.github.durun.nitron.core.MD5
import java.sql.Blob


data class Pattern(
    val hash: Pair<MD5?, MD5?>,
    val blob: Pair<Blob?, Blob?>,
    val text: Pair<String, String>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Pattern

        if (hash != other.hash) return false

        return true
    }

    override fun hashCode(): Int {
        return hash.hashCode()
    }
}

data class Metrics(
    val pattern: Pattern,

    val support: Int,
    val confidence: Double,
    val projects: Int,
    val files: Int,
    val authors: Int,
    val bugfixWords: Double,// コミットメッセージに bugfix keyword が含まれているchangesの数
    val testFiles: Double,  // テストコードであるchangesの数
)

data class AdditionalMetrics(
    val pattern: Pattern,

    val projectIdf: Double,
    val fileIdf: Double,
    val dChars: Int,    // Pattern前後の文字数の増減
    val dTokens: Int,   // Pattern前後のトークン数の増減
    val changeToLessThan: Int,  // >, >= から <, <= への変更がされた数
    val styleOnly: Boolean, // 違いは { を挿入するかだけか?
)