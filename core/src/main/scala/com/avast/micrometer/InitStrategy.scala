package com.avast.micrometer

sealed trait InitStrategy {
  def init(initFunction: => Unit): Unit
}

object InitStrategy {
  final val InitEnv: String = "CATS_MICROMETER_INIT"

  /** Returns concrete `InitStrategy` instance according to CATS_MICROMETER_INIT environment variable. */
  def fromEnv(): InitStrategy = {
    if (System.getenv(InitEnv) == "true") InitStrategy.DoInit else NoOp
  }

  /** This InitStrategy does nothing. */
  case object NoOp extends InitStrategy {
    override def init(initFunction: => Unit): Unit = ()
  }

  /** This InitStrategy does the initialization as the specific metric requires. */
  case object DoInit extends InitStrategy {
    override def init(initFunction: => Unit): Unit = initFunction
  }
}
