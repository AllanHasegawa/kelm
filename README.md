# Kelm

Kelm eases the pain when dealing with complex app state and asynchronous tasks.
Kelm is a Kotlin library based on the [Elm Architecture](https://guide.elm-lang.org/architecture) and [RxJava](http://reactivex.io/).

## Introduction

The Elm Architecture has three cleanly separated parts:

- **Model** - the state of the application
- **Update** - a way to update the state
- **View** - a way to view your state

Kelm is a Kotlin-only library, therefore, it doesn't have a **View**.

The **Model** can only be updated in the **Update** function. **Messages** are the events that can change the **Model**.

**Messages** can be UI events, responses from APIs, or the result of computations.

A good strategy when designing the initial version of a **Model** and **Messages** is to write down every event the UI can generate, and everything it can render.

### A simple example

Below is a simple Kelm app. Try to read and guess what it does:

```kotlin
data class Model(val count: Int)

sealed class Msg {
    object Increment : Msg()
    object Decrement : Msg()
}

val msgSubj = PublishSubject.create<Msg>()

Kelm.build<Model, Msg, Nothing>(
    msgObserver = msgSubj,
    initModel = Model(0)
) { model, msg -> 
    when (msg) {
        is Increment -> Model(model.count + 1)
        is Decrement -> Model(model.count - 1)
    }
}.subscribe { model ->
    println("The total count is ${model.count}")
}

msgSubj.onNext(Msg.Increment)
msgSubj.onNext(Msg.Increment)
msgSubj.onNext(Msg.Decrement)
``` 

The above example shows how the main flow of a Kelm app works.
We first declare our **Model** and our **Messages**, we then build the main **Update** function.

The **Update** is a *pure function* that takes the current **Model** and a **Message** and returns a new **Model**.

The return of the `Kelm::build` is of type `Observable<Model>`. The **View** (a Android View, for example) can subscribe to this `Observable` and render it when it changes.

See the [Counter Sample](sample-andorid/src/main/java/kelm/sample/CounterSampleActivity.kt) for a working implementation.

#### Rules for the **Update** function:

1. No side-effects
    1. Side-effects are performed with **Commands** (see below)
1. Synchronous and fast
    1. Any heavy or async work must be executed with a **Command** 

### Commands

**Commands** are asynchronous tasks that finish with *one* **Message**.
This **Message** indicates the result of a task, be it a successful result
or an error.

Here's an example using commands to fetch data from an API:

```kotlin
fun fetchFromApi(): Single<Response> = ...

sealed class Model {
    object Loading : Model()
    data class LoadedContent(val response: Response) : Model()
}

sealed class Msg {
    object FetchClick : Msg()
    data class ContentFetched(val response: Response) : Msg()
}

sealed class Cmd : kelm.Cmd() {
    object FetchFromApi : Cmd()
}

val msgSubj = PublishSubject.create<Msg>()

Kelm.build<Model, Msg, Cmd>(
    msgObserver = msgSubj,
    initModel = Model.Loading,
    cmdToSingle = { cmd ->
        when (cmd) {
            is Cmd.FetchFromApi ->
                  fetchFromApi().map { Msg.ContentFetched(it) }
        }
    },
    update = { model, msg ->
        when (msg) {
            is FetchClick -> Model.Loading.also {
                runCmd(Cmd.FetchFromApi)
            }
            is ContentFetched -> Model.LoadedContent(msg.response)
        }
    }
)
```

To work with **Commands** we first declare their type.
Then we create a `cmdToSingle` function that takes a **Command** and transforms it into a `Single<Msg>`.
This `Single<Msg>` is the action of a **Command** and *it should never emit any error in its error channel*.

The **Update** function has some special implicit functions like the `runCmd` function. `runCmd` adds the **Command** to be executed.

See the [Fox Service Sample](sample-andorid/src/main/java/kelm/sample/FoxServiceSampleActivity.kt) for a working implementation.

### Subscriptions

```
TODO
```

### FAQ

```
TODO
```

### License

```
Copyright (c) 2019 Allan Yoshio Hasegawa

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