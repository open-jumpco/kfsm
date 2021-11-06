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
 * Represents a guarded transition. The transition will be considered if the guard expression is true
 * @author Corneil du Plessis
 * @soundtrack Wolfgang Amadeus Mozart
 * @param startState The given state
 * @param event The given event
 * @param targetState when optional represents an internal transition
 * @param guard Expression lambda returning a Boolean
 * @param action An optional lambda that will be invoked.
 */
open class SyncGuardedTransition<S, E, C, A, R>(
    startState: S,
    event: E?,
    targetState: S?,
    targetMap: String?,
    automatic: Boolean,
    type: TransitionType,
    val guard: StateGuard<C, A>,
    action: SyncStateAction<C, A, R>?
) : SimpleSyncTransition<S, E, C, A, R>(startState, event, targetState, targetMap, automatic, type, action) {
    /**
     * This function will invoke the guard expression using the provided context to determine if transition can be considered.
     * @param context The provided context
     * @return result of guard lambda
     */
    fun guardMet(context: C, arg: A?): Boolean = guard.invoke(context, arg)
}
