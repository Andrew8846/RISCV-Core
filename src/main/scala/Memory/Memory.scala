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
  val instPriority = WireInit(true.B) // Prioritize instruction fetch by default3
  val dackReadReg = WireInit(false.B)
  val dackWriteReg = WireInit(false.B) // check these two as well
  val iackReg = WireInit(false.B) // todo make it wire

  // initialise memory
  memory.io.memWrite := false.B
  memory.io.memRead := false.B
  memory.io.addr := 0.U
  memory.io.wrData := 0.U

  val instReadReq :: instReadyToRead :: Nil = Enum(2)
  val instReadStateReg = RegInit(instReadReq)


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

    io.instruction := 0.U
    io.dataOut := 0.U
    switch(instReadStateReg) {
      is(instReadReq) {
        memory.io.memWrite := false.B
        memory.io.addr := io.instructionAddress
        memory.io.memRead := true.B
        instReadStateReg := instReadyToRead
      }
      is(instReadyToRead) {
        io.instruction := memory.io.rdData
        io.dataOut := 0.U
        dackReadReg := false.B
        dackWriteReg := false.B
        iackReg := true.B

        instReadStateReg := instReadReq
      }
    }

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

  io.dcacheReadAck := dackReadReg
  io.dcacheWriteAck := dackWriteReg
  io.icacheReadAck := iackReg
}
