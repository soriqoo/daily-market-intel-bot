// build.gradle.kts
plugins {
	kotlin("jvm") version "2.0.0"
	kotlin("plugin.spring") version "2.0.0"
	id("org.springframework.boot") version "3.3.0"
	id("io.spring.dependency-management") version "1.1.6"
}

group = "com.dbot"
version = "0.1.0"

java {
	toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

repositories { mavenCentral() }

dependencies {
	implementation("org.springframework.boot:spring-boot-starter")
	implementation("org.springframework.boot:spring-boot-starter-webflux") // WebClient
	implementation("org.springframework.boot:spring-boot-starter-jdbc")
	implementation("org.springframework.boot:spring-boot-starter-mail") // Gmail SMTP

	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")

	runtimeOnly("org.postgresql:postgresql")   // 정식은 PostgreSQL 사용

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("io.projectreactor:reactor-test")
	testRuntimeOnly("com.h2database:h2")	// 테스트는 H2사용
}

tasks.withType<Test> { useJUnitPlatform() }
