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
    val rdd: RandomRDD[Row] = new RandomRDD(spark.sparkContext, n, spark.sparkContext.statusTracker.getExecutorInfos.length, Row(rnd.nextInt(3).toLong))
    val schema = StructType(Seq(StructField("timestamp", LongType, false)))
    @transient val upToCurrentWindow = Window.rowsBetween(Window.unboundedPreceding, Window.currentRow)
    @transient val orderedWindow = Window.orderBy("timestamp")
    spark.createDataFrame(rdd, schema)
      .withColumn("timestamp", from_unixtime(sum($"timestamp").over(upToCurrentWindow) + start))
      .withColumn("eventID", concat(typedLit("event"), row_number().over(orderedWindow)))
      .withColumn("part", floor(row_number().over(orderedWindow) / 1000))
  }

  def writeGeneratedData(path: String, n: Int): Unit = {
    generateTimestampData(java.sql.Timestamp.valueOf("2018-12-01 09:00:00").getTime / 1000L, n)
      .repartition(27)
      .write
      .partitionBy("part")
      .option("compression", "snappy")
      .mode(SaveMode.Overwrite)
      .format("parquet")
      .save(path)
  }

  def getRddInfo(df: DataFrame) = {
    def incMap(m: Map[(String, Long), Int], s: (String, Long)) = if (m.contains(s)) m + (s -> (m(s) + 1)) else m + (s -> 1)
    df.rdd.mapPartitionsWithIndex{ case(i, rows) =>
      val s = rows.foldLeft((Map[(String, Long), Int](), 0)){ case(a, r) => (incMap(a._1, (r.getString(0), r.getLong(1))), a._2 + 1) }
      Iterator((i, s._1, s._1.size, s._2))
    }.toDF("partition_id", "pairs", "num_pairs", "num_rows")
  }

  def main(args: Array[String]): Unit = {
    val rnd = new scala.util.Random()
    val rndCat = udf(() => (rnd.nextInt(26) + 65).toChar.toString)
    //val df = spark.read.parquet("data/generated/ts_data_10K")
    val df = generateTimestampData(java.sql.Timestamp.valueOf("2018-12-01 09:00:00").getTime / 1000L, 10000)
      .withColumn("category", rndCat())
      .repartition(5, $"part", $"category")
    df
      .write
      .partitionBy("category")
      .mode(SaveMode.Overwrite)
      .parquet("data/generated/ts_data_no_categories_10K")
    spark.sparkContext.getConf.getAll.foreach(println)
    val df_written = spark.read.parquet("data/generated/ts_data_no_categories_10K")
    getRddInfo(df.select($"category", $"part")).show(false)
    getRddInfo(df_written.select($"category", $"part")).show(false)
  }
}
