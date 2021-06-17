package io.github.durun.nitron.core.parser.antlr

import org.snt.inmemantlr.utils.Tuple

operator fun <K, T> Tuple<K, T>.component1(): K = this.first
operator fun <K, T> Tuple<K, T>.component2(): T = this.second