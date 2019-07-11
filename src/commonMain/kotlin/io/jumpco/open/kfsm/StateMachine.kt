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
 * @param S is an enum representing all the states of the FSM
 * @param E is en enum representing all the events the FSM may receive
 * @param C is the class of the Context where the action will be applied.
 */
class StateMachine<S : Enum<S>, E : Enum<E>, C> {
    internal var deriveInitialState: StateQuery<C, S>? = null
    internal val transitionRules: MutableMap<Pair<S, E>, TransitionRules<S, E, C>> = mutableMapOf()
    internal val defaultTransitions: MutableMap<E, DefaultTransition<E, S, C>> = mutableMapOf()
    internal val entryActions: MutableMap<S, DefaultChangeAction<C, S>> = mutableMapOf()
    internal val exitActions: MutableMap<S, DefaultChangeAction<C, S>> = mutableMapOf()
    internal val defaultActions: MutableMap<S, DefaultStateAction<C, S, E>> = mutableMapOf()
    internal var globalDefault: DefaultStateAction<C, S, E>? = null
    internal var defaultEntryAction: DefaultChangeAction<C, S>? = null
    internal var defaultExitAction: DefaultChangeAction<C, S>? = null

    /**
     * This function defines a transition from the currentState equal to startState to the endState when event is
     * received and the guard expression is met. The action is executed after any exit action and before entry actions.
     * @param startState The transition will be considered when currentState matches stateState
     * @param event The event will trigger the consideration of this transition
     * @param endState The transition will change currentState to endState after executing the option action
     * @param guard The guard expression will have to be met to consider the transition
     * @param action The optional action will be executed when the transition occurs.
     */
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

    /**
     * This function defines a transition that doesn't change the state also know as an internal transition.
     * The transition will only occur if the guard expression is met.
     * @param startState The transition will be considered when currentState matches stateState
     * @param event The event will trigger the consideration of this transition
     * @param guard The guard expression will have to be met to consider the transition
     * @param action The optional action will be executed when the transition occurs.
     */
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
            transitionRules[key] = TransitionRules(
                transition = SimpleTransition(
                    startState,
                    event,
                    null,
                    action
                )
            )
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
     * This function defines an action to be invoked when no action is found matching the current state and event.
     * This will be an internal transition and will not cause a change in state or invoke entry or exit functions.
     * @param action This action will be performed
     */
    fun defaultAction(action: DefaultStateAction<C, S, E>) {
        globalDefault = action
    }

    /**
     * This method is the entry point to creating the DSL
     * @sample io.jumpco.open.kfsm.TurnstileFSM.define()
     */
    inline fun stateMachine(handler: DslStateMachineHandler<S, E, C>.() -> Unit) = DslStateMachineHandler(this).apply(handler)


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
     * This function defines an action to be invoked when no transitions are found matching the given state and on.
     * @param currentState The provided state
     * @param action This action will be invoked
     */
    fun default(currentState: S, action: DefaultStateAction<C, S, E>) {
        require(defaultActions[currentState] == null) { "Default defaultAction already defined for $currentState" }
        defaultActions[currentState] = action
    }

    /**
     * This function defines an action to be invoked when no transitions match the event. The currentState will be change to second parameter of the Pair.
     * @param event The Pair holds the event and endState and can be written as `event to state`
     * @param action The option action will be executed when this default transition occurs.
     */
    fun default(event: EventState<E, S>, action: StateAction<C>?) {
        require(defaultTransitions[event.first] == null) { "Default transition for ${event.first} already defined" }
        defaultTransitions[event.first] = DefaultTransition(event.first, event.second, action)
    }

    /**
     * This function defines an action to be invoked when no transitions are found for given event.
     * @param The event to match this transition.
     * @param action The option action will be executed when this default transition occurs.
     */
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
