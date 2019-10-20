# Kelm

Kelm simplifies management of complex app states and asynchronous tasks.

Kelm is a Kotlin library based on the [Elm Architecture](https://guide.elm-lang.org/architecture) and [RxJava](http://reactivex.io/).

## Introduction

The Elm Architecture has three cleanly separated parts:

- **Model** - the state of the application
- **Update** - a way to update the state
- **View** - a way to view your state

Kelm is a Kotlin-only library, therefore, it doesn't have a **View**.

The **Model** can only be updated in the **Update** function. **Messages** are the events that can change the **Model**.

**Messages** can be UI events, responses from APIs, or the result of computations.

A good strategy when designing the initial version of a **Model** and **Messages** is to write down every event the UI can generate and everything it can render.

### A simple example

Below is a simple Kelm app. Try to read and guess what it does:

```kotlin
object CounterElement : Kelm.Sandbox<Model, Msg>() {
    sealed class Msg {
        object MinusClick : Msg()
        object PlusClick : Msg()
        object ResetClick : Msg()
    }

    data class Model(val count: Int) {
        val resetBtEnabled = count > 0
        val minusBtEnabled = resetBtEnabled
    }

    fun initModel() = Model(count = 0)

    override fun updateSimple(model: Model, msg: Msg): Model? =
        when (msg) {
            is Msg.MinusClick -> model.copy(count = model.count - 1)
            is Msg.PlusClick -> model.copy(count = model.count + 1)
            is Msg.ResetClick -> model.copy(count = 0)
        }
}

val msgSubj = PublishSubject.create<Msg>()

CounterElement.start(
    initModel = CounterElement.initModel(),
    msgInput = msgSubj
).subscribe { model ->
    println("The total count is ${model.count}")
}

msgSubj.onNext(Msg.Increment)
msgSubj.onNext(Msg.Increment)
msgSubj.onNext(Msg.Decrement)
``` 

The above example shows how the main flow of a Kelm app works.
We first declare an **Sandbox** object†, the contract for our **Model** and our **Messages**,
and how they interact with the **update** function. 

† Your **Sandbox** implementation should be an **object** with no properties.

The **Update** is a *pure function* that takes the current **Model** and a **Message** and returns a new **Model**.

The return of the `Element::start` is of type `Observable<Model>`.
The **View** (an Android View, for example) can subscribe to this `Observable` and render it when it changes.

See the [Counter Sample](sample-android/src/main/java/kelm/sample/CounterSampleActivity.kt) for a working implementation.

#### Rules for the **Update** function:

1. No side-effects
    1. Side-effects are performed with **Commands** (see below)
1. Synchronous and fast
    1. Any heavy or async work must be executed with a **Command** 

### Commands

**Commands** are asynchronous tasks that finish with *at most one* **Message**.
This **Message** indicates the result of a task, be it a successful result
or an error.

* All side-effects and expensive computations must be done with **Commands**.


To work with **Commands** implement an ``Kelm::Element`` instead of a ``Kelm::Sandbox``.

Then create a `cmdToMaybe` function that takes a **Command** and transforms it into a `Maybe<Msg>`.
This `Maybe<Msg>` is the action of a **Command** and *it should never emit any error in its error channel*.

The **Update** function has some special implicit functions like the `runCmd` function. `runCmd` adds the **Command** to be executed.

See the [Fox Service Sample](sample-android/src/main/java/kelm/sample/FoxServiceSampleActivity.kt).

### Subscriptions

```
TODO
```

See the [Clock Sample](sample-android/src/main/java/kelm/sample/ClockSampleActivity.kt).

### Dealing with complex projects

See the [Advanced Sample](sample-android/src/main/java/kelm/sample/signUp).

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