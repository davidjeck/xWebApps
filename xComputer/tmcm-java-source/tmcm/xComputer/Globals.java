
package tmcm.xComputer;

import java.util.BitSet;


final class Converter {
   short data;
   void set(short d) {
      data = d;
   }
   short getSigned() {
      return data;
   }
   int getUnsigned() {
      if (data >= 0)
         return data;
      else
         return data + 0x10000;
   }
   String getAscii() {
      int ch1 = (data >> 8) & 0xFF;
      int ch2 = (data & 0xFF);
      String s1,s2;
      if (ch1 <= 32 || ch1 > 127)
         s1 = "<#" + ch1 + ">";
      else
         s1 = String.valueOf(((char)ch1));
      if (ch2 <= 32 || ch2 > 127)
         s2 = "<#" + ch2 + ">";
      else
         s2 = String.valueOf(((char)ch2));
      return s1 + s2;
   }
   int getMode() {
      return (data & 0xC000) >> 14;
   }
   int getData() {
      return data & 0x3FF;
   }
   int getInstruction() {
      return (data >> 10) & 15;
   }
   String getAssembly() {
      int i = getInstruction();
      int d;
      switch (getMode()) {
         case Globals.direct:
            d = getData();
            if (d != 0 || Globals.hasData.get(i))
               return Globals.InstructionName[i] + " " + d;
            else
               return Globals.InstructionName[i];
         case Globals.indirect:
            return Globals.InstructionName[i] + "-I " + getData();
         case Globals.constant:
            return Globals.InstructionName[i] + "-C " + getData();
         case Globals.illegal:
            return Globals.InstructionName[i] + "-? " + getData();
         default:
            return null;
      }
   }
}


final class Globals {
 
   static java.awt.Color flashColor = new java.awt.Color(255,255,180);
 
   final static int _add = 0, _sub = 1, _and = 2, _or = 3,      // instructions
                    _not = 4, _shl = 5, _shr = 6, _inc = 7,
                    _dec = 8, _lod = 9, _sto = 10, _hlt = 11,
                    _jmp = 12, _jmz = 13, _jmn = 14, _jmf = 15;
                    
   final static int                                            // control wires
        select_add = 0, select_subtract = 1, select_and = 2,
        select_or = 3, select_not = 4, select_shift_left = 5, select_shift_right = 6,
        load_data_into_memory = 7, load_x_from_ac = 8, load_y_from_memory = 9, load_y_from_ir = 10,
        load_ac_from_alu = 11, load_ac_from_memory = 12, load_ac_from_ir = 13, increment_ac = 14,
        decrement_ac = 15, load_flag_from_alu = 16, load_pc_from_memory = 17, load_pc_from_ir = 18,
        increment_pc = 19, load_ir_from_memory = 20, load_addr_from_ir = 21, load_addr_from_y = 22,
        load_addr_from_pc = 23, set_count_to_zero = 24, stop_clock = 25;
   
   final static int 
        direct = 0, constant = 1, indirect = 2, illegal = 3;   // address modes
        
   
   static final String[] InstructionName = { "ADD", "SUB", "AND", "OR",
                                             "NOT", "SHL", "SHR", "INC",
                                             "DEC", "LOD", "STO", "HLT",
                                             "JMP", "JMZ", "JMN", "JMF" };
   
   
   
   static final String[] WireName =
     {   "Select-Add", "Select-Subtract", "Select-AND",
         "Select-OR", "Select-NOT", "Select-Shift-Left", "Select-Shift-Right",
         "Load-Data-Into-Memory", "Load-X-From-AC", "Load-Y-From-Memory", "Load-Y-From-IR",
         "Load-AC-From-ALU", "Load-AC-From-Memory", "Load-AC-From-IR", "Increment-AC",
         "Decrement-AC", "Load-Flag-From-ALU", "Load-PC-From-Memory", "Load-PC-From-IR",
         "Increment-PC", "Load-IR-From-Memory", "Load-ADDR-From-IR", "Load-ADDR-From-Y",
         "Load-ADDR-From-PC", "Set-COUNT-To-Zero", "Stop-Clock" };
         
   static BitSet hasConstantMode, hasIndirectMode, hasData;
        
