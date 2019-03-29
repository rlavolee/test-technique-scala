package io.ubilab.result.model

final case class ResultId (value: Int) extends AnyVal {
  override def toString: String = value.toString
}
