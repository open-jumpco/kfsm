package io.jumpco.open.kfsm.example

import io.jumpco.open.kfsm.functionalStateMachine
import org.junit.Test

/*
 * Copyright (c) 2020. Open JumpCO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * This example was inspired by http://stephen-walsh.com/?p=264
 */
sealed class CarState(val identifier: String) {
    override fun toString(): String {
        return "$identifier"
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
            default {
                onEntry { fromState, toState, _ ->
                    println("$fromState -> $toState")
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
    fun `test CarFSM`() {
        var vwGolf = Car(VWGolf)
        vwGolf += Driving
        vwGolf += Parked
        vwGolf += Crashed
        vwGolf += Driving
        vwGolf += Crashed
    }
}
