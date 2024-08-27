package Memory
import chisel3._
import chisel3.util._
import chisel3.util.experimental.loadMemoryFromFileInline

class UnifiedMemoryHelper(memoryFile: String = "src/main/scala/Memory/combinedMemory") extends Module {
  val io = IO(new Bundle {
    val addr = Input(UInt(32.W))
    val wrData = Input(UInt(32.W))
    val memRead = Input(Bool())
    val memWrite = Input(Bool())
    val rdData = Output(UInt(32.W))
  })
  // data starts after instr. soo 4096 is not really only for instructions.
  val memory = SyncReadMem(1052672, UInt(32.W)) // 1052672 = 1048576 (for data) + 4096 (for instructions) -- can be parametrized
  // maybe make memory 2^32 = 4 294 967 296

  loadMemoryFromFileInline(memory,memoryFile)

  when(io.memWrite) {
    memory(io.addr) := io.wrData
  }

  io.rdData := 0.U
  when(io.memRead) {
    io.rdData := memory(io.addr)
  }

}
