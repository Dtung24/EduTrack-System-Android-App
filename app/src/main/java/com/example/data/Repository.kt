package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Repository(
    context: Context,
    private val database: AppDatabase
) {
    private val studentDao = database.studentDao()
    private val classDao = database.classDao()
    private val logDao = database.logDao()
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        FirebaseHelper.initialize(context)
        startFirebaseSynchronization()
    }

    // Flows for visual observation in ViewModel
    val students: Flow<List<StudentEntity>> = studentDao.getAllStudents()
    val classes: Flow<List<ClassEntity>> = classDao.getAllClasses()
    val logs: Flow<List<LogEntity>> = logDao.getAllLogs()

    // ── FIREBASE LISTENERS SYNCING DIRECTLY TO ROOM SQLite ──
    private fun startFirebaseSynchronization() {
        try {
            val dbRef = FirebaseHelper.database

            // 1. Sync Students
            dbRef.getReference("students").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    scope.launch {
                        try {
                            studentDao.clear()
                            for (child in snapshot.children) {
                                val uid = child.key ?: continue
                                val name = child.child("name").value as? String ?: "Unregistered Student"
                                val studentId = child.child("id").value?.toString() ?: "---------"
                                val className = child.child("class").value as? String
                                val photo = child.child("photo").value as? String
                                
                                studentDao.insertStudent(
                                    StudentEntity(uid = uid, name = name, studentId = studentId, className = className, photo = photo)
                                )
                            }
                        } catch (e: Exception) {
                            Log.e("Repository", "Sync students error: ${e.message}")
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("Repository", "Sync students cancelled: ${error.message}")
                }
            })

            // 2. Sync Classes
            dbRef.getReference("classes").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    scope.launch {
                        try {
                            classDao.clear()
                            for (child in snapshot.children) {
                                val className = child.key ?: continue
                                classDao.insertClass(ClassEntity(className = className))
                            }
                        } catch (e: Exception) {
                            Log.e("Repository", "Sync classes error: ${e.message}")
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("Repository", "Sync classes cancelled: ${error.message}")
                }
            })

            // 3. Sync Attendance Logs
            dbRef.getReference("attendance_logs").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    scope.launch {
                        try {
                            logDao.clearAll()
                            val list = mutableListOf<LogEntity>()
                            for (child in snapshot.children) {
                                val key = child.key
                                val uid = child.child("ma_the").value?.toString() 
                                    ?: child.child("uid").value?.toString() 
                                    ?: "Unknown"
                                val deviceId = child.child("phong_hoc").value?.toString()
                                    ?: child.child("device_id").value?.toString()
                                    ?: "---"
                                val event = child.child("trang_thai").value?.toString()
                                    ?: child.child("event").value?.toString()
                                    ?: "---"
                                
                                val durationVal = child.child("thoi_luong_hoc(giay)").value as? Long
                                    ?: child.child("thoi_luong_hoc").value as? Long

                                // Fetch timestamp, fallback to parse display fields or current date if empty
                                var timestamp = child.child("timestamp").value as? Long ?: 0L
                                
                                var displayTimeStr = child.child("thoi_gian_check_in").value?.toString()
                                    ?: child.child("thoi_gian_check_out").value?.toString()
                                    ?: child.child("thoi_gian_vi_pham").value?.toString()

                                if (timestamp == 0L) {
                                    if (displayTimeStr != null) {
                                        timestamp = tryParseDate(displayTimeStr)
                                    } else {
                                        timestamp = System.currentTimeMillis()
                                    }
                                }

                                if (displayTimeStr == null) {
                                    val formatter = SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault())
                                    displayTimeStr = formatter.format(Date(timestamp))
                                }

                                list.add(
                                    LogEntity(
                                        firebaseKey = key,
                                        uid = uid,
                                        deviceId = deviceId,
                                        event = event,
                                        durationSeconds = durationVal,
                                        timestamp = timestamp,
                                        displayTime = displayTimeStr
                                    )
                                )
                            }
                            logDao.insertAll(list)
                        } catch (e: Exception) {
                            Log.e("Repository", "Sync logs error: ${e.message}")
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("Repository", "Sync logs cancelled: ${error.message}")
                }
            })

        } catch (e: Exception) {
            Log.e("Repository", "Setup listeners failed: ${e.message}")
        }
    }

    private fun tryParseDate(dateStr: String): Long {
        return try {
            // "HH:mm:ss dd/MM/yyyy" format
            val df = SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault())
            df.parse(dateStr)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            try {
                // "dd/MM/yyyy" format or other formats
                val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                df.parse(dateStr)?.time ?: System.currentTimeMillis()
            } catch (ex: Exception) {
                System.currentTimeMillis()
            }
        }
    }

    // ── DATABASE OPERATIONS (WRITING TO FIREBASE WHICH TRiggers LOCAL ROOM WRITES VIA REAL-TIME VALUE LISTENERS) ──

    fun saveStudent(uid: String, name: String, studentId: String, className: String?, photoBase64: String?, onComplete: (Boolean, String?) -> Unit) {
        val studentData = mutableMapOf<String, Any>(
            "name" to name,
            "id" to studentId
        )
        if (className != null) studentData["class"] = className
        if (photoBase64 != null) studentData["photo"] = photoBase64

        FirebaseHelper.database.getReference("students").child(uid).setValue(studentData)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onComplete(true, null)
                } else {
                    onComplete(false, task.exception?.message ?: "Unknown Firebase Error")
                }
            }
    }

    fun deleteStudent(uid: String, onComplete: (Boolean, String?) -> Unit) {
        FirebaseHelper.database.getReference("students").child(uid).removeValue()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onComplete(true, null)
                } else {
                    onComplete(false, task.exception?.message)
                }
            }
    }

    fun saveClass(className: String, onComplete: (Boolean, String?) -> Unit) {
        FirebaseHelper.database.getReference("classes").child(className).setValue(true)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onComplete(true, null)
                } else {
                    onComplete(false, task.exception?.message)
                }
            }
    }

    fun deleteClass(className: String, onComplete: (Boolean, String?) -> Unit) {
        FirebaseHelper.database.getReference("classes").child(className).removeValue()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onComplete(true, null)
                } else {
                    onComplete(false, task.exception?.message)
                }
            }
    }

    fun clearAllLogs(onComplete: (Boolean, String?) -> Unit) {
        FirebaseHelper.database.getReference("attendance_logs").removeValue()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onComplete(true, null)
                } else {
                    onComplete(false, task.exception?.message)
                }
            }
    }
}
