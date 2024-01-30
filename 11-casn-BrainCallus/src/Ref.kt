import kotlinx.atomicfu.atomic

class Ref<T>(init: T) {
    val reference = atomic<Any?>(init)

    val value: T
        get() {
            while (true) {
                val curValue = reference.value
                if (curValue is Descriptor) {
                    curValue.complete()
                } else {
                    @Suppress("UNCHECKED_CAST") return curValue as T
                }
            }
        }

    fun cas(expected: Any?, update: Any?): Boolean {
        while (true) {
            val curValue = reference.value
            if (curValue is Descriptor) {
                curValue.complete()
            } else if (curValue == expected) {
                if (reference.compareAndSet(expected, update)) return true
            } else {
                return false
            }
        }
    }
}