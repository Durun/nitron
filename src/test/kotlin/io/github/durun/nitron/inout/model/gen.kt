package io.github.durun.nitron.inout.model

import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import java.io.File

fun Arb.Companion.code(software: String? = null): Arb<Code> = arbitrary { rs ->
	Code(
			softwareName = software ?: string(1..5).single(rs),
			rawText = string().single(rs),
			normalizedText = string().single(rs),
			range = int().take(2, rs).toList().sorted().let { (from, to) -> from..to }
	)
}

fun Arb.Companion.change(software: String? = null): Arb<Change> = arbitrary { rs ->
	val software = software ?: string(1..5).single(rs)
	val changeType = enum<ChangeType>().single(rs)
	val code = when(changeType) {
		ChangeType.CHANGE -> pair(code(software), code(software)).single(rs)
		ChangeType.ADD -> null to code(software).single(rs)
		ChangeType.DELETE -> code(software).single(rs) to null
	}
	Change(
			softwareName = software,
			filePath = file().map(File::toPath).single(rs),
			author = string(3..10).single(rs),
			beforeCode = code.first,
			afterCode = code.second,
			commitHash = string(40).single(rs),
			date = localDateTime().single(rs),
			changeType = changeType,
			diffType = enum<DiffType>().single(rs)
	)
}