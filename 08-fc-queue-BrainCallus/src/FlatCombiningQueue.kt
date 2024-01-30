import Result
import java.util.NoSuchElementException
import java.util.concurrent.*
import java.util.concurrent.atomic.*

/**
 * @author Churakova Alexandra
 */

class FlatCombiningQueue<E> : Queue<E> {
    private val queue = ArrayDeque<E>() // sequential queue
    private val combinerLock = AtomicBoolean(false) // unlocked initially
    private val tasksForCombiner = AtomicReferenceArray<Any?>(TASKS_FOR_COMBINER_SIZE)

    override fun enqueue(element: E) {
        if (tryLock()) {
            return operationWithHelp(queue.addLast(element))
        } else {
            var idx = randomCellIndex()
            while (!tasksForCombiner.compareAndSet(idx, null, element)) {
                idx = randomCellIndex()
            }
            while (true) {
                val res = tasksForCombiner.get(idx)
                if (res !== null) {
                    if (res is Result<*>) {
                        tasksForCombiner.compareAndSet(idx, res, null)
                        return
                    }
                }
                if (tryLock()) {
                    val res1 = tasksForCombiner.get(idx)
                    tasksForCombiner.compareAndExchange(idx, res1, null)
                    if (res1 != null) {
                        if (res1 is Result<*>) {
                            releaseLock()
                            return
                        } else {
                            return operationWithHelp(queue.addLast(element))
                        }
                    } else {
                        releaseLock()
                    }
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun dequeue(): E? {
        if (tryLock()) {
            return operationWithHelp(removeAndGet())
        } else {
            var idx = randomCellIndex()
            while (!tasksForCombiner.compareAndSet(idx, null, Dequeue)) {
                idx = randomCellIndex()
            }
            while (true) {
                val res = tasksForCombiner[idx]
                if (res !== null) {
                    if (res is Result<*>) {
                        tasksForCombiner.compareAndSet(idx, res, null)
                        return res.value as E
                    }
                }
                if (tryLock()) {
                    val res1 = tasksForCombiner[idx]
                    tasksForCombiner.compareAndSet(idx, res1, null)
                    if (res1 != null) {
                        if (res1 == Dequeue) {
                            return operationWithHelp(removeAndGet())
                        } else {
                            releaseLock()
                            if (res1 is Result<*>) {
                                return res1.value as E
                            }
                        }
                    } else {
                        releaseLock()
                    }
                }
            }
        }
    }

    private fun removeAndGet(): E? {
        val el = queue.firstOrNull()
        try {
            queue.removeFirst()
        } catch (e: NoSuchElementException) {
            // ignored
        }
        return el
    }

    private fun <T> operationWithHelp(res: T): T {
        helpOther()
        releaseLock()
        return res
    }

    @Suppress("UNCHECKED_CAST")
    private fun helpOther() {
        for (i in 0 until tasksForCombiner.length()) {
            val operation = tasksForCombiner[i]
            if (operation == null || operation is Result<*>) {
                continue
            }
            if (operation == Dequeue) {
                tasksForCombiner.compareAndSet(i, operation, Result(removeAndGet()))
            } else {
                val el = operation as E? ?: throw RuntimeException("Element is null")
                queue.addLast(el)
                if (queue.isEmpty()) {
                    throw RuntimeException("Failed to add element")
                }
                tasksForCombiner.compareAndSet(i, operation, Result(el))
            }
        }
    }

    private fun tryLock(): Boolean {
        return combinerLock.compareAndSet(false, true)
    }

    private fun releaseLock() {
        combinerLock.set(false)
    }

    private fun randomCellIndex(): Int =
        ThreadLocalRandom.current().nextInt(tasksForCombiner.length())
}

private const val TASKS_FOR_COMBINER_SIZE = 3 // Do not change this constant!

// TODO: Put this token in `tasksForCombiner` for dequeue().
// TODO: enqueue()-s should put the inserting element.
private object Dequeue

// TODO: Put the result wrapped with `Result` when the operation in `tasksForCombiner` is processed.
private class Result<V>(
    val value: V
)
