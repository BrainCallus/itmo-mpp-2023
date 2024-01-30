package dijkstra

import java.util.*
import java.util.concurrent.Phaser
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.Comparator
import kotlin.concurrent.thread
import kotlin.random.Random

/**
 * @author Churakova Alexandra
 */

private val NODE_DISTANCE_COMPARATOR = Comparator<Node> { o1, o2 -> o1!!.distance.compareTo(o2!!.distance) }

// Returns `Integer.MAX_VALUE` if a path has not been found.
fun shortestPathParallel(start: Node) {
    val workers = Runtime.getRuntime().availableProcessors()
    // The distance to the start node is `0`
    start.distance = 0
    // Create a priority (by distance) queue and add the start node into it
    val q = MultiQueue(workers, NODE_DISTANCE_COMPARATOR)
    q.add(start)
    // Run worker threads and wait until the total work is done
    val onFinish = Phaser(workers + 1) // `arrive()` should be invoked at the end by each worker
    val activeNodes = AtomicInteger(1)
    repeat(workers) {
        thread {
            while (activeNodes.get() != 0) {
                val cur = q.poll() ?: if (activeNodes.get() == 0) break else continue
                for (e in cur.outgoingEdges) {
                    while (true) {
                        val curDist = e.to.distance
                        val newDist = e.weight + cur.distance
                        if (curDist <= newDist) {
                            break
                        } else if (e.to.casDistance(curDist, newDist)) {
                            q.add(e.to)
                            activeNodes.incrementAndGet()
                            break
                        }
                    }
                }
                activeNodes.decrementAndGet()
            }
            onFinish.arrive()
        }
    }
    onFinish.arriveAndAwaitAdvance()
}

class MultiQueue<T>(private val cnt: Int, private val comparator: Comparator<T>) {
    private val queuesWithLock = List(cnt) { Pair(PriorityQueue(comparator), ReentrantLock()) }
    fun add(element: T) {
        while (true) {
            val idx = Random.nextInt(cnt)
            if (withLock(idx) { q -> q.add(element) }.second) return
        }
    }

    fun poll(): T? {
        while (true) {
            val i1 = Random.nextInt(cnt)
            val i2 = Random.nextInt(cnt)
            val queue1 = queuesWithLock[i1].first
            val queue2 = queuesWithLock[i2].first
            val p1 = queue1.peek()
            val p2 = queue2.peek()
            val queueIndex = if (p1 == null && p2 == null) return null
            else if (p1 != null && p2 == null) i1
            else if (p1 == null) i2
            else if (comparator.compare(p1, p2) > 0) i1 else i2
            val res = withLock(queueIndex) { q -> q.poll() }
            if (res.second) return res.first
        }
    }

    private fun <E> withLock(index: Int, action: (PriorityQueue<T>) -> E): Pair<E?, Boolean> {
        if (queuesWithLock[index].second.tryLock()) {
            try {
                return Pair(action(queuesWithLock[index].first), true)
            } finally {
                queuesWithLock[index].second.unlock()
            }
        }
        return Pair(null, false)
    }
}