package vivid.money.elmslie.rx3

import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.rx3.asFlow
import kotlinx.coroutines.rx3.asObservable
import vivid.money.elmslie.core.store.Actor
import vivid.money.elmslie.core.store.ElmStore
import vivid.money.elmslie.core.store.StateReducer
import vivid.money.elmslie.core.store.Store

/** A [Store] implementation that uses RxJava3 for multithreading */
class RxElmStore<Event : Any, State : Any, Effect : Any, Command : Any>(
    initialState: State,
    reducer: StateReducer<Event, State, Effect, Command>,
    actor: RxActor<Command, out Event>,
    startEvent: Event? = null,
) :
    Store<Event, Effect, State> by ElmStore(
        initialState = initialState,
        reducer = reducer,
        actor = actor.toActor(),
        startEvent = startEvent,
    )

private fun <Command : Any, Event : Any> RxActor<Command, Event>.toActor() =
    object : Actor<Command, Event>() {
        override fun execute(command: Command): Flow<Event> {
            return this@toActor.execute(command).asFlow()
        }
    }

/** An extension for accessing [Store] states as an [Observable] */
val <Event : Any, Effect : Any, State : Any> Store<Event, Effect, State>.states: Observable<State>
    get() = states().asObservable()

/** An extension for accessing [Store] effects as an [Observable] */
val <Event : Any, Effect : Any, State : Any> Store<Event, Effect, State>.effects: Observable<Effect>
    get() = effects().asObservable()
