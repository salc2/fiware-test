package com.chucho

import akka.actor.{Actor, ActorSystem, Props}
import akka.stream.ActorMaterializer
import org.eclipse.paho.client.mqttv3.{IMqttMessageListener, MqttClient, MqttConnectOptions, MqttMessage}
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

import scala.io.StdIn
import scala.util.Random

/**
  * Created by g100536 on 31/01/17.
  */
object Device {
    lazy implicit val system = ActorSystem("reactive-kafka")
    lazy implicit val materializer = ActorMaterializer()
    import system.dispatcher

  def main(args: Array[String]): Unit = {
      val actors = for(x <- 1 to 10) yield system.actorOf(Props(classOf[DeviceActor],s"thermotank$x"))

    system.scheduler.schedule(5 seconds, 200 milliseconds, () => {
      actors foreach(_.!("Tick"))
    })

    StdIn.readLine()
  }

}


class DeviceActor(name:String) extends Actor{
  val random = new Random()
  val brokerHost = "tcp://10.97.244.183:1883"

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    super.preStart()
//    context.system.scheduler.schedule(second seconds,1 seconds,self,"Tick")
  }

  override def receive: Receive = {
    case _ => publish(s"""{"temperature": "${random.nextFloat()}"}""".stripMargin)
  }

  private val persistence = new MemoryPersistence
  private val sampleClient = new MqttClient(brokerHost, name, persistence)

  private val optCon = new MqttConnectOptions {
    setCleanSession(true)
  }
  System.out.println(s"$name Connecting to broker: $brokerHost")
  sampleClient.connect(optCon)

  def subscribe(topic:String, listener:IMqttMessageListener):Unit ={
    sampleClient.subscribe(topic,listener)
  }

  def publish(msg:String):Unit ={
    val topic = s"/calefon123/$name/attrs"
    println(s"publishing ... $topic")
    sampleClient.publish(topic,new MqttMessage(msg.getBytes))
  }
}
