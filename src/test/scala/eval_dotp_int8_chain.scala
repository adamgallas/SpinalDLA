import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.eda.bench.Rtl
import xilinx.DSP48E2._
import xilinx.DSP48E2IntArith.singleCascade.dotp_int8_chain

import scala.language.postfixOps
import scala.util.Random

object eval_dotp_int8_chain extends App {

  val sampleLength = 1024
  val vecLength = 8
  val splitAt = -1
  val a = Array.fill(sampleLength)(Array.fill(vecLength)(Random.nextInt(128)))
  val b = Array.fill(sampleLength)(Array.fill(vecLength)(Random.nextInt(128)))
  val c = Array.fill(sampleLength)(Array.fill(vecLength)(Random.nextInt(128)))
  val ab = (a, b).zipped.map((a, b) => (a, b).zipped.map(_ * _).sum)
  val ac = (a, c).zipped.map((a, c) => (a, c).zipped.map(_ * _).sum)

  SimConfig.withFstWave
    .addRtl("data/sim/DSP48E2.v")
    .compile(new dotp_int8_chain(vecLength, splitAt))
    .doSim { dut =>
      import dut._

      io.a.foreach(_ #= 0)
      io.b.foreach(_ #= 0)
      io.c.foreach(_ #= 0)

      clockDomain.forkStimulus(10)
      clockDomain.waitSampling(32)

      for (i <- 0 until sampleLength + latency) {
        for (j <- 0 until vecLength) {
          if (j <= i && i < sampleLength + j) {
            io.a(j) #= a(i - j)(j) & 0xff
            io.b(j) #= b(i - j)(j) & 0xff
            io.c(j) #= c(i - j)(j) & 0xff
          }
          else {
            io.a(j) #= 0
            io.b(j) #= 0
            io.c(j) #= 0
          }
        }
        if (i > latency) {
          val index = i - latency - 1
          assert(io.ab.toInt == (ab(index) & 0x3ffff))
          assert(io.ac.toInt == (ac(index) & 0x3ffff))
        }
        clockDomain.waitSampling(1)
      }

      io.a.foreach(_ #= 0)
      io.b.foreach(_ #= 0)
      io.c.foreach(_ #= 0)
      clockDomain.waitSampling(32)

      simSuccess()
    }

}
