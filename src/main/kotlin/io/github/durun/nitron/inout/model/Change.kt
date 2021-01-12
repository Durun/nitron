package io.github.durun.nitron.inout.model

import java.nio.file.Path
import java.time.LocalDateTime

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
        val date: LocalDateTime,
        val changeType: ChangeType,
        val diffType: DiffType = DiffType.TYPE3, // TODO
        var id: Int? = null
) {
    override fun toString(): String {
        return "Change($beforeCode -> $afterCode)"
    }

    override fun hashCode(): Int {
        var result = softwareName.hashCode()
        result = 31 * result + filePath.hashCode()
        result = 31 * result + author.hashCode()
        result = 31 * result + (beforeCode?.hashCode() ?: 0)
        result = 31 * result + (afterCode?.hashCode() ?: 0)
        result = 31 * result + commitHash.hashCode()
        result = 31 * result + date.hashCode()
        result = 31 * result + changeType.hashCode()
        result = 31 * result + diffType.hashCode()
        result = 31 * result + (id ?: 0)
        return result
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