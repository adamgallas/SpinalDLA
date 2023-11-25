import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.eda.bench.Rtl
import xilinx.DSP48E2._
import xilinx.DSP48E2IntArith.mul_int8

import scala.language.postfixOps
import scala.util.Random

object eval_mul_int8 extends App {

  // simulate

  val length = 1024
  val a = Array.fill(length)(Random.nextInt(256) - 128)
  val b = Array.fill(length)(Random.nextInt(256) - 128)
  val c = Array.fill(length)(Random.nextInt(256) - 128)
  val ab = (a, b).zipped.map(_ * _)
  val ac = (a, c).zipped.map(_ * _)

  SimConfig.withFstWave
    .addRtl("data/sim/DSP48E2.v")
    .compile(new mul_int8)
    .doSim { dut =>
      import dut._

      io.a #= 0
      io.b #= 0
      io.c #= 0

      clockDomain.forkStimulus(10)
      clockDomain.waitSampling(32)

      for (i <- 0 until length + latency) {
        if (i < length) {
          io.a #= a(i) & 0xff
          io.b #= b(i) & 0xff
          io.c #= c(i) & 0xff
        }
        else {
          io.a #= 0
          io.b #= 0
          io.c #= 0
        }
        if (i > latency) {
          val index = i - latency - 1
          assert(
            io.ab.toInt == (ab(index) & 0xffff),
            s"ab ${a(index)}, ${b(index)}, ${c(index)}, ${(io.ab.toInt << 16) >> 16}, ${ab(index)}")
          assert(
            io.ac.toInt == (ac(index) & 0xffff),
            s"ac ${a(index)}, ${b(index)}, ${c(index)}, ${(io.ac.toInt << 16) >> 16}, ${ac(index)}")
        }
        clockDomain.waitSampling(1)
      }
      io.a #= 0
      io.b #= 0
      io.c #= 0

      clockDomain.waitSampling(32)
      simSuccess()
    }
}
