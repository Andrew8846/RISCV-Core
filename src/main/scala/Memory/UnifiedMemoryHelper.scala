package Memory
import chisel3._
import chisel3.util._
import chisel3.util.experimental.loadMemoryFromFileInline

class UnifiedMemoryHelper(memoryFile: String) extends Module {
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
  val memAddressWire = WireInit(0.asUInt(32.W))
  val rdDataWire = WireInit(0.asUInt(32.W))
  memAddressWire := io.addr(31,2)


  loadMemoryFromFileInline(memory, memoryFile)

  rdDataWire := memory.read(memAddressWire, io.memRead)


  val memReadValidReg = RegInit(false.B)
  val rdDataRegister  = RegInit(UInt(32.W), 0.U)

  // divide addr by 4
  // concat 00 and (31,2)

  memReadValidReg := io.memRead
  when (memReadValidReg) {
    io.rdData := rdDataWire
  }.otherwise {
    io.rdData := 0.U
  }
//

//  memAddressWire := io.addr(31,2) // + 1.U

//  val readEnable = RegNext(io.memRead, false.B)  // Pipeline register for read enable
//  val readAddr = RegNext(memAddressWire)

  when(io.memWrite) {
    memory.write(io.addr, io.wrData)
//    memory(io.addr) := io.wrData
  }

//  memory.write(io.addr, io.wrData)

//  io.rdData := 0.U
//  when(io.memRead) {
//    io.rdData := memory(memAddressWire) // this should work for instruction but in case of data what happens?
//  }

}
