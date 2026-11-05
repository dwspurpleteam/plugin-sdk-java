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

События и действия отправляются через существующий `PluginClient`:

```java
var client = PluginClient.builder("calls.vareonix.cc:443")
    .metadata("authorization", "Bearer " + token)
    .build();
client.sendEvent(callId, activationId, "subtitle.updated", List.of(participantId), "hello");
client.muteMicrophone(callId, activationId, participantId);
// Или через тот же универсальный метод:
client.sendEvent(callId, activationId, "microphone.mute", List.of(participantId), "");
client.removeParticipant(callId, activationId, participantId);
client.updateParticipantPermissions(callId, activationId, participantId, Map.of("canPublish", false));
```

`callId` — SID звонка LiveKit, `activationId` — UUID активной активации, получатели — identity участников. `data` — строка с произвольным содержимым, например JSON. Список получателей не должен быть пустым. Команды `microphone.mute`, `participant.remove` и `participant.permissions.update` применяются к одному участнику; остальные имена отправляются участникам через LiveKit. Разрешения: `canPublish`, `canSubscribe`, `canPublishData`, `canUpdateMetadata`. Непереданные разрешения сохраняются. Удаление не запрещает повторное подключение с действующим токеном.

При ошибке метод выбрасывает `ActionException` с кодом gRPC. Успешный вызов подтверждает выполнение команды или передачу события в LiveKit, но не обработку в интерфейсе. Для stage в metadata также передаётся cookie окружения.
