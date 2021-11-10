package com.example.demo

import com.example.SparkEnv

object Main extends SparkEnv {

  import spark.implicits._

  def main(args: Array[String]): Unit = {
    if (args.length <= 1) throw new Exception("Missing input and output paths")
    val (inputPath, outputPath) = (args(0), args(1))
    spark
      .read
      .parquet(inputPath)
      .write
      .parquet(outputPath)
  }

}
