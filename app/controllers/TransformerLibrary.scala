package controllers

import akka.actor.Actor
import play.api.Logger
import Transformers._
import java.io.File
import play.api.libs.json.Json

sealed abstract class Event
case class GetTransformer(name:String) extends Event
case class PutTransformer(name:String,transformer:Transformer) extends Event

class TransformerLibrary extends Actor{
  var transformers:Map[String,Transformer] = loadInitialTransformers

  def loadInitialTransformers: Map[String, Transformer] = {
    val root: java.io.File = new File(ResponderLibrary.documentBase)
    if (root.exists()) {
      root.listFiles().filter(_.isDirectory).filter(parentDir => !parentDir.list().isEmpty).filter(parentDir => !parentDir.list(ExactMatchFilenameFilter.forName("transformers.info")).isEmpty)
        .map(parent => parent.getName -> loadTransformer(parent.listFiles(ExactMatchFilenameFilter.forName("transformers.info"))(0))).toMap
    }
    else {
      Map()
    }
  }

  def loadTransformer(file:File): Transformer = {
    Json.parse(scala.io.Source.fromFile(file).mkString).as[Transformer]
  }

  def receive = {
    case GetTransformer(name) => sender ! transformers.get(name)
    case PutTransformer(name, transformer) => createTransformer(name, transformer)
  }


  def createTransformer(name: String, transformer: Transformer) {
    transformers = transformers + (name -> transformer)
    val f = new File(ResponderLibrary.documentBase + "/" + name)
    if (!f.exists()) {
      f.mkdirs()
    }
    printToFile(new File(ResponderLibrary.documentBase + "/" + name + "/transformers.info")) {
      p => p.print(Json.stringify(Json.toJson(transformer)))
    }
    sender ! true
  }

  def printToFile(f: java.io.File)(op: java.io.PrintWriter => Unit) {
    if (!f.exists()) {
      f.createNewFile()
    }
    val p = new java.io.PrintWriter(f)
    try {
      op(p)
    }
    finally {
      p.close()
    }
  }
}
