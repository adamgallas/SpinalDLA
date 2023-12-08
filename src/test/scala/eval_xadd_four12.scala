import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.eda.bench.Rtl
import xilinx.DSP48E2._
import xilinx.DSP48E2IntArithmetic.standalone.int12_xadd

import scala.language.postfixOps
import scala.util.Random

object eval_xadd_four12 extends App {

  // simulate

  val length = 1024
  val aSel = Array.fill(length)(Random.nextInt(2))
  val bSel = Array.fill(length)(Random.nextInt(2))
  val a = Array.fill(length)(Array.fill(4)(Random.nextInt(4096) - 2048))
  val b = Array.fill(length)(Array.fill(4)(Random.nextInt(4096) - 2048))
  val ab = for (i <- 0 until length) yield {
    for (j <- 0 until 4) yield {
      aSel(i) * a(i)(j) + bSel(i) * b(i)(j)
    }
  }

  SimConfig.withFstWave
    .addRtl("data/sim/DSP48E2.v")
    .compile(new int12_xadd)
    .doSim { dut =>
      import dut._

      io.aSel #= 0
      io.bSel #= 0
      io.a.foreach(_ #= 0)
      io.b.foreach(_ #= 0)

      clockDomain.forkStimulus(10)
      clockDomain.waitSampling(32)

      for (i <- 0 until length + latency) {
        if (i < length) {
          io.aSel #= aSel(i)
          io.bSel #= bSel(i)
          io.a.zip(a(i)).foreach { case (pin, value) => pin #= value & 0xfff }
          io.b.zip(b(i)).foreach { case (pin, value) => pin #= value & 0xfff }
        }
        else {
          io.aSel #= 0
          io.bSel #= 0
          io.a.foreach(_ #= 0)
          io.b.foreach(_ #= 0)
        }
        if (i > latency) {
          val index = i - latency - 1

          for (t <- 0 until 4) {
            assert(
              io.ab(t).toInt == (ab(index)(t) & 0xfff),
              s"ab ${aSel(index)}, ${bSel(index)}, ${a(index)(t)}, ${b(index)(t)}, ${(io.ab(t).toInt << 20) >> 20}, ${ab(index)(t)}")
          }
        }
        clockDomain.waitSampling(1)
      }
      io.aSel #= 0
      io.bSel #= 0
      io.a.foreach(_ #= 0)
      io.b.foreach(_ #= 0)

      clockDomain.waitSampling(32)
      simSuccess()
    }
}
