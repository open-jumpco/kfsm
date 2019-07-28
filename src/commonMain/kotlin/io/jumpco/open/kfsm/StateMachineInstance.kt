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
 * This class represents an instance of a state machine.
 * It will process events, matching transitions, invoking entry, transition and exit actions.
 * @param context The events may trigger actions on the context of class C
 * @param definition The defined state machine that provides all the behaviour
 * @param initialState The initial state of the instance.
 */
class StateMachineInstance<S : Enum<S>, E : Enum<E>, C>(
    /**
     * The transition actions are performed by manipulating the context.
     */
    private val context: C,
    /**
     * The Immutable definition of the state machine.
     */
    val definition: StateMachineDefinition<S, E, C>,
    /**
     * The initialState will be assigned to the currentState
     */
    initialState: S
) {
    /**
     * This represents the current state of the state machine.
     * It will be modified during transitions
     */
    var currentState: S = initialState
        private set

    private fun execute(transition: Transition<S, E, C>, args: Array<out Any>) {
        transition.execute(context, this, args)
        if (transition.isExternal()) {
            currentState = transition.targetState!!
        }
    }

    internal fun executeEntry(context: C, targetState: S, args: Array<out Any>) {
        definition.entryActions[targetState]?.invoke(context, currentState, targetState, args)
        definition.defaultEntryAction?.invoke(context, currentState, targetState, args)
    }

    internal fun executeExit(context: C, targetState: S, args: Array<out Any>) {
        definition.exitActions[currentState]?.invoke(context, currentState, targetState,args)
        definition.defaultExitAction?.invoke(context, currentState, targetState, args)
    }

    private fun executeDefaultAction(event: E, args: Array<out Any>) {
        val defaultAction = definition.defaultActions[currentState] ?: definition.globalDefault
        if (defaultAction == null) {
            error("Transition from $currentState on $event not defined")
        } else {
            defaultAction.invoke(context, currentState, event, args)
        }
    }

    /**
     * This function will process the on and advance the state machine according to the FSM definition.
     * @param event The on received,
     */
    fun sendEvent(event: E, vararg args: Any) {
        val transitionRules = definition.transitionRules[Pair(currentState, event)]
        if (transitionRules != null) {
            val guardedTransition = transitionRules.findGuard(context, args)
            if (guardedTransition != null) {
                execute(guardedTransition, args)
            } else {
                val transition = transitionRules.transition
                if (transition != null) {
                    execute(transition, args)
                } else {
                    executeDefaultAction(event, args)
                }
            }
        } else {
            val transition = definition.defaultTransitions[event]
            if (transition != null) {
                execute(transition, args)
            } else {
                executeDefaultAction(event, args)
            }
        }
    }

    /**
     * This function will provide the list of allowed events given the current state of the machine.
     * @param includeDefaults When `true` will include default transitions in the list of allowed events.
     * @see StateMachineDefinition.allowed
     */
    fun allowed(includeDefaults: Boolean = false) = definition.allowed(currentState, includeDefaults)

    /**
     * This function will provide an indication whether the given event is allow in the current state.
     * @param event The given event that will be used in combination with current state.
     */
    fun eventAllowed(event: E, includeDefault: Boolean): Boolean = definition.eventAllowed(event, currentState, includeDefault)
}