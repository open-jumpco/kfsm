package com.github.corneil.kfsm


class StateMachine<T : Enum<T>, E : Enum<E>, C>() {
    private lateinit var deriveInitialState: ((C) -> T)
    private val transitions: MutableMap<Pair<T, E>, Transition<T, E, C>> = mutableMapOf()
    private val defaultTransitions: MutableMap<E, DefaultTransition<E, T, C>> = mutableMapOf()
    private val entryActions: MutableMap<T, (C, T, T) -> Unit> = mutableMapOf()
    private val exitActions: MutableMap<T, (C, T, T) -> Unit> = mutableMapOf()
    private val defaultActions: MutableMap<T, (C, T, E) -> Unit> = mutableMapOf()
    private var globalDefault: ((C, T, E) -> Unit)? = null
    private var defaultEntryAction: ((C, T, T) -> Unit)? = null
    private var defaultExitAction: ((C, T, T) -> Unit)? = null

    data class DefaultTransition<E : Enum<E>, T : Enum<T>, C>(
        val event: E,
        val endState: T? = null,
        val action: ((C) -> Unit)? = null
    ) {
        fun execute(context: C, instance: StateMachineInstance<T, E, C>) {
            if (endState != null) {
                instance.fsm.defaultExitAction?.let { it ->
                    it.invoke(context, instance.currentState, endState)
                }
            }
            action?.invoke(context)
            if (endState != null) {
                instance.fsm.defaultEntryAction?.let { it ->
                    it.invoke(context, instance.currentState, endState)
                }
            }
        }
    }

    data class Transition<T : Enum<T>, E : Enum<E>, C>(
        val startState: T,
        val event: E,
        val endState: T?,
        val action: ((C) -> Unit)?
    ) {
        fun execute(context: C, instance: StateMachineInstance<T, E, C>) {
            if (endState != null) {
                val exitAction = instance.fsm.exitActions[startState]
                if (exitAction != null) {
                    exitAction.invoke(context, startState, endState)
                } else {
                    instance.fsm.defaultExitAction?.invoke(context, instance.currentState, endState)
                }
            }
            action?.invoke(context)
            if (endState != null) {
                val entryAction = instance.fsm.entryActions[endState]
                if (entryAction != null) {
                    entryAction.invoke(context, startState, endState)
                } else {
                    instance.fsm.defaultEntryAction?.invoke(context, instance.currentState, endState)
                }
            }
        }
    }

    class StateMachineDslEventHelper<T : Enum<T>, E : Enum<E>, C>(
        private val currentState: T,
        private val fsm: StateMachine<T, E, C>
    ) {
        fun default(action: (C, T, E) -> Unit) {
            fsm.default(currentState, action)
        }

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


    class StateMachineDslHelper<T : Enum<T>, E : Enum<E>, C>(private val fsm: StateMachine<T, E, C>) {

        fun initial(deriveInitialState: (C) -> T): StateMachineDslHelper<T, E, C> {
            fsm.initial(deriveInitialState)
            return this
        }

        fun state(currentState: T, handler: StateMachineDslEventHelper<T, E, C>.() -> Unit):
                StateMachineDslEventHelper<T, E, C> {
            return StateMachineDslEventHelper(currentState, fsm).apply(handler)
        }

        fun default(handler: StateMachineDefaultEventHelper<T, E, C>.() -> Unit):
                StateMachineDefaultEventHelper<T, E, C> {
            return StateMachineDefaultEventHelper(fsm).apply(handler)
        }

        fun build() = fsm
    }

    class StateMachineDefaultEventHelper<T : Enum<T>, E : Enum<E>, C>(private val fsm: StateMachine<T, E, C>) {
        fun action(action: (C, T, E) -> Unit) {
            fsm.defaultAction(action)
        }

        fun entry(action: (C, T, T) -> Unit) {
            fsm.defaultEntryAction = action
        }

        fun exit(action: (C, T, T) -> Unit) {
            fsm.defaultExitAction = action
        }

        fun event(event: Pair<E, T>, action: ((C) -> Unit)?): StateMachineDefaultEventHelper<T, E, C> {
            assert(fsm.defaultTransitions[event.first] == null) { "Default transition for ${event.first} already defined" }
            fsm.defaultTransitions[event.first] = DefaultTransition(event.first, event.second, action)
            return this
        }

        fun event(event: E, action: ((C) -> Unit)?): StateMachineDefaultEventHelper<T, E, C> {
            assert(fsm.defaultTransitions[event] == null) { "Default transition for $event already defined" }
            fsm.defaultTransitions[event] = DefaultTransition<E, T, C>(event, null, action)
            return this
        }
    }


    class StateMachineInstance<T : Enum<T>, E : Enum<E>, C>(
        private val context: C,
        val fsm: StateMachine<T, E, C>,
        private val initialState: T
    ) {
        var currentState: T = initialState
            private set

        fun event(event: E) {
            val transition = fsm.transitions[Pair(currentState, event)]
            if (transition == null) {
                val defaultAction = fsm.defaultActions[currentState] ?: fsm.globalDefault
                if (defaultAction == null) {
                    error("Transition from $currentState on $event not defined")
                } else {
                    defaultAction.invoke(context, currentState, event)
                }
            } else {
                transition.execute(context, this)
                if (transition.endState != null) {
                    currentState = transition.endState
                }
            }
        }
    }

    fun transition(startState: T, event: E, endState: T, action: ((C) -> Unit)?) {
        val key = Pair(startState, event)
        assert(transitions[key] == null) {}
        transitions[key] = Transition(startState, event, endState, action)
    }

    fun transition(startState: T, event: E, action: ((C) -> Unit)?) {
        val key = Pair(startState, event)
        assert(transitions[key] == null) { "Transition for $startState transition $event already defined" }
        transitions[key] = Transition(startState, event, null, action)
    }

    fun create(context: C, initialState: T? = null) = StateMachineInstance(
        context,
        this,
        initialState ?: deriveInitialState.invoke(context) ?: error("Definition requires deriveInitialState")
    )

    fun defaultAction(action: (C, T, E) -> Unit) {
        globalDefault = action
    }

    fun dsl(handler: StateMachineDslHelper<T, E, C>.() -> Unit): StateMachineDslHelper<T, E, C> {
        return StateMachineDslHelper(this).apply(handler)
    }

    fun defaultEntry(action: (C, T, T) -> Unit) {
        defaultEntryAction = action
    }

    fun defaultExit(action: (C, T, T) -> Unit) {
        defaultExitAction = action
    }

    fun default(currentState: T, action: (C, T, E) -> Unit) {
        assert(defaultActions[currentState] == null) { "Default defaultAction already defined for $currentState" }
        defaultActions[currentState] = action
    }

    fun entry(currentState: T, action: (C, T, T) -> Unit) {
        assert(entryActions[currentState] == null) { "Entry defaultAction already defined for $currentState" }
        entryActions[currentState] = action
    }

    fun exit(currentState: T, action: (C, T, T) -> Unit) {
        assert(exitActions[currentState] == null) { "Exit defaultAction already defined for $currentState" }
        exitActions[currentState] = action
    }

    fun initial(init: (C) -> T) {
        deriveInitialState = init
    }
}



