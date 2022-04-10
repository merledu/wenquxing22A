/**************************************************************************************
* Copyright (c) 2020 Institute of Computing Technology, CAS
* Copyright (c) 2020 University of Chinese Academy of Sciences
* 
* NutShell is licensed under Mulan PSL v2.
* You can use this software according to the terms and conditions of the Mulan PSL v2. 
* You may obtain a copy of Mulan PSL v2 at:
*             http://license.coscl.org.cn/MulanPSL2 
* 
* THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER 
* EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR 
* FIT FOR A PARTICULAR PURPOSE.  
*
* See the Mulan PSL v2 for more details.  
***************************************************************************************/

package nutcore

import chisel3._
import chisel3.util._

class STDPIO(val len: Int) extends NutCoreBundle{
    val in = Flipped(DecoupledIO(new Bundle{
        val src1 = Input(UInt(len.W))
        val src2 = Input(UInt(len.W))
        val op   = Input(UInt(len.W))
        val imm  = Input(UInt(len.W))
        val output = Input(UInt(len.W))
        val vinit = Input(UInt(len.W))
    }))
    val out = DecoupledIO(new Bundle{
        val res = Output(UInt(len.W))
        val en = Output(Bool())
    }) 
}

class STDP(len: Int) extends NutCoreModule{
    val io = IO(new STDPIO(len))
    
    val (valid, src1, src2, op) = (io.in.valid, io.in.bits.src1, io.in.bits.src2, io.in.bits.op)
    val imm = io.in.bits.imm
    val stdpEnable = io.in.bits.vinit(0)
    val output_reg = RegInit(0.U(len.W))
    val input_reg  = RegInit(0.U(len.W))
    val syn_reg    = RegInit(0.U(len.W))

    io.in.ready := io.out.ready
    io.out.valid := valid
    io.out.bits := DontCare

    when(op === SNNOpType.sinit && valid) {
        io.out.bits.res := io.in.bits.imm
    }.elsewhen(op === SNNOpType.sup && valid && stdpEnable){
        output_reg := io.in.bits.output
        input_reg  := io.in.bits.src2
        syn_reg    := io.in.bits.src1
        val syn_new = VecInit(syn_reg.asBools)
        for (i <- 0 to (len - 1)){
            if(output_reg(i) == 1.U)  syn_new(i) := input_reg(i) && output_reg(i)
        else    syn_new(i) := syn_reg(i)
        }

        io.out.bits.res := syn_new.asUInt
    }.elsewhen(op === SNNOpType.sup && valid && !stdpEnable) {
        io.out.bits.res := syn_reg
    }.otherwise {
        io.out.bits.res := DontCare
    }
    when (valid && SNNDebug.enablePrint){
        printf("[stdp]option = 0x%x\n",op)
        printf("[stdp]src1 = 0x%x\n", src1)
        printf("[stdp]src2 = 0x%x\n", src2)
        printf("[stdp]imm = 0x%x\n", imm)
        printf("[stdp]res = 0x%x\n", io.out.bits.res)       
        printf("\n")
    }
}