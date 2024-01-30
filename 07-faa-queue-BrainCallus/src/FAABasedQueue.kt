import java.util.concurrent.atomic.*
import kotlin.math.max
import kotlin.math.min

/**
 * @author Churakova Alexandra
 *
 * TODO: Copy the code from `FAABasedQueueSimplified`
 * TODO: and implement the infinite array on a linked list
 * TODO: of fixed-size `Segment`s.
 */
class FAABasedQueue<E> : Queue<E> {

    private val dummy = Segment(0)
    private val head = AtomicReference(dummy)
    private val tail = AtomicReference(dummy)
    private val enqIdx = AtomicLong(0)
    private val deqIdx = AtomicLong(0)
    override fun enqueue(element: E) {
        while (true) {
            val i = enqIdx.getAndIncrement()
            val curSeg = findSegment(i, false)
            if (curSeg.cells.compareAndSet((i % SEGMENT_SIZE).toInt(), null, element)) {
                return
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun dequeue(): E? {
        while (true) {
            if (!shouldTryToDequeue()) return null
            val i = deqIdx.getAndIncrement()
            val curSeg = findSegment(i, true)
            if (curSeg.cells.compareAndSet((i % SEGMENT_SIZE).toInt(), null, POISONED)) {
                continue
            }

            return curSeg.cells.getAndSet((i % SEGMENT_SIZE).toInt(), null) as E
        }
    }

    private fun findSegment(idx: Long, f: Boolean): Segment {
        var curSeg = (if (f) head else tail).get()
        while (curSeg.id < idx / SEGMENT_SIZE) {
            val nextSeg = curSeg.next.get()
            if (nextSeg == null) {
                curSeg.next.compareAndSet(null, Segment(curSeg.id + 1))
            }
            curSeg = curSeg.next.get()!!
        }
        return curSeg
    }

    private fun shouldTryToDequeue(): Boolean {
        while (true) {
            val curDeqIdx = deqIdx.get()
            val curEnqIdx = enqIdx.get()
            if (curDeqIdx != deqIdx.get()) continue
            return curDeqIdx < curEnqIdx
        }
    }
}

private class Segment(val id: Long) {
    val next = AtomicReference<Segment?>(null)
    val cells = AtomicReferenceArray<Any?>(SEGMENT_SIZE)
}

// DO NOT CHANGE THIS CONSTANT
private const val SEGMENT_SIZE = 2

// TODO: poison cells with this value.
private val POISONED = Any()
