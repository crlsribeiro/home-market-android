package app.carlosribeiro.homemarket.data.local

import androidx.room.TypeConverter

class Converters {
    /** Firestore ids never contain commas, so a comma-separated string is enough. */
    @TypeConverter
    fun fromStringList(value: List<String>): String = value.joinToString(SEPARATOR)

    @TypeConverter
    fun toStringList(value: String): List<String> = if (value.isEmpty()) emptyList() else value.split(SEPARATOR)

    private companion object {
        const val SEPARATOR = ","
    }
}
