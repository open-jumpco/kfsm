package io.jumpco.open.kfsm

class Lock(initial: Int = 1) {
    var locked: Int = initial
        private set

    fun lock() {
        assert(locked == 0)
        println("Lock")
        locked += 1
    }

    fun doubleLock() {
        assert(locked == 1)
        println("DoubleLock")
        locked += 1
    }

    fun unlock() {
        assert(locked == 1)
        println("Unlock")
        locked -= 1
    }

    fun doubleUnlock() {
        assert(locked == 2)
        println("DoubleUnlock")
        locked -= 1
    }

    override fun toString(): String {
        return "Lock(locked=$locked)"
    }

}

enum class LockStates {
    LOCKED,
    DOUBLE_LOCKED,
    UNLOCKED
}

enum class LockEvents {
    LOCK,
    UNLOCK
}

class LockFSM(private val context: Lock) {
    companion object {
        private val definition = StateMachine<LockStates, LockEvents, Lock>().dsl {
            initial { context ->
                when (context.locked) {
                    0 -> LockStates.UNLOCKED
                    1 -> LockStates.LOCKED
                    2 -> LockStates.DOUBLE_LOCKED
                    else -> error("Invalid state locked=${context.locked}")
                }
            }
            default {
                action { context, state, event ->
                    println("Default action for state($state) -> event($event) for $context")
                }
                entry { context, startState, endState ->
                    println("entering:$startState -> $endState for $context")
                }
                exit { context, startState, endState ->
                    println("exiting:$startState -> $endState for $context")
                }
            }
            state(LockStates.LOCKED) {
                event(LockEvents.LOCK to LockStates.DOUBLE_LOCKED) { context ->
                    context.doubleLock()
                }
                event(LockEvents.UNLOCK to LockStates.UNLOCKED) { context ->
                    context.unlock()
                }
            }
            state(LockStates.DOUBLE_LOCKED) {
                event(LockEvents.UNLOCK to LockStates.LOCKED) { context ->
                    context.doubleUnlock()
                }
            }
            state(LockStates.UNLOCKED) {
                event(LockEvents.LOCK to LockStates.LOCKED) { context ->
                    context.lock()
                }
            }
        }.build()
    }

    private val fsm = definition.create(context)

    fun unlock() = fsm.event(LockEvents.UNLOCK)
    fun lock() = fsm.event(LockEvents.LOCK)
}
