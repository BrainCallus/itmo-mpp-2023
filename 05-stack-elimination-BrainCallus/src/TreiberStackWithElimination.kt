import java.util.concurrent.*
import java.util.concurrent.atomic.*

/**
 * @author Churakova Alexandra
 */
@Suppress("ControlFlowWithEmptyBody")
open class TreiberStackWithElimination<E> : Stack<E> {
    private val stack = TreiberStack<E>()

    private val eliminationArray = AtomicReferenceArray<Any?>(ELIMINATION_ARRAY_SIZE)

    override fun push(element: E) {
        while (!stack.tryPush(element) && !tryPushElimination(element));
    }

    protected open fun tryPushElimination(element: E): Boolean {
        val randIndex = randomCellIndex()
        if (eliminationArray.compareAndSet(randIndex, CELL_STATE_EMPTY, element)) {
            for (i in 0 until ELIMINATION_WAIT_CYCLES) {
                // skip
            }
            return !eliminationArray.compareAndSet(randIndex, element, CELL_STATE_EMPTY)
        }
        return false
    }


    override fun pop(): E? {
        while (true) {
            val element = tryPopElimination()
            return if (element == null) {
                val other = stack.tryPop()
                if (other.second) other.first else pop()
            } else {
                element
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun tryPopElimination(): E? {
        return eliminationArray.getAndSet(randomCellIndex(), CELL_STATE_EMPTY) as E?
    }

    private fun randomCellIndex(): Int =
        ThreadLocalRandom.current().nextInt(eliminationArray.length())

    companion object {
        private const val ELIMINATION_ARRAY_SIZE = 2 // Do not change!
        private const val ELIMINATION_WAIT_CYCLES = 1 // Do not change!

        // Initially, all cells are in EMPTY state.
        private val CELL_STATE_EMPTY = null

        // `tryPopElimination()` moves the cell state
        // to `RETRIEVED` if the cell contains element.
        private val CELL_STATE_RETRIEVED = Any()
    }
}
