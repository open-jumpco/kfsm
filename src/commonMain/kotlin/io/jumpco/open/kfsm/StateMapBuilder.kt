/*
 * Copyright (c) 2019. Open JumpCO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.jumpco.open.kfsm

class StateMapBuilder<S, E : Enum<E>, C>(
    val validStates: Set<S>,
    val name: String? = null,
    val parentBuilder: StateMachineBuilder<S, E, C>
) {
    private val transitionRules: MutableMap<Pair<S, E>, TransitionRules<S, E, C>> = mutableMapOf()
    private val defaultTransitions: MutableMap<E, DefaultTransition<E, S, C>> = mutableMapOf()
    private val entryActions: MutableMap<S, DefaultChangeAction<C, S>> = mutableMapOf()
    private val exitActions: MutableMap<S, DefaultChangeAction<C, S>> = mutableMapOf()
    private val defaultActions: MutableMap<S, DefaultStateAction<C, S, E>> = mutableMapOf()
    private val automaticTransitions: MutableMap<S, TransitionRules<S, E, C>> = mutableMapOf()
    private var globalDefault: DefaultStateAction<C, S, E>? = null
    private var defaultEntryAction: DefaultChangeAction<C, S>? = null
    private var defaultExitAction: DefaultChangeAction<C, S>? = null

    /**
     * This function defines a transition from the currentState equal to startState to the targetState when event is
     * received and the guard expression is met. The action is executed after any exit action and before entry actions.
     * @param startState The transition will be considered when currentState matches stateState
     * @param event The event will trigger the consideration of this transition
     * @param targetState The transition will change currentState to targetState after executing the option action
     * @param guard The guard expression will have to be met to consider the transition
     * @param action The optional action will be executed when the transition occurs.
     */
    fun transition(startState: S, event: E, targetState: S, guard: StateGuard<C>, action: StateAction<C>?) {
        require(validStates.contains(startState)) { "$startState must be one of $validStates" }
        val key = Pair(startState, event)
        val transitionRule = transitionRules[key]
        val transition = GuardedTransition(
            startState,
            event,
            targetState,
            null,
            false,
            TransitionType.NORMAL,
            guard,
            action
        )
        if (transitionRule == null) {
            val rule = TransitionRules<S, E, C>()
            rule.addGuarded(transition)
            transitionRules[key] = rule
        } else {
            transitionRule.addGuarded(transition)
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
        require(validStates.contains(startState)) { "$startState must be one of $validStates" }
        val key = Pair(startState, event)
        val transitionRule = transitionRules[key]
        val transition = GuardedTransition(
            startState,
            event,
            null,
            null,
            false,
            TransitionType.NORMAL,
            guard,
            action
        )
        if (transitionRule == null) {
            val rule = TransitionRules<S, E, C>()
            rule.addGuarded(transition)
            transitionRules[key] = rule
        } else {
            transitionRule.addGuarded(transition)
        }
    }


    /**
     * This function defines a transition that will be triggered when the currentState is the same as the startState and on is received. The FSM currentState will change to the targetState after the action was executed.
     * Entry and Exit actions will also be performed.
     * @param startState transition applies when FSM currentState is the same as stateState.
     * @param event transition applies when on received.
     * @param targetState FSM will transition to targetState.
     * @param action The actions will be invoked
     */
    fun transition(startState: S, event: E, targetState: S, action: StateAction<C>?) {
        require(validStates.contains(startState)) { "$startState must be one of $validStates" }
        val key = Pair(startState, event)
        val transitionRule = transitionRules[key]
        val transition = SimpleTransition(
            startState,
            event,
            targetState,
            null,
            false,
            TransitionType.NORMAL,
            action
        )
        if (transitionRule == null) {
            transitionRules[key] = TransitionRules(transition = transition)
        } else {
            require(transitionRule.transition == null) { "Unguarded Transition for $startState on $event already defined" }
            transitionRule.transition = transition
        }
    }

    /**
     * This function defines a transition that doesn't change the state of the state machine when the currentState is startState and the on is received and after the action was performed. No entry or exit actions performed.
     * @param startState transition applies when when FSM currentState is the same as stateState
     * @param event transition applies when on received
     * @param action actions will be invoked
     */
    fun transition(startState: S, event: E, action: StateAction<C>?) {
        require(validStates.contains(startState)) { "$startState must be one of $validStates" }
        val key = Pair(startState, event)
        val transitionRule = transitionRules[key]
        val transition = SimpleTransition(
            startState,
            event,
            null,
            null,
            false,
            TransitionType.NORMAL,
            action
        )
        if (transitionRule == null) {
            transitionRules[key] = TransitionRules(transition = transition)
        } else {
            require(transitionRule.transition == null) { "Unguarded Transition for $startState on $event already defined" }
            transitionRule.transition = transition
        }

    }

    fun popTransition(startState: S, event: E, targetState: S?, targetMap: String?, action: StateAction<C>?) {
        val key = Pair(startState, event)
        val transitionRule = transitionRules[key]
        val transition = SimpleTransition(
            startState,
            event,
            targetState,
            targetMap,
            false,
            TransitionType.POP,
            action
        )
        if (transitionRule == null) {
            transitionRules[key] = TransitionRules(transition = transition)
        } else {
            require(transitionRule.transition == null) { "Unguarded Transition for $startState on $event already defined" }
            transitionRule.transition = transition
        }
    }

    fun popTransition(
        startState: S,
        event: E,
        targetState: S?,
        targetMap: String?,
        guard: StateGuard<C>,
        action: StateAction<C>?
    ) {
        require(validStates.contains(startState)) { "$startState must be one of $validStates" }
        val key = Pair(startState, event)
        val transitionRule = transitionRules[key]
        val transition = GuardedTransition(
            startState,
            event,
            targetState,
            targetMap,
            false,
            TransitionType.POP,
            guard,
            action
        )
        if (transitionRule == null) {
            val rule = TransitionRules<S, E, C>()
            rule.addGuarded(transition)
            transitionRules[key] = rule
        } else {
            transitionRule.addGuarded(transition)
        }
    }

    fun pushTransition(startState: S, event: E, targetMap: String, targetState: S, action: StateAction<C>?) {
        require(validStates.contains(startState)) { "$startState must be one of $validStates" }
        require(parentBuilder.namedStateMaps.containsKey(targetMap)) { "$targetMap map not found in ${parentBuilder.namedStateMaps.keys}" }
        val key = Pair(startState, event)
        val transitionRule = transitionRules[key]
        val transition = SimpleTransition(startState, event, targetState, targetMap, false, TransitionType.PUSH, action)
        if (transitionRule == null) {
            transitionRules[key] = TransitionRules(mutableListOf(), transition)
        } else {
            require(transitionRule.transition == null) { "Unguarded Transition for $startState on $event already defined" }
            transitionRule.transition = transition
        }
    }

    fun pushTransition(
        startState: S,
        event: E,
        targetMap: String,
        targetState: S,
        guard: StateGuard<C>,
        action: StateAction<C>?
    ) {
        require(validStates.contains(startState)) { "$startState must be one of $validStates" }
        require(parentBuilder.namedStateMaps.containsKey(targetMap)) { "$targetMap map not found in ${parentBuilder.namedStateMaps.keys}" }
        val key = Pair(startState, event)
        val transitionRule = transitionRules[key]
        val transition =
            GuardedTransition(startState, event, targetState, targetMap, false, TransitionType.PUSH, guard, action)
        if (transitionRule == null) {
            val rule = TransitionRules<S, E, C>()
            rule.addGuarded(transition)
            transitionRules[key] = rule
        } else {
            transitionRule.addGuarded(transition)
        }
    }

    fun automatic(startState: S, targetState: S, action: StateAction<C>?) {
        require(validStates.contains(targetState)) { "$targetState must be one of $validStates" }
        require(validStates.contains(startState)) { "$startState must be one of $validStates" }
        val transitionRule = automaticTransitions[startState]
        val transition =
            SimpleTransition<S, E, C>(startState, null, targetState, null, true, TransitionType.NORMAL, action)
        if (transitionRule == null) {
            automaticTransitions[startState] = TransitionRules<S, E, C>(mutableListOf(), transition)
        } else {
            require(transitionRule.transition == null) { "Unguarded Transition for $startState on already defined" }
            transitionRule.transition = transition
        }
    }

    fun automatic(startState: S, targetMap: String, targetState: S, action: StateAction<C>?) {
        require(validStates.contains(targetState)) { "$targetState must be one of $validStates" }
        require(validStates.contains(startState)) { "$startState must be one of $validStates" }
        val transitionRule = automaticTransitions[startState]
        val transition =
            SimpleTransition<S, E, C>(startState, null, targetState, targetMap, true, TransitionType.NORMAL, action)
        if (transitionRule == null) {
            automaticTransitions[startState] = TransitionRules<S, E, C>(mutableListOf(), transition)
        } else {
            require(transitionRule.transition == null) { "Unguarded Transition for $startState on already defined" }
            transitionRule.transition = transition
        }
    }

    fun automatic(startState: S, targetState: S, guard: StateGuard<C>, action: StateAction<C>?) {
        require(validStates.contains(targetState)) { "$targetState must be one of $validStates" }
        require(validStates.contains(startState)) { "$startState must be one of $validStates" }
        val transitionRule = automaticTransitions[startState]
        val transition =
            GuardedTransition<S, E, C>(startState, null, targetState, null, true, TransitionType.NORMAL, guard, action)
        if (transitionRule == null) {
            automaticTransitions[startState] = TransitionRules<S, E, C>(mutableListOf(transition))
        } else {
            transitionRule.addGuarded(transition)
        }
    }

    fun automatic(startState: S, targetMap: String, targetState: S, guard: StateGuard<C>, action: StateAction<C>?) {
        require(validStates.contains(targetState)) { "$targetState must be one of $validStates" }
        require(validStates.contains(startState)) { "$startState must be one of $validStates" }
        val transitionRule = automaticTransitions[startState]
        val transition =
            GuardedTransition<S, E, C>(
                startState,
                null,
                targetState,
                targetMap,
                true,
                TransitionType.NORMAL,
                guard,
                action
            )
        if (transitionRule == null) {
            automaticTransitions[startState] = TransitionRules<S, E, C>(mutableListOf(transition))
        } else {
            transitionRule.addGuarded(transition)
        }
    }

    fun automaticPop(startState: S, targetState: S, action: StateAction<C>?) {
        require(validStates.contains(startState)) { "$startState must be one of $validStates" }
        val transitionRule = automaticTransitions[startState]
        val transition =
            SimpleTransition<S, E, C>(startState, null, targetState, null, true, TransitionType.POP, action)
        if (transitionRule == null) {
            automaticTransitions[startState] = TransitionRules<S, E, C>(mutableListOf(), transition)
        } else {
            require(transitionRule.transition == null) { "Unguarded Transition for $startState on already defined" }
            transitionRule.transition = transition
        }
    }

    fun automaticPop(startState: S, action: StateAction<C>?) {
        require(validStates.contains(startState)) { "$startState must be one of $validStates" }
        val transitionRule = automaticTransitions[startState]
        val transition =
            SimpleTransition<S, E, C>(startState, null, null, null, true, TransitionType.POP, action)
        if (transitionRule == null) {
            automaticTransitions[startState] = TransitionRules<S, E, C>(transition = transition)
        } else {
            require(transitionRule.transition == null) { "Unguarded Transition for $startState on already defined" }
            transitionRule.transition = transition
        }
    }

    fun automaticPop(startState: S, targetMap: String, targetState: S, action: StateAction<C>?) {
        require(validStates.contains(startState)) { "$startState must be one of $validStates" }
        val transitionRule = automaticTransitions[startState]
        val transition =
            SimpleTransition<S, E, C>(startState, null, targetState, targetMap, true, TransitionType.POP, action)
        if (transitionRule == null) {
            automaticTransitions[startState] = TransitionRules<S, E, C>(transition = transition)
        } else {
            require(transitionRule.transition == null) { "Unguarded Transition for $startState on already defined" }
            transitionRule.transition = transition
        }
    }

    fun automaticPop(startState: S, targetState: S, guard: StateGuard<C>, action: StateAction<C>?) {
        require(validStates.contains(startState)) { "$startState must be one of $validStates" }
        val transitionRule = automaticTransitions[startState]
        val transition =
            GuardedTransition<S, E, C>(startState, null, targetState, null, true, TransitionType.POP, guard, action)
        if (transitionRule == null) {
            automaticTransitions[startState] = TransitionRules<S, E, C>(mutableListOf(transition))
        } else {
            transitionRule.addGuarded(transition)
        }
    }

    fun automaticPop(startState: S, guard: StateGuard<C>, action: StateAction<C>?) {
        require(validStates.contains(startState)) { "$startState must be one of $validStates" }
        val transitionRule = automaticTransitions[startState]
        val transition =
            GuardedTransition<S, E, C>(startState, null, null, null, true, TransitionType.POP, guard, action)
        if (transitionRule == null) {
            automaticTransitions[startState] = TransitionRules<S, E, C>(mutableListOf(transition))
        } else {
            transitionRule.addGuarded(transition)
        }
    }

    fun automaticPop(startState: S, targetMap: String, targetState: S, guard: StateGuard<C>, action: StateAction<C>?) {
        require(validStates.contains(startState)) { "$startState must be one of $validStates" }
        val transitionRule = automaticTransitions[startState]
        val transition =
            GuardedTransition<S, E, C>(
                startState,
                null,
                targetState,
                targetMap,
                true,
                TransitionType.POP,
                guard,
                action
            )
        if (transitionRule == null) {
            automaticTransitions[startState] = TransitionRules<S, E, C>(mutableListOf(transition))
        } else {
            transitionRule.addGuarded(transition)
        }
    }

    fun automaticPush(startState: S, targetMap: String, targetState: S, action: StateAction<C>?) {
        require(validStates.contains(startState)) { "$startState must be one of $validStates" }
        val transitionRule = automaticTransitions[startState]
        val transition =
            SimpleTransition<S, E, C>(startState, null, targetState, targetMap, true, TransitionType.PUSH, action)
        if (transitionRule == null) {
            automaticTransitions[startState] = TransitionRules<S, E, C>(transition = transition)
        } else {
            require(transitionRule.transition == null) { "Unguarded Transition for $startState on already defined" }
            transitionRule.transition = transition
        }
    }

    fun automaticPush(startState: S, targetMap: String, targetState: S, guard: StateGuard<C>, action: StateAction<C>?) {
        require(validStates.contains(startState)) { "$startState must be one of $validStates" }
        val transitionRule = automaticTransitions[startState]
        val transition =
            GuardedTransition<S, E, C>(
                startState,
                null,
                targetState,
                targetMap,
                true,
                TransitionType.PUSH,
                guard,
                action
            )
        if (transitionRule == null) {
            automaticTransitions[startState] = TransitionRules<S, E, C>(mutableListOf(transition))
        } else {
            transitionRule.addGuarded(transition)
        }
    }

    /**
     * This function defines an action to be invoked when no action is found matching the current state and event.
     * This will be an internal transition and will not cause a change in state or invoke entry or exit functions.
     * @param action This action will be performed
     */
    fun defaultAction(action: DefaultStateAction<C, S, E>) {
        globalDefault = action
    }

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
        require(validStates.contains(currentState)) { "$currentState must be one of $validStates" }
        require(defaultActions[currentState] == null) { "Default defaultAction already defined for $currentState" }
        defaultActions[currentState] = action
    }

    /**
     * This function defines an action to be invoked when no transitions match the event. The currentState will be change to second parameter of the Pair.
     * @param event The Pair holds the event and targetState and can be written as `event to state`
     * @param action The option action will be executed when this default transition occurs.
     */
    fun default(event: EventState<E, S>, action: StateAction<C>?) {
        require(defaultTransitions[event.first] == null) { "Default transition for ${event.first} already defined" }
        defaultTransitions[event.first] =
            DefaultTransition(event.first, event.second, null, false, TransitionType.NORMAL, action)
    }

    /**
     * This function defines an action to be invoked when no transitions are found for given event.
     * @param event The event to match this transition.
     * @param action The option action will be executed when this default transition occurs.
     */
    fun default(event: E, action: StateAction<C>?) {
        require(defaultTransitions[event] == null) { "Default transition for $event already defined" }
        defaultTransitions[event] = DefaultTransition<E, S, C>(event, null, null, false, TransitionType.NORMAL, action)
    }

    /**
     * This function defines an action to be invoked when the FSM changes to the provided state
     * @param currentState The provided state
     * @param action This action will be invoked
     */
    fun entry(currentState: S, action: DefaultChangeAction<C, S>) {
        require(validStates.contains(currentState)) { "$currentState must be one of $validStates" }
        require(entryActions[currentState] == null) { "Entry defaultAction already defined for $currentState" }
        entryActions[currentState] = action
    }

    /**
     * This function defines an action to be invoke when the FSM changes from the provided state
     * @param currentState The provided state
     * @param action This action will be invoked
     */
    fun exit(currentState: S, action: DefaultChangeAction<C, S>) {
        require(validStates.contains(currentState)) { "$currentState must be one of $validStates" }
        require(exitActions[currentState] == null) { "Exit defaultAction already defined for $currentState" }
        exitActions[currentState] = action
    }

    fun toMap(): StateMapDefinition<S, E, C> = StateMapDefinition(
        this.name,
        this.validStates,
        this.transitionRules.toMap(),
        this.defaultTransitions.toMap(),
        this.entryActions.toMap(),
        this.exitActions.toMap(),
        this.defaultActions.toMap(),
        this.automaticTransitions.toMap(),
        this.globalDefault,
        this.defaultEntryAction,
        this.defaultExitAction
    )

}
