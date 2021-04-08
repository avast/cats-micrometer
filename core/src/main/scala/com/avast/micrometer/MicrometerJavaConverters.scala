package com.avast.micrometer

import com.avast.micrometer.api.Tag
import io.micrometer.core.instrument.{Tag => JavaTag}

import java.lang.{Iterable => JavaIterable}
import scala.jdk.CollectionConverters._

private[micrometer] object MicrometerJavaConverters {
  implicit class ScalaTagsConverter(private val tags: Iterable[Tag]) extends AnyVal {
    def asJavaTags: JavaIterable[JavaTag] = {
      tags.map { t =>
        new JavaTag {
          override def getKey: String = t.key
          override def getValue: String = t.value
        }
      }.asJava
    }
  }

  implicit class JavaTagsConverter(private val tags: JavaIterable[JavaTag]) extends AnyVal {
    def asScalaTags: Seq[Tag] = {
      tags.asScala.map { t =>
        Tag(t.getKey, t.getValue)
      }.toSeq
    }
  }

}
