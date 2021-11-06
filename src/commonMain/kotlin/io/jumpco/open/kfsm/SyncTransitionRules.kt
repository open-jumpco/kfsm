/*
    Copyright 2019-2021 Open JumpCO

    Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
    documentation files (the "Software"), to deal in the Software without restriction, including without limitation
    the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
    and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all copies or substantial
    portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
    THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
    CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
    DEALINGS IN THE SOFTWARE.
 */

package io.jumpco.open.kfsm

/**
 * @suppress
 * Represents a collection of rule with 1 transition and a list of guarded transitions.
 * @author Corneil du Plessis
 * @soundtrack Wolfgang Amadeus Mozart
 * @param guardedTransitions The list of guarded transitions
 * @param transition The transition to use if there are no guarded transitions or no guarded transitions match.
 */
class SyncTransitionRules<S, E, C, A, R>(
    val guardedTransitions: MutableList<SyncGuardedTransition<S, E, C, A, R>> = mutableListOf(),
    transition: SimpleSyncTransition<S, E, C, A, R>? = null
) {
    var transition: SimpleSyncTransition<S, E, C, A, R>? = transition
        internal set

    /**
     * Add a guarded transition to the end of the list
     */
    fun addGuarded(guardedTransition: SyncGuardedTransition<S, E, C, A, R>) {
        guardedTransitions.add(guardedTransition)
    }

    /**
     * Find the first entry in the list of guarded transitions that match/
     * @param context The given context.
     */
    fun findGuard(context: C, arg: A? = null): SyncGuardedTransition<S, E, C, A, R>? =
        guardedTransitions.firstOrNull { it.guardMet(context, arg) }
}
