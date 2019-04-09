package com.example.demo

import com.example.SparkEnv
import com.example.random.RandomRDD
import org.apache.spark.sql.{DataFrame, Row, SaveMode}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.types._

object Main extends SparkEnv {

  import spark.implicits._

  def test1() = {
    val df = spark.read.json("src/main/scala/com/example/resources/months.json")
    @transient val w1 = Window.orderBy("monthNumber")
    val indexedDF = df.withColumn("monthNumber", monotonically_increasing_id() + 1)
    val lagIndexDF = indexedDF
      .where(!$"flag")
      .withColumn("prevFalseMonthNumber", lag("monthNumber", 1, 0).over(w1))
    val resultDF = indexedDF.as("indexedDF")
      .join(lagIndexDF.as("lagIndexDF"), $"indexedDF.monthNumber" > $"lagIndexDF.prevFalseMonthNumber" && $"indexedDF.monthNumber" <= $"lagIndexDF.monthNumber", "left")
      .select($"indexedDF.month", $"lagIndexDF.month".as("nextOrCurrentFalse"))
      .orderBy($"indexedDF.monthNumber".asc)
    resultDF.show
  }

  def test2() = {
    val df = spark.read.parquet("data/generated/ts_data")
    val q = df.select("timestamp").where($"part" > 50 && $"timestamp" > "2018-12-01 18:00:00")
    q.explain(true)
    df.printSchema()
    df.show
    q.show
  }

  def generateTimestampData(start: Long, n: Int): DataFrame = {
    val rnd = scala.util.Random
    val rdd = new RandomRDD(spark.sparkContext, n, spark.sparkContext.statusTracker.getExecutorInfos.length, Row(rnd.nextInt(3).toLong))
    val schema = StructType(Seq(StructField("timestamp", LongType, false)))
    @transient val upToCurrentWindow = Window.rowsBetween(Window.unboundedPreceding, Window.currentRow)
    @transient val orderedWindow = Window.orderBy("timestamp")
    spark.createDataFrame(rdd, schema)
      .withColumn("timestamp", from_unixtime(sum($"timestamp").over(upToCurrentWindow) + start))
      .withColumn("eventID", concat(typedLit("event"), row_number().over(orderedWindow)))
      .withColumn("part", floor(row_number().over(orderedWindow) / 100))
  }

  def writeGeneratedData(): Unit = {
    generateTimestampData(java.sql.Timestamp.valueOf("2018-12-01 09:00:00").getTime / 1000L, 1000000)
      .repartition(27)
      .write
      .partitionBy("part")
      .option("compression", "snappy")
      .mode(SaveMode.Overwrite)
      .format("parquet")
      .save("data/generated/ts_data_1M")
  }

  def main(args: Array[String]): Unit = {
    writeGeneratedData()
  }
}
