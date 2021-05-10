/*
 * Copyright (c) 2020. Open JumpCO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.jumpco.open.kfsm.async

/**
 * This handler will be active inside the top level of the stateMachine definition.
 */
class AsyncDslStateMapHandler<S, E, C, A, R>(private val fsm: AsyncStateMapBuilder<S, E, C, A, R>) {

    /**
     * Defines a section for a specific state.
     * @param currentState The give state
     * @param handler A lambda with definitions for the given state
     */
    fun whenState(currentState: S, handler: AsyncDslStateMapEventHandler<S, E, C, A, R>.() -> Unit):
        AsyncDslStateMapEventHandler<S, E, C, A, R> =
        AsyncDslStateMapEventHandler(currentState, fsm).apply(handler)

    /**
     * Defines a section for default behaviour for the state machine.
     * @param handler A lambda with definition for the default behaviour of the state machine.
     */
    fun default(handler: AsyncDslStateMapDefaultEventHandler<S, E, C, A, R>.() -> Unit):
        AsyncDslStateMapDefaultEventHandler<S, E, C, A, R> =
        AsyncDslStateMapDefaultEventHandler(fsm).apply(handler)
}
