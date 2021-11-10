package com.example.demo

import com.example.LocalSparkEnv
import com.example.random.RandomRDD
import org.apache.spark.sql.{DataFrame, Row, SaveMode}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.types._

object RandomLocalSpark extends LocalSparkEnv {

  import spark.implicits._

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

  def getRddInfo(df: DataFrame) = {
    def incMap(m: Map[(String, Long), Int], s: (String, Long)) = if (m.contains(s)) m + (s -> (m(s) + 1)) else m + (s -> 1)
    df.rdd.mapPartitionsWithIndex{ case(i, rows) =>
      val s = rows.foldLeft((Map[(String, Long), Int](), 0)){ case(a, r) => (incMap(a._1, (r.getString(0), r.getLong(1))), a._2 + 1) }
      Iterator((i, s._1, s._1.size, s._2))
    }.toDF("partition_id", "pairs", "num_pairs", "num_rows")
  }

  def randomTest(): Unit = {
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
