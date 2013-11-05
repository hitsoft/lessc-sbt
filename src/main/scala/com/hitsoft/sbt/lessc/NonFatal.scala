package com.hitsoft.sbt.lessc

/** 2.10 shim for classifying Non fatal exceptions in exception handling */
private [lessc] object NonFatal {
  def apply(t: Throwable): Boolean = t match {
    case _: StackOverflowError => true
    case _: VirtualMachineError | _: ThreadDeath | _: InterruptedException | _: LinkageError  => false
    case _ => true
  }
  def unapply(t: Throwable): Option[Throwable] = if (apply(t)) Some(t) else None
}
