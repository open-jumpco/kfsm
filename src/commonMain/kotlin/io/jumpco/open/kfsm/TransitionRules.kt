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
 * Represents a collection of rule with 1 transition and a list of guarded transitions.
 * @param guardedTransitions The list of guarded transitions
 * @param transition The transition to use if there are no guarded transitions or no guarded transitions match.
 */
class TransitionRules<S, E : Enum<E>, C>(
    private val guardedTransitions: MutableList<GuardedTransition<S, E, C>> = mutableListOf(),
    internal var transition: SimpleTransition<S, E, C>? = null
) {
    /**
     * Add a guarded transition to the end of the list
     */
    fun addGuarded(guardedTransition: GuardedTransition<S, E, C>) {
        guardedTransitions.add(guardedTransition)
    }

    /**
     * Find the first entry in the list of guarded transitions that match/
     * @param context The given context.
     */
    fun findGuard(context: C, args: Array<out Any>): GuardedTransition<S, E, C>? =
        guardedTransitions.firstOrNull { it.guardMet(context, args) }
}
