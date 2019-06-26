package com.github.corneil.kfsm


class StateMachine<T : Enum<T>, E : Enum<E>, C>() {
    data class Transition<T : Enum<T>, E : Enum<E>, C>(
        val startState: T,
        val event: E,
        val endState: T,
        val action: ((C) -> Unit)?
    ) {
        fun execute(context: C, fsm: StateMachine<T, E, C>) {
            if (startState != endState) {
                fsm.exitActions[startState]?.let { it ->
                    it.invoke(context, startState, endState)
                }
            }
            action?.invoke(context)
            if (startState != endState) {
                fsm.entryActions[endState]?.let { it ->
                    it.invoke(context, startState, endState)
                }
            }
        }
    }

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
        private val fsm: StateMachine<T, E, C>
    ) {
        fun entry(action: (C, T, T) -> Unit) {
            fsm.entry(currentState, action)
        }

        fun exit(action: (C, T, T) -> Unit) {
            fsm.exit(currentState, action)
        }

        fun event(event: Pair<E, T>, action: ((C) -> Unit)?): StateMachineDslEventHelper<T, E, C> {
            fsm.transition(currentState, event.first, event.second, action)
            return this
        }

        fun event(event: E, action: ((C) -> Unit)?): StateMachineDslEventHelper<T, E, C> {
            fsm.transition(currentState, event, action)
            return this
        }
    }

    fun transition(startState: T, event: E, endState: T, action: ((C) -> Unit)?) {
        val key = Pair(startState, event)
        assert(transitions[key] == null) { "Transition for $startState transition $event already defined" }
        transitions[key] = Transition(startState, event, endState, action)
    }

    fun transition(startState: T, event: E, action: ((C) -> Unit)?) {
        val key = Pair(startState, event)
        assert(transitions[key] == null) { "Transition for $startState transition $event already defined" }
        transitions[key] = Transition(startState, event, startState, action)
    }

    fun create(context: C, initialState: T? = null) = StateMachineInstance(
        context,
        this,
        initialState ?: deriveInitialState.invoke(context) ?: error("Definition requires deriveInitialState")
    )

    fun dsl(handler: StateMachineDslHelper<T, E, C>.() -> Unit): StateMachineDslHelper<T, E, C> {
        return StateMachineDslHelper(this).apply(handler)
    }

    private lateinit var deriveInitialState: ((C) -> T)
    private val transitions: MutableMap<Pair<T, E>, Transition<T, E, C>> = mutableMapOf()
    private val entryActions: MutableMap<T, (C, T, T) -> Unit> = mutableMapOf()
    private val exitActions: MutableMap<T, (C, T, T) -> Unit> = mutableMapOf()
    fun entry(currentState: T, action: (C, T, T) -> Unit) {
        assert(entryActions[currentState] == null) { "Entry action already defined for $currentState" }
        entryActions[currentState] = action
    }

    fun exit(currentState: T, action: (C, T, T) -> Unit) {
        assert(exitActions[currentState] == null) { "Exit action already defined for $currentState" }
        exitActions[currentState] = action
    }

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
            val transition = fsm.transitions[currentState to event]
                ?: error("Statemachine doesn't provide for $event in $currentState")
            transition.execute(context, fsm)
            currentState = transition.endState
        }
    }
}



