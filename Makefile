.DEFAULT_GOAL := test

.PHONY: build
build:
	./gradlew build --no-daemon

.PHONY: test
test:
	@./gradlew test --no-daemon

.PHONY: lint
lint:
	@./gradlew checkstyleMain checkstyleTest --no-daemon

.PHONY: vuln
vuln:
	@osv-scanner scan source --lockfile=gradle.lockfile

.PHONY: generate
generate:
	@./gradlew generateProto --no-daemon

.PHONY: consumer
consumer:
	@./gradlew publishSdkPublicationToTestRepository --no-daemon
	@./gradlew -p examples/consumer build --no-daemon

.PHONY: lock
lock:
	@./gradlew dependencies --write-locks --no-daemon > /dev/null
