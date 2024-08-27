package ICache

import DCache.Cache
import InstructionMemory.InstructionMemory
import Memory.Memory
import chisel3._
import config.IMEMsetupSignals

class ICacheAndIMemory (I_memoryFile: String) extends Module {
  val testHarness = IO(
    new Bundle {
      val setupSignals     = Input(new IMEMsetupSignals)
      val requestedAddress = Output(UInt())
    }
  )

  val io = IO(new Bundle {
    val instr_addr = Input(UInt(32.W))
    val instr_out = Output(UInt(32.W))
    val valid = Output(Bool())
    val busy = Output(Bool())
  })

  val imem = Module(new InstructionMemory(I_memoryFile))
//  val icache = Module(new Cache("src/main/scala/ICache/ICacheContent.bin", read_only = true))
//
//  icache.io.read_en := true.B // Always reading for instruction cache
//  icache.io.data_addr := io.instr_addr
//  io.valid := icache.io.valid
//  io.instr_out := icache.io.data_out
//  io.busy := icache.io.busy
//
//  imem.io.instructionAddress := icache.io.mem_data_addr // input to memory /4.U
//  icache.io.mem_data_out := imem.io.instruction // output from memory

//  imem.testHarness.setupSignals := testHarness.setupSignals
  testHarness.requestedAddress := 0.U


  // for unified memory

    val mem = Module(new Memory(""))
    val icache = Module(new Cache("src/main/scala/ICache/ICacheContent.bin", read_only = true))

    icache.io.read_en := true.B // Always reading for instruction cache
    icache.io.data_addr := io.instr_addr
    io.valid := icache.io.valid
    io.instr_out := icache.io.data_out
    io.busy := icache.io.busy

    mem.io.instructionAddress := icache.io.mem_data_addr // input to memory /4.U
    mem.io.dataAddress := 0.U
    mem.io.dataIn := 0.U
    mem.io.dataReadEnable := 0.U
    mem.io.dataWriteEnable := 0.U
    mem.io.instrReadEnable := true.B //todo  should not be true always. cache should give signal for that
  // question: not sure. when exactly do i need to enable reading instruction from memory? maybe check icache.io.valid? or mem_write_en from cache?
//    val cacheMiss = !icache.io.valid && icache.io.busy
//    mem.io.instrReadEnable := cacheMiss
    icache.io.instReadAck := mem.io.icacheReadAck
    icache.io.dataReadAck := mem.io.dcacheReadAck
    icache.io.dataWriteAck := mem.io.dcacheWriteAck
    icache.io.mem_data_out := mem.io.instruction // output from memory

}
