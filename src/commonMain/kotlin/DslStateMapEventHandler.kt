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
 * This class is used in dsl to handle the state declarations.
 */
class DslStateMapEventHandler<S, E, C, A, R>(
    private val currentState: S,
    private val fsm: StateMapBuilder<S, E, C, A, R>
) {
    /**
     * Defines a default action when no other transition are matched
     * @param action The action will be performed when no matching state can be found
     */
    fun default(action: DefaultStateAction<C, S, E, A, R>) {
        fsm.default(currentState, action)
    }

    /**
     * Defines an action to be performed before transition causes a change in state
     * @param action The action will be performed when entering a new state
     */
    fun onEntry(action: DefaultEntryExitAction<C, S, A>) {
        fsm.entry(currentState, action)
    }

    /**
     * Defines an action to be performed after a transition has changed the state
     * @param action The action will be invoke when exiting a state.
     */
    fun onExit(action: DefaultEntryExitAction<C, S, A>) {
        fsm.exit(currentState, action)
    }

    /**
     * Defines a transition when the state is the currentState and the on is received. The state is changed to the targetState.
     * @param event A Pair with the first being the on and the second being the targetState.
     * @param action The action will be performed
     */
    fun onEvent(event: EventState<E, S>, action: SyncStateAction<C, A, R>?): DslStateMapEventHandler<S, E, C, A, R> {
        fsm.transition(currentState, event.first, event.second, action)
        return this
    }

    /**
     * Defines a guarded transition. Where the transition will only be used if the guarded expression is met
     * @param event The event and targetState the defines the transition
     * @param guard The guard expression must be met before the transition is considered.
     * @param action The optional action that may be executed
     */
    fun onEvent(
        event: EventState<E, S>,
        guard: StateGuard<C, A>,
        action: SyncStateAction<C, A, R>?
    ): DslStateMapEventHandler<S, E, C, A, R> {
        fsm.transition(currentState, event.first, event.second, guard, action)
        return this
    }

    /**
     * Defines a transition where an on causes an action but doesn't change the state.
     * @param event The event and targetState the defines the transition
     * @param action The optional action that may be executed
     */
    fun onEvent(event: E, action: SyncStateAction<C, A, R>?): DslStateMapEventHandler<S, E, C, A, R> {
        fsm.transition(currentState, event, action)
        return this
    }

    /**
     * Defines a guarded transition where an on causes an action but doesn't change the state and will only be used
     * if the guard expression is met. This will be an internal transition with no change in state.
     * @param event The event that will trigger the transition.
     * @param guard The guard expression must be met before the transition is considered.
     * @param action The optional action that may be executed
     */
    fun onEvent(
        event: E,
        guard: StateGuard<C, A>,
        action: SyncStateAction<C, A, R>?
    ): DslStateMapEventHandler<S, E, C, A, R> {
        fsm.transition(currentState, event, guard, action)
        return this
    }

    /**
     * Defines a push transition on event for a targetMap and targetState.
     * @param event The event the will trigger the transition.
     * @param targetMap The named stateMap that will be targeted.
     * @param targetState The new state within the targetMap
     * @param action The optional action that may be executed.
     */
    fun onEventPush(
        event: E,
        targetMap: String,
        targetState: S,
        action: SyncStateAction<C, A, R>?
    ): DslStateMapEventHandler<S, E, C, A, R> {
        fsm.pushTransition(currentState, event, targetMap, targetState, action)
        return this
    }

    /**
     * Defines a push transition on event for a targetMap and targetState when the guard evaluates to `true`
     * @param event The event the will trigger the transition.
     * @param targetMap The named stateMap that will be targeted.
     * @param targetState The new state within the targetMap
     * @param action The optional action that may be executed.
     */
    fun onEventPush(
        event: E,
        targetMap: String,
        targetState: S,
        guard: StateGuard<C, A>,
        action: SyncStateAction<C, A, R>?
    ): DslStateMapEventHandler<S, E, C, A, R> {
        fsm.pushTransition(currentState, event, targetMap, targetState, guard, action)
        return this
    }

    fun onEventPop(event: E, action: SyncStateAction<C, A, R>?): DslStateMapEventHandler<S, E, C, A, R> {
        fsm.popTransition(currentState, event, null, null, action)
        return this
    }

    fun onEventPop(event: Pair<E, S>, action: SyncStateAction<C, A, R>?): DslStateMapEventHandler<S, E, C, A, R> {
        fsm.popTransition(currentState, event.first, event.second, null, action)
        return this
    }

    fun onEventPop(
        event: Pair<E, S>,
        guard: StateGuard<C, A>,
        action: SyncStateAction<C, A, R>?
    ): DslStateMapEventHandler<S, E, C, A, R> {
        fsm.popTransition(currentState, event.first, event.second, null, guard, action)
        return this
    }

    fun onEventPop(
        event: E,
        targetMap: String,
        targetState: S,
        action: SyncStateAction<C, A, R>?
    ): DslStateMapEventHandler<S, E, C, A, R> {
        fsm.popTransition(currentState, event, targetState, targetMap, action)
        return this
    }

    fun onEventPop(
        event: E,
        guard: StateGuard<C, A>,
        action: SyncStateAction<C, A, R>?
    ): DslStateMapEventHandler<S, E, C, A, R> {
        fsm.popTransition(currentState, event, null, null, guard, action)
        return this
    }

    fun onEventPop(
        event: E,
        targetMap: String,
        targetState: S,
        guard: StateGuard<C, A>,
        action: SyncStateAction<C, A, R>?
    ): DslStateMapEventHandler<S, E, C, A, R> {
        fsm.popTransition(currentState, event, targetState, targetMap, guard, action)
        return this
    }

    fun automatic(targetState: S, action: SyncStateAction<C, A, R>?) {
        fsm.automatic(currentState, targetState, action)
    }

    fun automatic(targetState: S, guard: StateGuard<C, A>, action: SyncStateAction<C, A, R>?) {
        fsm.automatic(currentState, targetState, guard, action)
    }

    fun automaticPop(targetState: S, action: SyncStateAction<C, A, R>?) {
        fsm.automaticPop(currentState, targetState, action)
    }

    fun automaticPop(action: SyncStateAction<C, A, R>?) {
        fsm.automaticPop(currentState, action)
    }

    fun automaticPop(targetMap: String, targetState: S, action: SyncStateAction<C, A, R>?) {
        fsm.automaticPop(currentState, targetMap, targetState, action)
    }

    fun automaticPop(targetState: S, guard: StateGuard<C, A>, action: SyncStateAction<C, A, R>?) {
        fsm.automaticPop(currentState, targetState, guard, action)
    }

    fun automaticPop(guard: StateGuard<C, A>, action: SyncStateAction<C, A, R>?) {
        fsm.automaticPop(currentState, guard, action)
    }

    fun automaticPop(targetMap: String, targetState: S, guard: StateGuard<C, A>, action: SyncStateAction<C, A, R>?) {
        fsm.automaticPop(currentState, targetMap, targetState, guard, action)
    }

    fun automaticPush(targetMap: String, targetState: S, action: SyncStateAction<C, A, R>?) {
        fsm.automaticPush(currentState, targetMap, targetState, action)
    }

    fun automaticPush(targetMap: String, targetState: S, guard: StateGuard<C, A>, action: SyncStateAction<C, A, R>?) {
        fsm.automaticPush(currentState, targetMap, targetState, guard, action)
    }
}
