package io.github.durun.nitron.analyze.query

import io.github.durun.nitron.analyze.Pattern


class FirstQuery<out R : Any> private constructor(
        private val bases: Iterable<AnalyzeQuery<R?>>
) : AnalyzeQuery<R?> {
    companion object {
        fun <T : Any> of(vararg bases: AnalyzeQuery<T?>): AnalyzeQuery<T?> {
            return FirstQuery(bases.asIterable())
        }
    }

    override fun analyze(pattern: Pattern): R? {
        return bases.asSequence()
                .mapNotNull { it.analyze(pattern) }
                .firstOrNull()
    }
}
