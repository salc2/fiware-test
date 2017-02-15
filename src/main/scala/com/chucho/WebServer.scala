package com.chucho

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.PathMatchers
import akka.http.scaladsl.server.Directives.{complete, extractUpgradeToWebSocket, get, getFromResource, path, _}
import akka.stream.ActorMaterializer
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.{Cancel, Request}
import akka.stream.scaladsl.{Sink, Source}

import scala.annotation.tailrec
import scala.io.StdIn
import scala.util.Try


/**
  * Created by g100536 on 01/02/17.
  */
object WebServer {


  implicit val system = ActorSystem("webserver-system")
  implicit val materializer = ActorMaterializer()
  private implicit val executionContext = system.dispatcher
  val actor = system.actorOf(Props(classOf[ActorAccumulator]))


  val route =
    pathSingleSlash{
      get {
        getFromResource("index.html")
      }
    } ~
      path("assets" / Remaining) { path =>
        getFromResource(path)
      } ~
    path("accumulate"){
      post {
        entity(as[String]) { body =>
          complete{
            actor ! body
            "{}"
          }
        }
      }
    }  ~
      path("ws"){
        extractUpgradeToWebSocket { upgrade =>
          val sink = Sink.ignore
          val source = Source.actorPublisher(Props(classOf[FiWarePublisher],actor))
          complete(upgrade.handleMessagesWithSinkSource(sink,source))
        }
      }



  def main(args:Array[String]):Unit = {

    val bindingFuture = Http().bindAndHandle(route, "10.38.67.155", 8080)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine()
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }

}


class ActorAccumulator extends Actor with ActorLogging{
  private var subscribers:Set[ActorRef] = Set.empty
  override def receive: Receive = {
    case FiWare.Subscribe => subscribers = subscribers + sender()
    case msg => {
      subscribers.foreach(_.!(msg))
      log.info("Message:> {}",msg.toString)
    }
  }
}

object FiWare{
  object Subscribe
}
class FiWarePublisher(mainActor:ActorRef)
  extends ActorPublisher[Message] with ActorLogging{

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    super.preStart()
    mainActor ! FiWare.Subscribe
  }

  val MaxBufferSize = 100
  var buf = Vector.empty[String]

  def accumulate(msg:String) {
    if (buf.isEmpty && totalDemand > 0)
      onNext(TextMessage(msg))
    else {
      buf :+= msg
      deliverBuf()
    }
  }

  override def receive: Receive = {
    case Request(_) => deliverBuf()
    case Cancel => context.stop(self)
    case msg:String => accumulate(msg)
  }


  @tailrec final def deliverBuf(): Unit =
    if (totalDemand > 0) {
      if (totalDemand <= Int.MaxValue) {
        val (use, keep) = buf.splitAt(totalDemand.toInt)
        buf = keep
        use.foreach( ms => onNext(TextMessage(ms)) )
      } else {
        val (use, keep) = buf.splitAt(Int.MaxValue)
        buf = keep
        use.foreach( ms => onNext(TextMessage(ms)) )
        deliverBuf()
      }
    }
}

