package com.github.durun.nitron.app.preparse

import com.github.durun.nitron.core.MD5
import com.github.durun.nitron.core.config.NitronConfig
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.revwalk.RevCommit
import org.jetbrains.exposed.dao.EntityID
import org.joda.time.DateTime
import java.net.URL
import java.util.*

internal class RepositoryInfo(
    val id: EntityID<Int>,
    val url: URL,
    commaDelimitedLangs: String
) {
    val languages: List<String> = commaDelimitedLangs.split(',')
    fun fileExtensions(config: NitronConfig): List<String> {
        return languages.mapNotNull {
            config.langConfig[it]?.extensions
        }.flatten()
    }
}

internal data class CommitInfo(
    val id: String,
    val date: DateTime,
    val message: String,
    val author: String,
    val files: Collection<FileInfo>
)

internal data class FileInfo(
    val path: String,
    val objectId: ObjectId,
    private val lazytext: () -> String
) {
    val text: String by lazy { lazytext.invoke() }
    fun readText(): String = lazytext.invoke()
    val checksum: MD5 by lazy { MD5.digest(readText()) }
}

internal fun RevCommit.getDate(): Date {
    return Date(this.commitTime * 1000L)
}


internal data class ParseJobInfo(
    val repoId: EntityID<Int>,
    val astId: EntityID<Int>,
    val fileObjectId: String,
    val lang: String
)

internal data class ParseJobResult(
    val astId: EntityID<Int>,
    val parseResult: String?
)