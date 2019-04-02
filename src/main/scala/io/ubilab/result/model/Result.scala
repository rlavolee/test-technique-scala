package io.ubilab.result.model

final case class Result(
  id:              ResultId,
  idOwner:         OwnerId,
  idRecipients:    List[Int],
  isSeen:          Boolean,
  contentOfResult: String
)
