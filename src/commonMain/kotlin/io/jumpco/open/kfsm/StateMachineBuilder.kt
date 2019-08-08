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
class StateMachineBuilder<S, E : Enum<E>, C>(validMapStates: Set<S>) {
    private var completed = false
    private var deriveInitialState: StateQuery<C, S>? = null
    private var deriveInitialStateMap: StateMapQuery<C, S>? = null
    internal val defaultStateMap = StateMapBuilder<S, E, C>(validMapStates, null, this)
    internal val namedStateMaps: MutableMap<String, StateMapBuilder<S, E, C>> = mutableMapOf()
    /**
     * This function is used to provide a method for determining the initial state of the FSM using the provided content.
     * @param init Is a function that receives a context and returns the state that represents the context
     */
    fun initial(init: StateQuery<C, S>) {
        require(!completed) { "Statemachine has been completed" }
        deriveInitialState = init
    }

    fun initialMap(init: StateMapQuery<C, S>) {
        require(!completed) { "Statemachine has been completed" }
        deriveInitialStateMap = init
    }

    fun stateMap(
        name: String,
        validStates: Set<S>,
        handler: DslStateMapHandler<S, E, C>.() -> Unit
    ): DslStateMapHandler<S, E, C> {
        require(name != "default") { "Map cannot be named 'default'" }
        val stateMapBuilder = StateMapBuilder<S, E, C>(validStates, name, this)
        namedStateMaps[name] = stateMapBuilder
        return DslStateMapHandler(stateMapBuilder).apply(handler)
    }

    /**
     * This function enables completed for the state machine definition prevent further changes to the state
     * machine behaviour.
     */
    fun complete(): StateMachineDefinition<S, E, C> {
        completed = true
        if (namedStateMaps.isNotEmpty()) {
            require(this.deriveInitialStateMap != null) { "initialMap must be defined when using named state maps" }
            require(this.deriveInitialState == null) { "deriveInitialState cannot be used with named state maps" }
        }
        return StateMachineDefinition(
            this.deriveInitialState,
            this.deriveInitialStateMap,
            this.defaultStateMap.toMap(),
            this.namedStateMaps.map { Pair(it.key, it.value.toMap()) }.toMap()
        )
    }

    inline fun stateMachine(handler: DslStateMachineHandler<S, E, C>.() -> Unit): DslStateMachineHandler<S, E, C> =
        DslStateMachineHandler(this).apply(handler)

    /**
     * This function defines a transition from the currentState equal to startState to the targetState when event is
     * received and the guard expression is met. The action is executed after any exit action and before entry actions.
     * @param startState The transition will be considered when currentState matches stateState
     * @param event The event will trigger the consideration of this transition
     * @param targetState The transition will change currentState to targetState after executing the option action
     * @param guard The guard expression will have to be met to consider the transition
     * @param action The optional action will be executed when the transition occurs.
     */

    fun transition(startState: S, event: E, targetState: S, guard: StateGuard<C>, action: StateAction<C>?) =
        defaultStateMap.transition(startState, event, targetState, guard, action)

    /**
     * This function defines a transition that doesn't change the state also know as an internal transition.
     * The transition will only occur if the guard expression is met.
     * @param startState The transition will be considered when currentState matches stateState
     * @param event The event will trigger the consideration of this transition
     * @param guard The guard expression will have to be met to consider the transition
     * @param action The optional action will be executed when the transition occurs.
     */

    fun transition(startState: S, event: E, guard: StateGuard<C>, action: StateAction<C>?) =
        defaultStateMap.transition(startState, event, guard, action)


    /**
     * This function defines a transition that will be triggered when the currentState is the same as the startState and on is received. The FSM currentState will change to the targetState after the action was executed.
     * Entry and Exit actions will also be performed.
     * @param startState transition applies when FSM currentState is the same as stateState.
     * @param event transition applies when on received.
     * @param targetState FSM will transition to targetState.
     * @param action The actions will be invoked
     */
    fun transition(startState: S, event: E, targetState: S, action: StateAction<C>?) =
        defaultStateMap.transition(startState, event, targetState, action)

    /**
     * This function defines a transition that doesn't change the state of the state machine when the currentState is startState and the on is received and after the action was performed. No entry or exit actions performed.
     * @param startState transition applies when when FSM currentState is the same as stateState
     * @param event transition applies when on received
     * @param action actions will be invoked
     */
    fun transition(startState: S, event: E, action: StateAction<C>?) =
        defaultStateMap.transition(startState, event, action)


    /**
     * This function defines an action to be invoked when no action is found matching the current state and event.
     * This will be an internal transition and will not cause a change in state or invoke entry or exit functions.
     * @param action This action will be performed
     */
    fun defaultAction(action: DefaultStateAction<C, S, E>) =
        defaultStateMap.defaultAction(action)

    /**
     * This function defines an action to be invoked when no entry action is defined for the current state.
     * @param action This action will be invoked
     */
    fun defaultEntry(action: DefaultChangeAction<C, S>) =
        defaultStateMap.defaultEntry(action)

    /**
     * This function defines an action to be invoked when no exit action is defined for the current state.
     * @param action This action will be invoked
     */
    fun defaultExit(action: DefaultChangeAction<C, S>) =
        defaultStateMap.defaultExit(action)


    /**
     * This function defines an action to be invoked when no transitions are found matching the given state and on.
     * @param currentState The provided state
     * @param action This action will be invoked
     */
    fun default(currentState: S, action: DefaultStateAction<C, S, E>) =
        defaultStateMap.default(currentState, action)

    /**
     * This function defines an action to be invoked when no transitions match the event. The currentState will be change to second parameter of the Pair.
     * @param event The Pair holds the event and targetState and can be written as `event to state`
     * @param action The option action will be executed when this default transition occurs.
     */
    fun default(event: EventState<E, S>, action: StateAction<C>?) =
        defaultStateMap.default(event, action)

    /**
     * This function defines an action to be invoked when no transitions are found for given event.
     * @param event The event to match this transition.
     * @param action The option action will be executed when this default transition occurs.
     */
    fun default(event: E, action: StateAction<C>?) =
        defaultStateMap.default(event, action)


    /**
     * This function defines an action to be invoked when the FSM changes to the provided state
     * @param currentState The provided state
     * @param action This action will be invoked
     */
    fun entry(currentState: S, action: DefaultChangeAction<C, S>) =
        defaultStateMap.entry(currentState, action)

    /**
     * This function defines an action to be invoke when the FSM changes from the provided state
     * @param currentState The provided state
     * @param action This action will be invoked
     */
    fun exit(currentState: S, action: DefaultChangeAction<C, S>) =
        defaultStateMap.exit(currentState, action)
}
