package com.example.ocrproject

data class Receipt(
    val merchantName: String,
    val date: String,
    val items: String,
    val totalAmount: Int
)

object DataMapper {

    fun mapToReceipt(
        merchantName: String,
        date: String,
        items: String,
        totalAmount: Int
    ): Receipt {
        return Receipt(
            merchantName = merchantName,
            date = date,
            items = items,
            totalAmount = totalAmount
        )
    }
}