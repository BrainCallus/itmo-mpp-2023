import kotlinx.atomicfu.*
import kotlin.math.abs


/**
 * Int-to-Int hash map with open addressing and linear probes.
 * @author Churakova Alexandra
 */

class IntIntHashMap {
    private val core = atomic(Core(INITIAL_CAPACITY))

    /**
     * Returns value for the corresponding key or zero if this key is not present.
     *
     * @param key a positive key.
     * @return value for the corresponding or zero if this key is not present.
     * @throws IllegalArgumentException if key is not positive.
     */
    operator fun get(key: Int): Int {
        require(key > 0) { "Key must be positive: $key" }
        return toValue(core.value.getInternal(key))
    }

    /**
     * Changes value for the corresponding key and returns old value or zero if key was not present.
     *
     * @param key   a positive key.
     * @param value a positive value.
     * @return old value or zero if this key was not present.
     * @throws IllegalArgumentException if key or value are not positive, or value is equal to
     * [Integer.MAX_VALUE] which is reserved.
     */
    fun put(key: Int, value: Int): Int {
        require(key > 0) { "Key must be positive: $key" }
        require(isValue(value)) { "Invalid value: $value" }
        return toValue(putAndRehashWhileNeeded(key, value))
    }

    /**
     * Removes value for the corresponding key and returns old value or zero if key was not present.
     *
     * @param key a positive key.
     * @return old value or zero if this key was not present.
     * @throws IllegalArgumentException if key is not positive.
     */
    fun remove(key: Int): Int {
        require(key > 0) { "Key must be positive: $key" }
        return toValue(putAndRehashWhileNeeded(key, DEL_VALUE))
    }

    private fun putAndRehashWhileNeeded(key: Int, value: Int): Int {
        while (true) {
            val curCore = core.value
            val oldValue = curCore.putInternal(key, value)
            if (oldValue != NEEDS_REHASH) return oldValue
            core.compareAndSet(curCore, curCore.rehash())
        }
    }

    private class Core(capacity: Int) {
        // Pairs of <key, value> here, the actual
        // size of the map is twice as big.
        val map: AtomicIntArray = AtomicIntArray(2 * capacity)
        val shift: Int
        val next: AtomicRef<Core?> = atomic(null)

        init {
            val mask = capacity - 1
            assert(mask > 0 && mask and capacity == 0) { "Capacity must be power of 2: $capacity" }
            shift = 32 - Integer.bitCount(mask)
        }

        fun getInternal(key: Int): Int {
            var index = index(key)
            var probes = 0
            while (map[index].value != key) { // optimize for successful lookup
                if (map[index].value == NULL_KEY || ++probes >= MAX_PROBES) return NULL_VALUE // not found -- no value
                if (index == 0) index = map.size
                index -= 2
            }
            // found key -- return value
            return when (val value = map[index + 1].value) {
                DEL_VALUE, NULL_VALUE -> {
                    NULL_VALUE
                }

                NEGINF_VALUE -> {
                    next.value!!.getInternal(key)
                }

                else -> {
                    abs(value)
                }
            }
        }

        fun putInternal(key: Int, value: Int): Int {
            var index = index(key)
            var probes = 0
            while (true) { // optimize for successful lookup
                val elem = map[index].value
                if (elem == key) {
                    break
                }
                if (elem == NULL_KEY) {
                    // not found -- claim this slot
                    if (value == DEL_VALUE) return NULL_VALUE // remove of missing item, no need to claim slot
                    if (map[index].compareAndSet(NULL_KEY, key)) {
                        break
                    } else {
                        continue
                    }
                }
                if (++probes >= MAX_PROBES) return NEEDS_REHASH
                if (index == 0) index = map.size
                index -= 2
            }
            // found key -- update value
            while (true) {
                val prevValue = map[index + 1].value
                return if (prevValue == NEGINF_VALUE) {
                    next.value!!.putInternal(key, value)
                } else if (prevValue < 0) {
                    NEEDS_REHASH
                } else if (map[index + 1].compareAndSet(prevValue, value)) {
                    if (prevValue == DEL_VALUE) {
                        NULL_VALUE
                    } else {
                        abs(prevValue)
                    }
                } else {
                    continue
                }
            }
        }

        fun rehash(): Core {
            if (next.value == null) {
                next.compareAndSet(null, Core(map.size))
            }
            val nextCore = next.value!!
            var idx = 0
            while (idx < map.size) {
                val value = map[idx + 1].value

                if (value == NEGINF_VALUE) {
                    idx += 2
                    continue
                }
                if (isValue(map[idx + 1].value)) {
                    map[idx + 1].compareAndSet(value, -value)
                } else {
                    if (value < 0) {
                        nextCore.internalSetValue(map[idx].value, -value)
                    }
                    map[idx + 1].compareAndSet(value, NEGINF_VALUE)
                }
            }
            return next.value!!
        }

        /**
         * Returns an initial index in map to look for a given key.
         */
        fun index(key: Int): Int = (key * MAGIC ushr shift) * 2

        private fun internalSetValue(key: Int, value: Int) {
            var idx = index(key)
            var prob = 0
            while (true) {
                val elem = map[idx].value
                if (elem == key) {
                    break
                }
                if (elem == NULL_KEY) {
                    if (value == DEL_VALUE) {
                        return
                    }
                    if (map[idx].compareAndSet(NULL_KEY, key)) {
                        break
                    } else {
                        continue
                    }
                }
                if (++prob >= MAX_PROBES) return
                if (idx == 0) idx = map.size
                idx -= 2
            }
            map[idx + 1].compareAndSet(NULL_VALUE, value)
        }
    }
}

private const val MAGIC = -0x61c88647 // golden ratio
private const val INITIAL_CAPACITY = 2 // !!! DO NOT CHANGE INITIAL CAPACITY !!!
private const val MAX_PROBES = 8 // max number of probes to find an item
private const val NULL_KEY = 0 // missing key (initial value)
private const val NULL_VALUE = 0 // missing value (initial value)
private const val DEL_VALUE = Int.MAX_VALUE // mark for removed value
private const val NEEDS_REHASH = -1 // returned by `putInternal` to indicate that rehash is needed
private const val NEGINF_VALUE = Int.MIN_VALUE

// Checks is the value is in the range of allowed values
private fun isValue(value: Int): Boolean = value in (1 until DEL_VALUE)

// Converts internal value to the public results of the methods
private fun toValue(value: Int): Int = if (isValue(value)) value else 0