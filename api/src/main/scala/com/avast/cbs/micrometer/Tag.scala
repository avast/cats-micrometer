package com.avast.cbs.micrometer

final case class Tag(key: String, value: String)

object Tag {
  def apply(key: String, value: Number): Tag = Tag(key, value.toString)
}
