import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.eda.bench.Rtl
import xilinx.DSP48E2._
import xilinx.DSP48E2IntArithmetic.cascade.int8_dotp_ddr

import scala.collection.mutable.ArrayBuffer
import scala.language.postfixOps
import scala.util.Random

object eval_int8_dotp_ddr extends App {

  val sampleLength = 32
  val vecLength = 8

  SpinalVerilog(new int8_dotp_ddr(vecLength))

  val a = Array.fill(sampleLength)(Array.fill(vecLength)(Random.nextInt(256) - 128))
  val b = Array.fill(sampleLength)(Array.fill(vecLength)(Random.nextInt(256) - 128))
  val c = Array.fill(sampleLength)(Array.fill(vecLength)(Random.nextInt(256) - 128))

  val res = ArrayBuffer[Int]()
  for (s <- 0 until sampleLength / 2) {
    val ac = (a(s * 2), b(s * 2)).zipped.map(_ * _).sum
    val ad = (a(s * 2 + 1), b(s * 2)).zipped.map(_ * _).sum
    val bc = (a(s * 2), b(s * 2 + 1)).zipped.map(_ * _).sum
    val bd = (a(s * 2 + 1), b(s * 2 + 1)).zipped.map(_ * _).sum
    print(ac, bc, ad, bd)

    val ac_ = (a(s * 2), c(s * 2)).zipped.map(_ * _).sum
    val ad_ = (a(s * 2 + 1), c(s * 2)).zipped.map(_ * _).sum
    val bc_ = (a(s * 2), c(s * 2 + 1)).zipped.map(_ * _).sum
    val bd_ = (a(s * 2 + 1), c(s * 2 + 1)).zipped.map(_ * _).sum
    println(ac_, bc_, ad_, bd_)
  }

  SimConfig.withFstWave
    .addRtl("data/sim/DSP48E2.v")
    .compile(new int8_dotp_ddr(vecLength))
    .doSim { dut =>
      import dut._

      io.a.foreach(_ #= 0)
      io.b.foreach(_ #= 0)
      io.c.foreach(_ #= 0)

      clockDomain.forkStimulus(10)
      clockDomain.waitSampling(32)

      def check() = {
        for (j <- 0 until vecLength) {
          fork {
            clockDomain.waitSampling(j)
            for (i <- 0 until sampleLength) {
              io.a(j) #= a(i)(j) & 0xff
              io.b(j) #= b(i)(j) & 0xff
              io.c(j) #= c(i)(j) & 0xff
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
