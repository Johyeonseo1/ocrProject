package com.example.ocrproject

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
**/

class MainActivity : AppCompatActivity() {
    private val ocrManager by lazy { OcrManager(this) }
    private val receiptPro = ReceiptPro()
    private lateinit var receiptProcess: ReceiptProcess
    private lateinit var resultTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. ReceiptProcess 조립
        receiptProcess = ReceiptProcess(ocrManager, receiptPro)

        // 2. 뷰 초기화
        resultTextView = findViewById(R.id.resultTextView)
        val btn = findViewById<Button>(R.id.testButton)

        // 💡 버튼 클릭 리스너 완성본
        btn.setOnClickListener {
            val testUri = Uri.parse("android.resource://$packageName/raw/receipt_sample1")

            // 영수증 처리 프로세스 시작
            receiptProcess.processNewReceipt(testUri) { receiptResult ->
                // OCR 작업은 백그라운드에서 돌기 때문에 UI 변경은 반드시 runOnUiThread 안에서!
                runOnUiThread {
                    val displayLayout = buildString {
                        appendLine("가맹점 : ${receiptResult.merchantName}")
                        appendLine("결제일 : ${receiptResult.date}")
                        appendLine("----------------------------------")

                        if (receiptResult.items.isEmpty()) {
                            appendLine("   추출된 상품 내역이 없습니다.")
                        } else {
                            // 상품 리스트 출력
                            receiptResult.items.forEach { item ->
                                appendLine("• ${item.name} | ${item.quantity}개 | ${item.price}원")
                            }
                        }

                        appendLine("----------------------------------")
                        // 세 자리마다 쉼표(,) 찍어서 금액 표현
                        val formattedAmount = String.format("%,d", receiptResult.totalAmount)
                        appendLine("총 결제 금액 : ${formattedAmount}원")
                    }

                    // 💡 빨간 줄이 떴던 binding 대신 미리 찾아둔 resultTextView를 직접 사용합니다!
                    resultTextView.text = displayLayout.trimEnd()
                }
            }
        }
    }
}
