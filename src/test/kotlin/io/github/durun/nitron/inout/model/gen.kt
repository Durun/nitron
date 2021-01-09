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
	Change(
			softwareName = software ?: string(1..5).single(rs),
			filePath = file().map(File::toPath).single(rs),
			author = string(3..10).single(rs),
			beforeCode = code(software).single(rs),
			afterCode = code(software).single(rs),
			commitHash = string(40).single(rs),
			date = localDateTime().single(rs),
			changeType = enum<ChangeType>().single(rs),
			diffType = enum<DiffType>().single(rs)
	)
}