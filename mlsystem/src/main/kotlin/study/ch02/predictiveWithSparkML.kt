package study.ch02



import org.apache.spark.api.java.JavaPairRDD
import org.apache.spark.mllib.evaluation.RegressionMetrics
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.regression.LinearRegressionWithSGD
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession
import scala.Tuple2
import java.io.File
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

    File("data/train.csv").printWriter().use { out ->
        trainData.forEach { (y, x12) ->
            val (x1, x2) = x12
            out.println("%.2f,%.2f,%.2f".format(y, x1, x2))
        }
    }
    File("data/test.csv").printWriter().use { out ->
        testData.forEach { (y, x12) ->
            val (x1, x2) = x12
            out.println("%.2f,%.2f,%.2f".format(y, x1, x2))
        }
    }
}

// train a model with the data stored in file
fun buildModel() {
    val session = SparkSession.builder()
            .master("local[*]")
            .appName("predictiveWithSpark")
            .getOrCreate()
    val trainData = session.read().csv("data/train.csv")
            .toJavaRDD().map { row ->
                val y = row[0].toString().toDouble()
                val x1 = row[1].toString().toDouble()
                val x2 = row[2].toString().toDouble()
                LabeledPoint(y, Vectors.dense(x1, x2))
            }.cache()
    val numIterations = 5000
    val model = LinearRegressionWithSGD.train(trainData.rdd(), numIterations)
    println("Trained Model: coeffs=${model.weights()}, bias=${model.intercept()}")

    val testData = session.read().csv("data/test.csv")
            .toJavaRDD().map { row ->
                val y = row[0].toString().toDouble()
                val x1 = row[1].toString().toDouble()
                val x2 = row[2].toString().toDouble()
                LabeledPoint(y, Vectors.dense(x1, x2))
            }.cache()
    val testPredictionWithLabel = testData.map { point ->
        Tuple2(model.predict(point.features()), point.label())
    }
    // REALLY BAD, why??
    testPredictionWithLabel.foreach {println(it)}
}

fun main(args: Array<String>) {
    generateData()
    buildModel()
}