package com.github.rgafiyatullin.akka_stream_scodec

import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.util.ByteString
import scodec.{Attempt, Codec}
import scodec.interop.akka._

final case class SCEncodeStage[T](codec: Codec[T]) extends GraphStage[FlowShape[T, ByteString]] {
  val inlet: Inlet[T] = Inlet("In:PDU")
  val outlet: Outlet[ByteString] = Outlet("Out:ByteString")

  override def shape: FlowShape[T, ByteString] = FlowShape.of(inlet, outlet)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {
      setHandler(inlet, new InHandler {
        override def onPush(): Unit = {
          val value = grab(inlet)
          val encodeAttempt = codec.encode(value)
          encodeAttempt match {
            case Attempt.Successful(bits) =>
              push(outlet, bits.bytes.toByteString)

            case Attempt.Failure(reason) =>
              fail(outlet, SCEncodeError(reason, codec))
          }
        }
      })

      setHandler(outlet, new OutHandler {
        override def onPull(): Unit =
          pull(inlet)
      })
    }
}