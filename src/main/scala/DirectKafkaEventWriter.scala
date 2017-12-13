import java.util.{Calendar, UUID}

import com.datastax.driver.core.Session
import com.datastax.driver.core.utils.UUIDs
import com.datastax.spark.connector.SomeColumns
import com.datastax.spark.connector.cql.CassandraConnector
import com.datastax.spark.connector.streaming._
import kafka.serializer.StringDecoder
import org.apache.spark.SparkConf
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka._

import scala.collection.mutable.ListBuffer


object DirectKafkaEventWriter {

  case class Events(
                     year: Int,
                     month: Int,
                     day: Int,
                     hour: Int,
                     minute: Int,
                     second: Int,
                     uuid: UUID,
                     recordTypeCode: String,
                     value: String) extends Serializable

  case class EventAgg(
                       rollupType: String,
                       rollupTime: String,
                       rollupGroupingId: String,
                       rollupSubGroupingId: String,
                       rollupValue: Int) extends Serializable

  def main(args: Array[String]) {

    val Array(brokers, topics) = Array("localhost:9092", "events")

    val sparkConf = new SparkConf().setAppName("DirectKafkaEventWriter").setMaster("local[2]")
    val ssc = new StreamingContext(sparkConf, Seconds(10))

    val connector = CassandraConnector(ssc.sparkContext.getConf)

    val topicsSet = topics.split(",").toSet
    val kafkaParams = Map[String, String]("metadata.broker.list" -> brokers)
    val messages = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](
      ssc, kafkaParams, topicsSet)

