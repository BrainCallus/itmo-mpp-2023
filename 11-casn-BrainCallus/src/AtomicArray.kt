import kotlinx.atomicfu.*

/**
 * @author Churakova Alexandra
 */

class AtomicArray<E>(size: Int, initialValue: E) {
    private val a = atomicArrayOfNulls<Ref<E>>(size)

    init {
        for (i in 0 until size) a[i].value = Ref(initialValue)
    }

    fun get(index: Int) =
        a[index].value!!.value

    fun set(index: Int, value: E) {
        a[index].value!!.cas(a[index].value!!.value, value)
    }

    fun cas(index: Int, expected: E, update: E) =
        a[index].value!!.cas(expected, update)

    fun cas2(
        index1: Int, expected1: E, update1: E,
        index2: Int, expected2: E, update2: E
    ): Boolean {
        if (index1 == index2) {
            return if (expected1 == expected2) {
                cas(index2, expected2, update2)
            } else {
                false
            }
        }
        if (index1 < index2) {
            return cas2(index2, expected2, update2, index1, expected1, update1)
        }
        while (true) {
            val descriptor =
                CASDescriptor(a[index1].value!!, expected1, update1, a[index2].value!!, expected2, update2)
            if (a[index1].value!!.cas(expected1, descriptor)) {
                descriptor.complete()
                return descriptor.state.value == State.SUCCESS
            } else if (a[index1].value!!.value != expected1) {
                return false
            }
        }
    }
}