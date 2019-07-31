# Kotlin Finite-state machine

This is a small implementation of an FSM in Kotlin.

The model supports events when triggered may cause a simpleTransition from one state to another while performing an optional action as well as entry and exit actions.

## Resources
* [Documentation](https://open.jumpco.io/projects/kfsm/index.html)
* [API Docs](https://open.jumpco.io/projects/kfsm/javadoc/kfsm/index.html)
* [Sample Project](https://github.com/open-jumpco/kfsm-samples)

## How to get it?

### Repository
```groovy
repositories {
    maven {
        url 'https://oss.sonatype.org/content/groups/public' 
    }
}
```

### Kotlin/JVM Projects
```groovy
dependencies {
    implementation 'io.jumpco.open:kfsm-jvm:0.6.0-SNAPSHOT'
}
```
### KotlinJS Projects
```groovy
dependencies {
    implementation 'io.jumpco.open:kfsm-js:0.6.0-SNAPSHOT'
}
```
### Kotlin/Native Projects using WASM
```groovy
dependencies {
    implementation 'io.jumpco.open:kfsm-wasm32:0.6.0-SNAPSHOT'    
}
```
### Kotlin/Native Projects using LinuxX64
```groovy
dependencies {
    implementation 'io.jumpco.open:kfsm-linuxX64:0.6.0-SNAPSHOT'    
}
```
### Kotlin/Native Projects using MinGW64
```groovy
dependencies {
    implementation 'io.jumpco.open:kfsm-mingwX64:0.6.0-SNAPSHOT'    
}
```
### Kotlin/Native Projects using macOS
```groovy
dependencies {
    implementation 'io.jumpco.open:kfsm-macosX64:0.6.0-SNAPSHOT'    
}
```
### Building locally

```cmd
git clone https://github.com/open-jumpco/kfsm.git
./gradlew publishToMavenLocal -Pprofile=default
```
The property `defaultProfile` is configured to `jvm,js,wasm,default`

The `profile=default` will detect and add the current native platform.

## Questions:
* Should entry / exit action receive state or event as arguments?
* Should default actions recieve state or event as arguments?
* Is there a more elegant way to define States and Events using sealed classes?
* Are any features missing from the implementation?

