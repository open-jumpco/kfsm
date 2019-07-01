package io.jumpco.open.kfsm

class Turnstile(var locked: Boolean = true) {
    fun unlock() {
        assert(locked) { "Cannot unlock when not locked" }
        println("Unlock")
        locked = false
    }

    fun lock() {
        assert(!locked) { "Cannot lock when locked" }
        println("Lock")
        locked = true
    }

    fun alarm() {
        println("Alarm")
    }

    fun thankYou() {
        println("Thank You")
    }

    override fun toString(): String {
        return "Turnstile(locked=$locked)"
    }

}

enum class TurnstileStates {
    LOCKED,
    UNLOCKED
}

enum class TurnstileEvents {
    COIN,
    PASS
}


class TurnstileFSM(private val turnstile: Turnstile) {
    companion object {
        private val definition = StateMachine<TurnstileStates, TurnstileEvents, Turnstile>().dsl {
            initial { if (it.locked) TurnstileStates.LOCKED else TurnstileStates.UNLOCKED }
            state(TurnstileStates.LOCKED) {
                entry { context, startState, endState ->
                    println("entering:$startState -> $endState for $context")
                }
                event(TurnstileEvents.COIN to TurnstileStates.UNLOCKED) { ts ->
                    ts.unlock()
                }
                event(TurnstileEvents.PASS) { ts ->
                    ts.alarm()
                }
                exit { context, startState, endState ->
                    println("exiting:$startState -> $endState for $context")
                }
            }
            state(TurnstileStates.UNLOCKED) {
                entry { context, startState, endState ->
                    println("entering:$startState -> $endState for $context")
                }
                event(TurnstileEvents.COIN) { ts ->
                    ts.thankYou()
                }
                event(TurnstileEvents.PASS to TurnstileStates.LOCKED) { ts ->
                    ts.lock();
                }
                exit { context, startState, endState ->
                    println("exiting:$startState -> $endState for $context")
                }
            }
        }.build()
    }
    private val fsm = definition.create(turnstile)

    fun coin() = fsm.event(TurnstileEvents.COIN)
    fun pass() = fsm.event(TurnstileEvents.PASS)
}
