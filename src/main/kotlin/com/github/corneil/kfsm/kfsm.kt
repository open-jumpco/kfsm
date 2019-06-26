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
            fsm.initial(deriveInitialState)
            return this
        }

        fun state(currentState: T, handler: StateMachineDslEventHelper<T, E, C>.() -> Unit):
                StateMachineDslEventHelper<T, E, C> {
            return StateMachineDslEventHelper(currentState, fsm).apply(handler)
        }

        fun build() = fsm
    }

    class StateMachineDslEventHelper<T : Enum<T>, E : Enum<E>, C>(
        private val currentState: T,
        private val fsm: StateMachine<T, E, C>) {
        fun event(event: Pair<E, T>, action: ((C) -> Unit)?): StateMachineDslEventHelper<T, E, C> {
            val key = Pair(currentState, event.first)
            assert(fsm.transitions[key] == null) { "Transition for ${currentState} transition ${event.first} already defined" }
            fsm.transitions[key] = Transition(currentState, event.first, event.second, action)
            return this
        }

        fun event(event: E, action: ((C) -> Unit)?): StateMachineDslEventHelper<T, E, C> {
            val key = Pair(currentState, event)
            assert(fsm.transitions[key] == null) { "Transition for $currentState transition $event already defined" }
            fsm.transitions[key] = Transition(currentState, event, currentState, action)
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

    private lateinit var deriveInitialState: ((C) -> T)
    private val transitions: MutableMap<Pair<T, E>, Transition<T, E, C>> = mutableMapOf()
    fun initial(init: (C) -> T) {
        deriveInitialState = init
    }

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
