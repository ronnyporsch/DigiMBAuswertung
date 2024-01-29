import java.io.File
import kotlin.math.abs

lateinit var rootPath: String
const val minTimeBetweenBatteries = 30

fun main(args: Array<String>) {
    rootPath = args[0]
    val results = mutableListOf<Pair<String, Int>>()
    File(rootPath).walk().forEach {

        if (it.isDirectory && (it.name == "bayes" || it.name == "default")) {
            val result = Pair(it.path, evalAlgorithm(it))
            results.add(result)
        }
    }
    results.sortBy { abs(it.second) }
    results.forEach {
        println(it)
    }
}


fun evalAlgorithm(dir: File): Int {
    var errorCounter = 0
    dir.walk().forEach {
        if (it.isFile && it.extension == "txt") {
            if (it.name.contains("SSG_montage2")) return@forEach
            val counts = countModelAndVideoOccurrencesInFile(it)
            val errors = abs(counts.first-counts.second)
            errorCounter += errors
        }
    }
    return errorCounter
}

fun countModelAndVideoOccurrencesInFile(file: File): Pair<Int, Int> {
    val text = file.readText()
    val modelTimes = readStartAndEndTimesModel(text)
    var counterOfBatteriesToExclude = 0
    for (i in 1..<modelTimes.size) {
        if ((modelTimes[i].first - modelTimes[i - 1].second) < minTimeBetweenBatteries) {
            counterOfBatteriesToExclude++
        }
    }
    return Pair(countWordOccurrencesInString(text, "Model") - counterOfBatteriesToExclude, countWordOccurrencesInString(text, "Video"))
}

/**
 * @return pair.first: startNumber, pair.second: endNumber
 */
fun readStartAndEndTimesModel(string: String): List<Pair<Int, Int>> {
    val list = mutableListOf<Pair<Int, Int>>()
    string.lines().forEach {
        if (it.contains("Video")) return@forEach
        val (startNumber, endNumber) = getNumbersFromString(it)
        if (startNumber != null && endNumber != null) {
            list.add(Pair(startNumber, endNumber))
        }
    }
    return list
}

fun getNumbersFromString(input: String): Pair<Int?, Int?> {
    val regex = Regex("\\d+")
    val matches = regex.findAll(input)

    val numbers = matches.map { it.value.toInt() }.toList()

    return when (numbers.size) {
        2 -> Pair(numbers[0], numbers[1])
        1 -> Pair(numbers[0], null)
        else -> Pair(null, null)
    }
}

fun countWordOccurrencesInString(str: String, searchStr: String): Int {
    var count = 0
    var startIndex = 0

    while (startIndex < str.length) {
        val index = str.indexOf(searchStr, startIndex)
        if (index >= 0) {
            count++
            startIndex = index + searchStr.length
        } else {
            break
        }
    }
    return count
}

//data class FileContent(
//    val modelName
//)
//
//data class LineContent(
//    val isVideo: Boolean,
//    val startTime: Int,
//    val endTime: Int
//)