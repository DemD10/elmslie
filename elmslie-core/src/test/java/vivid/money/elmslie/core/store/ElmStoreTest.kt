package vivid.money.elmslie.core.store

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import vivid.money.elmslie.core.testutil.model.Command
import vivid.money.elmslie.core.testutil.model.Effect
import vivid.money.elmslie.core.testutil.model.Event
import vivid.money.elmslie.core.testutil.model.State
import vivid.money.elmslie.test.background.executor.TestDispatcherExtension

@OptIn(ExperimentalCoroutinesApi::class)
class ElmStoreTest {

    @JvmField @RegisterExtension val testDispatcherExtension = TestDispatcherExtension()

    @Test
    fun `Should stop the store properly`() = runTest {
        val store = store(State())

        store.start()
        store.accept(Event())
        store.stop()
        advanceUntilIdle()

        assert(!store.isStarted)
    }

    @Test
    fun `Should stop getting state updates when the store is stopped`() = runTest {
        val store =
            store(
                    state = State(),
                    reducer = { _, state ->
                        Result(state = state.copy(value = state.value + 1), command = Command())
                    },
                    actor = { flow { emit(Event()) }.onEach { delay(1000) } }
                )
                .start()

        val emittedStates = mutableListOf<State>()
        val collectJob = launch { store.states().toList(emittedStates) }
        store.accept(Event())
        runCurrent()
        delay(3500)
        store.stop()

        assertEquals(
            mutableListOf(
                State(0), // Initial state
                State(1), // State after receiving trigger Event
                State(2), // State after executing the first command
                State(3), // State after executing the second command
                State(4) // State after executing the third command
            ),
            emittedStates
        )
        collectJob.cancel()
    }

    @Test
    fun `Should update state when event is received`() = runTest {
        val store =
            store(
                    state = State(),
                    reducer = { event, state -> Result(state = state.copy(value = event.value)) },
                )
                .start()

        assertEquals(
            State(0),
            store.currentState,
        )
        store.accept(Event(value = 10))
        advanceUntilIdle()

        assertEquals(State(10), store.currentState)
    }

    @Test
    fun `Should not update state when it's equal to previous one`() = runTest {
        val store =
            store(
                    state = State(),
                    reducer = { event, state -> Result(state = state.copy(value = event.value)) },
                )
                .start()

        val emittedStates = mutableListOf<State>()
        val collectJob = launch { store.states().toList(emittedStates) }

        store.accept(Event(value = 0))
        advanceUntilIdle()

        assertEquals(
            mutableListOf(
                State(0) // Initial state
            ),
            emittedStates
        )
        collectJob.cancel()
    }

    @Test
    fun `Should collect all emitted effects`() = runTest {
        val store =
            store(
                    state = State(),
                    reducer = { event, state ->
                        Result(state = state, effect = Effect(value = event.value))
                    }
                )
                .start()

        val effects = mutableListOf<Effect>()
        val collectJob = launch { store.effects().toList(effects) }
        store.accept(Event(value = 1))
        store.accept(Event(value = -1))
        advanceUntilIdle()

        assertEquals(
            mutableListOf(
                Effect(value = 1), // The first effect
                Effect(value = -1), // The second effect
            ),
            effects
        )
        collectJob.cancel()
    }

    @Test
    fun `Should skip the effect which is emitted before subscribing to effects`() = runTest {
        val store =
            store(
                    state = State(),
                    reducer = { event, state ->
                        Result(state = state, effect = Effect(value = event.value))
                    }
                )
                .start()

        val effects = mutableListOf<Effect>()
        store.accept(Event(value = 1))
        runCurrent()
        val collectJob = launch { store.effects().toList(effects) }
        store.accept(Event(value = -1))
        runCurrent()

        assertEquals(
            mutableListOf(
                Effect(value = -1),
            ),
            effects
        )
        collectJob.cancel()
    }

    @Test
    fun `Should collect all effects emitted once per time`() = runTest {
        val store =
            store(
                    state = State(),
                    reducer = { event, state ->
                        Result(
                            state = state,
                            commands = emptyList(),
                            effects =
                                listOf(
                                    Effect(value = event.value),
                                    Effect(value = event.value),
                                ),
                        )
                    }
                )
                .start()

        val effects = mutableListOf<Effect>()
        val collectJob = launch { store.effects().toList(effects) }
        store.accept(Event(value = 1))
        advanceUntilIdle()

        assertEquals(
            mutableListOf(
                Effect(value = 1), // The first effect
                Effect(value = 1), // The second effect
            ),
            effects
        )
        collectJob.cancel()
    }

    @Test
    fun `Should collect all emitted effects by all collectors`() = runTest {
        val store =
            store(
                    state = State(),
                    reducer = { event, state ->
                        Result(state = state, effect = Effect(value = event.value))
                    }
                )
                .start()

        val effects1 = mutableListOf<Effect>()
        val effects2 = mutableListOf<Effect>()
        val collectJob1 = launch { store.effects().toList(effects1) }
        val collectJob2 = launch { store.effects().toList(effects2) }
        store.accept(Event(value = 1))
        store.accept(Event(value = -1))
        advanceUntilIdle()

        assertEquals(
            mutableListOf(
                Effect(value = 1), // The first effect
                Effect(value = -1), // The second effect
            ),
            effects1
        )
        assertEquals(
            mutableListOf(
                Effect(value = 1), // The first effect
                Effect(value = -1), // The second effect
            ),
            effects2
        )
        collectJob1.cancel()
        collectJob2.cancel()
    }

    @Test
    fun `Should collect duplicated effects`() = runTest {
        val store =
            store(
                    state = State(),
                    reducer = { event, state ->
                        Result(state = state, effect = Effect(value = event.value))
                    }
                )
                .start()

        val effects = mutableListOf<Effect>()
        val collectJob = launch { store.effects().toList(effects) }
        store.accept(Event(value = 1))
        store.accept(Event(value = 1))
        advanceUntilIdle()

        assertEquals(
            mutableListOf(
                Effect(value = 1),
                Effect(value = 1),
            ),
            effects
        )
        collectJob.cancel()
    }

    @Test
    fun `Should collect event caused by actor`() = runTest {
        val store =
            store(
                    state = State(),
                    reducer = { event, state ->
                        Result(
                            state = state.copy(value = event.value),
                            command = Command(event.value - 1).takeIf { event.value > 0 }
                        )
                    },
                    actor = { command -> flowOf(Event(command.value)) },
                )
                .start()

        val states = mutableListOf<State>()
        val collectJob = launch { store.states().toList(states) }

        store.accept(Event(3))
        advanceUntilIdle()

        assertEquals(
            mutableListOf(
                State(0), // Initial state
                State(3), // State after receiving Event with command number
                State(2), // State after executing the first command
                State(1), // State after executing the second command
                State(0) // State after executing the third command
            ),
            states
        )

        collectJob.cancel()
    }

    private fun store(
        state: State,
        reducer: StateReducer<Event, State, Effect, Command> = NoOpReducer(),
        actor: DefaultActor<Command, Event> = NoOpActor()
    ) = ElmStore(state, reducer, actor)
}
