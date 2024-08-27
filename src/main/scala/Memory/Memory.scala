package Memory
import chisel3._
import chisel3.util._

class Memory(memoryFile: String) extends Module {
  val io = IO(new Bundle {
    // Inputs for instruction fetch
    val instructionAddress = Input(UInt(32.W))
    val instrReadEnable = Input(Bool())
    val instruction = Output(UInt(32.W))

    // Inputs for data access
    val dataAddress = Input(UInt(32.W))
    val dataIn = Input(UInt(32.W))
    val dataReadEnable = Input(Bool())
    val dataWriteEnable = Input(Bool())
    val dataOut = Output(UInt(32.W))
    val dcacheWriteAck = Output(Bool())
    val dcacheReadAck = Output(Bool())
    val icacheReadAck = Output(Bool())
  })

  // todo cases to handle: when i have *instrReadEnable and dataReadEnable true* simultaneously and when i have *instrReadEnable and dataWriteEnable*
  // i cannot have dataReadEnable and dataWriteEnable true at the same time

  val memory = Module(new UnifiedMemoryHelper(memoryFile))
  val instPriority = RegInit(true.B) // Prioritize instruction fetch by default
  val dackReadReg = RegInit(false.B)
  val dackWriteReg = RegInit(false.B)
  val iackReg = RegInit(false.B)

  // initialise memory
  memory.io.memWrite := false.B
  memory.io.memRead := false.B
  memory.io.addr := 0.U
  memory.io.wrData := 0.U


  // Prioritized memory access
  when(io.dataReadEnable) {
    // Priority 1: Data Read
    memory.io.memWrite := false.B
    memory.io.addr := io.dataAddress
    memory.io.memRead := true.B
    io.dataOut := memory.io.rdData
    io.instruction := 0.U
    dackReadReg := true.B
    dackWriteReg := false.B
    iackReg := false.B

  }.elsewhen(io.dataWriteEnable) {
    // Priority 2: Data Write
    memory.io.memWrite := true.B
    memory.io.wrData := io.dataIn
    memory.io.addr := io.dataAddress
    memory.io.memRead := false.B
    io.dataOut := 0.U
    io.instruction := 0.U
    dackReadReg := false.B
    dackWriteReg := true.B
    iackReg := false.B

  }.elsewhen(io.instrReadEnable) {
    // Priority 3: Instruction Read
    memory.io.memWrite := false.B
    memory.io.addr := io.instructionAddress
    memory.io.memRead := true.B
    io.instruction := memory.io.rdData
    io.dataOut := 0.U
    dackReadReg := false.B
    dackWriteReg := false.B
    iackReg := true.B

  }.otherwise {
    // No operations
    memory.io.memWrite := false.B
    memory.io.memRead := false.B
    io.instruction := 0.U
    io.dataOut := 0.U
    dackReadReg := false.B
    dackWriteReg := false.B
    iackReg := false.B
  }


//  when(io.instrReadEnable && io.dataReadEnable) {
//    // Both instruction fetch and data read are requested
//    instPriority := ~instPriority
//  }

//  val isInstCycle = io.instrReadEnable && (instPriority || !io.dataReadEnable)
//  val isDataReadCycle = io.dataReadEnable && (!instPriority || !io.instrReadEnable)


  // Write buffer
  // to address problem : instrReadEnable and dataWriteEnable. instrReadEnable - priority
  // todo make this buffer sized 3 (not needed)
  // todo priorities : 1 data read, 2 data write, 3 inst read.
  // todo i need buffers for instuction reads. (not needed)

  // todo what happens when you have cache miss in icache? if memory is bussy doing dataread/write, then you just wait longer to get answer for your instructuion.

//  when(io.dataWriteEnable && !isInstCycle) {
//    // no conflict, write data in memory directly
//    memory.io.memWrite := true.B
//    memory.io.wrData := io.dataIn
//    memory.io.addr := io.dataAddress
//    memBusyReg := true.B
//  }.otherwise {
//    memory.io.memWrite := false.B
//    memBusyReg := false.B // maybe extra
//  }
//
//  memory.io.addr := Mux(isInstCycle, io.instructionAddress, io.dataAddress) // question: this will overwrite above when else statement?
//  memory.io.memRead := isInstCycle || isDataReadCycle
//
//  io.instruction := 0.U
//  io.dataOut := 0.U
//
//  when(isInstCycle) {
//    io.instruction := memory.io.rdData
//    memBusyReg := true.B
//  }
//
//  when(isDataReadCycle) {
//    io.dataOut := memory.io.rdData
//    memBusyReg := true.B
//  }

  io.dcacheReadAck := dackReadReg
  io.dcacheWriteAck := dackWriteReg
  io.icacheReadAck := iackReg
}