   static BitSet[][][] step;
   static { 
      hasConstantMode = new BitSet(16);
      hasConstantMode.set(_add);
      hasConstantMode.set(_sub);
      hasConstantMode.set(_and);
      hasConstantMode.set(_or);
      hasConstantMode.set(_lod);
      
      hasIndirectMode = new BitSet(16);
      hasIndirectMode.set(_add);
      hasIndirectMode.set(_sub);
      hasIndirectMode.set(_and);
      hasIndirectMode.set(_or);
      hasIndirectMode.set(_lod);
      hasIndirectMode.set(_sto);
      hasIndirectMode.set(_jmp);
      hasIndirectMode.set(_jmz);
      hasIndirectMode.set(_jmn);
      hasIndirectMode.set(_jmf);
      
      hasData = hasIndirectMode;
      
      step = new BitSet[16][4][11];
      BitSet b0 = new BitSet(26);
      BitSet b1 = new BitSet(26);
      b1.set(load_addr_from_pc);
      BitSet b2 = new BitSet(26);
      b2.set(load_ir_from_memory);
      BitSet b3 = new BitSet(26);
      b3.set(increment_pc);
      for (int i = _add; i <= _jmp; i++)
        for (int j = 0; j < 4; j++) {
           step[i][j][0] = b0;
           step[i][j][1] = b1;
           step[i][j][2] = b2;
           step[i][j][3] = b3; 
        }
      for (int i = _add; i <= _jmp; i++)
        for (int j = 0; j < 4; j++)
          for (int k = 4; k < 11; k++)
              step[i][j][k] = new BitSet(26);
	  step[_add][direct][4].set(load_addr_from_ir);
	  step[_add][direct][5].set(load_y_from_memory);
	  step[_add][direct][5].set(load_x_from_ac);
	  step[_add][direct][6].set(select_add);
	  step[_add][direct][6].set(load_ac_from_alu);
	  step[_add][direct][6].set(load_flag_from_alu);
	  step[_add][direct][7].set(select_add);
	  step[_add][constant][4].set(load_y_from_ir);
	  step[_add][constant][4].set(load_x_from_ac);
	  step[_add][constant][5].set(select_add);
	  step[_add][constant][5].set(load_ac_from_alu);
	  step[_add][constant][5].set(load_flag_from_alu);
	  step[_add][constant][6].set(select_add);
	  step[_add][indirect][4].set(load_addr_from_ir);
	  step[_add][indirect][5].set(load_y_from_memory);
	  step[_add][indirect][6].set(load_addr_from_y);
	  step[_add][indirect][7].set(load_y_from_memory);
	  step[_add][indirect][7].set(load_x_from_ac);
	  step[_add][indirect][8].set(select_add);
	  step[_add][indirect][8].set(load_ac_from_alu);
	  step[_add][indirect][8].set(load_flag_from_alu);
	  step[_add][indirect][9].set(select_add);

	  step[_sub][direct][4].set(load_addr_from_ir);
	  step[_sub][direct][5].set(load_y_from_memory);
	  step[_sub][direct][5].set(load_x_from_ac);
	  step[_sub][direct][6].set(select_subtract);
	  step[_sub][direct][6].set(load_ac_from_alu);
	  step[_sub][direct][6].set(load_flag_from_alu);
	  step[_sub][direct][7].set(select_subtract);
	  step[_sub][constant][4].set(load_y_from_ir);
	  step[_sub][constant][4].set(load_x_from_ac);
	  step[_sub][constant][5].set(select_subtract);
	  step[_sub][constant][5].set(load_ac_from_alu);
	  step[_sub][constant][5].set(load_flag_from_alu);
	  step[_sub][constant][6].set(select_subtract);
	  step[_sub][indirect][4].set(load_addr_from_ir);
	  step[_sub][indirect][5].set(load_y_from_memory);
	  step[_sub][indirect][6].set(load_addr_from_y);
	  step[_sub][indirect][7].set(load_y_from_memory);
	  step[_sub][indirect][7].set(load_x_from_ac);
	  step[_sub][indirect][8].set(select_subtract);
	  step[_sub][indirect][8].set(load_ac_from_alu);
	  step[_sub][indirect][8].set(load_flag_from_alu);
	  step[_sub][indirect][9].set(select_subtract);

	  step[_and][direct][4].set(load_addr_from_ir);
	  step[_and][direct][5].set(load_x_from_ac);
	  step[_and][direct][5].set(load_y_from_memory);
	  step[_and][direct][6].set(select_and);
	  step[_and][direct][6].set(load_ac_from_alu);
	  step[_and][direct][7].set(select_and);
	  step[_and][constant][4].set(load_y_from_ir);
	  step[_and][constant][4].set(load_x_from_ac);
	  step[_and][constant][5].set(load_ac_from_alu);
	  step[_and][constant][5].set(select_and);
	  step[_and][constant][6].set(select_and);
	  step[_and][indirect][4].set(load_addr_from_ir);
	  step[_and][indirect][5].set(load_y_from_memory);
	  step[_and][indirect][6].set(load_addr_from_y);
	  step[_and][indirect][7].set(load_x_from_ac);
	  step[_and][indirect][7].set(load_y_from_memory);
	  step[_and][indirect][8].set(load_ac_from_alu);
	  step[_and][indirect][8].set(select_and);
	  step[_and][indirect][9].set(select_and);

	  step[_or][direct][4].set(load_addr_from_ir);
	  step[_or][direct][5].set(load_x_from_ac);
	  step[_or][direct][5].set(load_y_from_memory);
	  step[_or][direct][6].set(load_ac_from_alu);
	  step[_or][direct][6].set(select_or);
	  step[_or][direct][7].set(select_or);
	  step[_or][constant][4].set(load_x_from_ac);
	  step[_or][constant][4].set(load_y_from_ir);
	  step[_or][constant][5].set(select_or);
	  step[_or][constant][5].set(load_ac_from_alu);
	  step[_or][constant][6].set(select_or);
	  step[_or][indirect][4].set(load_addr_from_ir);
	  step[_or][indirect][5].set(load_y_from_memory);
	  step[_or][indirect][6].set(load_addr_from_y);
	  step[_or][indirect][7].set(load_y_from_memory);
	  step[_or][indirect][7].set(load_x_from_ac);
	  step[_or][indirect][8].set(load_ac_from_alu);
	  step[_or][indirect][8].set(select_or);
	  step[_or][indirect][9].set(select_or);

	  step[_shl][direct][4].set(load_x_from_ac);
	  step[_shl][direct][5].set(select_shift_left);
	  step[_shl][direct][5].set(load_ac_from_alu);
	  step[_shl][direct][5].set(load_flag_from_alu);
	  step[_shl][direct][6].set(select_shift_left);

	  step[_shr][direct][4].set(load_x_from_ac);
	  step[_shr][direct][5].set(select_shift_right);
	  step[_shr][direct][5].set(load_ac_from_alu);
	  step[_shr][direct][5].set(load_flag_from_alu);
	  step[_shr][direct][6].set(select_shift_right);

	  step[_not][direct][4].set(load_x_from_ac);
	  step[_not][direct][5].set(load_ac_from_alu);
	  step[_not][direct][5].set(load_ac_from_alu);
	  step[_not][direct][6].set(select_not);

	  step[_inc][direct][4].set(increment_ac);

	  step[_dec][direct][4].set(decrement_ac);

	  step[_lod][direct][4].set(load_addr_from_ir);
	  step[_lod][direct][5].set(load_ac_from_memory);
	  step[_lod][constant][4].set(load_ac_from_ir);
	  step[_lod][indirect][4].set(load_addr_from_ir);
	  step[_lod][indirect][5].set(load_y_from_memory);
	  step[_lod][indirect][6].set(load_addr_from_y);
	  step[_lod][indirect][7].set(load_ac_from_memory);

	  step[_sto][direct][4].set(load_addr_from_ir);
	  step[_sto][direct][5].set(load_data_into_memory);
	  step[_sto][indirect][4].set(load_addr_from_ir);
	  step[_sto][indirect][5].set(load_y_from_memory);
	  step[_sto][indirect][6].set(load_addr_from_y);
	  step[_sto][indirect][7].set(load_data_into_memory);

	  step[_hlt][direct][4].set(stop_clock);

	  step[_jmp][direct][4].set(load_pc_from_ir);
	  step[_jmp][indirect][4].set(load_addr_from_ir);
	  step[_jmp][indirect][5].set(load_pc_from_memory);
	  
      for (int i = _add; i <= _jmp; i++)
        for (int j = 0; j < 4; j++)
          for (int k = 4; k < 11; k++)
              if (step[i][j][k].equals(b0))
                  step[i][j][k].set(set_count_to_zero);


	  step[_jmz] = step[_jmp];
	  step[_jmn] = step[_jmp];
	  step[_jmf] = step[_jmp];


    }  // end of static initializer

}