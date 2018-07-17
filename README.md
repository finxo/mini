# Mini
Mini is a minimal Kotlin and Flux architecture with a mix of useful features.

###Introduction
TODO
## How to Use
#### Actions
Actions are helpers that pass data to the Dispatcher. They represent use cases of our application and are the start point of any change made during the application lifetime.

```kotlin
data class LoginAction(val username: String,
                       val password: String) : Action
```

#### Dispatcher
The dispatcher receives these Actions and broadcast payloads to registered callbacks. The instance of the Dispatcher must be unique across the whole application and it will execute all the logic in the main thread making all the code synchronous.   

```kotlin
//Dispatch the action in the current Thread
dispatcher.dispatch(LoginAction(username = "user", password = "123"))

//Dispatch the action in the UI Thread
dispatcher.dispatchOnUi(LoginAction(username = "user", password = "123"))

//Post and event that will dispatch the action on the Ui thread 
//and block until the dispatch is complete.
dispatcher.dispatchOnUiSync(LoginAction(username = "user", password = "123"))
```

#### Store
The Stores act as containers for application state & logic. The real work in the application is done in the Stores. The Stores registered to listen in on the actions of the Dispatcher will do accordingly and update the Views.

The Stores represent the application though States. An State is a group of information that represent all the application needed information for the logic that this store is taking care of.

Finally, Stores are subscribed to different actions expecting to change the application state when this action is dispatched. Mini manages this using the `@Reducer` annotation. Which should be used together with a **non-private function that receives an Action as parameter**. It can also receive a state for unitary testing purposes. 
```kotlin
data class SessionState(val loginTask : Task = taskIdle(),
                        val loggedUsername : String? = null)
                        

class SessionStore : Store<SessionState>() {

    @Reducer
    fun login(action: LoginAction): SessionState {
        return state.copy(loginTask = taskRunning())
    }

    @Reducer
    fun loginComplete(action: LoginCompleteAction, state: SessionState): WarcraftState {
        return state.copy(loginTask = taskSuccess(), loggedUsername = action.name)
    }
}
```

####View changes
TODO
####Select and combine
TODO
####Tasks
TODO
####Navigation
TODO
####UI Testing with Mini
TODO
####Logging
TODO

## Setting Up

####Import the library
To setup Mini in your application, first you will need to add the library itself together with the annotation processor:
```groovy
implementation 'com.github.pabloogc:Mini:1.0.5'
annotationProcessor 'com.github.pabloogc.Mini:mini-processor:1.0.5'
```

####Setting up your App file
After setting it up on your gradle application. You will need to initialize a `Dispatcher` unique instance in your application together with a list of `Stores`. To achieve this you can use your favorite dependency injection framework.

With your Dispatcher and Stores ready, you will need to add the `MiniActionReducer` which is auto-generated depending of your `Stores` and `Reducers`. 

Finally, if you want to add *action-state* changes logging to your application you can add the `LoggerInterceptor` provided by the library or create your own one.  
```kotlin
val actionReducer = MiniActionReducer(stores = stores())
val loggerInterceptor = CustomLoggerInterceptor(stores().values)

dispatcher.addActionReducer(actionReducer)
dispatcher.addInterceptor(loggerInterceptor) //Optional

FluxUtil.initStores(stores())
```