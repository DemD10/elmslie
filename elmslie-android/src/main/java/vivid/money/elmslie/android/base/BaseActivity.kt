package vivid.money.elmslie.android.base

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import vivid.money.elmslie.android.screen.MviDelegate
import vivid.money.elmslie.android.screen.ScreenMvi
import vivid.money.elmslie.android.util.fastLazy
import vivid.money.elmslie.core.store.Store as ElmStore

abstract class ElmslieActivity<Event : Any, Effect : Any, State : Any, Store : ElmStore<Event, Effect, State>> :
    AppCompatActivity(), MviDelegate<Event, Effect, State, Store> {

    @Suppress("LeakingThis")
    private val mvi = ScreenMvi(this) { this }

    override val screenLifecycle: Lifecycle
        get() = lifecycle

    protected val store by fastLazy { mvi.store }
}