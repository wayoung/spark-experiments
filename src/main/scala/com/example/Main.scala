package com.example

import org.apache.spark.sql.functions._
import org.apache.spark.sql.expressions.Window

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

  def main(args: Array[String]) = {
    test2()
  }
}
