package com.example.ocrproject

import kotlin.math.abs

object ReceiptParser {

    fun String.toPriceInt(): Int {
        return this.replace(",", "").toIntOrNull() ?: 0
    }

    fun formatReceiptText(lineList: List<LineData>): String {
        if (lineList.isEmpty()) return ""

        // Y 좌표 기준으로 그룹화 후 X축 정렬하여 1차 병합
        val initialLines = mutableListOf<MutableList<LineData>>()

        lineList.sortedBy { it.frame?.top ?: 0 }.forEach { line ->
            val lastGroup = initialLines.lastOrNull()
            val lastY = lastGroup?.firstOrNull()?.frame?.top ?: 0

            // Y축 오차가 25px 미만이면 기존 그룹에 추가, 아니면 새 그룹 생성
            if (lastGroup != null && abs((line.frame?.top ?: 0) - lastY) < 25) {
                lastGroup.add(line)
            } else {
                initialLines.add(mutableListOf(line))
            }
        }

        // 각 그룹을 왼쪽(X축) 정렬 후 하나의 문자열로 변환
        val mergedLines = initialLines.map { group ->
            group.sortedBy { it.frame?.left ?: 0 }.joinToString(" ") { it.text }.trim()
        }

        // 상품명 아래로 처진 가격/수량 정보 한 줄로 합치기
        val finalLines = mutableListOf<String>()

        mergedLines.forEach { line ->
            val lastLine = finalLines.lastOrNull()

            // 현재 줄이 숫자/공백/콤마로만 되어 있고, 윗줄에 금액(3자리 이상 숫자)이 없다면 합치기
            if (line.matches(Regex("^[0-9, ]+$")) && lastLine != null) {
                finalLines[finalLines.lastIndex] = "$lastLine $line"
            } else {
                finalLines.add(line)
            }
        }

        return finalLines.joinToString("\n")
    }


    fun cleanReceiptText(cleanedText: String): String {
        val tempResultList = cleanedText.lines()

        return tempResultList.mapNotNull { line ->
            // :, [, ] 중 하나라도 포함되어 있다면 완전히 제외
            if (line.contains(":") || line.contains("[") || line.contains("]")) {
                null
            } else {
                // 💡 숫자 사이에 낀 [쉼표 + 공백]을 찾아서 흔적도 없이 지웁니다.
                // "1,500" -> "1500" / "1, 500" -> "1500" 둘 다 완벽하게 잡아냅니다.
                line.replace(Regex("(?<=\\d),\\s*(?=\\d)"), "")
            }
        }.joinToString("\n") // 다시 하나의 이쁜 String으로 합쳐서 반환
    }

    fun extractValidReceiptItems(listText: String): String {
        return listText.lines().filter { line ->
            if (line.isBlank()) return@filter false

            // + 기호 로직 (기존 유지)
            val hasLettersBeforePlus = line.substringBefore("+").any { it.isLetter() }
            val hasLettersAfterPlus = line.substringAfter("+").any { it.isLetter() }
            val isMenuPlus = line.contains("+") && hasLettersBeforePlus && hasLettersAfterPlus

            // 💡 하이픈(-) 필터링 로직 개선
            if (line.contains("-")) {
                val trimmed = line.trim()

                // 1. 맨 앞이나 뒤에 '-'가 붙어있으면 노이즈로 간주하고 버림 (ex: "- 불고기", "불고기 -")
                if (trimmed.startsWith("-") || trimmed.endsWith("-")) return@filter false

                // 2. 글자-글자 형태가 하나도 없다면 의미 없는 하이픈으로 간주하고 버림
                // (즉, [문자/숫자]-[문자/숫자] 패턴이 아예 안 보이면 탈락)
                if (!line.contains(Regex("[a-zA-Z0-9가-힣]-[a-zA-Z0-9가-힣]"))) return@filter false
            }

            // + 로직은 그대로 유지
            if (line.contains("+") && !isMenuPlus) return@filter false

            // (이하 나머지 코드는 동일)
            val tokens = line.trim().split("\\s+".toRegex())
            val pureNumberChunks = tokens.filter { token -> token.matches(Regex("^[0-9]+$")) }

            val zeroChunksCount = pureNumberChunks.count { it.toIntOrNull() == 0 }
            if (zeroChunksCount >= 2) return@filter false

            val hasAtLeastTwoNumbers = pureNumberChunks.size >= 2
            val hasLetters = line.any { it.isLetter() }

            hasAtLeastTwoNumbers && hasLetters
        }.joinToString("\n")
    }

