package app.jammes.plugado.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [SegmentEntity::class, DeviceEntity::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun segments(): SegmentDao
    abstract fun devices(): DeviceDao
}
