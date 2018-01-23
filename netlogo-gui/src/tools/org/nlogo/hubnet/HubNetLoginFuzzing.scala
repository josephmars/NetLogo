// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.hubnet

import java.net.Socket
import java.io.{ ObjectOutputStream}
import java.util.concurrent.Executors // , ExecutorService, TimeUnit, LinkedBlockingQueue}

import org.nlogo.api.Version
import org.nlogo.util.ClassLoaderObjectInputStream
import protocol.{ ActivityCommand, EnterMessage, ExitMessage, HandshakeFromClient, HandshakeFromServer }

import scala.util.Random

object HubNetLoginFuzzing extends App {
  sealed trait Action
  case class SendAction(send: Send) extends Action
  case object WaitOneMessage extends Action

  sealed trait Send
  case class SendVersion(version: String) extends Send
  case class SendHandshake(clientId: String) extends Send
  case object SendEnter extends Send
  case object SendExit extends Send
  case class SendActivityMessage(tag: String, value: AnyRef) extends Send

  lazy val threadPool = Executors.newFixedThreadPool(4)

  val random = new Random(0)

  val actions: Iterable[ActionRunnable] =
    Iterator
      .continually(makeActions(random))
      .map(makeActionRunnables _)
      .take(800)
      .toSeq

  actions.foreach(threadPool.submit _)

  threadPool.shutdown()

  while (! threadPool.isTerminated()) { }

  actions.foreach { a =>
    println(a.result)
  }

  lazy val messagePool: Seq[(Random => Send)] =
    Seq(
    { (r) => SendVersion(Version.version) },
    { (r) => SendHandshake(randString(r)) },
    { (r) => SendHandshake(null) },
    { (r) => SendEnter },
    { (r) => SendActivityMessage("Choice", Double.box(3)) },
    { (r) => SendExit })

  def makeActions(rand: Random): Seq[Action] = {
    (1 to rand.nextInt(10))
      .map(_ => messagePool(rand.nextInt(messagePool.length)))
      .flatMap { messageMaker =>
          val madeMessage = messageMaker(rand)
          madeMessage match {
            case _: SendActivityMessage | SendExit | SendEnter => Seq(SendAction(madeMessage))
            case _ => Seq(SendAction(madeMessage), WaitOneMessage)
          }
      }
  }

  private def randString(rand: Random): String = {
    (1 to 5).map(_ => rand.nextPrintableChar()).mkString("")
  }

  def makeActionRunnables(actions: Seq[Action]): ActionRunnable =
    new ActionRunnable(actions)

  class ActionRunnable(actions: Seq[Action]) extends Runnable {
    sealed trait Event
    case class PendingSend(send: Send) extends Event
    case class Sent(send: Send) extends Event
    case class Received(any: AnyRef) extends Event {
      override def toString = any match {
        case hs: HandshakeFromServer => "Received(HandshakeFromServer(" + hs.activityName + "...))"
        case _ => "Received(" + any.toString + ")"
      }
    }
    case class ExceptionRaised(e: Exception) extends Event
    case object Timeout extends Event
    case object SocketClosed extends Event

    var events: List[AnyRef] = Nil
    var remainingActions = actions

    def makeConnection(): Option[(Socket, ObjectOutputStream, ClassLoaderObjectInputStream)] = {
      var socket: Socket = null
      var out: ObjectOutputStream = null
      var in: ClassLoaderObjectInputStream = null

      try {
        while (socket == null || out == null || in == null) {
          socket = new Socket("127.0.0.1", 9173) { setSoTimeout(250) }
          while (! socket.isConnected()) {}

          while (socket != null && out == null) {
            try {
              out = new ObjectOutputStream(socket.getOutputStream)
            } catch {
              case _: java.net.SocketException =>
                socket = null
                println("output stream opening exception...")
            }
          }
          while (socket != null && in == null) {
            try {
              in = ClassLoaderObjectInputStream(Thread.currentThread.getContextClassLoader, socket.getInputStream)
            } catch {
              case _: java.net.SocketException =>
                socket = null
                println("input stream opening exception...")
            }
          }
        }
        Some((socket, out, in))
      } catch {
        case e: Exception =>
          ExceptionRaised(e) :: events
          None
      }
    }

    override def run() {
      makeConnection().foreach {
        case (socket, out, in) =>
          try {
            while (remainingActions.nonEmpty && ! socket.isClosed()) {
              remainingActions.head match {
                case SendAction(send: Send) =>
                  events = PendingSend(send) :: events
                  send match {
                    case SendVersion(v) =>
                      out.writeObject(v); out.flush()
                    case SendHandshake(clientId) =>
                      out.writeObject(HandshakeFromClient(clientId, "COMPUTER")); out.flush()
                    case SendEnter =>
                      out.writeObject(EnterMessage); out.flush()
                    case SendActivityMessage(tag, value) =>
                      out.writeObject(ActivityCommand(tag, value)); out.flush()
                    case SendExit =>
                      out.writeObject(ExitMessage("DONE!")); out.flush()
                  }
                  events = Sent(send) :: events.tail
                    case WaitOneMessage =>
                      try {
                        val obj = in.readObject()
                        events = Received(obj) :: events
                      } catch {
                        case to: java.net.SocketTimeoutException =>
                          events = Timeout :: events
                      }
              }
              remainingActions = remainingActions.tail
            }
            if (socket.isClosed()) {
              events = SocketClosed :: events
            }
          } catch {
            case e: Exception =>
              events = ExceptionRaised(e) :: events
          } finally {
            if (socket != null) {
              socket.close()
            }
          }
      }
    }

    def result: String = {
      actions.mkString(",") + " => " + events.reverse.mkString(",")
    }
  }

  // messages:
  // Version (right / wrong)
  // HandshakeFromClient (clientId, "COMPUTER")
  // EnterMessage
  // ActivityCommand (tag, value)
}
