package app.jammes.plugado.audio

import app.jammes.domain.model.AudioSnapshot
import app.jammes.domain.model.Output
import app.jammes.plugado.util.ticker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformLatest
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

data class AggregatorConfig(
    /** Os callbacks chegam em rajadas: espera esse silêncio antes de ler. */
    val debounce: Duration = 400.milliseconds,
    /**
     * Releitura depois de cada rajada. O `isMusicActive` cai até ~0,7 s depois do pause e
     * nenhum callback avisa. Vale pra qualquer gatilho, não só playback: ler de novo é barato.
     */
    val settleDelay: Duration = 1.5.seconds,
    /** Enquanto alguém ouve, relê nesse intervalo. Rede de segurança pra virada sem callback. */
    val pollInterval: Duration = 5.seconds,
)

/**
 * Junta os gatilhos das fontes e devolve snapshots. Lê o estado depois de cada rajada de
 * gatilhos, de novo [AggregatorConfig.settleDelay] depois, e em polling enquanto alguém ouve.
 * Só emite quando algo mudou além do horário.
 */
class AudioStateAggregator(
    private val triggers: Flow<Unit>,                       // as fontes juntas (merge)
    private val reader: AudioStateReader,
    private val notHeadphones: Flow<Set<Output>> = flowOf(emptySet()),
    private val config: AggregatorConfig = AggregatorConfig(),
    private val now: () -> Instant = Instant::now,
) {

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val snapshots: Flow<AudioSnapshot> = flow {
        // Sem ninguém ouvindo não precisa de polling: a volta do play sempre chega com callback.
        val listening = MutableStateFlow(false)

        val afterTriggers = triggers
            .onStart { emit(Unit) }                         // o app pode abrir com música tocando
            .debounce(config.debounce)
            .transformLatest {
                emit(Unit)
                delay(config.settleDelay)
                emit(Unit)
            }
        val polls = listening.flatMapLatest { if (it) ticker(config.pollInterval) else emptyFlow() }

        // A lista de "não é fone" mudando também relê, na hora.
        val snapshots = combine(merge(afterTriggers, polls), notHeadphones) { _, excluded ->
            reader.read(now(), excluded)
        }
        emitAll(
            snapshots
                .distinctUntilChanged { old, new -> old.copy(at = new.at) == new }
                .onEach { listening.value = it.isListening },
        )
    }
}
