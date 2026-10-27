# plugin-sdk-java

Java библиотека для получения аудио, видео и событий звонков через streamapi

## Подключение

Библиотека собирается для Java 21 и выше, не зависит от Spring и подходит для Gradle и Maven проектов

До настройки общего репозитория пакетов можно собрать локальный Maven репозиторий:

```shell
./gradlew publishSdkPublicationToTestRepository
```

Добавьте `build/test-repository` как Maven репозиторий в проект потребителя и подключите `pluginsdk:plugin-sdk:0.1.0-SNAPSHOT`

Релизный тег вида `v0.1.0` запускает проверки и публикацию в GitHub Packages, затем создает GitHub Release. Для подключения из GitHub Packages укажите Maven репозиторий `https://maven.pkg.github.com/<owner>/plugin-sdk-java` и учетные данные GitHub с правом `read:packages`, затем добавьте зависимость `pluginsdk:plugin-sdk:0.1.0`. Сейчас используется нейтральный groupId `pluginsdk`, для Maven Central потребуется зарегистрированный namespace

Пример для Gradle Kotlin DSL:

```kotlin
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/<owner>/plugin-sdk-java")
        credentials {
            username = System.getenv("GITHUB_USER")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation("pluginsdk:plugin-sdk:0.1.0")
}
```

Для чтения пакета GitHub требует авторизацию даже в публичном репозитории. При локальной разработке подойдет personal access token (classic) с правом `read:packages`

## Подписка

```java
try (var client = PluginClient.builder("calls.vareonix.cc:443").build()) {
    var events = client.subscribeEvents(callId, EventFilter.all(), event ->
        System.out.println(event.type()));

    var media = client.subscribeMedia(callId, roomName, MediaFilter.audio(), packet ->
        System.out.println(packet.track().participantId() + " " + packet.sequenceNumber()));

    // Закройте подписки, когда данные звонка больше не нужны
    media.close();
    events.close();
}
```

Вызовы не блокируют поток до получения данных. `completion()` возвращает `CLOSED` при локальном закрытии и `REMOTE_COMPLETED` при успешном завершении сервером, при ошибке завершается исключением. `close()` отменяет стрим и запланированные повторы, клиент закрывает все свои подписки и канал

SDK передает полный RTP пакет, заголовок и payload доступны через `MediaPacket`. Аудио и видео остаются сжатыми, декодирование выполняет приложение. Буферы доступны только для чтения и сохраняют данные после callback

Описание дорожки приходит перед ее первым полученным пакетом. Список дорожек не является снимком всех дорожек звонка, как и события не содержат текущий список участников

`x-call-id` добавляется автоматически. Для stage передайте metadata через `PluginClient.builder(...).metadata("cookie", value)`, значение cookie нужно получить из конфигурации окружения. Для локального подключения без TLS используйте `plaintext()`

Обработчики одной подписки вызываются последовательно. Обработчики должны быстро возвращать управление: медленный потребитель может достичь серверных лимитов и потерять подписку

SDK повторяет подписку после `UNAVAILABLE` с ограниченным числом попыток и сообщает об обрыве через `onDisconnected`. `onConnected` означает получение заголовков ответа, а не наличие всех дорожек. При переподключении список дорожек сбрасывается; события и RTP за время обрыва не восстанавливаются. Подписки завершаются при остальных ошибках и нормальном окончании стрима

## Пример

Пример подключает SDK из локального Maven репозитория и выводит события и RTP пакеты:

```shell
./gradlew publishSdkPublicationToTestRepository
./gradlew -p examples/consumer run --args='calls.vareonix.cc:443 RM_CALL_ID room_name'
```

Чтобы подписаться только на события, не передавайте имя комнаты. Для stage задайте переменную `PLUGIN_SDK_COOKIE`, для локального сервера без TLS задайте `PLUGIN_SDK_PLAINTEXT=true`

## Проверки

```shell
./gradlew test build
./gradlew publishSdkPublicationToTestRepository
./gradlew -p examples/consumer build
```

Задача `test` включена в CI, тестовые исходники в репозитории отсутствуют

Контракты protobuf находятся в `src/main/proto`, соответствуют API streamapi и дополняются Java настройками генерации
