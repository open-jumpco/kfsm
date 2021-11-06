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

package io.jumpco.open.kfsm.async

import io.jumpco.open.kfsm.AsyncStateAction
import io.jumpco.open.kfsm.TransitionType

open class AsyncTransition<S, E, C, A, R>(
    val targetState: S? = null,
    val targetMap: String? = null,
    val automatic: Boolean = false,
    val type: TransitionType = TransitionType.NORMAL,
    val action: AsyncStateAction<C, A, R>? = null
) {
    init {
        if (type == TransitionType.PUSH) {
            require(targetState != null) { "targetState is required for push transition" }
        }
    }

    /**
     * Executed exit, optional and entry actions specific in the transition.
     */
    open suspend fun execute(context: C, instance: AsyncStateMapInstance<S, E, C, A, R>, arg: A?): R? {

        if (isExternal()) {
            instance.executeExit(context, targetState!!, arg)
        }
        val result = action?.invoke(context, arg)
        if (isExternal()) {
            instance.executeEntry(context, targetState!!, arg)
        }
        return result
    }

    /**
     * Executed exit, optional and entry actions specific in the transition.
     */
    open suspend fun execute(
        context: C,
        sourceMap: AsyncStateMapInstance<S, E, C, A, R>,
        targetMap: AsyncStateMapInstance<S, E, C, A, R>?,
        arg: A?
    ): R? {
        if (isExternal()) {
            sourceMap.executeExit(context, targetState!!, arg)
        }
        val result = action?.invoke(context, arg)
        if (targetMap != null && isExternal()) {
            targetMap.executeEntry(context, targetState!!, arg)
        }
        return result
    }

    /**
     * This function provides an indicator if a Transition is internal or external.
     * When there is no targetState defined a Transition is considered internal and will not trigger entry or exit actions.
     */
    fun isExternal(): Boolean = targetState != null
}
