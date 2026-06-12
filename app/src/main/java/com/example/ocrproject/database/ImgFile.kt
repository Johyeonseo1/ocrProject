package com.example.ocrproject.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "ImgInfo")
@TypeConverters(ReceiptConverters::class)
data class ImgFile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val imgUri: String,                  // 영수증 이미지 경로 (기존 필드가 있다면 그대로 유지)
    val merchantName: String,            // 가게명
    val date: String,                    // 결제일시
    val totalAmount: Int,                // 총 금액
    val items: List<com.example.ocrproject.ReceiptItem> // 상품 리스트
)

class ReceiptConverters {
    @TypeConverter
    fun fromReceiptItemList(value: List<com.example.ocrproject.ReceiptItem>): String {
        return Gson().toJson(value) // 리스트를 글자(JSON)로 변경
    }

    @TypeConverter
    fun toReceiptItemList(value: String): List<com.example.ocrproject.ReceiptItem> {
        val listType = object : TypeToken<List<com.example.ocrproject.ReceiptItem>>() {}.type
        return Gson().fromJson(value, listType) // 글자를 다시 리스트로 변경
    }
}


