package util

import spinal.core._
import spinal.lib._

import scala.language.postfixOps

case class PairBundle[T <: Data, T2 <: Data](valueType: HardType[T], linkedType: HardType[T2]) extends Bundle {
  val A = valueType()
  val B = linkedType()
}
