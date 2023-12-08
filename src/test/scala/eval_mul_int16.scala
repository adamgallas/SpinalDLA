import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.eda.bench.Rtl
import xilinx.DSP48E2._
import xilinx.DSP48E2IntArithmetic.standalone.int16_mul

import scala.language.postfixOps
import scala.util.Random

object eval_mul_int16 extends App {

  val length = 32
  val a = Array.fill(length)(Random.nextInt(256) - 128)
  val b = Array.fill(length)(Random.nextInt(256) - 128)
  val ab = (a, b).zipped.map(_ * _)

  SimConfig.withFstWave
    .addRtl("data/sim/DSP48E2.v")
    .compile(new int16_mul)
    .doSim { dut =>
      import dut._

      io.a #= 0
      io.b #= 0

      clockDomain.forkStimulus(10)
      clockDomain.waitSampling(32)

      for (i <- 0 until length + latency) {
        if (i < length) {
          io.a #= a(i) & 0xffff
          io.b #= b(i) & 0xffff
        }
        else {
          io.a #= 0
          io.b #= 0
        }
        if (i > latency) {
          val index = i - latency - 1
          assert(ab(index) == io.ab.toBigInt.toInt)
        }
        clockDomain.waitSampling(1)
      }
      io.a #= 0
      io.b #= 0

      clockDomain.waitSampling(32)
      simSuccess()
    }

}
