package Memory

import DCache.Cache
import chisel3._
import config.IMEMsetupSignals

class CachesAndMemory(I_memoryFile: String) extends Module{
  val testHarness = IO(
    new Bundle {
      val setupSignals     = Input(new IMEMsetupSignals)
      val requestedAddress = Output(UInt())
    }
  )
  testHarness.requestedAddress := 0.U
  val io = IO(
    new Bundle{
      val write_data = Input(UInt(32.W))
      val address = Input(UInt(32.W))
      val write_en = Input(Bool())
      val read_en_data = Input(Bool())
      val d_valid = Output(Bool())
      val data_out = Output(UInt(32.W))
      val d_busy = Output(Bool())

      val instr_addr = Input(UInt(32.W))
      val read_en_instr = Input(Bool())
      val instr_out = Output(UInt(32.W))
      val i_valid = Output(Bool())
      val i_busy = Output(Bool())
    }
  )

  val mem  = Module(new Memory(I_memoryFile))
  val dcache = Module(new Cache("src/main/scala/DCache/CacheContent.bin", read_only = false))
  val icache = Module(new Cache("src/main/scala/ICache/ICacheContent.bin", read_only = true))

  dcache.io.data_in.foreach(_ := io.write_data)
  dcache.io.data_addr := io.address
  dcache.io.write_en.foreach(_ := io.write_en)
  dcache.io.read_en := io.read_en_data
  io.d_valid := dcache.io.valid
  io.data_out := dcache.io.data_out
  io.d_busy := dcache.io.busy

  icache.io.read_en := io.read_en_instr // Always reading for instruction cache
  icache.io.data_addr := io.instr_addr
  io.i_valid := icache.io.valid
  io.instr_out := icache.io.data_out
  io.i_busy := icache.io.busy

  mem.io.dataAddress := dcache.io.mem_data_addr / 4.U
  mem.io.dataIn := dcache.io.mem_data_in
  mem.io.dataReadEnable := dcache.io.mem_read_en_data
  mem.io.dataWriteEnable := dcache.io.mem_write_en

  dcache.io.instReadAck := mem.io.icacheReadAck
  dcache.io.dataReadAck := mem.io.dcacheReadAck
  dcache.io.dataWriteAck := mem.io.dcacheWriteAck
  dcache.io.mem_data_out := mem.io.dataOut


  mem.io.instructionAddress := icache.io.mem_data_addr // input to memory /4.U
  mem.io.instrReadEnable := icache.io.mem_read_en_inst

  icache.io.instReadAck := mem.io.icacheReadAck
  icache.io.dataReadAck := mem.io.dcacheReadAck
  icache.io.dataWriteAck := mem.io.dcacheWriteAck
  icache.io.mem_data_out := mem.io.instruction // output from memory


}
