package com.avast.micrometer

import com.avast.micrometer.api.Tag
import io.micrometer.core.instrument.Tag as JavaTag

import java.lang.Iterable as JavaIterable
import java.util
import scala.jdk.CollectionConverters.*

private[micrometer] object MicrometerJavaConverters {
  implicit class ScalaTagsConverter(private val tags: Iterable[Tag]) extends AnyVal {
    def asJavaTags: JavaIterable[JavaTag] = {
      // The explicit conversion to `util.ArrayList` is needed - otherwise Java has some difficulties with comparison
      new util.ArrayList(tags.map { t =>
        JavaTag.of(t.key, t.value)
      }.asJavaCollection)
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
