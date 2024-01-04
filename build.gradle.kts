plugins {
    id("java")
}

group = "de.tum.cit.ase"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.8.0.202311291450-r")
    implementation ("commons-io:commons-io:2.15.1")
    implementation ("org.slf4j:slf4j-api:2.0.10")
    implementation("io.reactivex.rxjava3:rxjava:3.1.8")
    implementation("io.projectreactor.netty:reactor-netty:1.1.14")
    implementation("org.springframework.boot:spring-boot-starter-webflux:3.2.1")
    implementation ("io.netty:netty-resolver-dns-native-macos:4.1.100.Final:osx-aarch_64")
}

tasks.test {
    useJUnitPlatform()
}
