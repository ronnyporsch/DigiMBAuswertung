import java.io.File
import kotlin.math.abs

lateinit var rootDir: String //set via command line argument
var minTimeBetweenBatteries: Int = 30 //can be changed via command line argument

/**
 * Walks through all folders of the [rootDir]. For each model folder, [evalAlgorithm] will be called. and the results are put into a list.
 * Prints the list at the end, ordered by number of errors (the best algorithms will be printed first)
 */
fun main(args: Array<String>) {
    rootDir = args[0]
    if (args.size >= 2) minTimeBetweenBatteries = args[1].toInt()

    val results = mutableListOf<Pair<String, Int>>()
    File(rootDir).walk().forEach {

        if (it.isModelFolder()) {
            val result = Pair(it.path, evalAlgorithm(it))
            results.add(result)
        }
    }
    results.sortBy { abs(it.second) }
    results.forEach {
        println("Errors: ${it.second} Algo: ${it.first.removePrefix(rootDir)}")
    }
}

/**
 * true for folders named "bayes" and "default"
 */
private fun File.isModelFolder() : Boolean {
    return isDirectory && (name == "bayes" || name == "default")
}


/**
 * evaluates the algorithm
 * @return the number of false positives + false negatives
 */
private fun evalAlgorithm(dir: File): Int {
    var errorCounter = 0
    dir.walk().forEach {
        if (it.isFile && it.extension == "txt") {
            if (it.name.contains("SSG_montage2")) return@forEach
            val errors = countErrorsInFile(it)
            errorCounter += errors
        }
    }
    return errorCounter
}

/**
 * counts occurrences of the "words" model and "video" in the file.
 * Excludes too frequent battery occurrences in the model based on [minTimeBetweenBatteries]
 * @return the difference between numbers of batteries in model and in video (after excluding too frequent occurrences of model batteries)
 */
private fun countErrorsInFile(file: File): Int {
    val text = file.readText()
    val modelTimes = readStartAndEndTimesModel(text)
    var counterOfBatteriesToExclude = 0
    for (i in 1..<modelTimes.size) {
        if ((modelTimes[i].first - modelTimes[i - 1].second) < minTimeBetweenBatteries) {
            counterOfBatteriesToExclude++
        }
    }
    val batteriesInModel = countWordOccurrencesInString(text, "Model") - counterOfBatteriesToExclude
    val batteriesInVideo = countWordOccurrencesInString(text, "Video")
    var errors = batteriesInModel - batteriesInVideo
    if (errors < 1) errors *= 2
    return abs(errors)
}

/**
 * @return pair.first: startNumber, pair.second: endNumber
 */
private fun readStartAndEndTimesModel(string: String): List<Pair<Int, Int>> {
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

/**
 * reads a pair of two Integers if they are in a string
 */
private fun getNumbersFromString(input: String): Pair<Int?, Int?> {
    val regex = Regex("\\d+")
    val matches = regex.findAll(input)

    val numbers = matches.map { it.value.toInt() }.toList()

    return when (numbers.size) {
        2 -> Pair(numbers[0], numbers[1])
        else -> Pair(null, null)
    }
}

/**
 * counts times a word is contained in a string
 */
private fun countWordOccurrencesInString(str: String, word: String): Int {
    var count = 0
    var startIndex = 0

    while (startIndex < str.length) {
        val index = str.indexOf(word, startIndex)
        if (index >= 0) {
            count++
            startIndex = index + word.length
        } else {
            break
        }
    }
    return count
}