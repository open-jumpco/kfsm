# Kotlin Finite-state machine

This is a small implementation of an FSM in Kotlin.

The model supports events when triggered may cause a simpleTransition from one state to another while performing an optional action as well as entry and exit actions.

## Resources
* [Documentation](src/doc/asciidoc/kfsm.adoc)
* [Sample Project](https://github.com/open-jumpco/kfsm-samples)

## How to get it?

### Building locally

```cmd
git clone https://github.com/open-jumpco/kfsm.git
./gradlew publishToMavenLocal -Pprofile=default
```
The property `defaultProfile` is configured to `jvm,js,wasm,default`

The `profile=default` will detect and add the current native platform.

### Repository
```groovy
repositories {
    maven {
        url 'https://oss.sonatype.org/content/groups/public' 
    }
}
```

### JVM Projects
```groovy
dependencies {
    implementation 'io.jumpco.open:kfsm-jvm:0.5.0-SNAPSHOT'
}
```
### KotlinJS Projects
```groovy
dependencies {
    implementation 'io.jumpco.open:kfsm-js:0.5.0-SNAPSHOT'
}
```
### Kotlin/Native Projects using WASM
```groovy
dependencies {
    implementation 'io.jumpco.open:kfsm-wasm32:0.5.0-SNAPSHOT'    
}
```
### Kotlin/Native Projects using LinuxX64
```groovy
dependencies {
    implementation 'io.jumpco.open:kfsm-linuxX64:0.5.0-SNAPSHOT'    
}
```
### Kotlin/Native Projects using MinGW64
```groovy
dependencies {
    implementation 'io.jumpco.open:kfsm-mingwX64:0.5.0-SNAPSHOT'    
}
```
### Kotlin/Native Projects using macOS
```groovy
dependencies {
    implementation 'io.jumpco.open:kfsm-macosX64:0.5.0-SNAPSHOT'    
}
```

