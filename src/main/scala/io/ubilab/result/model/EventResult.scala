package io.ubilab.result.model

import java.time.Instant

final case class EventResult(
  status:    EventResultStatus,
  idOwner:   OwnerId,
  createdAt: Instant
)
