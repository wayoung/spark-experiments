package com.example.random

import org.apache.spark.Partition

import scala.reflect.ClassTag


// Each random partition will hold `numValues` items
final class RandomPartition[A: ClassTag](val index: Int, numValues: Int, random: => A) extends Partition {
  def values: Iterator[A] = Iterator.fill(numValues)(random)
}
