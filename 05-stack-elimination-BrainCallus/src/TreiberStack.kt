import java.util.concurrent.atomic.AtomicReference

/**
 * @author TODO: Churakova Alexandra
 */
class TreiberStack<E> : Stack<E> {
    // Initially, the stack is empty.
    private val top = AtomicReference<Node<E>?>(null)

    @Suppress("ControlFlowWithEmptyBody")
    override fun push(element: E) {
        while (!tryPush(element));
    }

    fun tryPush(element: E): Boolean {
        val curHead = top.get();
        val newHead = Node(element, curHead)
        return top.compareAndSet(curHead, newHead)
    }

    override fun pop(): E? {
        val pair = tryPop()
        return if (pair.second) pair.first else pop()
    }

    fun tryPop(): Pair<E?, Boolean> {
        val curHead = top.get() ?: return null to true;
        val newHead = curHead.next
        if (top.compareAndSet(curHead, newHead))
            return curHead.element to true
        return null to false
    }

    private class Node<E>(
        val element: E,
        val next: Node<E>?
    )
}
