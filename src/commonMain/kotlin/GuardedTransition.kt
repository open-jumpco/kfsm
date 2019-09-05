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
 * @suppress
 * Represents a guarded transition. The transition will be considered if the guard expression is true
 * @param startState The given state
 * @param event The given event
 * @param targetState when optional represents an internal transition
 * @param guard Expression lambda returning a Boolean
 * @param action An optional lambda that will be invoked.
 */
open class GuardedTransition<S, E, C>(
    startState: S,
    event: E?,
    targetState: S?,
    targetMap: String?,
    automatic: Boolean,
    type: TransitionType,
    private val guard: StateGuard<C>,
    action: StateAction<C>?
) : SimpleTransition<S, E, C>(startState, event, targetState, targetMap, automatic, type, action) {
    /**
     * This function will invoke the guard expression using the provided context to determine if transition can be considered.
     * @param context The provided context
     * @return result of guard lambda
     */
    fun guardMet(context: C, args: Array<out Any>): Boolean = guard.invoke(context, args)
}
