package xilinx.FloatingIP

import spinal.core._
import spinal.lib._

import scala.language.postfixOps

class one_op_stub(width: Int, name: String, lat: Int) extends BlackBox {
  val latency = lat
  val io = new Bundle {
    val aclk = if (lat != 0) in Bool() else null
    val a = slave(Flow(Bits(width bits)))
    val r = master(Flow(Bits(width bits)))
  }
  noIoPrefix()

  io.a.setName("s_axis_a")
  io.r.setName("m_axis_result")

  util.AxiStreamSpecRenamer(io.a)
  util.AxiStreamSpecRenamer(io.r)
  if (lat != 0) mapClockDomain(clock = io.aclk)
  this.setDefinitionName(name)
}

class two_op_stub(width: Int, name: String, lat: Int) extends BlackBox {
  val latency = lat
  val io = new Bundle {
    val aclk = if (lat != 0) in Bool() else null
    val a = slave(Flow(Bits(width bits)))
    val b = slave(Flow(Bits(width bits)))
    val r = master(Flow(Bits(width bits)))
  }
  noIoPrefix()

  io.a.setName("s_axis_a")
  io.b.setName("s_axis_b")
  io.r.setName("m_axis_result")

  util.AxiStreamSpecRenamer(io.a)
  util.AxiStreamSpecRenamer(io.b)
  util.AxiStreamSpecRenamer(io.r)
  if (lat != 0) mapClockDomain(clock = io.aclk)
  this.setDefinitionName(name)
}

class three_op_stub(width:Int, name: String, lat: Int) extends BlackBox {
  val latency = lat
  val io = new Bundle {
    val aclk = if (lat != 0) in Bool() else null
    val a = slave(Flow(Bits(width bits)))
    val b = slave(Flow(Bits(width bits)))
    val c = slave(Flow(Bits(width bits)))
    val r = master(Flow(Bits(width bits)))
  }
  noIoPrefix()

  io.a.setName("s_axis_a")
  io.b.setName("s_axis_b")
  io.c.setName("s_axis_c")
  io.r.setName("m_axis_result")

  util.AxiStreamSpecRenamer(io.a)
  util.AxiStreamSpecRenamer(io.b)
  util.AxiStreamSpecRenamer(io.c)
  util.AxiStreamSpecRenamer(io.r)
  if (lat != 0) mapClockDomain(clock = io.aclk)
  this.setDefinitionName(name)
}

class acc_stub(width:Int, name: String, lat: Int) extends BlackBox {
  val latency = lat
  val io = new Bundle {
    val aclk = if (lat != 0) in Bool() else null
    val a = slave(Flow(Fragment(Bits(width bits))))
    val r = master(Flow(Fragment(Bits(width bits))))
  }
  noIoPrefix()

  io.a.setName("s_axis_a")
  io.r.setName("m_axis_result")

  util.AxiStreamSpecRenamer(io.a)
  util.AxiStreamSpecRenamer(io.r)
  if (lat != 0) mapClockDomain(clock = io.aclk)
  this.setDefinitionName(name)
}

class cmp_stub(width:Int, name: String, lat: Int) extends BlackBox {
  val latency = lat
  val io = new Bundle {
    val aclk = if (lat != 0) in Bool() else null
    val a = slave(Flow(Bits(width bits)))
    val b = slave(Flow(Bits(width bits)))
    val r = master(Flow(Bits(8 bits)))
  }
  noIoPrefix()

  io.a.setName("s_axis_a")
  io.b.setName("s_axis_b")
  io.r.setName("m_axis_result")

  util.AxiStreamSpecRenamer(io.a)
  util.AxiStreamSpecRenamer(io.b)
  util.AxiStreamSpecRenamer(io.r)
  if (lat != 0) mapClockDomain(clock = io.aclk)
  this.setDefinitionName(name)
}