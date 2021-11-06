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
 * This handler will be active inside the top level of the stateMachine definition.
 * @author Corneil du Plessis
 * @soundtrack Wolfgang Amadeus Mozart
 */
class DslStateMachineHandler<S, E, C, A, R>(private val fsm: StateMachineBuilder<S, E, C, A, R>) {
    /**
     * Defines an expression that will determine the initial state of the state machine based on the values of the context.
     * @param deriveInitialState A lambda expression receiving context:C and returning state S.
     */
    fun initialState(deriveInitialState: StateQuery<C, S>): DslStateMachineHandler<S, E, C, A, R> {
        fsm.initialState(deriveInitialState)
        return this
    }

    var defaultInitialState: S?
        get() = fsm.defaultInitialState
        set(state) {
            fsm.defaultInitialState = state
        }

    /**
     * Provides for a list of pairs with state and map name that will be pushed and the last entry will be popped and become the current map.
     * This is required when using state machine with named maps.
     *

    ```
    initialStates {
    mutableListOf<StateMapItem<PayingTurnstileStates>>().apply {
    if (locked) {
    this.add(PayingTurnstileStates.LOCKED to "default")
    } else {
    this.add(PayingTurnstileStates.UNLOCKED to "default")
    }
    if (coins > 0) {
    this.add(PayingTurnstileStates.COINS to "coins")
    }
    }
    }
    ```

     */
    fun initialStates(deriveInitialMap: StateMapQuery<C, S>): DslStateMachineHandler<S, E, C, A, R> {
        fsm.initialStates(deriveInitialMap)
        return this
    }

    fun invariant(message: String, condition: Condition<C>): DslStateMachineHandler<S, E, C, A, R> {
        fsm.invariant(message, condition)
        return this
    }

    /**
     * Defines an action that will be invoked after a transition to a new state.
     * Any exceptions thrown by the action will be ignored.
     */
    fun onStateChange(action: StateChangeAction<C, S>) {
        fsm.afterStateChange(action)
    }

    /**
     * Defines a section for a specific state.
     * @param currentState The give state
     * @param handler A lambda with definitions for the given state
     */
    fun whenState(currentState: S, handler: DslStateMapEventHandler<S, E, C, A, R>.() -> Unit):
        DslStateMapEventHandler<S, E, C, A, R> =
        DslStateMapEventHandler(currentState, fsm.defaultStateMap).apply(handler)

    /**
     * Defines a section for default behaviour for the state machine.
     * @param handler A lambda with definition for the default behaviour of the state machine.
     */
    fun default(handler: DslStateMapDefaultEventHandler<S, E, C, A, R>.() -> Unit):
        DslStateMapDefaultEventHandler<S, E, C, A, R> =
        DslStateMapDefaultEventHandler(fsm.defaultStateMap).apply(handler)

    /**
     * Returns the completed fsm.
     */
    fun build() = fsm.complete()

    /**
     * creates a named statemap
     */
    fun stateMap(
        /**
         * The name of the state map. <ust be unique and not empty
         */
        name: String,
        /**
         * A set of valid states for the state map
         */
        validStates: Set<S>,
        /**
         * The lambda to configure the statemap
         */
        handler: DslStateMapHandler<S, E, C, A, R>.() -> Unit
    ): DslStateMapHandler<S, E, C, A, R> {
        return DslStateMapHandler(fsm.stateMap(name.trim(), validStates)).apply(handler)
    }
}
