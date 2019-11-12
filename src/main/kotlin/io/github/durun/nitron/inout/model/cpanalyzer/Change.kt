package io.github.durun.nitron.inout.model.cpanalyzer

import java.nio.file.Path
import java.util.*

enum class ChangeType(val rawValue: Int) {
    CHANGE(1), ADD(2), DELETE(3)
}

enum class DiffType(val rawValue: Int) {
    TYPE1(1), TYPE2(2), TYPE3(3)
}

class Change(
        val softwareName: String,
        val filePath: Path,
        val author: String,
        val beforeCode: Code?,
        val afterCode: Code?,
        val commitHash: String,
        val date: Date,
        val changeType: ChangeType,
        val diffType: DiffType = DiffType.TYPE3, // TODO
        val id: Int? = null
) {
    override fun toString(): String {
        return "Change[$softwareName $id] $commitHash $date $author\n" +
                "$filePath $changeType $diffType\n" +
                "before:\n${beforeCode.toString().prependIndent("\t")}\n" +
                "after :\n${afterCode.toString().prependIndent("\t")}\n"
    }

    override fun hashCode(): Int {
        return arrayOf(
                softwareName,
                filePath,
                author,
                beforeCode,
                afterCode,
                commitHash,
                date,
                changeType,
                diffType
        ).hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Change

        if (softwareName != other.softwareName) return false
        if (filePath != other.filePath) return false
        if (author != other.author) return false
        if (beforeCode != other.beforeCode) return false
        if (afterCode != other.afterCode) return false
        if (commitHash != other.commitHash) return false
        if (date != other.date) return false
        if (changeType != other.changeType) return false
        if (diffType != other.diffType) return false

        return true
    }
}