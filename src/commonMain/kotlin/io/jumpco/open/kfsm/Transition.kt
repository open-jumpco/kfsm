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
 * The base for all transitions.
 * @param endState when optional represents an internal transition
 * @param action optional lambda will be invoked when transition occurs.
 */
open class Transition<S : Enum<S>, E : Enum<E>, C>(
    internal val endState: S? = null,
    private val action: StateAction<C>? = null
) {
    /**
     * Executed exit, optional and entry actions specific in the transition.
     */
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
