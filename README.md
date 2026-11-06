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

События и действия отправляются через `PluginClient`:

```java
var client = PluginClient.builder("streamapi:19090")
    .plaintext()
    .build();
client.sendEvent(callId, activationId, "subtitle.updated", List.of(participantId), "hello");
client.muteMicrophone(callId, activationId, participantId);
// Или через тот же универсальный метод:
client.sendEvent(callId, activationId, "microphone.mute", List.of(participantId), "");
client.removeParticipant(callId, activationId, participantId);
client.updateParticipantPermissions(callId, activationId, participantId, Map.of("canPublish", false));
```

В `callId` передаётся SID звонка LiveKit, в `activationId` — UUID активации. Проверка активации пока не реализована. Получателей указывают по identity участников; список не должен быть пустым. Поле `data` содержит строку, например JSON.

Команды `microphone.mute`, `participant.remove` и `participant.permissions.update` применяются к одному участнику. События с другими именами отправляются участникам через LiveKit. При изменении прав можно передать `canPublish`, `canSubscribe`, `canPublishData`, `canUpdateMetadata`; остальные права сохранятся. Удалённый участник может подключиться снова с действующим токеном.

При ошибке метод выбрасывает `ActionException` с кодом gRPC. Успешный вызов подтверждает выполнение команды или отправку события в LiveKit, но не его обработку в интерфейсе. Для stage во внутренней сети используется адрес `streamapi-stage:19090`.
