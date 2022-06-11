# Kelm

Kelm simplifies management of complex app states and asynchronous tasks by enforcing a pattern
based on the [Elm Architecture](https://guide.elm-lang.org/architecture).

Kelm is a Kotlin-only library using Kotlin Coroutines.

Read the motivation behind Kelm in [this blog post](https://medium.com/android-frontier/kelm-kotlin-ui-architecture-ea91fb745478).

## Kelm 2 status

Kelm 1 is currently the stable version for Kelm and uses RxJava.

Kelm 2 is currently in Alpha version, which means the API can change in the future.
Kelm 2 differs from Kelm 1 as it uses Kotlin Coroutines and a simplified API.

## Download

[![Maven Central Badge](https://img.shields.io/maven-central/v/io.github.allanhasegawa.kelm/kelm-core?style=for-the-badge)](https://search.maven.org/artifact/io.github.allanhasegawa.kelm/kelm-core)

Find the latest JAR here: https://repo1.maven.org/maven2/io/github/allanhasegawa/kelm/

Or at the following coordinates from Maven Central:

```
io.github.allanhasegawa.kelm:kelm-core:{version}
```

**(Optional)** For extra extensions and classes specific to Android, import this instead:

```
io.github.allanhasegawa.kelm:kelm-android:{version}
```

## Introduction

Kelm is an implementation that enforces a pattern for managing complex app states in an asynchronous world.

The core principles of Kelm are broken down in two parts:

- **Model** - the state of the application
- **Update** - a way to update the state through **Messages**

The **Model** is immutable and is derived in the **Update** function.
**Messages** are the events that can change the **Model**.

**Messages** can be UI events, responses from APIs, or the result of computations.

This differs slightly from the Elm Architecture because Kelm doesn't have a **View**. The **View**
is up to the application consuming this library. This gives Kelm an advantage as it's not limited to
the UI layer. It can be used to manage state in the domain layer, or even in the data layer if needed.

### A simple example

Below is a simple Kelm app with two buttons that can increment/decrement a counter:

```kotlin
data class Model(val count: Int) {
   val minusButtonEnabled = count > 0
}

sealed class Msg {
   object PlusClick : Msg()
   object MinusClick : Msg()
}

// Sandbox is part of Kelm and requires the implementation of the `updateSimple` function
object CounterElement : Sandbox<Model, Msg>() {
   fun initModel() = Model(count = 0)

   override fun updateSimple(model: Model, msg: Msg): Model? =
      when (msg) {
         is Msg.PlusClick -> model.copy(count = model.count + 1)
         is Msg.MinusClick -> model.copy(count = model.count - 1)
      }
}

// `Sandbox::runUpdateManually` takes a model and a sequence of messages and returns a
// sequence of `UpdateStep`, a data class containing the new model and more.
CounterElement.runUpdateManually(
   model = CounterElement.initModel(),
   null, Msg.PlusClick, Msg.PlusClick, Msg.MinusClick, // vararg of Msg
).map { it.modelNonNull }.forEach {
   println("The total count is ${it.count}; minus button is ${it.minusButtonEnabled}.")
}
``` 

Running it prints:

```
The total count is 0; minus button is false.
The total count is 1; minus button is true.
The total count is 2; minus button is true.
The total count is 1; minus button is true.
```

The above example shows how the main flow of a Kelm app works.
The **Model** is our state, in this case, a simple counter. The **Msg** are the events that can
change our state.

`Sandbox` is the simplest way to start using Kelm as it has only support for synchronous operations.
To use it all we need is to implement the `Sandbox::updateSimple` function that takes a model and a
message and returns a new model.

Although the sample above works, running the update function manually is not useful nn a real app.
Instead, Coroutines are used to observe the changes to the model as messages are processed:

```kotlin
val msgChannel = Channel<Msg>(capacity = 4)

runBlocking {
    listOf(Msg.PlusClick, Msg.PlusClick, Msg.MinusClick).forEach(msgChannel::trySend)

    CounterElement.buildModelFlow(
        initModel = CounterElement.initModel(),
        msgInput = msgChannel,
    ).take(4).collect {
        println("The total count is ${it.count}; minus button is ${it.minusButtonEnabled}.")
    }
}
```

The code snippet above will output the same result as the one prior. The `Sandbox::buildModelFlow`
function takes an initial model and a `Channel<Msg>` as input and returns a `Flow<Model>`.
For each message sent through the channel, a new `Model` is returned. The app then can react to
the new model to create a new **View**.

See the [Counter Sample](sample-android/src/main/java/kelm/sample/simpleSample).

#### Rules for the **Update** function:

1. No side-effects
   1. Side-effects are performed with **Commands** or **Subscriptions** (see below)
1. Synchronous and fast
   1. Any heavy or async work must be executed with a **Command** 

### Commands (side-effects and asynchronous tasks)

**Commands** are asynchronous tasks that finish with *at most one* **Message**.
This **Message** indicates the result of a task, be it a successful result
or an error.

* All side-effects and expensive computations must be done with **Commands**.

To work with **Commands** implement a `Element` instead of a `Sandbox` and
return commands along with your model in the `Element::update` function. *The update function
still a pure function with no side-effects. The commands we are returning here are just data and
aren't being executed yet.*

The execution of commands are done outside of the update function, and for that create a suspend
function that takes a **Command** and transforms it into a **Message**,
or `<MsgT, CmdT> suspend (CmdT) -> MsgT?` and pass it to the `Element::buildModelFlow` function.

See the [Fox Service Sample](sample-android/src/main/java/kelm/sample/commandSample).

### Subscriptions

**Subscriptions** allows for a continuous stream of messages. Some use cases are:
listening to a web socket for new data in a chat application, or setting up interval timers.

Although subscriptions can also be used for side-effects and asynchronous tasks, it differs from
commands. To start, each subscription must have a unique ID. The reason is that a subscription
has a lifetime. Apps may chose to launch and cancel subscriptions as the model changes.
The other difference is that subscriptions are not created in the update function. There's a
dedicated `Element::subscriptions` function that converts a model into a list of subscriptions, or
`<ModelT, SubT> (ModelT) -> List<SubT>`.

See the [Clock Sample](sample-android/src/main/java/kelm/sample/subscriptionSample).

### Testing

A strong advantage for Kelm is that testing is trivial because the `Element::update` function is
a pure function that receives data and returns data. There are no side-effects or asynchronous calls.

When testing an application using Kelm, the `Element` can be heavily tested for all possible scenarios
as those tests are quite cheap. See the [CounterElementTest](sample-android/src/test/java/kelm/sample/CounterElementTest.kt)

Anything outside of an `Element` is tested as you would do normally.

### License

```
Copyright (c) 2022 Allan Yoshio Hasegawa

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
