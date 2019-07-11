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

    private fun executeDefaultAction(event: E) {
        val defaultAction = fsm.defaultActions[currentState] ?: fsm.globalDefault
        if (defaultAction == null) {
            error("Transition from $currentState on $event not defined")
        } else {
            defaultAction.invoke(context, currentState, event)
        }
    }

    /**
     * This function will process the on and advance the state machine according to the FSM definition.
     * @param event The on received,
     */
    fun sendEvent(event: E) {
        val transitionRules = fsm.transitionRules[Pair(currentState, event)]
        if (transitionRules != null) {
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
        } else {
            val transition = fsm.defaultTransitions[event]
            if (transition != null) {
                execute(transition)
            } else {
                executeDefaultAction(event)
            }
        }
    }
}
