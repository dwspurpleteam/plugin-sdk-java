# plugin-sdk-java

Java библиотека для получения аудио, видео и событий звонков через streamapi

Сборка и проверки

```bash
./gradlew build
```

Форматирование кода

```bash
./gradlew spotlessApply
```

Проверка примера подключения

```bash
./gradlew publishSdkPublicationToTestRepository
./gradlew -p examples/consumer build
```

Генерация proto и обновление зависимостей

```bash
./gradlew generateProto
./gradlew dependencies --write-locks
```

Проверка уязвимостей с установленным OSV Scanner

```bash
osv-scanner scan source --lockfile=gradle.lockfile
```

Действия плагина в звонке выполняются через тот же `PluginClient`:

```java
var client = PluginClient.builder("calls.vareonix.cc:443")
    .metadata("authorization", "Bearer " + token)
    .build();
client.muteMicrophone(callId, activationId, participantId);
```

`callId` — SID звонка LiveKit, `activationId` — UUID активной активации, `participantId` — identity участника. При ошибке метод выбрасывает `ActionException` с кодом gRPC. Для stage в metadata также передаётся cookie окружения.
