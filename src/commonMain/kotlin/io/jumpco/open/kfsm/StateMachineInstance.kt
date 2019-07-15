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

    private fun execute(transition: Transition<S, E, C>, args: Array<out Any>) {
        transition.execute(context, this, args)
        if (transition.isExternal()) {
            currentState = transition.endState!!
        }
    }

    internal fun executeEntry(context: C, endState: S, args: Array<out Any>) {
        fsm.entryActions[endState]?.invoke(context, currentState, endState, args)
        fsm.defaultEntryAction?.invoke(context, currentState, endState, args)
    }

    internal fun executeExit(context: C, endState: S, args: Array<out Any>) {
        fsm.exitActions[currentState]?.invoke(context, currentState, endState,args)
        fsm.defaultExitAction?.invoke(context, currentState, endState, args)
    }

    private fun executeDefaultAction(event: E, args: Array<out Any>) {
        val defaultAction = fsm.defaultActions[currentState] ?: fsm.globalDefault
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
        val transitionRules = fsm.transitionRules[Pair(currentState, event)]
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
            val transition = fsm.defaultTransitions[event]
            if (transition != null) {
                execute(transition, args)
            } else {
                executeDefaultAction(event, args)
            }
        }
    }

    /**
     * This function will provide the list of allowed events given the current state of the machine.
     * @param includeDefault When `true` will include default transitions in the list of allowed events.
     * @see StateMachine.allowed
     */
    fun allowed(includeDefaults: Boolean = false) = fsm.allowed(currentState, includeDefaults)

    /**
     * This function will provide an indication whether the given event is allow in the current state.
     * @param event The given event that will be used in combination with current state.
     */
    fun eventAllowed(event: E, includeDefault: Boolean): Boolean = fsm.eventAllowed(event, currentState, includeDefault)
}
