# Keyple Kotlin Multiplatform Distributed Client Library

## Overview

The **Keyple Interop Distributed JSON Client Library** is a Kotlin Multiplatform implementation enabling distributed remote
client communications across Android, iOS and desktop platforms. This library provides a distributed architecture layer
for remote terminals, making it easier to develop cross-platform applications connecting to a Keyple server.

> **This library is client-only.** It does not contain any Calypso business logic (which application to select,
> which APDU commands to send, how to interpret the result). All of that logic must be implemented on a **Keyple
> server** that you develop and deploy yourself — this client only relays the server's instructions to the card and
> sends back the responses. See [How It Works](#how-it-works) below.

## How It Works

The transaction is **driven by the server**, not by the client: once you call `executeRemoteService(...)`, the
library repeatedly asks the server "what do I do next?" and executes whatever it is told (select an application,
send APDU commands) until the server signals that the transaction is complete. Your mobile/desktop app never
decides *what* to read or write on the card — it only executes what your Keyple server asks for a given
`serviceId` (e.g. `"MY_SERVICE_NAME"`). Defining that behaviour server-side (with the Keyple server SDK and the
relevant Calypso Card Extension) is a separate development effort from integrating this client library.

For the full sequence diagram and a step-by-step breakdown of this workflow, please see the
[Non-Keyple Client User Guide — Workflow overview](https://keyple.org/user-guides/non-keyple-client/content/#workflow-overview).

## Documentation & Contribution Guide
Full documentation available at [keyple.org](https://keyple.org)

## Supported Platforms
- Android 7.0+ (API 24+)
- iOS
- JVM 11+

## Build
The code is built with **Gradle** and targets **Android**, **iOS**, and **JVM** platforms.
To build and publish the artifacts for all supported targets locally, use:
```
./gradlew publishToMavenLocal
```
Note: you need to use a mac to build or use iOS artifacts. Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…


## API Documentation
API documentation & class diagrams are available
at [docs.keyple.org/keyple-interop-jsonapi-client-kmp-lib](https://docs.keyple.org/keyple-interop-jsonapi-client-kmp-lib/)

You will need to provide two implementations to use this library: a network client, and a Local NFC reader.

* The network client is used to communicate with the Keyple server. Usually, it will be an HTTP client configured by you with basic or advanced authentication, allowing to communicate with your Keyple server.
  See the [demo app](https://github.com/calypsonet/keyple-demo-ticketing-reloading-remote/blob/main/client/kmp/composeApp/src/commonMain/kotlin/org/calypsonet/keyple/demo/reload/remote/network/SimpleHttpNetworkClient.kt) for a simple example, using Ktor with HTTP basic-auth.

* The Local NFC reader provides the actual NFC communication depending on the platform.
  For most use cases, you can just use the provided [mobile NFC Reader lib](https://github.com/eclipse-keyple/keyple-interop-localreader-nfcmobile-kmp-lib) that supports Android, iOS and JVM desktop (using PC/SC NFC readers) out of the box.

Create a KeypleTerminal object and wait for a NFC card to be presented:
```kotlin
val keypleTerminal = KeypleTerminal(networkClient, nfcReader, "MY_CLIENT_ID")
// Wait for a card to be presented (in iOS, this will trigger the system mandatory NFC popup)

// Using the sync API (suspending):
val cardFound = keypleService.waitForCard() 
if (cardFound) readContracts()

// or using the Async API (callback)
keypleTerminal.waitForCard {
    // Card found...
    viewModelScope.launch(Dispatchers.IO) { readContracts() }
}
```

When we have a card on the NFC interface, we connect it to the Keyple server: 
```kotlin
fun readContracts() {
    when (keypleTerminal.executeRemoteService("MY_SERVICE_NAME", inputData, inputSerializer, outputSerializer)) {
        is KeypleResult.Failure -> {
            // Handle error, message is in result.error
        }
        is KeypleResult.Success -> {
            // Keyple transaction completed, result is in result.data. Check for applicative status and payload
        }
    }
}
```

`"MY_SERVICE_NAME"` above must match a service implemented on your Keyple server — this library has no built-in
services of its own.

### Error Handling

`executeRemoteService(...)` returns a `KeypleResult<T>`, either:
- `KeypleResult.Success<T>`: the transaction completed and `data` holds the (optionally typed) server output.
- `KeypleResult.Failure<T>`: the transaction failed. `status` gives a machine-readable `Status` (e.g. `TAG_LOST`,
  `NETWORK_ERROR`, `READER_ERROR`), `message` a human-readable description, and `data` might still carry a partial
  server payload if one was attached before the failure.

### Card Selection Scenario

It is strongly recommended that you implement a Card Selection Scenario strategy. See the [Selection JSON Specification here](https://keyple.org/user-guides/non-keyple-client/selection-json-specification/) to learn more.
Retrieve the JSON Selection strategy from your server, and pass it to the KeypleTerminal (prior to waiting for a card):
```kotlin
keypleTerminal.setCardSelectionScenarioJsonString(scenarioJsonString)
```

Without a selection scenario, the client performs no application selection on its own before contacting the
server: the server will have to drive the selection step itself (via `TRANSMIT_CARD_SELECTION_REQUESTS`), adding a
round-trip to the server before the transaction can start. Providing a scenario lets the client select the
application locally as soon as a card is presented, and send the result to the server as `initialCardContent`.

## Glossary

This library and its documentation assume familiarity with smart card terminology defined by the
[ISO/IEC 7816](https://en.wikipedia.org/wiki/ISO/IEC_7816) family of standards. Quick reference:

| Term | Meaning |
|---|---|
| **AID** | Application Identifier — identifies a specific application on the card (e.g. a Calypso application) to select. |
| **APDU** | Application Protocol Data Unit — the basic command/response unit exchanged with a smart card. |
| **ATR / power-on data** | Data returned by the card (or, for contactless, provided by the reader) when it is first detected. |
| **Physical channel** | The low-level connection between the reader and the card (opened/closed independently of any application selection). |
| **Logical channel** | The channel opened to a specific application on the card after a successful `SELECT` (AID). |
| **Status word (SW)** | The 2-byte status code (e.g. `9000`) returned at the end of every APDU response. |
