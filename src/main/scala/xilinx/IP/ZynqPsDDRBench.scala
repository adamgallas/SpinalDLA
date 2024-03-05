package xilinx.IP

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba4.axi.Axi4SpecRenamer
import spinal.lib.bus.amba4.axilite.{AxiLite4, AxiLite4Config, AxiLite4SlaveFactory, AxiLite4SpecRenamer}
import util.AxiStreamSpecRenamer

import scala.language.postfixOps

class ZynqPsDDRBench(dataWidth: Int, addrWidth: Int = 32) extends Component {

  require(addrWidth % 8 == 0)

  val io = new Bundle {
    val ctrl = slave(AxiLite4(AxiLite4Config(32, 32)))
    val mm2s = slave(Stream(Bits(dataWidth bits)))
    val cmd = master(Stream(Bits(32 + 8 + addrWidth bits)))
  }

  noIoPrefix()
  AxiLite4SpecRenamer(io.ctrl)
  AxiStreamSpecRenamer(io.mm2s)
  AxiStreamSpecRenamer(io.cmd)

  io.mm2s.freeRun()

  val baseAddr = UInt(32 bits).setAsReg().init(0)
  val baseAddrEx = if (addrWidth > 32) UInt(addrWidth - 32 bits).setAsReg().init(0) else null
  val addr = UInt(32 bits).setAsReg().init(0)
  val len = UInt(23 bits).setAsReg().init(0)
  val total = UInt(32 bits).setAsReg().init(0)

  val valid = Bool().setAsReg().init(False)
  val ready = Bool()

  val halt = Bool().setAsReg().init(True)
  val clear = Bool().setAsReg().init(False)

  val cfg = new AxiLite4SlaveFactory(io.ctrl)
  cfg.write(baseAddr, 0x00, 0)
  if (addrWidth > 32) cfg.write(baseAddrEx, 0x40, 0)
  cfg.write(addr, 0x04, 0)
  cfg.write(len, 0x08, 0)
  cfg.readAndWrite(total, 0x0c, 0)

  cfg.write(valid, 0x10, 0)
  cfg.read(ready, 0x14, 0)
  valid.clear()

  cfg.readAndWrite(halt, 0x20, 0)
  cfg.write(clear, 0x24, 0)
  clear.clear()

  val baseAddrPack = if (addrWidth > 32) (baseAddrEx ## baseAddr).asUInt else baseAddr
  val cmdFifo = StreamFifo(util.PairBundle(UInt(32 bits), UInt(23 bits)), 1024)
  io.cmd << AxiDataMoverCmdGen(cmdFifo.io.pop, baseAddrPack, True, True).haltWhen(halt)

  cmdFifo.io.push.valid := valid
  cmdFifo.io.push.payload.A := addr
  cmdFifo.io.push.payload.B := len
  ready := cmdFifo.io.push.ready

  val transaction = UInt(32 bits).setAsReg().init(0)
  val finish = transaction === total
  when(io.mm2s.fire) {
    transaction := transaction + 1
  }
  when(clear) {
    transaction.clearAll()
  }

  val clockCnt = UInt(32 bits).setAsReg().init(0)
  val tictok = Bool().setAsReg().init(False)
  tictok.setWhen(io.mm2s.fire).clearWhen(finish)
  when(tictok) {
    clockCnt := clockCnt + 1
  }
  when(clear) {
    clockCnt.clearAll()
  }

  halt.setWhen(finish)

  cfg.read(transaction, 0x30, 0)
  cfg.read(clockCnt, 0x34, 0)
}

object ZynqPsDDRBench extends App {
  SpinalVerilog(new ZynqPsDDRBench(512, 40))
}