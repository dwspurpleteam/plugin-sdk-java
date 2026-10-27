plugins { application }

repositories {
    maven { url = uri("../../build/test-repository") }
    mavenCentral()
}

configurations.configureEach { resolutionStrategy.cacheChangingModulesFor(0, "seconds") }

dependencies { implementation("pluginsdk:plugin-sdk:0.1.0-SNAPSHOT") }

tasks.withType<JavaCompile>().configureEach { options.release = 21 }
application { mainClass = "SdkExample" }
