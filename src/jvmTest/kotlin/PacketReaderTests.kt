/*
 * Copyright (c) 2019. Open JumpCO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.jumpco.open.kfsm

import java.io.ByteArrayOutputStream

class Block {
    val byteArrayOutputStream = ByteArrayOutputStream(32)
    fun addByte(byte: Int) {
        byteArrayOutputStream.write(byte)
    }
}

class Packet {
    val fields = mutableListOf<ByteArray>()
    var currentField: Block? = null
    var checksumValid: Boolean = false
        private set
        public get
    val checkSum = Block()
    fun addField() {
        currentField = Block()
    }

    fun endField() {
        val field = currentField
        require(field != null) { "expected currentField to have a value" }
        fields.add(field.byteArrayOutputStream.toByteArray())
        currentField = null
    }

    fun addByte(byte: Int) {
        val field = currentField
        require(field != null) { "expected currentField to have a value" }
        field.addByte(byte)
    }

    fun addChecksum(byte: Int) {
        checkSum.addByte(byte)
    }

    fun checksum() {
        require(checkSum.byteArrayOutputStream.size() > 0)
        val checksumBytes = checkSum.byteArrayOutputStream.toByteArray()
        // TODO calc checksum using all fields and verify with checksumBytes and update checksumValid
    }

    fun sendNACK() {
        println("NACK")
    }

    fun sendACK() {
        println("ACK")
    }
}

enum class ReaderEvents(val code: Int) {
    BYTE(0x00),
    SOH(0x01),
    STX(0x02),
    ETX(0x03),
    EOT(0x04),
    ESC(0x1b)
}

enum class ReaderStates {
    START,
    RCVPCKT,
    RCVDATA,
    RCVESC,
    RCVCHK,
    RCVCHKESC,
    CHKSUM,
    END
}

class PacketReaderFSM(private val packet: Packet) {
    companion object {
        private val definition = stateMachine(ReaderStates.values().toSet(), ReaderEvents::class, Packet::class) {
            default {
                transition(ReaderEvents.BYTE to ReaderStates.END) {
                    sendNACK()
                }
                transition(ReaderEvents.SOH to ReaderStates.END) {
                    sendNACK()
                }
                transition(ReaderEvents.STX to ReaderStates.END) {
                    sendNACK()
                }
                transition(ReaderEvents.ETX to ReaderStates.END) {
                    sendNACK()
                }
                transition(ReaderEvents.EOT to ReaderStates.END) {
                    sendNACK()
                }
                transition(ReaderEvents.ESC to ReaderStates.END) {
                    sendNACK()
                }
            }
            state(ReaderStates.START) {
                transition(ReaderEvents.SOH to ReaderStates.RCVPCKT) {}
            }
            state(ReaderStates.RCVPCKT) {
                transition(ReaderEvents.STX to ReaderStates.RCVDATA) {
                    addField()
                }
                transition(ReaderEvents.BYTE to ReaderStates.RCVCHK) { args ->
                    require(args.size == 1)
                    args as Array<Int>
                    addChecksum(args[0])
                }
            }
            state(ReaderStates.RCVDATA) {
                transition(ReaderEvents.BYTE) { args ->
                    require(args.size == 1)
                    args as Array<Int>
                    addByte(args[0])
                }
                transition(ReaderEvents.ETX to ReaderStates.RCVPCKT) {
                    endField()
                }
                transition(ReaderEvents.ESC to ReaderStates.RCVESC) {}
            }
            state(ReaderStates.RCVESC) {
                transition(ReaderEvents.ESC to ReaderStates.RCVDATA) {
                    addByte(ReaderEvents.ESC.code)
                }
                transition(ReaderEvents.STX to ReaderStates.RCVDATA) {
                    addByte(ReaderEvents.STX.code)
                }
                transition(ReaderEvents.SOH to ReaderStates.RCVDATA) {
                    addByte(ReaderEvents.SOH.code)
                }
                transition(ReaderEvents.ETX to ReaderStates.RCVDATA) {
                    addByte(ReaderEvents.ETX.code)
                }
                transition(ReaderEvents.EOT to ReaderStates.RCVDATA) {
                    addByte(ReaderEvents.EOT.code)
                }
            }
            state(ReaderStates.RCVCHK) {
                transition(ReaderEvents.BYTE) { args ->
                    require(args.size == 1)
                    args as Array<Int>
                    addChecksum(args[0])
                }
                transition(ReaderEvents.ESC to ReaderStates.RCVCHKESC) {}
                transition(ReaderEvents.EOT to ReaderStates.CHKSUM) {
                    checksum()
                }
            }
            state(ReaderStates.CHKSUM) {
                automatic(ReaderStates.END, guard = { !checksumValid }) {
                    sendNACK()
                }
                automatic(ReaderStates.END, guard = { checksumValid }) {
                    sendACK()
                }
            }
            state(ReaderStates.RCVCHKESC) {
                transition(ReaderEvents.ESC to ReaderStates.RCVCHK) {
                    addChecksum(ReaderEvents.ESC.code)
                }
                transition(ReaderEvents.SOH to ReaderStates.RCVCHK) {
                    addChecksum(ReaderEvents.SOH.code)
                }
                transition(ReaderEvents.EOT to ReaderStates.RCVCHK) {
                    addChecksum(ReaderEvents.EOT.code)
                }
                transition(ReaderEvents.STX to ReaderStates.RCVCHK) {
                    addChecksum(ReaderEvents.STX.code)
                }
                transition(ReaderEvents.ETX to ReaderStates.RCVCHK) {
                    addChecksum(ReaderEvents.ETX.code)
                }
            }
        }.build()
    }

    private val fsm = definition.create(packet)
    fun receiveByte(byte: Int) {
        when (byte) {
            ReaderEvents.SOH.code -> fsm.sendEvent(ReaderEvents.SOH)
            ReaderEvents.STX.code -> fsm.sendEvent(ReaderEvents.STX)
            ReaderEvents.ETX.code -> fsm.sendEvent(ReaderEvents.ETX)
            ReaderEvents.EOT.code -> fsm.sendEvent(ReaderEvents.EOT)
            ReaderEvents.ESC.code -> fsm.sendEvent(ReaderEvents.ESC)
            ReaderEvents.BYTE.code -> fsm.sendEvent(ReaderEvents.BYTE, byte)
            else -> fsm.sendEvent(ReaderEvents.BYTE, byte)
        }
    }
}
