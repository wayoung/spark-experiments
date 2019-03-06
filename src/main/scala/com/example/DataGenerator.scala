package com.example

import Math.{max, min}
import java.sql.Timestamp
import java.sql.Timestamp.valueOf

import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.{DataFrame, Row, SaveMode}
import org.apache.spark.sql.types._
import org.apache.spark.sql.functions._

object DataGenerator extends SparkEnv {

  import spark.implicits._

  val batchSize = 100
  val rnd = scala.util.Random

  def generateTimestampData(n: Int): DataFrame = {
    val timestampDataFields = Seq(StructField("timestamp", TimestampType, false))
    val initDF = spark.createDataFrame(spark.sparkContext.emptyRDD[Row], StructType(timestampDataFields))
    def loop(data: DataFrame, lastTime: Long, _n: Int): DataFrame = {
      if (_n == 0) {
        val w = Window.orderBy("timestamp")
        data.withColumn("eventID", concat(typedLit("event"), row_number().over(w)))
      } else {
        var thisTime = lastTime
        def rts(ts: Long): Stream[Long] = ts #:: { thisTime = ts + rnd.nextInt(3) * 1000; rts(thisTime) }
        val thisBatch = rts(lastTime)
          .map(new Timestamp(_))
          .take(min(batchSize, _n))
          .toDF("timestamp")
        loop(data union thisBatch, thisTime, max(_n - batchSize, 0))
      }
    }
    loop(initDF, valueOf("2018-12-01 09:00:00").getTime(), n)
  }

  def main(args: Array[String]): Unit = {
    val w = Window.orderBy("timestamp")
    val df = generateTimestampData(10015)
      .withColumn("part", floor(row_number().over(w) / 100))
    df.repartition(27)
      .write
      .partitionBy("part")
      .option("compression", "snappy")
      .mode(SaveMode.Overwrite)
      .parquet("src/main/scala/com/example/resources/ts_data")
  }

}
