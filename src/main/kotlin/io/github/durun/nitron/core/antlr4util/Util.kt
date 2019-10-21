package io.github.durun.nitron.core.antlr4util

import org.antlr.v4.runtime.tree.ParseTree

val ParseTree.children: List<ParseTree>
    get() = (0 until this.childCount).map(this::getChild)
