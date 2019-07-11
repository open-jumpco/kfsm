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
 * Represents a transition from a given state and event.
 * @param startState The given state
 * @param event The given event
 * @param endState when optional represents an internal transition
 * @param action An optional lambda that will be invoked.
 */
open class SimpleTransition<S : Enum<S>, E : Enum<E>, C>(
    internal val startState: S,
    internal val event: E,
    endState: S?,
    action: StateAction<C>?
) : Transition<S, E, C>(endState, action)
