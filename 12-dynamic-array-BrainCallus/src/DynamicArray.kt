package mpp.dynamicarray

import kotlinx.atomicfu.*

/**
 * @author Churakova Alexandra
 */
interface DynamicArray<E> {
    /**
     * Returns the element located in the cell [index],
     * or throws [IllegalArgumentException] if [index]
     * exceeds the [size] of this array.
     */
    fun get(index: Int): E

    /**
     * Puts the specified [element] into the cell [index],
     * or throws [IllegalArgumentException] if [index]
     * exceeds the [size] of this array.
     */
    fun put(index: Int, element: E)

    /**
     * Adds the specified [element] to this array
     * increasing its [size].
     */
    fun pushBack(element: E)

    /**
     * Returns the current size of this array,
     * it increases with [pushBack] invocations.
     */
    val size: Int
}

class DynamicArrayImpl<E> : DynamicArray<E> {
    private val core = atomic(Core<Element<E>>(INITIAL_CAPACITY))
    private val _size = atomic(0)

    override fun get(index: Int): E {
        require(index < size)
        return core.value.get(index).element
    }

    override fun put(index: Int, element: E) {
        require(index < size)
        while (true) {
            val currentCore = core.value
            val cell = currentCore.get(index)
            if (cell.trans) {
                extend(currentCore)
                continue
            }
            if (currentCore.cas(index, cell, Element(element))) return
        }
    }

    override fun pushBack(element: E) {
        while (true) {
            val curCore = core.value
            val curSize = size
            if (curSize < curCore.capacity) {
                if (curCore.cas(curSize, null, Element(element))) {
                    _size.compareAndSet(curSize, curSize + 1)
                    break
                }
                _size.compareAndSet(curSize, curSize + 1)
            } else {
                extend(curCore)
            }
        }
    }

    private fun extend(currentCore: Core<Element<E>>) {
        currentCore.next.compareAndSet(null, Core(currentCore.capacity * 2))
        val nextCore = currentCore.next.value!!
        for (i in 0 until currentCore.capacity) {
            while (true) {
                val curElement = currentCore.get(i)
                if (currentCore.cas(i, curElement, Element(curElement.element, true))) {
                    nextCore.cas(i, null, Element(curElement.element))
                    break
                }
            }
        }
        core.compareAndSet(currentCore, nextCore)
    }

    override val size: Int get() = _size.value
}

private class Core<E>(
    val capacity: Int,
) {
    private val array = atomicArrayOfNulls<E>(capacity)
    val next = atomic<Core<E>?>(null)

    @Suppress("UNCHECKED_CAST")
    fun get(index: Int): E = array[index].value as E
    fun cas(index: Int, expected: E?, updated: E?): Boolean =
        array[index].compareAndSet(expected, updated)
}

private class Element<E>(val element: E, val trans: Boolean = false)


private const val INITIAL_CAPACITY = 1 // DO NOT CHANGE ME