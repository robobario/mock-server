package controllers

import akka.actor.Actor
import play.api.Logger

sealed abstract class Event
case class GetTransformer(name:String) extends Event
case class PutTransformer(name:String,transformer:Transformer) extends Event

class TransformerLibrary extends Actor{
  var transformers:Map[String,Transformer] = Map();
  def receive = {
    case GetTransformer(name) => sender ! transformers.get(name)
    case PutTransformer(name, transformer) => transformers = transformers + (name -> transformer);sender ! true
  }
}
