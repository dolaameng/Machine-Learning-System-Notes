package study.ch02

import org.apache.spark.SparkConf
import org.apache.spark.api.java.JavaSparkContext
import java.io.File
import java.io.IOException
import java.util.*

// Using Spark ML for a simple regression problem

// Generate a simple regression data and save them into
// train and test files
fun generateData() {

    val randy = Random()
    val x1s = (0..50).map { it * 2.0 / 50 }.asSequence()
    val x2s = (0..50).map { it * 2.0 / 50 }.asSequence()
    val ys = x1s.zip(x2s).map { (x1, x2) ->
        2 * x1 - 5 * x2 + 3 + randy.nextDouble() / 10
    }

    val data = ys.zip(x1s.zip(x2s))
    val trainData = data.filterIndexed { index, datum ->
        index % 4 != 0
    }
    val testData = data.filterIndexed { index, datum ->
        index % 4 == 0
    }

    File("data/train.file").printWriter().use { out ->
        trainData.forEach { (y, x12) ->
            val (x1, x2) = x12
            out.println("$y,$x1,$x2")
        }
    }
    File("data/test.file").printWriter().use { out ->
        testData.forEach { (y, x12) ->
            val (x1, x2) = x12
            out.println("$y,$x1,$x2")
        }
    }
}

// train a model with the data stored in file
fun buildModel() {
    // new spark sql api starts with a session, like in tensorflow
//    val config = SparkConf().setMaster("localhost").setAppName("predictiveWithSpark")
    val sc = JavaSparkContext()

}

fun main(args: Array<String>) {
    generateData()
    buildModel()
}