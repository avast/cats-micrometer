package com.avast.cbs.micrometer

trait ToDouble[-A] {
  def toDouble(value: A): Double
}

object ToDouble {

  def apply[A](implicit ev: ToDouble[A]): ToDouble[A] = ev

  implicit val intToDouble: ToDouble[Int] = _.toDouble
  implicit val floatToDouble: ToDouble[Float] = _.toDouble
  implicit val lngToDouble: ToDouble[Long] = _.toDouble
  implicit val numberToDouble: ToDouble[Number] = _.doubleValue()

}