    fun parseRawReceiptDetails(filteredText: String): List<Triple<String, Int, Int>> {
        return filteredText.lines().filter { it.isNotBlank() }.map { line ->
            // 공백 기준으로 단어 쪼개기
            val tokens = line.trim().split("\\s+".toRegex())

            // 숫자 부분만 모으고, 0은 제외하기
            val pureNumbers = tokens
                .filter { it.matches(Regex("^[0-9]+$")) }
                .mapNotNull { it.toIntOrNull() }
                .filter { it > 0 } // 0 제외

            // 제일 작은 수와 제일 큰 수 구하기
            val quantity: Int
            val price: Int

            if (pureNumbers.size >= 2) {
                // 1. 가장 큰 숫자는 무조건 '총 금액'으로 가정합니다.
                val maxPrice = pureNumbers.maxOrNull() ?: 0
                price = maxPrice

                // 2. 전체 숫자 리스트에서 총 금액 하나를 제외한 나머지 숫자들만 모읍니다.
                val remaining = pureNumbers.toMutableList()
                remaining.remove(maxPrice) // 첫 번째로 매칭되는 총액 하나만 제거

                if (remaining.contains(maxPrice)) {
                    // 단가와 총액이 같은 경우 (ex: [5000, 5000] -> 하나 지워도 5000이 남음)
                    quantity = 1
                } else {
                    // 남은 숫자 중 100 미만의 소형 숫자가 있다면 수량일 확률이 높음
                    val likelyQuantity = remaining.firstOrNull { it < 100 } ?: remaining.minOrNull() ?: 1

                    if (likelyQuantity > 100 && maxPrice % likelyQuantity == 0) {
                        // 만약 수량이 안 찍히고 [단가, 총액]만 찍힌 경우 (ex: [2500, 5000])
                        // 총액을 단가로 나눈 몫을 수량으로 계산 (5000 / 2500 = 2개)
                        quantity = maxPrice / likelyQuantity
                    } else if (likelyQuantity > 100) {
                        // 나누어 떨어지지 않는 큰 수라면 수량이 아니라고 판단하고 1로 방어막 구축
                        quantity = 1
                    } else {
                        // 일반적인 수량 (ex: 1, 2, 3 등)
                        quantity = likelyQuantity
                    }
                }
            } else if (pureNumbers.size == 1) {
                quantity = 1
                price = pureNumbers.first()
            } else {
                quantity = 1
                price = 0
            }

            // 숫자를 제외한 나머지를 합쳐서 상품명으로 저장
            val rawProductName = tokens
                .filter { !it.matches(Regex("^[0-9]+$")) }
                .joinToString(" ") // 단어 사이 공백 유지

            // 시작지점부터 특수문자 제거
            val productName = rawProductName.replaceFirst(Regex("^[^a-zA-Z0-9가-힣]+"), "").trim()

            Triple(productName, quantity, price)
        }
    }

    fun extractStoreName(fullText: String): String {
        val lineList = fullText.lines()
        val keywords = listOf("상호명", "가게명", "상호", "가맹점명", "가맹점", "점포명", "매장명")

        // 1단계: 단어 전체가 정확히 일치하는 키워드가 있는지 검사
        for (line in lineList) {
            val tokens = line.split(Regex("[\\s:\\-\\[\\](),.]+"))
            val matchedKeyword = keywords.find { keyword -> tokens.contains(keyword) }

            if (matchedKeyword != null) {
                val rawName = line.replace(matchedKeyword, "").trim()
                if (rawName.isNotEmpty()) return rawName
            }
        }

        // 2단계: 만족하는 단어가 하나도 없다면, 무조건 맨 위에 있는 데이터 가져오기
        val firstTopLine = lineList.map { it.trim() }.firstOrNull { it.isNotEmpty() }
        return firstTopLine ?: "알 수 없는 가게"
    }

