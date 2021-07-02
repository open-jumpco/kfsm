/*
 * Copyright (c) 2020-2021. Open JumpCO
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package io.jumpco.open.example.kfsm

import io.jumpco.open.kfsm.PacketReaderFSM
import io.jumpco.open.kfsm.PayingTurnstileFSM
import io.jumpco.open.kfsm.SecureTurnstileFSM
import io.jumpco.open.kfsm.TurnstileFSM
import io.jumpco.open.kfsm.viz.plantUml
import io.jumpco.open.kfsm.viz.visualize
import org.junit.Before
import org.junit.Test
import java.io.File

class VisualizeTurnstileTest {
    @Before
    fun setup() {
        val generated = File("generated")
        if (generated.exists() && !generated.isDirectory) {
            error("Expected generated to be a directory")
        } else if (!generated.exists()) {
            generated.mkdirs()
        }
    }

    @Test
    fun produceVisualizationTurnstileFSM() {
        println("== TurnStile")
        val visualization =
            visualize(TurnstileFSM.definition)
        visualization.forEach { v ->
            println("$v")
        }
        File("generated", "turnstile.plantuml").writeText(plantUml(visualization))
    }

    @Test
    fun produceVisualizationPayingTurnstile() {
        println("== PayingTurnstile")
        val visualization =
            visualize(PayingTurnstileFSM.definition)
        visualization.forEach { v ->
            println("$v")
        }
        File("generated", "paying-turnstile.plantuml").writeText(plantUml(visualization))
    }

    @Test
    fun produceVisualizationSecureTurnstile() {
        println("== SecureTurnstile")
        val visualization =
            visualize(SecureTurnstileFSM.definition)
        visualization.forEach { v ->
            println("$v")
        }
        File("generated", "secure-turnstile.plantuml").writeText(plantUml(visualization))
    }

    @Test
    fun produceVisualizationPacketReader() {
        println("== PacketReader")
        val visualization =
            visualize(PacketReaderFSM.definition)
        visualization.forEach { v ->
            println("$v")
        }
        File("generated", "packet-reader.plantuml").writeText(plantUml(visualization))
    }
}
