package com.example

import org.apache.spark.sql.SparkSession

trait SparkEnv {

  lazy val spark = SparkSession.builder().getOrCreate()

}
