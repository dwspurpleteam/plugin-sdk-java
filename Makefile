.PHONY: test build lint generate consumer lock vuln

test:
	./gradlew test --no-daemon

build:
	./gradlew build --no-daemon

lint:
	./gradlew checkstyleMain checkstyleTest --no-daemon

generate:
	./gradlew generateProto --no-daemon

consumer:
	./gradlew publishSdkPublicationToTestRepository --no-daemon
	./gradlew -p examples/consumer build --no-daemon

lock:
	./gradlew dependencies --write-locks --no-daemon > /dev/null

vuln:
	osv-scanner scan source --lockfile=gradle.lockfile
