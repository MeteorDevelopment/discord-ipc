# Discord IPC
Pure Java 16 library for interacting with locally running Discord instance without the use of JNI.  
Currently, only supports retrieving the logged-in user and setting user's activity.  
The library is tested on Windows, Linux and macOS.

## Gradle
```groovy
repositories {
    maven {
        name = "meteor-maven"
        url = "https://maven.meteordev.org"
    }
}

dependencies {
    implementation "meteordevelopment:discord-ipc:1.0"
    implementation "com.google.code.gson:gson:2.8.9" // GSON is not included but required
}
```

## Examples
For examples check out `example/src/main/java/test/Main.java`.  
