package io.ubilab.result.model

final case class OwnerId (value: Int) extends AnyVal {
  override def toString: String = value.toString
}
