package com.stuq.chapter02

import _root_.kafka.serializer.StringDecoder
import com.stuq.nginx.parser.NginxParser
import org.apache.spark.SparkConf
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka.KafkaUtils

import scala.collection.mutable.{ArrayBuffer, SynchronizedBuffer}

/**
 * 4/25/16 WilliamZhu(allwefantasy@gmail.com)
 */
object StuqExampleBadCase {
  def main(args: Array[String]) = {

    val conf = new SparkConf().setAppName("测试Streaming应用")
    val isDebug = true
    val duration = 5
    if (isDebug) {
      conf.setMaster("local[2]")
    }
    val ssc = new StreamingContext(conf, Seconds(duration))

    val input = if (isDebug) new TestInputStream[String](ssc, Mock.items, 1)
    else {
      KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](ssc,
        Map("metadata.broker.list" -> "broker1"),
        Set("topic")
      ).map(f => f._2)
    }

    //Transform
    val result = input.map { nginxLogLine =>
      val items = NginxParser.parse(nginxLogLine)
      items(2).split("/")(2)
    }

    //这是错误的做法哦。。。。。
    val collectList = new ArrayBuffer[String]()

    result.foreachRDD { rdd =>
      rdd.map(line => collectList += line ).count()
    }

    println(collectList)

    ssc.start()
    ssc.awaitTermination()


  }
}
