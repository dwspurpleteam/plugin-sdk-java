import com.google.protobuf.gradle.id

plugins {
    `java-library`
    `maven-publish`
    checkstyle
    id("com.google.protobuf") version "0.10.0"
}

group = "pluginsdk"
version = providers.gradleProperty("releaseVersion").getOrElse("0.1.0-SNAPSHOT")

repositories { mavenCentral() }

val grpcVersion = "1.84.0"
val protobufVersion = "4.34.2"

dependencies {
    implementation("io.grpc:grpc-protobuf:$grpcVersion")
    implementation("io.grpc:grpc-stub:$grpcVersion")
    implementation("com.google.protobuf:protobuf-java:$protobufVersion")
    implementation("com.google.guava:guava:33.7.2-jre")
    runtimeOnly("io.grpc:grpc-netty-shaded:$grpcVersion")
    compileOnly("org.apache.tomcat:annotations-api:6.0.53")

}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 21
    options.encoding = "UTF-8"
}

protobuf {
    protoc { artifact = "com.google.protobuf:protoc:$protobufVersion" }
    plugins { id("grpc") { artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion" } }
    generateProtoTasks { all().configureEach { plugins { id("grpc") } } }
}

publishing {
    publications {
        create<MavenPublication>("sdk") {
            from(components["java"])
            pom {
                name = "Plugin SDK for Java"
                description = "Java SDK for call media and events"
                licenses {
                    license {
                        name = "Apache License, Version 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0"
                    }
                }
            }
        }
    }
}

dependencyLocking { lockAllConfigurations() }

publishing {
    repositories {
        maven {
            name = "test"
            url = uri(layout.buildDirectory.dir("test-repository"))
        }
        val githubRepository = providers.environmentVariable("GITHUB_REPOSITORY").orNull
        if (githubRepository != null) {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/${githubRepository.lowercase()}")
                credentials {
                    username = providers.environmentVariable("GITHUB_ACTOR").orNull
                    password = providers.environmentVariable("GITHUB_TOKEN").orNull
                }
            }
        }
    }
}

checkstyle { toolVersion = "14.3.0" }

configurations.named("checkstyle") {
    resolutionStrategy.force("com.google.guava:guava:33.7.2-jre")
}

tasks.withType<Checkstyle>().configureEach {
    maxHeapSize = "128m"
    exclude("pluginsdk/internal/proto/**")
}

tasks.javadoc {
    setSource(fileTree("src/main/java"))
    classpath += sourceSets.main.get().output
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
}
