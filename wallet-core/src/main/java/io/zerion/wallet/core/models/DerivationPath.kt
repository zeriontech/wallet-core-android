package io.zerion.wallet.core.models

/**
 * Created by rolea on 24.11.2021.
 */
class DerivationPath {

    @Transient
    private val indexCount = 5

    var indices = MutableList(indexCount) { Index(0) }

    /**
     * Purpose is a constant that designated the derivation mode: it's set to 44' (or 0x8000002C) for legacy,
     * 49' for segwit and 84' for native segwit.
     * It indicates the specification, according to which the subtree of this node is generated.
     */
    var purpose: Int
        get() = indices[0].value
        set(value) {
            indices[0] = Index(value, hardened = true)
        }

    var coinType: Int
        get() = indices[1].value
        set(value) {
            indices[1] = Index(value, hardened = true)
        }

    /**
     * This level splits the key space into independent user identities, so the wallet never mixes the coins across different accounts.
     */
    var account: Int
        get() = indices[2].value
        set(value) {
            indices[2] = Index(value, hardened = true)
        }

    var change: Int
        get() = indices[3].value
        set(value) {
            indices[3] = Index(value, hardened = false)
        }

    /**
     * Addresses are numbered from index 0 in sequentially increasing manner.
     */
    var address: Int
        get() = indices[4].value
        set(value) {
            indices[4] = Index(value, hardened = false)
        }

    constructor(indices: List<Index>) {
        if (indices.size != indexCount) throw IllegalArgumentException("Not enough indices")
        this.indices = indices.toMutableList()
    }

    constructor(purpose: Int, coinType: Int, account: Int = 0, change: Int = 0, address: Int = 0) {
        this.purpose = purpose
        this.coinType = coinType
        this.account = account
        this.change = change
        this.address = address
    }

    @Throws(NumberFormatException::class)
    constructor(path: String) {
        indices.clear()
        val components = path.split("/")
        components.forEach continuing@ {
            if (it == "m") return@continuing
            if (it.endsWith("'")) {
                val index = it.dropLast(1).toInt()
                indices.add(Index(index, hardened = true))
            } else {
                val index = it.toInt()
                indices.add(Index(index, hardened = false))
            }
        }
        if (indices.size != indexCount) throw IllegalArgumentException("Not enough indices")
    }

    override fun toString() = "m/${indices.joinToString("/") { it.toString() }}"

    override fun hashCode(): Int {
        return indices.fold(0) { total, next -> total * next.hashCode() }
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is DerivationPath) return false
        return indices == other.indices
    }

}

data class Index(val value: Int, val hardened: Boolean = true) {

    override fun toString() = if (hardened) {
        "$value'"
    } else {
        "$value"
    }

}