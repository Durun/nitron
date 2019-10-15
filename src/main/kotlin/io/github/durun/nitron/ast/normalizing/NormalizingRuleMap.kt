package io.github.durun.nitron.ast.normalizing

class NormalizingRuleMap(
        private val ruleMap: Map<List<String>, String>
): Map<List<String>, String> {
    constructor(vararg rulePairs: Pair<List<String>, String>): this(ruleMap = hashMapOf(*rulePairs))

    private val keyLengths = ruleMap.keys.map { it.size }.sorted().distinct()

    override val entries: Set<Map.Entry<List<String>, String>>
        get() = ruleMap.entries
    override val keys: Set<List<String>>
        get() = ruleMap.keys
    override val size: Int
        get() = ruleMap.size
    override val values: Collection<String>
        get() = ruleMap.values

    override fun containsKey(key: List<String>): Boolean = ruleMap.containsKey(key)

    override fun containsValue(value: String): Boolean = ruleMap.containsValue(value)

    override fun isEmpty(): Boolean = ruleMap.isEmpty()

    override fun get(key: List<String>): String? {
        return keyLengths.mapNotNull { length ->
            val subStack = key.takeLast(length)
            ruleMap[subStack]
        }.firstOrNull()
    }
}