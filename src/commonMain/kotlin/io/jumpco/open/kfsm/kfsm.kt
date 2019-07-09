/*
 * Copyright (c) 2019. Open JumpCO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.jumpco.open.kfsm

typealias StateAction<C> = (C) -> Unit
typealias StateGuard<C> = (C) -> Boolean
typealias StateQuery<C, S> = ((C) -> S)
typealias DefaultChangeAction<C, S> = (C, S, S) -> Unit
typealias DefaultStateAction<C, S, E> = (C, S, E) -> Unit

/**
 * This class represents the definition of a statemachine.
 * @param S is an enum representing all the states of the FSM
 * @param E is en enum representing all the events the FSM may receive
 * @param C is the class of the Context where the action will be applied.
 */
class StateMachine<S : Enum<S>, E : Enum<E>, C> {
    private var deriveInitialState: StateQuery<C, S>? = null
    private val transitionRules: MutableMap<Pair<S, E>, TransitionRules<S, E, C>> = mutableMapOf()
    private val defaultTransitions: MutableMap<E, DefaultTransition<E, S, C>> = mutableMapOf()
    private val entryActions: MutableMap<S, DefaultChangeAction<C, S>> = mutableMapOf()
    private val exitActions: MutableMap<S, DefaultChangeAction<C, S>> = mutableMapOf()
    private val defaultActions: MutableMap<S, DefaultStateAction<C, S, E>> = mutableMapOf()
    private var globalDefault: DefaultStateAction<C, S, E>? = null
    private var defaultEntryAction: DefaultChangeAction<C, S>? = null
    private var defaultExitAction: DefaultChangeAction<C, S>? = null

    /**
     * @suppress
     */
    open class Transition<S : Enum<S>, E : Enum<E>, C>(
        internal val endState: S? = null,
        private val action: StateAction<C>? = null
    ) {
        open fun execute(context: C, instance: StateMachineInstance<S, E, C>) {
            if (endState != null) {
                instance.executeExit(context, endState)
            }
            action?.invoke(context)
            if (endState != null) {
                instance.executeEntry(context, endState)
            }

        }
    }

    /**
     * @suppress
     */
    class DefaultTransition<E : Enum<E>, S : Enum<S>, C>(
        internal val event: E,
        endState: S? = null,
        action: StateAction<C>? = null
    ) : Transition<S, E, C>(endState, action)

    /**
     * @suppress
     */
    open class SimpleTransition<S : Enum<S>, E : Enum<E>, C>(
        internal val startState: S,
        internal val event: E,
        endState: S?,
        action: StateAction<C>?
    ) : Transition<S, E, C>(endState, action)

    /**
     * @suppress
     */
    class GuardedTransition<S : Enum<S>, E : Enum<E>, C>(
        startState: S,
        event: E,
        endState: S?,
        private val guard: StateGuard<C>,
        action: StateAction<C>?
    ) : SimpleTransition<S, E, C>(startState, event, endState, action) {
        fun guardMet(context: C): Boolean = guard.invoke(context)
    }

    /**
     * @suppress
     */
    class TransitionRules<S : Enum<S>, E : Enum<E>, C>(
        private val guardedTransitions: MutableList<GuardedTransition<S, E, C>> = mutableListOf(),
        internal var transition: SimpleTransition<S, E, C>? = null
    ) {
        fun addGuarded(guardedTransition: GuardedTransition<S, E, C>) {
            guardedTransitions.add(guardedTransition)
        }

        fun findGuard(context: C): GuardedTransition<S, E, C>? = guardedTransitions.firstOrNull { it.guardMet(context) }
    }

