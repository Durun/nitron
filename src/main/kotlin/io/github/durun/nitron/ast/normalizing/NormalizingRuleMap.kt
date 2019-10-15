package io.github.durun.nitron.ast.normalizing

/**
 * (正規化対象の規則, 正規化後の記号)の対応関係
 * @param [ruleMap] key=正規化対象の構造を 親->子孫 の順に格納したリスト, value=正帰化後の記号
 */
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

    /**
     * [key]の後ろ側に対応する(正規化後の記号)を返す.
     * 複数該当する場合は最短マッチ.
     *
     * @param [key] 構文木の規則のスタック
     * @return 正規化後の記号. 該当するものが存在しなければnullを返す.
     */
    override fun get(key: List<String>): String? {
        return keyLengths.mapNotNull { length ->
            val subStack = key.takeLast(length)
            ruleMap[subStack]
        }.firstOrNull()
    }
}