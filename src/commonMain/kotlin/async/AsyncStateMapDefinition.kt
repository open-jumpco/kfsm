/*
 * Copyright (c) 2020. Open JumpCO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.jumpco.open.kfsm.async

import io.jumpco.open.kfsm.AsyncStateChangeAction
import io.jumpco.open.kfsm.DefaultAsyncStateAction
import io.jumpco.open.kfsm.DefaultEntryExitAction

class AsyncStateMapDefinition<S, E, C, A, R>(
    /**
     * The name of the statemap. The top-level state map name is `null`
     */
    val name: String?,
    /**
     * A set of the valid states for this map.
     */
    val validStates: Set<S>,
    /**
     * transitionRule contains a map of TransitionRules that is keyed by a Pair of state,event
     * This will be the most common transition rule.
     */
    val transitionRules: Map<Pair<S, E>, AsyncTransitionRules<S, E, C, A, R>>,
    /**
     * The default transitions will be used if no transition of found matching a given event
     */
    val defaultTransitions: Map<E, DefaultAsyncTransition<S, E, C, A, R>>,
    /**
     * This is a map of actions keyed by the state. A specific action will be invoked when a state is entered.
     */
    val entryActions: Map<S, DefaultEntryExitAction<C, S, A>>,
    /**
     * This is a map of actions keyed by the state. A specific action will be invoked when a state is exited.
     */
    val exitActions: Map<S, DefaultEntryExitAction<C, S, A>>,
    /**
     * This is a map of default actions for event on specific startState.
     */
    val defaultActions: Map<S, DefaultAsyncStateAction<C, S, E, A, R>>,
    /**
     * This a map of TransitionRules by state for automatic transitions.
     */
    val automaticTransitions: Map<S, AsyncTransitionRules<S, E, C, A, R>>,
    /**
     * The timer definitions will be activated on entry to state and deactivated on state exit
     */
    val timerDefinitions: Map<S, AsyncTimerDefinition<S, E, C, A, R>>,
    /**
     * This is the action that will be invoked of no other has been matched
     */
    val globalDefault: DefaultAsyncStateAction<C, S, E, A, R>?,
    /**
     * This is the default action that will be invoked when entering any state when no other action has been matched.
     */
    val defaultEntryAction: DefaultEntryExitAction<C, S, A>?,
    /**
     * This is the default action that will be invoked when exiting any state when no other action has been matched.
     */
    val defaultExitAction: DefaultEntryExitAction<C, S, A>?,
    /**
     * This action will be invoked after a change in the state of the statemachine.
     * This machine will catch and ignore any exceptions thrown by the handler.
     */
    val afterStateChangeAction: AsyncStateChangeAction<C, S>?
) {
    /**
     * This function will provide the set of allowed events given a specific state. It isn't a guarantee that a
     * subsequent transition will be successful since a guard may prevent a transition. Default state handlers are not considered.
     * @param given The specific state to consider
     * @param includeDefault When `true` will include default transitions in the list of allowed events.
     */
    fun allowed(given: S, includeDefault: Boolean = false): Set<E> {
        val result = transitionRules.entries.filter {
            it.key.first == given
        }.map {
            it.key.second
        }.toSet()
        if (includeDefault && defaultTransitions.isNotEmpty()) {
            return result + defaultTransitions.keys
        }
        return result
    }

    /**
     * This function will provide an indicator if an event is allow for a given state.
     * When no state transition is declared this function will return false unless `includeDefault` is true and
     * there is a default transition of handler for the event.
     */
    fun eventAllowed(event: E, given: S, includeDefault: Boolean): Boolean =
        (includeDefault &&
            hasDefaultStateHandler(given)) ||
            allowed(given, includeDefault).contains(event)

    /**
     * This function will provide an indicator if a default action has been defined for a given state.
     */
    private fun hasDefaultStateHandler(given: S) = defaultActions.contains(given)
}
