package com.example.ocrproject.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val projectName: String,   // 프로젝트명
    val taskNumber: String,    // 과제 번호
    val dateTime: String,      // 일시
    val memberCount: Int       // 인원
)

data class Receipt(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val projectId: Int,        // 연관된 프로젝트 ID (외래키)
    val merchantName: String,  // 상호명
    val paymentDateTime: String, // 결제일시
    val details: String,       // 내역 (ex: "마미케어 바다포도 스킨팩 외 9")
    val amount: Int            // 금액
)