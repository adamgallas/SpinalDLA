import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.eda.bench.Rtl
import xilinx.DSP48E2._
import xilinx.DSP48E2IntArithmetic.standalone.pack_uint4_mul

import scala.language.postfixOps
import scala.util.Random

object eval_mul_uint4 extends App {

  val sampleLength = 1024
  val a1 = Array.fill(sampleLength)(Random.nextInt(16))
  val a2 = Array.fill(sampleLength)(Random.nextInt(16))
  val w1 = Array.fill(sampleLength)(Random.nextInt(16) - 8)
  val w2 = Array.fill(sampleLength)(Random.nextInt(16) - 8)

  val a1w1 = (a1, w1).zipped.map(_ * _)
  val a1w2 = (a1, w2).zipped.map(_ * _)
  val a2w1 = (a2, w1).zipped.map(_ * _)
  val a2w2 = (a2, w2).zipped.map(_ * _)

  def bits2int(src: Int) = {
    (src << (32 - 8) >> (32 - 8))
  }

  SimConfig.withFstWave
    .addRtl("data/sim/DSP48E2.v")
    .compile(new pack_uint4_mul)
    .doSim { dut =>
      import dut._

      io.a1 #= 0
      io.a2 #= 0
      io.w1 #= 0
      io.w2 #= 0

      clockDomain.forkStimulus(10)
      clockDomain.waitSampling(32)

      for (i <- 0 until sampleLength + latency) {
        if (i < sampleLength) {
          io.a1 #= a1(i) & 0xf
          io.a2 #= a2(i) & 0xf
          io.w1 #= w1(i) & 0xf
          io.w2 #= w2(i) & 0xf
        }
        else {
          io.a1 #= 0
          io.a2 #= 0
          io.w1 #= 0
          io.w2 #= 0
        }
        if (i > latency) {
          val index = i - latency - 1
          println(bits2int(io.a1w1.toInt), a1w1(index))
          println(bits2int(io.a2w1.toInt), a2w1(index))
          println(bits2int(io.a1w2.toInt), a1w2(index))
          println(bits2int(io.a2w2.toInt), a2w2(index))
        }
        clockDomain.waitSampling()
      }

      clockDomain.waitSampling(32)
      simSuccess()
    }
}
