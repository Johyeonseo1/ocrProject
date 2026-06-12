package com.example.ocrproject

// 개별 상품 정보
data class ReceiptItem(
    val name: String,
    val quantity: Int,
    val price: Int
)

// 전체 영수증 정보
data class Receipt(
    val merchantName: String,
    val date: String,
    val items: List<com.example.ocrproject.ReceiptItem>,
    val totalAmount: Int
)

object DataMapper {
    fun mapToReceipt(
        merchantName: String,
        date: String,
        items: List<com.example.ocrproject.ReceiptItem>,
        totalAmount: Int
    ): Receipt {
        return Receipt(
            merchantName = merchantName,
            date = date,
            items = items,
            totalAmount = totalAmount
        )
    }

    fun mapToFinalReceiptItems(rawItems: List<Triple<String, Int, Int>>): List<com.example.ocrproject.ReceiptItem> {
        return rawItems.map { (productName, quantity, price) ->
            com.example.ocrproject.ReceiptItem(
                name = productName,
                quantity = quantity,
                price = price
            )
        }
    }
}