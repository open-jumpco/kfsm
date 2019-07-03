# Kotlin Finite-state machine

This is a small implementation of an FSM in Kotlin.
This model supports events that trigger may cause a transition from one state to another while performing an optional action.

## Getting Started

Until we publish the first release you should clone, build and publish the project to local Maven.
```cmd
git clone https://github.com/open-jumpco/kfsm.git
./gradlew publish -Pprofile=default
```
The property `defaultProfile` is configured to `jvm,js,wasm`

The `profile=default` will detect and add the current native platform.

### JVM Projects
```groovy
dependencies {
    implementation 'io.jumpco.open:kfsm-jvm:0.1.0-SNAPSHOT'
}
```
### KotlinJS Projects
```groovy
dependencies {
    implementation 'io.jumpco.open:kfsm-js:0.1.0-SNAPSHOT'
}
```
### Kotlin/Native Projects using WASM
```groovy
dependencies {
    implementation 'io.jumpco.open:kfsm-wasm32:0.1.0-SNAPSHOT'    
}
```
### Kotlin/Native Projects using LinuxX64
```groovy
dependencies {
    implementation 'io.jumpco.open:kfsm-linux:0.1.0-SNAPSHOT'    
}
```
### Kotlin/Native Projects using MinGW64
```groovy
dependencies {
    implementation 'io.jumpco.open:kfsm-mingw:0.1.0-SNAPSHOT'    
}
```
### Kotlin/Native Projects using macOS
```groovy
dependencies {
    implementation 'io.jumpco.open:kfsm-macos:0.1.0-SNAPSHOT'    
}
```

An FSM can then be defined as follows:

```kotlin
class TurnstileFSM(private val turnstile: Turnstile) {
    companion object {
       private fun define() = StateMachine<TurnstileStates, TurnstileEvents, Turnstile>().dsl {
            initial { if (it.locked) TurnstileStates.LOCKED else TurnstileStates.UNLOCKED }
            state(TurnstileStates.LOCKED) {
                entry { context, startState, endState ->
                    println("entering:$startState -> $endState for $context")
                }
                event(TurnstileEvents.COIN to TurnstileStates.UNLOCKED) { ts ->
                    ts.unlock()
                }
                event(TurnstileEvents.PASS) { ts ->
                    ts.alarm()
                }
                exit { context, startState, endState ->
                    println("exiting:$startState -> $endState for $context")
                }
            }
            state(TurnstileStates.UNLOCKED) {
                entry { context, startState, endState ->
                    println("entering:$startState -> $endState for $context")
                }
                event(TurnstileEvents.COIN) { ts ->
                    ts.thankYou()
                }
                event(TurnstileEvents.PASS to TurnstileStates.LOCKED) { ts ->
                    ts.lock();
                }
                exit { context, startState, endState ->
                    println("exiting:$startState -> $endState for $context")
                }
            }
        }.build()
        private val definition by lazy { define() }
    }
    private val fsm = definition.create(turnstile)

    fun coin() = fsm.event(TurnstileEvents.COIN)
    fun pass() = fsm.event(TurnstileEvents.PASS)
}
```
Providing for simple code like:

```kotlin
val turnstile = Turnstile()
val fsm = TurnstileFSM(turnstile)

fsm.coin()
fsm.pass()
```


Questions:

Considering:
```kotlin
dsl {
    state(LOCKED) {
        event(COIN to UNLOCKED) { it.unlock() }
        event(PASS) { it.alarm() }
    }
}
```
* Will it be better to use `transition` than `event` in the DSL?
```kotlin
dsl {
    state(LOCKED) {
        transition(COIN to UNLOCKED) { it.unlock() }
        transition(PASS) { it.alarm() }
    }
}
```
* Will it be better to use `on` than `event` in the DSL?
```kotlin
dsl {
    state(LOCKED) {
        on(COIN to UNLOCKED) { it.unlock() }
        on(PASS) { it.alarm() }
    }
}
```
