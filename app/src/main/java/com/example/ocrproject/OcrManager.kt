package com.example.ocrproject

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import java.io.IOException

/**
class OcrManager (private val context: Context) {

    // 텍스트 인식 준비
    private val recognizer: TextRecognizer by lazy {
        // KoreanTextRecognizerOptions를 직접 사용합니다.
        val options = KoreanTextRecognizerOptions.Builder()
            .build()

        TextRecognition.getClient(options)
    }

    // 함수 : 받고 넘겨줌
    fun process(uri: Uri, onResult: (String) -> Unit) {
        val image: InputImage
        try {
            // 이미지 경로로부터 InputImage 생성
            image = InputImage.fromFilePath(context, uri)
        } catch (e: IOException) {
            e.printStackTrace()
            onResult("") // 에러 시 빈 문자열 전달
            return
        }

        // 텍스트 인식
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                // 인식 성공 시, 전체 텍스트를 rawText로 추출하여 콜백으로 전달
                val rawText = visionText.text
                onResult(rawText)
            }
            .addOnFailureListener { e ->
                // 인식 실패 시
                e.printStackTrace()
                onResult("") // 에러 시 빈 문자열 전달
            }
    }
}
        **/


data class LineData(val text: String, val frame: Rect?)

class OcrManager(private val context: Context) {

    private val recognizer: TextRecognizer by lazy {
        TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
    }

    // List<LineData>를 반환하도록 변경
    fun process(uri: Uri, onResult: (List<LineData>) -> Unit) {
        val image: InputImage = try {
            InputImage.fromFilePath(context, uri)
        } catch (e: IOException) {
            e.printStackTrace()
            onResult(emptyList()) // 에러 발생 시 빈 리스트 반환
            return
        }

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val lineList = mutableListOf<LineData>()

                // 블록 -> 줄 단위로 순회하여 데이터 저장
                for (block in visionText.textBlocks) {
                    for (line in block.lines) {
                        lineList.add(LineData(line.text, line.boundingBox))
                    }
                }
                onResult(lineList)
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                onResult(emptyList())
            }
    }
}
// 예전 코드
/**
data class WordData(val text: String, val frame: Rect?) {
    override fun toString(): String = if (frame != null) "[${frame.left}:${frame.top}] $text" else "[0:0] $text"
}

data class LineData(val yBase: Int, val words: List<WordData>)

class OcrManager {
    private val recognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())

    fun process(bitmap: Bitmap, onResult: (List<LineData>) -> Unit, onError: (Exception) -> Unit) {
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val allWords = visionText.textBlocks.flatMap { it.lines }.flatMap { it.elements }
                    .map { WordData(it.text, it.boundingBox) }

                if (allWords.isEmpty()) {
                    onResult(emptyList())
                    return@addOnSuccessListener
                }

                val sortedAllWords = allWords.sortedBy { it.frame?.top ?: 0 }
                val groupedLines = mutableListOf<LineData>()
                val threshold = 30

                var currentLineWords = mutableListOf<WordData>()
                var lineBaselineY = sortedAllWords[0].frame?.top ?: 0

                for (word in sortedAllWords) {
                    val currentY = word.frame?.top ?: 0
                    if (Math.abs(currentY - lineBaselineY) < threshold) {
                        currentLineWords.add(word)
                    } else {
                        groupedLines.add(LineData(lineBaselineY, currentLineWords.sortedBy { it.frame?.left ?: 0 }))
                        currentLineWords = mutableListOf(word)
                        lineBaselineY = currentY
                    }
                }
                if (currentLineWords.isNotEmpty()) {
                    groupedLines.add(LineData(lineBaselineY, currentLineWords.sortedBy { it.frame?.left ?: 0 }))
                }
                onResult(groupedLines)
            }
            .addOnFailureListener { onError(it) }
    }
}**/

// ocr + 글짜 정렬
/**
data class WordData(val text: String, val frame: Rect?)
data class LineData(val yBase: Int, val words: List<WordData>)

class OcrManager {
    private val recognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())

    fun process(bitmap: Bitmap, onResult: (List<LineData>) -> Unit, onError: (Exception) -> Unit) {
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val allWords = visionText.textBlocks.flatMap { it.lines }.flatMap { it.elements }
                    .map { WordData(it.text, it.boundingBox) }

                if (allWords.isEmpty()) {
                    onResult(emptyList())
                    return@addOnSuccessListener
                }

                val sortedAllWords = allWords.sortedBy { it.frame?.top ?: 0 }
                val groupedLines = mutableListOf<LineData>()
                val threshold = 30

                var currentLineWords = mutableListOf<WordData>()
                var lineBaselineY = sortedAllWords[0].frame?.top ?: 0

                for (word in sortedAllWords) {
                    val currentY = word.frame?.top ?: 0
                    if (Math.abs(currentY - lineBaselineY) < threshold) {
                        currentLineWords.add(word)
                    } else {
                        groupedLines.add(LineData(lineBaselineY, currentLineWords.sortedBy { it.frame?.left ?: 0 }))
                        currentLineWords = mutableListOf(word)
                        lineBaselineY = currentY
                    }
                }
                if (currentLineWords.isNotEmpty()) {
                    groupedLines.add(LineData(lineBaselineY, currentLineWords.sortedBy { it.frame?.left ?: 0 }))
                }
                onResult(groupedLines)
            }
            .addOnFailureListener { onError(it) }
    }
}
        **/