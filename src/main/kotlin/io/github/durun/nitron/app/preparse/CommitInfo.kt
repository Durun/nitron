package io.github.durun.nitron.app.preparse

import org.eclipse.jgit.revwalk.RevCommit
import org.joda.time.DateTime
import java.util.*

internal data class CommitInfo(
    val id: String,
    val date: DateTime,
    val message: String,
    val author: String,
    val files: Collection<FileInfo>
)

internal data class FileInfo(
    val path: String,
    val lazytext: () -> String
) {
    fun readText(): String = lazytext.invoke()
}

internal fun RevCommit.getDate(): Date {
    return Date(this.commitTime * 1000L)
}