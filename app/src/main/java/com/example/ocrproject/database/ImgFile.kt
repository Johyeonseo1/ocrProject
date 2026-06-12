package com.example.ocrproject.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "ImgInfo")
@TypeConverters(ReceiptConverters::class) // 💡 Room이 리스트를 인식할 수 있도록 컨버터를 장착합니다.
data class ImgFile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val imgUri: String,                  // 영수증 이미지 경로 (기존 필드가 있다면 그대로 유지)
    val merchantName: String,            // 가게명
    val date: String,                    // 결제일시
    val totalAmount: Int,                // 총 금액
    val items: List<com.example.ocrproject.ReceiptItem> // 상품 리스트
)

/**
 * 💡 List<ReceiptItem>을 데이터베이스가 이해할 수 있는 String(JSON)으로 변환해주는 도우미 클래스
 */
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