    /**
     * This class represents an instance of a state machine.
     * It will process events, matching transitions, invoking entry, transition and exit actions.
     * @param context The events may trigger actions on the context of class C
     * @param fsm The defined state machine that provides all the behaviour
     * @param initialState The initial state of the instance.
     */
    class StateMachineInstance<S : Enum<S>, E : Enum<E>, C>(
        private val context: C,
        /**
         * The fsm contains the definition of the state machine.
         */
        val fsm: StateMachine<S, E, C>,
        private val initialState: S
    ) {
        /**
         * This represents the current state of the state machine.
         * It will be modified during transitions
         */
        var currentState: S = initialState
            private set

        private fun execute(transition: Transition<S, E, C>) {
            transition.execute(context, this)
            if (transition.endState != null) {
                currentState = transition.endState
            }
        }

        internal fun executeEntry(context: C, endState: S) {
            fsm.entryActions[endState]?.invoke(context, currentState, endState)
            fsm.defaultEntryAction?.invoke(context, currentState, endState)
        }

        internal fun executeExit(context: C, endState: S) {
            fsm.exitActions[currentState]?.invoke(context, currentState, endState)
            fsm.defaultExitAction?.invoke(context, currentState, endState)
        }

        /**
         * This function will process the on and advance the state machine according to the FSM definition.
         * @param event The on received,
         */
        fun sendEvent(event: E) {
            val transitionRules = fsm.transitionRules[Pair(currentState, event)]
            if (transitionRules == null) {
                val transition = fsm.defaultTransitions[event]
                if (transition != null) {
                    execute(transition)
                } else {
                    executeDefaultAction(event)
                }
            } else {
                val guardedTransition = transitionRules.findGuard(context)
                if (guardedTransition != null) {
                    execute(guardedTransition)
                } else {
                    val transition = transitionRules.transition
                    if (transition != null) {
                        execute(transition)
                    } else {
                        executeDefaultAction(event)
                    }
                }
            }
        }

        private fun executeDefaultAction(event: E) {
            val defaultAction = fsm.defaultActions[currentState] ?: fsm.globalDefault
            if (defaultAction == null) {
                error("Transition from $currentState on $event not defined")
            } else {
                defaultAction.invoke(context, currentState, event)
            }
        }
    }

    fun transition(startState: S, event: E, endState: S, guard: StateGuard<C>, action: StateAction<C>?) {
        val key = Pair(startState, event)
        val transitionRule = transitionRules[key]
        if (transitionRule == null) {
            val rule = TransitionRules<S, E, C>()
            rule.addGuarded(GuardedTransition(startState, event, endState, guard, action))
            transitionRules[key] = rule
        } else {
            transitionRule.addGuarded(GuardedTransition(startState, event, endState, guard, action))
        }
    }

    fun transition(startState: S, event: E, guard: StateGuard<C>, action: StateAction<C>?) {
        val key = Pair(startState, event)
        val transitionRule = transitionRules[key]
        if (transitionRule == null) {
            val rule = TransitionRules<S, E, C>()
            rule.addGuarded(GuardedTransition(startState, event, null, guard, action))
            transitionRules[key] = rule
        } else {
            transitionRule.addGuarded(GuardedTransition(startState, event, null, guard, action))
        }
    }


    /**
     * This function defines a transition that will be triggered when the currentState is the same as the startState and on is received. The FSM currentState will change to the endState after the action was executed.
     * Entry and Exit actions will also be performed.
     * @param startState transition applies when FSM currentState is the same as stateState.
     * @param event transition applies when on received.
     * @param endState FSM will transition to endState.
     * @param action The actions will be invoked
     */
    fun transition(startState: S, event: E, endState: S, action: StateAction<C>?) {
        val key = Pair(startState, event)
        val transitionRule = transitionRules[key]
        if (transitionRule == null) {
            transitionRules[key] =
                TransitionRules(transition = SimpleTransition(startState, event, endState, action))
        } else {
            require(transitionRule.transition == null) { "Unguarded Transition for $startState on $event already defined" }
            transitionRule.transition = SimpleTransition(startState, event, endState, action)
        }
    }

