package io.ubilab.result.model

import java.util.Date

case class EventResult(
  status:    EventResultStatus, // created | received | seen
  idOwner:   Int,
  createdAt: Date
)

case class Result(
  id:              ResultId,
  idOwner:         Int,
  idRecipients:    List[Int],
  isSeen:          Boolean,
  eventResults:    List[EventResult],
  contentOfResult: String
)
