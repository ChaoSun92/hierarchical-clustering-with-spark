import org.apache.spark.mllib.clustering.HierarchicalClustering
import org.apache.spark.mllib.linalg.{Vector, Vectors}
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}

import scala.util.parsing.json.JSONObject

object HierarchicalClusteringApp {

  def main(args: Array[String]) {

    val master = args(0)
    val maxCores = args(1)
    val rows = args(2).toInt
    val dimension = args(3).toInt
    val numClusters = args(4).toInt
    val numPartitions = args(5).toInt

    val appName = s"${this.getClass().getSimpleName},maxCores,${maxCores},rows:${rows}:dim:${dimension},"
    val conf = new SparkConf()
        .setAppName(appName)
        .setMaster(master)
        .set("spark.cores.max", maxCores)
    val sc = new SparkContext(conf)

    val data = generateData(sc, numPartitions, rows, dimension, numClusters)
    data.repartition(numPartitions)
    data.cache
    val model = HierarchicalClustering.train(data, numClusters)

    val result = Map(
      "trainMilliSec" -> model.trainTime.toString,
      "rows" -> rows.toString,
      "dimension" -> dimension.toString,
      "numClusters" -> numClusters.toString,
      "numPartitions" -> numPartitions.toString,
      "maxCores" -> maxCores.toString
    )
    println(JSONObject(result).toString())
    model.clusterTree.toSeq().foreach(tree => println(tree.toString()))
  }


  def generateData(sc: SparkContext,
    numPartitions: Int,
    rows: Int,
    dim: Int,
    numClusters: Int): RDD[Vector] = {
    sc.parallelize((1 to rows.toInt), numPartitions).map { i =>
      val idx = (i % (numClusters - 1)) + 1
      val indexes = for (j <- 0 to (Math.floor(dim / numClusters).toInt - 1)) yield j * numClusters + idx
      val values: Array[Double] = (0 to (dim - 1)).map { j =>
        val value = indexes.contains(j) match {
          case true => idx + idx * 0.01 * Math.random()
          case false => 0.0
        }
        value
      }.toArray
      Vectors.dense(values)
    }
  }
}
