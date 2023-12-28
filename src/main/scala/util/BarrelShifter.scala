package util

import spinal.core._
import spinal.lib._
import spinal.core.sim._

import scala.language.postfixOps
import scala.util.Random

object BarrelShifter {

  def right(x: Bits, shift: UInt) = {
    require(shift.getWidth == log2Up(x.getWidth))
    val xExt = x ## x
    val xShift = xExt >> shift
    xShift.takeLow(x.getWidth)
  }

  def left(x: Bits, shift: UInt) = {
    require(shift.getWidth == log2Up(x.getWidth))
    val xExt = x ## x
    val xShift = xExt << shift
    xShift.dropLow(x.getWidth)
  }

  def rightT[T <: Data](x: Vec[T], shift: UInt) = {
    require(shift.getWidth == log2Up(x.length))
    val xBin = x.map(_.asBits.asBools).transpose
    val xShift = xBin.map(v => right(v.asBits, shift).asBools).transpose
    val ret = cloneOf(x)
    (ret, xShift).zipped.foreach((dst, src) => dst.assignFromBits(src.asBits))
    ret
  }

  def leftT[T <: Data](x: Vec[T], shift: UInt) = {
    require(shift.getWidth == log2Up(x.length))
    val xBin = x.map(_.asBits.asBools).transpose
    val xShift = xBin.map(v => left(v.asBits, shift).asBools).transpose
    val ret = cloneOf(x)
    (ret, xShift).zipped.foreach((dst, src) => dst.assignFromBits(src.asBits))
    ret
  }

  def main(args: Array[String]): Unit = {
//    SpinalVerilog(new Component {
//      val shift = in UInt (3 bits)
//      val x = in Vec(UInt(64 bits), 8)
//      val y = out Vec(UInt(64 bits), 8)
//      y := rightT(x, shift)
//    })

    SpinalVerilog(new Component {
      val shift = in Vec(UInt(3 bits), 8)
      val x = in Vec(UInt(64 bits), 8)
      val y = out Vec(UInt(64 bits), 8)
      y := Vec(shift.map(sel => x(sel)))
    })
  }
}
