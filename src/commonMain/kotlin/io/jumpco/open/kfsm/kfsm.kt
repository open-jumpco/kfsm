/*
 * Copyright (c) 2019. Open JumpCO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.jumpco.open.kfsm

/**
 * This class represents the definition of a statemachine.
 * @param T is an enum representing all the states of the FSM
 * @param E is en enum representing all the events the FSM may receive
 * @param C is the class of the Context where the action will be applied.
 */
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

    /**
     * @suppress
     */
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
    /**
     * @suppress
     */
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
    /**
     * @suppress
     */
    class DslStateMachineEventHelper<T : Enum<T>, E : Enum<E>, C>(
        private val currentState: T,
        private val fsm: StateMachine<T, E, C>
    ) {
        /**
         * Defines a default action when no other transition are matched
         * @param action The action will be performed
         */
        fun default(action: (C, T, E) -> Unit) {
            fsm.default(currentState, action)
        }

        /**
         * Defines an action to be performed before transition causes a change in state
         * @param action The action will be performed
         */
        fun entry(action: (C, T, T) -> Unit) {
            fsm.entry(currentState, action)
        }

        /**
         * Defines an action to be performed after a transition has changed the state
         */
        fun exit(action: (C, T, T) -> Unit) {
            fsm.exit(currentState, action)
        }

        /**
         * Defines a transition when the state is the currentState and the event is received. The state is changed to the endState.
         * @param event A Pair with the first being the event and the second being the endState.
         * @param action The action will be performed
         */
        fun event(event: Pair<E, T>, action: ((C) -> Unit)?): DslStateMachineEventHelper<T, E, C> {
            fsm.transition(currentState, event.first, event.second, action)
            return this
        }

        /**
         * Defines a transition where an event causes an action but doesn't change the state.
         */
        fun event(event: E, action: ((C) -> Unit)?): DslStateMachineEventHelper<T, E, C> {
            fsm.transition(currentState, event, action)
            return this
        }
    }

    /**
     * @suppress
     */
    class DslStateMachineHelper<T : Enum<T>, E : Enum<E>, C>(private val fsm: StateMachine<T, E, C>) {

        fun initial(deriveInitialState: (C) -> T): DslStateMachineHelper<T, E, C> {
            fsm.initial(deriveInitialState)
            return this
        }

        fun state(currentState: T, handler: DslStateMachineEventHelper<T, E, C>.() -> Unit):
                DslStateMachineEventHelper<T, E, C> {
            return DslStateMachineEventHelper(currentState, fsm).apply(handler)
        }

        fun default(handler: DslStateMachineDefaultEventHelper<T, E, C>.() -> Unit):
                DslStateMachineDefaultEventHelper<T, E, C> {
            return DslStateMachineDefaultEventHelper(fsm).apply(handler)
        }

        fun build() = fsm
    }
    /**
     * @suppress
     */
    class DslStateMachineDefaultEventHelper<T : Enum<T>, E : Enum<E>, C>(private val fsm: StateMachine<T, E, C>) {
        /**
         * Define a default action that will be applied when no other transitions are matched.
         */
        fun action(action: (C, T, E) -> Unit) {
            fsm.defaultAction(action)
        }

        /**
         * Defines an action to perform before a change in the currentState of the FSM
         * @param action This action will be performed
         */
        fun entry(action: (C, T, T) -> Unit) {
            fsm.defaultEntryAction = action
        }

        /**
         * Defines an action to be performed after the currentState was changed.
         * @param action The action will be performed
         */
        fun exit(action: (C, T, T) -> Unit) {
            fsm.defaultExitAction = action
        }

        /**
         * Defines a default transition when an event is received to a specific state.
         * @param event Pair representing an event and endState for transition. Can be written as EVENT to STATE
         * @param action The action will be performed before transition is completed
         */
        fun event(event: Pair<E, T>, action: ((C) -> Unit)?): DslStateMachineDefaultEventHelper<T, E, C> {
            require(fsm.defaultTransitions[event.first] == null) { "Default transition for ${event.first} already defined" }
            fsm.defaultTransitions[event.first] = DefaultTransition(event.first, event.second, action)
            return this
        }

        fun event(event: E, action: ((C) -> Unit)?): DslStateMachineDefaultEventHelper<T, E, C> {
            require(fsm.defaultTransitions[event] == null) { "Default transition for $event already defined" }
            fsm.defaultTransitions[event] = DefaultTransition<E, T, C>(event, null, action)
            return this
        }
    }

    /**
     * This class represents an instance of a state machine.
     * It will process events, matching transitions, invoking entry, transition and exit actions.
     */
    class StateMachineInstance<T : Enum<T>, E : Enum<E>, C>(
        private val context: C,
        /**
         * The fsm contains the definition of the state machine.
         */
        val fsm: StateMachine<T, E, C>,
        private val initialState: T
    ) {
        /**
         * This represents the current state of the state machine.
         * It will be modified during transitions
         */
        var currentState: T = initialState
            private set

        /**
         * This function will process the event and advance the state machine according to the FSM definition.
         * @param event The event received,
         */
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

    /**
     * This function defines a transition that will be triggered when the currentState is the same as the startState and event is received. The FSM currentState will change to the endState after the action was executed.
     * Entry and Exit actions will also be performed.
     * @param startState transition applies when FSM currentState is the same as stateState.
     * @param event transition applies when event received.
     * @param endState FSM will transition to endState.
     * @param action The actions will be invoked
     */
    fun transition(startState: T, event: E, endState: T, action: ((C) -> Unit)?) {
        val key = Pair(startState, event)
        require(transitions[key] == null) { "Transition for $startState on $event already defined"}
        transitions[key] = Transition(startState, event, endState, action)
    }
    /**
     * This function defines a transition that doesn't change the state of the state machine when the currentState is startState and the event is received and after the action was performed. No entry or exit actions performed.
     * @param startState transition applies when when FSM currentState is the same as stateState
     * @param event transition applies when event received
     * @param action actions will be invoked
     */
    fun transition(startState: T, event: E, action: ((C) -> Unit)?) {
        val key = Pair(startState, event)
        require(transitions[key] == null) { "Transition for $startState on $event already defined" }
        transitions[key] = Transition(startState, event, null, action)
    }

    /**
     * This function will create a state machine instance provided with content and optional initialState.
     * @param context The context will be provided to actions
     * @param initialState If this is not provided the function defined in `initial` will be invoked to derive the initialState.
     * @see initial
     */
    fun create(context: C, initialState: T? = null) = StateMachineInstance(
        context,
        this,
        initialState ?: deriveInitialState.invoke(context) ?: error("Definition requires deriveInitialState")
    )

    /**
     * This function defines an action to be invoked when no action is found matching the current state and event
     * @param action This action will be performed
     */
    fun defaultAction(action: (C, T, E) -> Unit) {
        globalDefault = action
    }

    /**
     * This method is the entry point to creating the DSL
     * @sample io.jumpco.open.kfsm.TurnstileFSM.Companion.define()
     */
    fun stateMachine(handler: DslStateMachineHelper<T, E, C>.() -> Unit): DslStateMachineHelper<T, E, C> {
        return DslStateMachineHelper(this).apply(handler)
    }

    /**
     * This function defines an action to be invoked when no entry action is defined for the current state.
     * @param action This action will be invoked
     */
    fun defaultEntry(action: (C, T, T) -> Unit) {
        defaultEntryAction = action
    }

    /**
     * This function defines an action to be invoked when no exit action is defined for the current state.
     * @param action This action will be invoked
     */
    fun defaultExit(action: (C, T, T) -> Unit) {
        defaultExitAction = action
    }

    /**
     * This function defined an action to be invoked when no actions are found matching the given state and event.
     * @param currentState The provided state
     * @param action This action will be invoked
     */
    fun default(currentState: T, action: (C, T, E) -> Unit) {
        require(defaultActions[currentState] == null) { "Default defaultAction already defined for $currentState" }
        defaultActions[currentState] = action
    }

    /**
     * This function defines an action to be invoked when the FSM changes to the provided state
     * @param currentState The provided state
     * @param action This action will be invoked
     */
    fun entry(currentState: T, action: (C, T, T) -> Unit) {
        require(entryActions[currentState] == null) { "Entry defaultAction already defined for $currentState" }
        entryActions[currentState] = action
    }

    /**
     * This function defines an action to be invoke when the FSM changes from the provided state
     * @param currentState The provided state
     * @param action This action will be invoked
     */
    fun exit(currentState: T, action: (C, T, T) -> Unit) {
        require(exitActions[currentState] == null) { "Exit defaultAction already defined for $currentState" }
        exitActions[currentState] = action
    }

    /**
     * This function is used to provide a method for determining the initial state of the FSM using the provided content.
     * @param init Is a function that receives a context and returns the state that represents the context
     */
    fun initial(init: (C) -> T) {
        deriveInitialState = init
    }
}



