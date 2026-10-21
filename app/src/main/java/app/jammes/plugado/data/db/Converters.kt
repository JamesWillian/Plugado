package app.jammes.plugado.data.db

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate

/** Instantes viram milissegundos (dá pra subtrair no SQL) e dias viram texto ("2026-10-20"). */
class Converters {
    @TypeConverter
    fun instantToMillis(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun millisToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun dayToText(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun textToDay(value: String?): LocalDate? = value?.let(LocalDate::parse)
}
