package io.github.durun.nitron.antlr4util

import org.antlr.v4.runtime.tree.ParseTree

public val ParseTree.children: List<ParseTree>
    get() = (0 until this.childCount).map(this::getChild)
