package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
    @Query("SELECT * FROM students ORDER BY name ASC")
    fun getAllStudents(): Flow<List<StudentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity)

    @Query("DELETE FROM students WHERE uid = :uid")
    suspend fun deleteStudent(uid: String)

    @Query("DELETE FROM students")
    suspend fun clear()
}

@Dao
interface ClassDao {
    @Query("SELECT * FROM classes ORDER BY className ASC")
    fun getAllClasses(): Flow<List<ClassEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClass(classEntity: ClassEntity)

    @Query("DELETE FROM classes WHERE className = :className")
    suspend fun deleteClass(className: String)

    @Query("DELETE FROM classes")
    suspend fun clear()
}

@Dao
interface LogDao {
    @Query("SELECT * FROM logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<LogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: LogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<LogEntity>)

    @Query("DELETE FROM logs WHERE firebaseKey = :key")
    suspend fun deleteLogByFirebaseKey(key: String)

    @Query("DELETE FROM logs")
    suspend fun clearAll()
}
