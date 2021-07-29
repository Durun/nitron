package com.github.durun.nitron.util

fun <E> MutableCollection<E>.removeAndGetIf(filter: (E) -> Boolean): List<E> {
    val each = this.iterator()
    val removed: MutableList<E> = mutableListOf()
    while (each.hasNext()) {
        val e = each.next()
        if (filter(e)) {
            each.remove()
            removed += e
        }
    }
    return removed
}