package com.github.corneil.kfsm


class StateMachine<T : Enum<T>, E : Enum<E>, C>() {
    data class Transition<T : Enum<T>, E : Enum<E>, C>(
        val startState: T,
        val event: E,
        val endState: T,
        val action: ((C) -> Unit)?
    )

    class StateMachineDslHelper<T : Enum<T>, E : Enum<E>, C>(private val fsm: StateMachine<T, E, C>) {

        fun initial(deriveInitialState: (C) -> T): StateMachineDslHelper<T, E, C> {
            fsm.deriveInitialState = deriveInitialState
            return this
        }

        fun event(event: E, handler: StateMachineDslEventHelper<T, E, C>.() -> Unit):
                StateMachineDslEventHelper<T, E, C> {
            return StateMachineDslEventHelper(event, fsm).apply(handler)
        }

        fun build() = fsm
    }

    class StateMachineDslEventHelper<T : Enum<T>, E : Enum<E>, C>(
        private val event: E,
        private val fsm: StateMachine<T, E, C>
    ) {
        fun state(states: Pair<T, T>, action: ((C) -> Unit)?): StateMachineDslEventHelper<T, E, C> {
            val key = states.first to event
            assert(fsm.transitions[key] == null) { "Transition for ${states.first} transition $event already defined" }
            fsm.transitions[key] = Transition(states.first, event, states.second, action)
            return this
        }

        fun state(startState: T, action: ((C) -> Unit)?): StateMachineDslEventHelper<T, E, C> {
            val key = startState to event
            assert(fsm.transitions[key] == null) { "Transition for $startState transition $event already defined" }
            fsm.transitions[key] = Transition(startState, event, startState, action)
            return this
        }
    }

    fun transition(event: E, startState: T, endState: T?, action: ((C) -> Unit)?) {
        assert(transitions[startState to event] == null) { "Transition for $startState transition $event already defined" }
        transitions[startState to event] = Transition(startState, event, endState ?: startState, action)
    }

    fun instance(context: C, initialState: T? = null) =
        StateMachineInstance(
            context,
            this,
            initialState ?: deriveInitialState.invoke(context) ?: error("Definition requires deriveInitialState")
        )

    fun dsl(handler: StateMachineDslHelper<T, E, C>.() -> Unit): StateMachineDslHelper<T, E, C> {
        return StateMachineDslHelper(this).apply(handler)
    }

    lateinit var deriveInitialState: ((C) -> T)
    val transitions: MutableMap<Pair<T, E>, Transition<T, E, C>> = mutableMapOf()

    class StateMachineInstance<T : Enum<T>, E : Enum<E>, C>(
        private val context: C,
        private val fsm: StateMachine<T, E, C>,
        private val initialState: T
    ) {
        var currentState: T = initialState
            private set

        fun event(event: E) {
            val state = fsm.transitions[currentState to event]
                ?: error("Statemachine doesn't provide for $event in $currentState")
            if (state.action != null) {
                state.action.invoke(context)
            }
            currentState = state.endState
        }
    }
}
