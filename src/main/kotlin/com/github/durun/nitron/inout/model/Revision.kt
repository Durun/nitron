package com.github.durun.nitron.inout.model

import java.time.LocalDateTime


class Revision(
        val softwareName: String,
        val commitHash: String,
        val date: LocalDateTime,
        val commitMessage: String,
        val author: String
) {
    override fun toString(): String {
        return "[$softwareName ${date.format(ammoniaDateTimeFormatter)} $commitHash $author]"
    }
}