import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.eda.bench.Rtl
import xilinx.DSP48E2._
import xilinx.DSP48E2IntArithmetic.cascade.int16_dotp_ddr

import scala.language.postfixOps
import scala.util.Random

object eval_int16_dotp_ddr extends App {

  val sampleLength = 16
  val vecLength = 4

  SpinalVerilog(new int16_dotp_ddr(vecLength))

  val a = Array.fill(sampleLength)(Array.fill(vecLength)(Random.nextInt(256) - 128))
  val b = Array.fill(sampleLength)(Array.fill(vecLength)(Random.nextInt(256) - 128))
  val c = Array.fill(sampleLength)(Array.fill(vecLength)(Random.nextInt(256) - 128))
  val d = Array.fill(sampleLength)(Array.fill(vecLength)(Random.nextInt(256) - 128))

  val ac = for (s <- 0 until sampleLength) yield (a(s), c(s)).zipped.map(_ * _).sum
  val ad = for (s <- 0 until sampleLength) yield (a(s), d(s)).zipped.map(_ * _).sum
  val bc = for (s <- 0 until sampleLength) yield (b(s), c(s)).zipped.map(_ * _).sum
  val bd = for (s <- 0 until sampleLength) yield (b(s), d(s)).zipped.map(_ * _).sum

  val aIn = for (s <- 0 until sampleLength) yield (a(s), b(s)).zipped.flatMap(Array(_, _))
  val bIn = for (s <- 0 until sampleLength) yield (c(s), d(s)).zipped.flatMap(Array(_, _))

  for (s <- 0 until sampleLength) {
    println(ac(s), ad(s), bc(s), bd(s))
  }


  SimConfig.withFstWave
    .addRtl("data/sim/DSP48E2.v")
    .compile(new int16_dotp_ddr(vecLength))
    .doSim { dut =>
      import dut._

      io.a.foreach(_ #= 0)
      io.b.foreach(_ #= 0)

      clockDomain.forkStimulus(10)
      clockDomain.waitSampling(32)

      def check() = {
        for (j <- 0 until vecLength) {
          fork {
            clockDomain.waitSampling(j)
            for (i <- 0 until sampleLength) {
              io.a(j) #= a(i)(j) & 0xffff
              io.b(j) #= b(i)(j) & 0xffff
              clockDomain.waitSampling(2)
            }
          }
        }
      }

      for (p <- 0 until 1) {
        fork {
          check()
        }
        clockDomain.waitSampling(sampleLength)
      }

      clockDomain.waitSampling(32)
      simSuccess()
    }
}
