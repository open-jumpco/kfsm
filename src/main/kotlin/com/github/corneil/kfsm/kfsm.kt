package com.github.corneil.kfsm

data class State<T, E, C>(
    val startState: T,
    val event: E,
    val endState: T,
    val action: ((C) -> Unit)?
)

class StateMachineInstance<T, E, C>(
    private val initialState: T,
    private val context: C,
    private val fsm: StateMachine<T, E, C>
) {
    var currentState: T = initialState
        private set

    fun event(event: E) {
        val state = fsm.states[currentState to event]
            ?: error("Statemachine doesn't provide for $event in $currentState")
        if (state.action != null) {
            state.action.invoke(context)
        }
        currentState = state.endState
    }
}

class StateMachine<T, E, C> {
    val states: MutableMap<Pair<T, E>, State<T, E, C>> = mutableMapOf()

    fun on(startState: T, event: E, endState: T?, action: ((C) -> Unit)?): StateMachine<T, E, C> {
        assert(states[startState to event] == null) { "Transition for $startState on $event already defined" }
        states[startState to event] = State(startState, event, endState ?: startState, action)
        return this
    }

    fun instance(initialState: T, context: C) = StateMachineInstance(initialState, context, this)
}