    val lines = messages.map(l => {
      val value = l._2
      val calendar = Calendar.getInstance
      new Events(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH),
        calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND),
        UUIDs.random(), "click", value)
    }).cache()


    lines.saveToCassandra("atwater", "raw_data", SomeColumns("year", "month", "day", "hour", "minute", "second", "uuid", "record_type_code", "value"))
    val count = lines.map(r => (r.recordTypeCode, 1))
    val event = count.reduceByKey(_ + _).cache()
    val eventAggSec = event.map(l => {
      val calendar = Calendar.getInstance
      calendar.set(Calendar.MILLISECOND, 0)
      new EventAgg("second", calendar.getTime.getTime.toString, l._1, "", l._2)
    })

    def mergeRollups(session: Session, partition: Iterator[EventAgg]): Iterator[EventAgg] = {
      var mergedRollups = ListBuffer[EventAgg]()
      val prepare = session.prepare("select rollup_value from atwater.rollup where rollup_type=? and rollup_time=? and rollup_grouping_id=? and rollup_sub_grouping_id=?")
      partition.foreach(l => {
        val bind = prepare.bind(l.rollupType, l.rollupTime, l.rollupGroupingId, l.rollupSubGroupingId)
        var oldValue = l.rollupValue
        val row = session.execute(bind).one()
        if (row != null) {
          oldValue += row.getInt("rollup_value")
        }
        mergedRollups.append(new EventAgg(l.rollupType, l.rollupTime, l.rollupGroupingId, l.rollupSubGroupingId, oldValue))
      })
      mergedRollups.iterator
    }

    val eventAggSec2 = eventAggSec.mapPartitions(
      partition => {
        val mergedRollup = connector.withSessionDo(session => {
          mergeRollups(session, partition)
        })
        mergedRollup
      }
    )


    eventAggSec2.saveToCassandra("atwater", "rollup", SomeColumns("rollup_type", "rollup_time", "rollup_grouping_id", "rollup_sub_grouping_id", "rollup_value"))
    val eventAggMin = event.map(l => {
      val calendar = Calendar.getInstance
      calendar.set(Calendar.SECOND, 0)
      calendar.set(Calendar.MILLISECOND, 0)
      new EventAgg("minute", calendar.getTime.getTime.toString, l._1, "", l._2)
    })


    val eventAggMin2 = eventAggMin.mapPartitions(
      partition => {
        val mergedRollup = connector.withSessionDo(session => {
          mergeRollups(session, partition)
        })
        mergedRollup
      }
    )


    eventAggMin2.saveToCassandra("atwater", "rollup", SomeColumns("rollup_type", "rollup_time", "rollup_grouping_id", "rollup_sub_grouping_id", "rollup_value"))

    val eventAggHour = event.map(l => {
      val calendar = Calendar.getInstance
      calendar.set(Calendar.SECOND, 0)
      calendar.set(Calendar.MILLISECOND, 0)
      calendar.set(Calendar.MINUTE, 0)
      new EventAgg("hour", calendar.getTime.getTime.toString, l._1, "", l._2)
    })
    val eventAggHour2 = eventAggHour.mapPartitions(
      partition => {
        val mergedRollup = connector.withSessionDo(session => {
          mergeRollups(session, partition)
        })
        mergedRollup
      }
    )


    eventAggHour2.saveToCassandra("atwater", "rollup", SomeColumns("rollup_type", "rollup_time", "rollup_grouping_id", "rollup_sub_grouping_id", "rollup_value"))

    val eventAggDay = event.map(l => {
      val calendar = Calendar.getInstance
      calendar.set(Calendar.SECOND, 0)
      calendar.set(Calendar.MILLISECOND, 0)
      calendar.set(Calendar.MINUTE, 0)
      calendar.set(Calendar.HOUR_OF_DAY, 0)
      new EventAgg("day", calendar.getTime.getTime.toString, l._1, "", l._2)
    })
    val eventAggDay2 = eventAggDay.mapPartitions(
      partition => {
        val mergedRollup = connector.withSessionDo(session => {
          mergeRollups(session, partition)
        })
        mergedRollup
      }
    )


    eventAggDay2.saveToCassandra("atwater", "rollup", SomeColumns("rollup_type", "rollup_time", "rollup_grouping_id", "rollup_sub_grouping_id", "rollup_value"))

    val eventAggMonth = event.map(l => {
      val calendar = Calendar.getInstance
      calendar.set(Calendar.SECOND, 0)
      calendar.set(Calendar.MILLISECOND, 0)
      calendar.set(Calendar.MINUTE, 0)
      calendar.set(Calendar.HOUR_OF_DAY, 0)
      calendar.set(Calendar.DAY_OF_MONTH, 0)
      new EventAgg("month", calendar.getTime.getTime.toString, l._1, "", l._2)
    })
    val eventAggMonth2 = eventAggMonth.mapPartitions(
      partition => {
        val mergedRollup = connector.withSessionDo(session => {
          mergeRollups(session, partition)
        })
        mergedRollup
      }
    )


    eventAggMonth2.saveToCassandra("atwater", "rollup", SomeColumns("rollup_type", "rollup_time", "rollup_grouping_id", "rollup_sub_grouping_id", "rollup_value"))

    val eventAggYear = event.map(l => {
      val calendar = Calendar.getInstance
      calendar.set(Calendar.SECOND, 0)
      calendar.set(Calendar.MILLISECOND, 0)
      calendar.set(Calendar.MINUTE, 0)
      calendar.set(Calendar.HOUR_OF_DAY, 0)
      calendar.set(Calendar.DAY_OF_MONTH, 0)
      calendar.set(Calendar.MONTH, 0)
      new EventAgg("year", calendar.getTime.getTime.toString, l._1, "", l._2)
    })
    val eventAggYear2 = eventAggYear.mapPartitions(
      partition => {
        val mergedRollup = connector.withSessionDo(session => {
          mergeRollups(session, partition)
        })
        mergedRollup
      }
    )


    eventAggYear2.saveToCassandra("atwater", "rollup", SomeColumns("rollup_type", "rollup_time", "rollup_grouping_id", "rollup_sub_grouping_id", "rollup_value"))

    ssc.start()
    ssc.awaitTermination()
  }
}