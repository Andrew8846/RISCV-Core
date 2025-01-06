/*
RISC-V Pipelined Project in Chisel

This project implements a pipelined RISC-V processor in Chisel. The pipeline includes five stages: fetch, decode, execute, memory, and writeback.
The core is part of an educational project by the Chair of Electronic Design Automation (https://eit.rptu.de/fgs/eis/) at RPTU Kaiserslautern, Germany.

Supervision and Organization: Tobias Jauch, Philipp Schmitz, Alex Wezel
Student Workers: Giorgi Solomnishvili, Zahra Jenab Mahabadi, Tsotne Karchava, Abdullah Shaaban Saad Allam.

*/

package Stage_IF

import ICache.ICacheAndIMemory
import chisel3._
import chisel3.util._
import config.{ControlSignals, IMEMsetupSignals, Inst, Instruction}
import config.Inst._
import InstructionMemory.InstructionMemory
import Memory.CachesAndMemory

class IF extends Module
{

  val testHarness = IO(
    new Bundle {
      val InstructionMemorySetup = Input(new IMEMsetupSignals)
      val PC        = Output(UInt())
    }
  )


  val io = IO(new Bundle {
    val branchAddr         = Input(UInt())
    val IFBarrierPC        = Input(UInt())
    val stall              = Input(Bool())
    val instruction_in     = Input(new Instruction)
    // new added
    val validInstruction              = Input(Bool())
    // Inputs for BTB, will come from EX stage and Hazard Unit
    val updatePrediction   = Input(Bool())
    val newBranch          = Input(Bool())
    val entryPC            = Input(UInt(32.W))
    val branchTaken        = Input(Bool())  // 1 means Taken -- 0 means Not Taken
    val branchMispredicted = Input(Bool())
    val PCplus4ExStage     = Input(UInt(32.W))
    val btbHit             = Output(Bool())
    val btbPrediction      = Output(Bool())
    val btbTargetPredict   = Output(UInt(32.W))
    val PC                 = Output(UInt())
    val instruction        = Output(new Instruction)
//    val fetchBusy          = Output(Bool()) // added this signal for stall
    val instr_addr_out     = Output(UInt(32.W))
  })

  val BTB               = Module(new BTB_direct)
  val nextPC            = WireInit(UInt(), 0.U)
  val PC                = RegInit(UInt(32.W), 0.U)
  val PCplus4           = Wire(UInt(32.W))
  val instruction       = Wire(new Instruction)
  val branch            = WireInit(Bool(), false.B)
  val branchAddrRegister = RegInit(UInt(32.W), 0.U)
  val isBranch          = RegInit(Bool(), false.B)
  val stall2            = Wire(Bool())
  stall2   := false.B
  testHarness.PC := 0.U

  instruction := io.instruction_in

  PCplus4 := PC + 4.U

  // BTB signals
  BTB.io.currentPC := PC
  BTB.io.newBranch := io.newBranch
  BTB.io.updatePrediction := io.updatePrediction
  BTB.io.entryPC := io.entryPC
  BTB.io.entryBrTarget := io.branchAddr
  BTB.io.branchMispredicted := io.branchMispredicted
  BTB.io.stall := io.stall
  io.btbPrediction := BTB.io.prediction
  io.btbHit := BTB.io.btbHit
  io.btbTargetPredict := BTB.io.targetAdr

  when(io.branchMispredicted){  // Case of branch mispredicted, we realize that in EX stage
    when(io.branchTaken){  // Branch Behavior is Taken, but Predicted Not-Taken
      branchAddrRegister := io.branchAddr
      isBranch := true.B
    }
      .otherwise{
        when(isBranch) {
          nextPC := branchAddrRegister
        }.otherwise {
          nextPC := io.PCplus4ExStage
        }
      }
  }
    .elsewhen(BTB.io.btbHit){  // BTB hits -> Choose nextPC as per the prediction
      when(BTB.io.prediction){  // Predict taken
        nextPC := BTB.io.targetAdr
      }
        .otherwise{ // Predict not taken
          nextPC := PCplus4
        }
    }
    .otherwise{ // Normal instruction OR assume not taken (BTB miss)
      when(isBranch) {
        nextPC := branchAddrRegister
      }.otherwise {
        nextPC := PCplus4
      }
    }


  // Stall PC
  when(io.stall){
    when(io.branchMispredicted) {
      PC := nextPC
    }.otherwise{
      PC := PC
    }

    io.instr_addr_out := io.IFBarrierPC //io.IFBarrierPC
  }.otherwise{
    //Fetch instruction
    io.instr_addr_out := PC // PC
    // PC register gets nextPC
    PC := nextPC
  }

  stall2 := !io.validInstruction && !isBranch
  when (!io.validInstruction) {
    PC := PC
  }.otherwise {
    isBranch := false.B
  }
  
  // Send PC to the rest of the pipeline
  io.PC := PC // PC

  io.instruction := instruction

  when(testHarness.InstructionMemorySetup.setup) {
    PC := 0.U
    instruction := Inst.NOP
  }
}
