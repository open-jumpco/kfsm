# Kotlin Finite-state machine

This is a small implementation of an FSM in Kotlin.

## Resources
* [Documentation](https://open.jumpco.io/projects/kfsm/index.html)
* [API Docs](https://open.jumpco.io/projects/kfsm/javadoc/kfsm/index.html)
* [Sample Project](https://github.com/open-jumpco/kfsm-samples)

## Getting Started

The state machine implementation supports events triggering transitions from one state to another while performing an optional action as well as entry and exit actions. 

## Features
* Event driven state machine.
* External and internal transitions 
* State entry and exit actions.
* Default state actions.
* Default entry and exit actions.
* Determine allowed events.

## Todo
- [ ] Multiple state maps
- [ ] Push / pop transitions

## Quick Tutorial
This is the classic turnstile FSM model from [SMC](http://smc.sourceforge.net/)

![turnstile-fsm](src/doc/asciidoc/turnstile_fsm.png)

### Context class

The context class will perform the relevant actions. As an extra precaution we can build assertions or checks into the context class to ensure it cannot be misused.

```kotlin
class Turnstile(var locked: Boolean = true) {
    fun unlock() {
        println("Unlock")
        locked = false
    }

    fun lock() {
        println("Lock")
        locked = true
    }

    fun alarm() {
        println("Alarm")
    }

    fun returnCoin() {
        println("Return coin")
    }
    override fun toString(): String {
        return "Turnstile(locked=$locked)"
    }
}
```
### Enums for States and Events
We declare 2 enums, one for the possible states and one for the possible events.

```kotlin
enum class TurnstileStates {
    LOCKED,
    UNLOCKED
}

enum class TurnstileEvents {
    COIN,
    PASS
}

```

### Packaged definition and state machine
```kotlin
class TurnstileFSM(turnstile: Turnstile) {
    companion object {
        private val definition = stateMachine(
                TurnstileStates::class, 
                TurnstileEvents::class, 
                Turnstile::class
        ) {
            initial {
            if (locked)
                TurnstileStates.LOCKED
            else
                TurnstileStates.UNLOCKED
            }
            default {
                action { _, _, _ ->
                    alarm()
                }
            }
            state(TurnstileStates.LOCKED) {
                transition(TurnstileEvents.COIN to TurnstileStates.UNLOCKED) {
                    unlock()
                }
            }
            state(TurnstileStates.UNLOCKED) {
                transition(TurnstileEvents.COIN) {
                    returnCoin()
                }
                transition(TurnstileEvents.PASS to TurnstileStates.LOCKED) {
                    lock()
                }
            }
        }.build()
    }

    private val fsm = definition.create(turnstile)

    fun coin() = fsm.sendEvent(TurnstileEvents.COIN)
    fun pass() = fsm.sendEvent(TurnstileEvents.PASS)

    fun allowedEvents() = fsm.allowed().map { it.name }.toSet()    
}
```

The set of possible events can provided. This is useful when you have to determine which buttons to enable or something more complex.

In this case we didn't specify the internal transition when state is `LOCKED` and the `PASS` event is received. Since this should be an alarm we made it a default event which means that `PASS` will not be reported as allowed when the state is `LOCKED` 

### Using the FSM

This state machine will determine the state based on the `initial` expression.

```kotlin
val turnstile = Turnstile()
val fsm = TurnstileFSM(turnstile)

fsm.coin()
fsm.pass()
```

This model means the FSM can be instantiated as needed if the context has values that represent the state. The idea is that the context will properly maintain it's internal state.

The FSM can derive the formal state from the value(s) of properties of the context.

## Dependency 

KFSM is a multiplatform project and relies on kotlin stdlib only.
Add the relevant dependency to your build file.

### Kotlin/JVM Projects
```groovy
dependencies {
    implementation 'io.jumpco.open:kfsm-jvm:0.8.0'
}
```
### KotlinJS Projects
```groovy
dependencies {
    implementation 'io.jumpco.open:kfsm-js:0.8.0'
}
```
### Kotlin/Native Projects using LinuxX64
```groovy
dependencies {
    implementation 'io.jumpco.open:kfsm-linuxX64:0.8.0'    
}
```
### Kotlin/Native Projects using MinGW64
```groovy
dependencies {
    implementation 'io.jumpco.open:kfsm-mingwX64:0.8.0'    
}
```
### Kotlin/Native Projects using macOS
```groovy
dependencies {
    implementation 'io.jumpco.open:kfsm-macosX64:0.8.0'    
}
```


## Questions:
* Should entry / exit actions receive state or event as arguments?
* Should default actions receive state or event as arguments?
* Is there a more elegant way to define States and Events using sealed classes?
* Would support for multiple named state maps be useful? 
* Push / Pop into named map with pop providing optional new state?
* Are any features missing from the implementation?

