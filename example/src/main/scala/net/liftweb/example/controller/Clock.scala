package net.liftweb.example.controller

/*                                                *\
  (c) 2007 WorldWide Conferencing, LLC
  Distributed under an Apache License
  http://www.apache.org/licenses/LICENSE-2.0
\*                                                 */

import net.liftweb.http._
import net.liftweb.util._
import net.liftweb.util.Helpers._
import scala.xml.{Text}

class Clock extends ControllerActor {
  case class Tick
  ActorPing.schedule(this, Tick, 10000L) // schedule a ping every 10 seconds so we redraw
  
  def render = bind(Map("time" -> Text(timeNow.toString)))
  
  override def lowPriority : PartialFunction[Any, Unit] = {
    case Tick() => 
    try {
    reRender // tell the component to redraw itself
    ActorPing.schedule(this, Tick, 10000L) // schedule an update in 10 seconds
    } catch {
     case e => e.printStackTrace
    }
    loop
  }
}
