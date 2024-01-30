import java.util.concurrent.atomic.*

/**
 * @author Churakova Alexandra
 */

class Solution(private val env: Environment) : Lock<Solution.Node> {
    private val tail = AtomicReference<Node>()

    override fun lock(): Node {
        val my = Node() // сделали узел
        val prev = tail.getAndSet(my) ?: return my
        my.locked.set(true)
        prev.nextNode.value = my
        while (my.locked.value) {
            env.park()
        }

        return my // вернули узел
    }

    override fun unlock(node: Node) {
        if (node.nextNode.value == null) {
            if (tail.compareAndSet(node, null)) {
                return
            }
            while (node.nextNode.value == null) {
                continue
            }
        }
        node.nextNode.value.locked.set(false)
        env.unpark(node.nextNode.value.thread)
    }


    class Node {
        val thread = Thread.currentThread() // запоминаем поток, которые создал узел
        val locked = AtomicReference(false)
        val nextNode = AtomicReference<Node>(null)
    }
}