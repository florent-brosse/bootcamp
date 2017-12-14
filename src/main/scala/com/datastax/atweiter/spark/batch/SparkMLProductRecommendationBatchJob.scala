package com.datastax.atweiter.spark.batch

import ch.qos.logback.classic.{Level, Logger}
import org.apache.spark.ml.feature.{IndexToString, StringIndexer}
import org.apache.spark.ml.recommendation.ALS
import org.apache.spark.sql.{SaveMode, SparkSession}
import org.apache.spark.sql.cassandra._
import org.slf4j.LoggerFactory


class SparkMLProductRecommendationBatchJob {
}

object SparkMLProductRecommendationBatchJob {
  def main(args: Array[String]): Unit = {
    val root: Logger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).asInstanceOf[Logger]
    root.setLevel(Level.ERROR)

    val session = SparkSession.builder().appName("SparkMLProductRecommendationBatchJob").config("spark.sql.crossJoin.enabled","true").master("local[2]").getOrCreate()




    val df = session.read.cassandraFormat("review", "atwater").load().select("product_id","customer_id","rating").coalesce(1).cache()

    //df.show(false)


    // prepare features should transform UUID en int for ALS
    val indexer = new StringIndexer()
      .setInputCol("product_id")
      .setOutputCol("product")

    val indexer2 = new StringIndexer()
      .setInputCol("customer_id")
      .setOutputCol("customer")

    val indexed = indexer.fit(df).transform(df)
    val indexed2 = indexer2.fit(indexed).transform(indexed).cache()

    // Build the recommendation model using ALS on the training data
    val als = new ALS()
      .setMaxIter(5)
      .setRegParam(0.01)
      .setImplicitPrefs(true)
      .setUserCol("customer")
      .setItemCol("product")
      .setRatingCol("rating")

    val model = als.fit(indexed2)


    // compute all products with all customers table without rank
    val product = indexed2.select("product").distinct()
    val customer = indexed2.select("customer").distinct()
    val crossJoin = product.join(customer)
    val data = crossJoin.except(indexed2.select("product","customer"))

    //prediction
    val predictions = model.transform(data)

    // find ids
    val converter = new IndexToString()
      .setInputCol("product")
      .setOutputCol("product_id")

    val converter2 = new IndexToString()
      .setInputCol("customer")
      .setOutputCol("customer_id")

    val converted = converter.transform(predictions)
    val converted2 = converter2.transform(converted).select("product_id","customer_id","prediction")

    //keep only when a score is > 0.01
    converted2.filter("prediction > 0.01").createOrReplaceTempView("predictions")

    //keep only 5 bests results
    val response = session.sql("""SELECT product_id,customer_id,prediction FROM
                  (SELECT product_id,customer_id,prediction,dense_rank()
                  OVER (PARTITION BY customer_id ORDER BY prediction DESC) as rank
                  FROM predictions) tmp
                  WHERE rank <= 5""")

    //response.show(false)
    response.write.cassandraFormat("prediction", "atwater").option("confirm.truncate",true).mode(SaveMode.Overwrite).save()

    session.stop()

  }
}