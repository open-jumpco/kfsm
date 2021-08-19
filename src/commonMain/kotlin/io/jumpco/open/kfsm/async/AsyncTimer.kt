/*
 * Copyright (c) 2021. Open JumpCO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.jumpco.open.kfsm.async

expect class AsyncTimer<S, E, C, A, R>(
    parentFsm: AsyncStateMapInstance<S, E, C, A, R>,
    context: C,
    arg: A?,
    definition: AsyncTimerDefinition<S, E, C, A, R>
) {
    fun cancel()

    suspend fun trigger()
}
