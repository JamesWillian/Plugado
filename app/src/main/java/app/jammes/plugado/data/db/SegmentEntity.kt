package app.jammes.plugado.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import app.jammes.domain.model.AudioKind
import app.jammes.domain.model.OutputType
import app.jammes.domain.model.Segment
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "segments", indices = [Index("day"), Index("sessionId")])
data class SegmentEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val day: LocalDate,
    val startedAt: Instant,
    val endedAt: Instant?,      // null = ainda aberto (ou o processo morreu no meio)
    val lastSeenAt: Instant,    // último heartbeat: vira o fim se o processo morrer
    val kind: AudioKind,
    val outputType: OutputType,
    val outputName: String,
    val volumeIndex: Int,
    val volumeMax: Int,
    val volumeDb: Float?,       // null quando o dB não pôde ser lido (o SQLite não guarda NaN)
)

fun Segment.toEntity() = SegmentEntity(
    id = id,
    sessionId = sessionId,
    day = day,
    startedAt = startedAt,
    endedAt = null,
    lastSeenAt = startedAt,
    kind = kind,
    outputType = output.type,
    outputName = output.name,
    volumeIndex = volume.index,
    volumeMax = volume.max,
    volumeDb = volume.db.takeUnless { it.isNaN() },
)
