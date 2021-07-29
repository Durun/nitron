package com.github.durun.nitron.core

abstract class NitronException(message: String): Exception(message)

class InvalidTypeException(types: Collection<String>): NitronException(
		message = "Invalid type: ${types.joinToString(", ")}"
)