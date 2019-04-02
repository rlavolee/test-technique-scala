package io.ubilab.result.model

sealed trait EventResultStatus

object EventResultStatus {
  final case object Created  extends EventResultStatus
  final case object Received extends EventResultStatus
  final case object Seen     extends EventResultStatus
  final case object UnSeen   extends EventResultStatus
}