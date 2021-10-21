package com.github.durun.nitron.core

abstract class NitronException(
    override val message: String,
    override val cause: Throwable? = null,
) : RuntimeException(message, cause)

class InvalidTypeException(types: Collection<String>) : NitronException(
    message = "Invalid type: ${types.joinToString(", ")}"
)

class ParsingException(
    override val message: String,
    override val cause: Throwable? = null
) : NitronException(message, cause)