    /**
    fun cleanStoreNameBrackets(rawStoreName: String): String {
        if (rawStoreName == "알 수 없는 가게") return rawStoreName

        // 괄호와 괄호 안의 문자열을 통째로 매칭하는 정규식
        val bracketRegex = Regex("\\[.*?\\]|\\(.*?\\)")

        // 1. 괄호와 안의 내용물 제거
        var cleanedName = rawStoreName.replace(bracketRegex, "")

        // 2. 남은 잡다한 기호(:, -, 대괄호 등) 및 공백 정리
        cleanedName = cleanedName.replace(Regex("[:\\s\\-\\[\\](),.]+"), " ").trim()

        return if (cleanedName.isNotEmpty()) cleanedName else "알 수 없는 가게"
    }
    **/

    fun cleanStoreNameBrackets(rawStoreName: String): String {
        if (rawStoreName == "알 수 없는 가게") return rawStoreName

        // 괄호와 괄호 안의 문자열을 통째로 매칭하는 정규식
        val bracketRegex = Regex("\\[.*?\\]|\\(.*?\\)")

        // 1. 괄호와 안의 내용물 제거
        var cleanedName = rawStoreName.replace(bracketRegex, "")

        // 2. 남은 잡다한 기호(:, -, 대괄호 등) 및 공백 정리
        cleanedName = cleanedName.replace(Regex("[:\\s\\-\\[\\](),.]+"), " ").trim()

        // 3. 영어 가맹점명을 한글로 변환해주는 매칭 리스트 (대문자 기준 등록)
        val englishToKoreanStoreMap = mapOf(
            "BURGER KING" to "버거킹",
            "LOTTERIA" to "롯데리아",
            "STARBUCKS" to "스타벅스"
        )

        // 사용자가 대소문자를 섞어 쓰더라도 매칭되도록 대문자로 변환 후 비교
        val upperCleanedName = cleanedName.uppercase()

        // 4. 변환 리스트에 존재하는 영어 이름이면 한글로 교체
        if (englishToKoreanStoreMap.containsKey(upperCleanedName)) {
            cleanedName = englishToKoreanStoreMap[upperCleanedName] ?: cleanedName
        }

        return if (cleanedName.isNotEmpty()) cleanedName else "알 수 없는 가게"
    }

    fun totalPrice(fullText: String): String {
        val lineList = fullText.lines()
        val keywords = listOf("합계금액", "결제금액", "받은금액", "합계 금액", "결제 금액")

        for (line in lineList) {
            val matchedKeyword = keywords.find { keyword ->
                val index = line.indexOf(keyword)
                if (index == -1) return@find false

                val beforeChar = if (index > 0) line[index - 1] else ' '
                val afterChar = if (index + keyword.length < line.length) line[index + keyword.length] else ' '

                !beforeChar.toString().matches(Regex("[가-힣]")) && !afterChar.toString().matches(Regex("[가-힣]"))
            }

            if (matchedKeyword != null) {
                // 💡 1. 키워드 제거 후, 콤마(,)와 모든 공백(\s)을 제거합니다.
                // 이렇게 하면 "9, 700" 이나 "9 700" 이 "9700"으로 합쳐집니다.
                val cleanLine = line.replace(matchedKeyword, "")
                    .replace(",", "")
                    .replace(Regex("\\s+"), "")

                // 💡 2. 이제 숫자로만 구성된 덩어리들을 찾습니다.
                val allNumbers = Regex("\\d+").findAll(cleanLine)
                    .mapNotNull { it.value.toIntOrNull() }
                    .toList()

                // 💡 3. 그중 가장 큰 값을 찾습니다.
                val maxPrice = allNumbers.maxOrNull()

                if (maxPrice != null) {
                    return maxPrice.toString()
                }
            }
        }

        return "0"
    }

    fun removePricePunctuation(rawPrice: String): String {
        // 💡 쉼표(,)와 마침표(.)를 찾아내서 빈 값("")으로 치환(제거)합니다.
        return rawPrice.replace(",", "")
            .replace(".", "")
            .trim()
    }