    /**
     * This function defines a transition that doesn't change the state of the state machine when the currentState is startState and the on is received and after the action was performed. No entry or exit actions performed.
     * @param startState transition applies when when FSM currentState is the same as stateState
     * @param event transition applies when on received
     * @param action actions will be invoked
     */
    fun transition(startState: S, event: E, action: StateAction<C>?) {
        val key = Pair(startState, event)
        val transitionRule = transitionRules[key]
        if (transitionRule == null) {
            transitionRules[key] = TransitionRules(transition = SimpleTransition(startState, event, null, action))
        } else {
            require(transitionRule.transition == null) { "Unguarded Transition for $startState on $event already defined" }
            transitionRule.transition = SimpleTransition(startState, event, null, action)
        }

    }

    /**
     * This function will create a state machine instance provided with content and optional initialState.
     * @param context The context will be provided to actions
     * @param initialState If this is not provided the function defined in `initial` will be invoked to derive the initialState.
     * @see initial
     */
    fun create(context: C, initialState: S? = null) = StateMachineInstance(
        context,
        this,
        initialState ?: deriveInitialState?.invoke(context) ?: error("Definition requires deriveInitialState")
    )

    /**
     * This function defines an action to be invoked when no action is found matching the current state and on
     * @param action This action will be performed
     */
    fun defaultAction(action: DefaultStateAction<C, S, E>) {
        globalDefault = action
    }

    /**
     * This method is the entry point to creating the DSL
     * @sample io.jumpco.open.kfsm.TurnstileFSM.define()
     */
    fun stateMachine(handler: DslStateMachineHandler<S, E, C>.() -> Unit) = DslStateMachineHandler(this).apply(handler)


    /**
     * This function defines an action to be invoked when no entry action is defined for the current state.
     * @param action This action will be invoked
     */
    fun defaultEntry(action: DefaultChangeAction<C, S>) {
        defaultEntryAction = action
    }

    /**
     * This function defines an action to be invoked when no exit action is defined for the current state.
     * @param action This action will be invoked
     */
    fun defaultExit(action: DefaultChangeAction<C, S>) {
        defaultExitAction = action
    }

    /**
     * This function defined an action to be invoked when no actions are found matching the given state and on.
     * @param currentState The provided state
     * @param action This action will be invoked
     */
    fun default(currentState: S, action: DefaultStateAction<C, S, E>) {
        require(defaultActions[currentState] == null) { "Default defaultAction already defined for $currentState" }
        defaultActions[currentState] = action
    }

    fun default(event: Pair<E, S>, action: StateAction<C>?) {
        require(defaultTransitions[event.first] == null) { "Default transition for ${event.first} already defined" }
        defaultTransitions[event.first] = DefaultTransition(event.first, event.second, action)
    }

    fun default(event: E, action: StateAction<C>?) {
        require(defaultTransitions[event] == null) { "Default transition for $event already defined" }
        defaultTransitions[event] = DefaultTransition<E, S, C>(event, null, action)
    }

    /**
     * This function defines an action to be invoked when the FSM changes to the provided state
     * @param currentState The provided state
     * @param action This action will be invoked
     */
    fun entry(currentState: S, action: DefaultChangeAction<C, S>) {
        require(entryActions[currentState] == null) { "Entry defaultAction already defined for $currentState" }
        entryActions[currentState] = action
    }

    /**
     * This function defines an action to be invoke when the FSM changes from the provided state
     * @param currentState The provided state
     * @param action This action will be invoked
     */
    fun exit(currentState: S, action: DefaultChangeAction<C, S>) {
        require(exitActions[currentState] == null) { "Exit defaultAction already defined for $currentState" }
        exitActions[currentState] = action
    }

    /**
     * This function is used to provide a method for determining the initial state of the FSM using the provided content.
     * @param init Is a function that receives a context and returns the state that represents the context
     */
    fun initial(init: StateQuery<C, S>) {
        deriveInitialState = init
    }
}



