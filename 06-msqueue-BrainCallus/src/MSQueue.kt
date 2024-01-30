import java.util.concurrent.atomic.*

/**
 * @author Churakova Alexandra
 */
class MSQueue<E> : Queue<E> {
    private val head: AtomicReference<Node<E>>
    private val tail: AtomicReference<Node<E>>

    init {
        val dummy = Node<E>(null)
        head = AtomicReference(dummy)
        tail = AtomicReference(dummy)
    }

    override fun enqueue(element: E) {
        while (true) {
            val curNode = Node(element)
            val curTail = tail.get()
            if (curTail.next.compareAndSet(null, curNode)) {
                tail.compareAndSet(curTail, curNode)
                return
            } else {
                tail.compareAndSet(curTail, curTail.next.get()!!)
            }
        }
    }

    override fun dequeue(): E? {
        while (true) {
            val curHead = head.get()
            val nextHead = curHead.next.get() ?: return null

            val res = nextHead.element
            if (head.compareAndSet(curHead, nextHead)) {
                head.get().element = null
                return res
            }
        }
    }

    // FOR TEST PURPOSE, DO NOT CHANGE IT.
    override fun validate() {
        check(tail.get().next.get() == null) {
            "At the end of the execution, `tail.next` must be `null`"
        }
        check(head.get().element == null) {
            "At the end of the execution, the dummy node shouldn't store an element"
        }
    }

    private class Node<E>(
        var element: E?
    ) {
        val next = AtomicReference<Node<E>?>(null)
    }
}
