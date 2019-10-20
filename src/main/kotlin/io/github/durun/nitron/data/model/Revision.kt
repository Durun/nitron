package io.github.durun.nitron.data.model

import java.util.*


class Revision(
        val softwareName: String,
        val commitHash: String,
        val date: Date,
        val commitMessage: String,
        val author: String
) {
    override fun toString(): String {
        return "[$softwareName ${ammoniaDateFormat.format(date)} $commitHash $author]"
    }
}