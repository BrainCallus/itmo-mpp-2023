abstract class Descriptor {
    val state = Ref(State.UNDECIDED)
    abstract fun complete()
}

class CASDescriptor<A, B>(
    private val aVal: Ref<A>,
    private val aExpect: A,
    private val updateA: A,
    private val bVal: Ref<B>,
    private val bExpect: B,
    private val updateB: B
) : Descriptor() {
    override fun complete() {
        if (bVal.reference.value == this) {
            state.reference.compareAndSet(State.UNDECIDED, State.SUCCESS)
        } else {
            val rdcss = RDCSSDescriptor(bVal, bExpect, this, state, State.UNDECIDED)
            if (bVal.cas(bExpect, rdcss)) {
                rdcss.complete()
                state.reference.compareAndSet(State.UNDECIDED, State.SUCCESS)
            } else {
                state.reference.compareAndSet(State.UNDECIDED, State.FAIL)
            }
        }

        if (state.value == State.SUCCESS) {
            aVal.reference.compareAndSet(this, updateA)
            bVal.reference.compareAndSet(this, updateB)
        } else {
            aVal.reference.compareAndSet(this, aExpect)
            bVal.reference.compareAndSet(this, bExpect)
        }
    }
}

class RDCSSDescriptor<A, B>(
    private val aVal: Ref<A>,
    private val aExpect: Any?,
    private val updateA: Any?,
    private val bVal: Ref<B>,
    private val bExpect: Any?
) : Descriptor() {
    override fun complete() {
        val newState = if (bVal.value == bExpect) State.SUCCESS else State.FAIL
        state.reference.compareAndSet(State.UNDECIDED, newState)

        val update = if (state.value == State.SUCCESS) updateA else aExpect
        aVal.reference.compareAndSet(this, update)
    }
}

enum class State {
    UNDECIDED, SUCCESS, FAIL
}