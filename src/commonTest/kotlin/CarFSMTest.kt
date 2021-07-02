/*
 * Copyright (c) 2020-2021. Open JumpCO
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package io.jumpco.open.kfsm.example

import io.jumpco.open.kfsm.functionalStateMachine
import kotlin.test.Test

/*
 * Copyright (c) 2020-2021. Open JumpCO
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

/**
 * This example was inspired by http://stephen-walsh.com/?p=264
 */
sealed class CarState(val identifier: String) {
    override fun toString(): String {
        return identifier
    }
}

object Driving : CarState("Driving")
object Parked : CarState("Parked")
object Crashed : CarState("Crashed")
object BeingRepaired : CarState("BeingRepaired")

sealed class CarEntity(val carType: String, val carModel: String, val yearOfManufacture: String) {
    override fun toString(): String {
        return "CarEntity(carType='$carType', carModel='$carModel', yearOfManufacture='$yearOfManufacture')"
    }
}

object Uninitialised : CarEntity("", "", "")
object VWGolf : CarEntity("VW", "GOLF", "2016")

data class Car(val carEntity: CarEntity, val state: CarState? = null)

class CarFSM(val car: Car) {
    val fsm = definition.create(car)
    fun sendEvent(event: CarState): Car {
        val result = fsm.sendEvent(event, car) ?: car
        return result.copy(state = fsm.currentState)
    }

    companion object {
        private val definition = functionalStateMachine(
            setOf(Driving, Parked, Crashed, BeingRepaired),
            setOf(Driving, Parked, Crashed, BeingRepaired),
            Car::class
        ) {
            initialState { state ?: Parked }
            onStateChange { oldState, newState ->
                println("onStateChange:$oldState -> $newState")
            }
            default {
                onEntry { fromState, toState, _ ->
                    println("onEntry:$fromState -> $toState")
                }
                action { state, event, _ ->
                    println("We cannot go from $state, to state $event (data = $this)")
                    this
                }
            }
            whenState(Parked) {
                onEvent(Driving to Driving) {
                    this
                }
                onEvent(BeingRepaired to BeingRepaired) {
                    this
                }
            }
            whenState(Driving) {
                onEvent(Parked to Parked) {
                    this
                }
                onEvent(Crashed to Crashed) {
                    println("We just crashed!!!")
                    this
                }
            }
            whenState(Crashed) {
                onEvent(BeingRepaired to BeingRepaired) {
                    this
                }
            }
        }.build()
    }
}

operator fun Car.plus(event: CarState): Car {
    val fsm = CarFSM(this)
    return fsm.sendEvent(event)
}

class CarFSMTest {
    @Test
    fun testCarFSM() {
        var vwGolf = Car(VWGolf)
        vwGolf += Driving
        vwGolf += Parked
        vwGolf += Crashed
        vwGolf += Driving
        vwGolf += Crashed
    }
}
