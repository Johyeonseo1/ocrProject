package com.example.ocrproject

import android.net.Uri
import com.example.ocrproject.DataMapper.mapToReceipt

class ReceiptProcess(
    private val ocrManager: OcrManager,
    private val receiptPro: ReceiptPro
) {
    fun processNewReceipt(uri: Uri, onSuccess: (Receipt) -> Unit) {

        ocrManager.process(uri) { lineList ->
            val cleanedText = ReceiptParser.formatReceiptText(lineList)

            // 데이터 매핑
            val parsedReceiptList = receiptPro.executeDataMapping(cleanedText)

            // 가게명 처리
            val storeName = ReceiptParser.extractStoreName(cleanedText)
            val cleanStoreNameBrackets = ReceiptParser.cleanStoreNameBrackets(storeName)

            // 가격 처리
            val totalPriceRaw = ReceiptParser.totalPrice(cleanedText)
            val priceString = ReceiptParser.removePricePunctuation(totalPriceRaw) // "5000" 같은 문자열

            // 문자열을 숫자로 변환
            val totalAmountInt = priceString.toIntOrNull() ?: 0

            // 일시 처리
            val extractDateTime = ReceiptParser.extractDateTime(cleanedText)
            val formatToShortDate = ReceiptParser.formatToShortDate(extractDateTime)

            val receipt = mapToReceipt(
                merchantName = cleanStoreNameBrackets,
                date = formatToShortDate,
                items = parsedReceiptList,
                totalAmount = totalAmountInt
            )

            onSuccess(receipt)
        }
    }
}

class ReceiptPro{
    fun executeDataMapping(filteredText: String): List<ReceiptItem> {

        val listText = ReceiptParser.cleanReceiptText(filteredText)

        val receiptItem = ReceiptParser.extractValidReceiptItems(listText)

        // 데이터를 필터링 및 정리
        val rawDataList = ReceiptParser.parseRawReceiptDetails(receiptItem)

        // ReceiptItem 데이터 클래스에 저장
        val finalReceiptList = DataMapper.mapToFinalReceiptItems(rawDataList)

        //결과 반환
        return finalReceiptList
    }
}

