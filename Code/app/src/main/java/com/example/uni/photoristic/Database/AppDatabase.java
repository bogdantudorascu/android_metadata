package com.example.uni.photoristic.Database;


import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.migration.Migration;

/**
 * Room database used to store image metadata for easier access
 */
@Database(entities = {ImageData.class}, version = 2)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ImageDao imageDao();


    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // we have not made any change so far
        }
    };
}