package vivid.money.elmslie.rx3.switcher

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.rx3.asFlow
import kotlinx.coroutines.rx3.asObservable
import vivid.money.elmslie.core.switcher.Switcher

/**
 * Cancels all scheduled actions after [delayMillis] pass.
 *
 * @param delayMillis Cancellation delay measured with milliseconds.
 */
fun Switcher.cancel(delayMillis: Long = 0): Observable<Any> =
    cancel<Any>(delayMillis = delayMillis).asObservable()

/**
 * Executes [action] and cancels all previous requests scheduled on this [Switcher]
 *
 * @param delayMillis Operation delay in milliseconds. Can be used to debounce requests
 * @param action Operation to be executed
 */
fun <Event : Any> Switcher.observable(
    delayMillis: Long = 0,
    action: () -> Observable<Event>,
): Observable<Event> {
    return switch(delayMillis) { action.invoke().asFlow() }.asObservable()
}

/** Same as [observable], but for [Single]. */
fun <Event : Any> Switcher.single(
    delayMillis: Long = 0,
    action: () -> Single<Event>,
): Single<Event> = observable(delayMillis = delayMillis) { action().toObservable() }.firstOrError()

/** Same as [observable], but for [Maybe]. */
fun <Event : Any> Switcher.maybe(
    delayMillis: Long = 0,
    action: () -> Maybe<Event>,
): Maybe<Event> = observable(delayMillis = delayMillis) { action().toObservable() }.firstElement()

/** Same as [observable], but for [Completable]. */
fun Switcher.completable(
    delayMillis: Long = 0,
    action: () -> Completable,
): Completable = observable(delayMillis = delayMillis) { action().toObservable() }.ignoreElements()
