package com.example

import org.apache.spark.SparkConf
import org.apache.spark.sql.{SparkSession, SparkSessionExtensions}

trait SparkEnv {

  lazy val spark = {
    val conf = new SparkConf()
      .setAppName("example")
      .setMaster("local[*]")
    type ExtensionsBuilder = SparkSessionExtensions => Unit
    def create(builder: ExtensionsBuilder): ExtensionsBuilder = builder
    val extension = create { extensions =>
      // extensions.injectParser((_, _) => MyCatalystSqlParser)
      ()
    }
    val session = SparkSession.builder().config(conf).getOrCreate()
    session.sparkContext.setLogLevel("ERROR")
    session
  }

}