    // 날짜 형식 변경
    fun formatDate(dateString: String): String {
        // 💡 정규식: 연도(4자리) + 구분자(.이나 -나 /) + 월(1~2자리) + 구분자 + 일(1~2자리)
        val regex = Regex("(\\d{4})[./-](\\d{1,2})[./-](\\d{1,2})")
        val match = regex.find(dateString) ?: return dateString // 형식이 안 맞으면 그냥 원래대로 반환

        val year = match.groupValues[1] // 2026
        val month = match.groupValues[2].padStart(2, '0') // 1자리 월을 2자리로 (ex: 6 -> 06)
        val day = match.groupValues[3].padStart(2, '0')   // 1자리 일을 2자리로 (ex: 2 -> 02)

        // 💡 연도의 뒤 2자리만 가져와서 YY-MM-DD로 조합
        return "${year.takeLast(2)}-$month-$day"
    }

    // 날짜 감지
    fun extractDateTime(fullText: String): String {
        val lineList = fullText.lines()
        val keywords = listOf("결제일시", "결제 일시", "발행일시", "발행 일시", "승인일시", "승인 일시", "승인일자", "승인 일자")

        // 키워드 검색
        for (line in lineList) {
            val matchedKeyword = keywords.find { keyword -> line.contains(keyword) }

            if (matchedKeyword != null) {
                val remainingText = line.replace(matchedKeyword, "").trim()
                val dateTime = remainingText.replace(Regex("^[:\\s\\-\\[\\]()]+"), "").trim()

                // 💡 찾은 날짜를 포맷 함수에 통과시키기
                return formatDate(dateTime)
            }
        }

        // 날짜 패턴 직접 검색
        val datePattern = Regex("\\d{4}[./-]\\d{1,2}[./-]\\d{1,2}")
        for (line in lineList) {
            val match = datePattern.find(line)
            if (match != null) {
                // 💡 찾은 날짜를 포맷 함수에 통과시키기
                return formatDate(match.value)
            }
        }

        return "알 수 없는 일시"
    }

    fun formatToShortDate(rawDateTime: String): String {
        // 문자열에서 숫자만 쏙 뽑음
        val digits = rawDateTime.filter { it.isDigit() }

        // 숫자가 정확히 12개인 경우
        if (digits.length == 12) {
            val yy = digits.substring(0, 2) // 앞의 2글자 (년)
            val mm = digits.substring(2, 4) // 중간 2글자 (월)
            val dd = digits.substring(4, 6) // 그다음 2글자 (일)
            return "$yy-$mm-$dd"
        }

        // 하이픈(-)이 포함되어 있는 경우
        if (rawDateTime.contains("-")) {
            // YYYY-MM-DD패턴을 찾음
            val regexFourDigitYear = Regex("\\d{4}-\\d{2}-\\d{2}")
            val matchResult = regexFourDigitYear.find(rawDateTime)

            if (matchResult != null) {
                val fullDate = matchResult.value // 예: "2026-06-06"
                return fullDate.substring(2)     // 앞의 "20"을 자르고 "26-06-06" 반환
            }

            // 만약 이미 "26-06-06" 형태로 들어왔다면 정규식에 안 걸리므로,
            // 숫자2개-숫자2개-숫자2개 패턴이 있는지 한 번 더 확인해서 그대로 반환
            val regexTwoDigitYear = Regex("\\d{2}-\\d{2}-\\d{2}")
            val shortMatch = regexTwoDigitYear.find(rawDateTime)
            if (shortMatch != null) {
                return shortMatch.value
            }
        }

        // 두 조건에 모두 해당하지 않으면 원본에서 양끝 공백만 제거하고 반환
        return rawDateTime.trim()
    }

    fun simpleList(rawDataList: List<Triple<String, Int, Int>>): String {
        if (rawDataList.isEmpty()) return ""

        val firstItemName = rawDataList.first().first
        val remainingCount = rawDataList.size - 1

        return if (remainingCount > 0) {
            "$firstItemName 외 $remainingCount"
        } else {
            firstItemName
        }
    }
}
