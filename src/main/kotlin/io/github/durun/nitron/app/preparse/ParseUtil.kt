package io.github.durun.nitron.app.preparse

import io.github.durun.nitron.core.config.NitronConfig
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ObjectId

class ParseUtil(
	val git: Git,
	val config: NitronConfig
) {
	fun readFile(objectId: String): String? {
		val loader = git.repository.open(ObjectId.fromString(objectId))
			?: return null
		return loader.cachedBytes.decodeToString()
	}
}