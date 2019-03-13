package com.example.random

import org.apache.spark.rdd.RDD
import org.apache.spark.{Partition, SparkContext, TaskContext}

import scala.reflect.ClassTag


// The RDD will parallelize the workload across `numSlices`
final class RandomRDD[A: ClassTag](@transient private val sc: SparkContext, numValues: Int, numSlices: Int, random: => A) extends RDD[A](sc, deps = Seq.empty) {

  // Based on the item and executor count, determine how many values are
  // computed in each executor. Distribute the rest evenly (if any).
  private val valuesPerSlice = numValues / numSlices
  private val slicesWithExtraItem = numValues % numSlices

  // Just ask the partition for the data
  override def compute(split: Partition, context: TaskContext): Iterator[A] =
    split.asInstanceOf[RandomPartition[A]].values

  // Generate the partitions so that the load is as evenly spread as possible
  // e.g. 10 partition and 22 items -> 2 slices with 3 items and 8 slices with 2
  override protected def getPartitions: Array[Partition] =
    ((0 until slicesWithExtraItem).view.map(new RandomPartition[A](_, valuesPerSlice + 1, random)) ++
      (slicesWithExtraItem until numSlices).view.map(new RandomPartition[A](_, valuesPerSlice, random))).toArray

}