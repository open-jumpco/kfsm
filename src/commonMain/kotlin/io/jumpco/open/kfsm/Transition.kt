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
 * @param targetState when optional represents an internal transition
 * @param action optional lambda will be invoked when transition occurs.
 */
open class Transition<S, E : Enum<E>, C>(
    val targetState: S? = null,
    val targetMap: String? = null,
    val automatic: Boolean = false,
    val type: TransitionType = TransitionType.NORMAL,
    private val action: StateAction<C>? = null
) {
    init {
        if (type == TransitionType.PUSH) {
            require(targetState != null) { "targetState is required for push transition" }
        }
    }
    /**
     * Executed exit, optional and entry actions specific in the transition.
     */
    open fun execute(context: C, instance: StateMapInstance<S, E, C>, args: Array<out Any>) {

        if (isExternal()) {
            instance.executeExit(context, targetState!!, args)
        }
        action?.invoke(context, args)
        if (isExternal()) {
            instance.executeEntry(context, targetState!!, args)
        }
    }

    /**
     * Executed exit, optional and entry actions specific in the transition.
     */
    open fun execute(
        context: C,
        sourceMap: StateMapInstance<S, E, C>,
        targetMap: StateMapInstance<S, E, C>?,
        args: Array<out Any>
    ) {

        if (isExternal()) {
            sourceMap.executeExit(context, targetState!!, args)
        }
        action?.invoke(context, args)
        if (targetMap != null && isExternal()) {
            targetMap.executeEntry(context, targetState!!, args)
        }
    }

    /**
     * This function provides an indicator if a Transition is internal or external.
     * When there is no targetState defined a Transition is considered internal and will not trigger entry or exit actions.
     */
    fun isExternal(): Boolean = targetState != null
}
