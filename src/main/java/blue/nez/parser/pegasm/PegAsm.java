/***********************************************************************
 * Copyright 2017 Kimio Kuramitsu and ORIGAMI project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***********************************************************************/

package blue.nez.parser.pegasm;

import java.lang.reflect.Field;
import java.util.HashMap;

import blue.nez.ast.Symbol;
import blue.nez.parser.ParserCode.MemoPoint;
import blue.nez.parser.PegAsmInst;
import blue.origami.util.OStringUtils;

public class PegAsm {
	public final static String[][] Specification = { //
			//
			{ "Nop", "name" }, // name is for debug symbol
			{ "Exit", "state" }, //
			{ "Cov", "uid", "state" }, //
			{ "Trap", "uid" }, //

			{ "Pos" }, //
			{ "Back" }, //
			{ "Move", "shift" }, //
			{ "Jump", "jump" }, //
			{ "Call", "jump", "uname" }, // name is for debug symbol
			{ "Ret" }, //
			{ "Alt", "jump" }, //
			{ "Succ" }, //
			{ "Fail" }, //
			{ "Guard" }, //
			{ "Step" }, //

			// Matching
			{ "Byte", "byteChar" }, //
			{ "Set", "byteSet" }, //
			{ "Str", "utf8" }, //
			{ "Any" }, //

			{ "NByte", "byteChar" }, //
			{ "NSet", "byteSet" }, //
			{ "NStr", "utf8" }, //
			{ "NAny" }, //

			{ "OByte", "byteChar" }, //
			{ "OSet", "byteSet" }, //
			{ "OStr", "utf8" }, //

			{ "RByte", "byteChar" }, //
			{ "RSet", "byteSet" }, //
			{ "RStr", "utf8" }, //

			// DFA instructions
			{ "Dispatch", "jumpIndex", "jumpTable" }, //
			{ "DDispatch", "jumpIndex", "jumpTable" }, //

			// AST Construction
			{ "TPush" }, //
			{ "TPop" }, //
			{ "TBegin", "shift" }, //
			{ "TEnd", "shift", "tag", "value" }, //
			{ "TTag", "tag" }, //
			{ "TReplace", "value" }, //
			{ "TLink", "label" }, //
			{ "TFold", "shift", "label" }, //
			{ "TEmit", "label" }, //

			// Symbol instructions
			{ "SOpen" }, //
			{ "SClose" }, //
			{ "SMask", "table" }, //
			{ "SDef", "table" }, //
			{ "SExists", "table" }, //
			{ "SIsDef", "table", "utf8" }, //
			{ "SMatch", "table" }, //
			{ "SIs", "table" }, //
			{ "SIsa", "table" }, //

			// Read N, Repeat N
			{ "NScan", "mask", "shift" }, //
			{ "NDec", "jump" }, //

			// Memoization
			{ "Lookup", "jump", "uid" }, //
			{ "Memo", "uid" }, //
			{ "MemoFail", "uid" }, //
			{ "TLookup", "jump", "uid" }, //
			{ "TMemo", "uid" }, //

	};

	static HashMap<String, String[]> specMap = new HashMap<>();
	static HashMap<String, java.lang.Byte> opcodeMap = new HashMap<>();

	static {
		for (String[] insts : Specification) {
			opcodeMap.put(insts[0], (byte) opcodeMap.size());
			specMap.put(insts[0], insts);
		}
	}

	static byte opCode(String name) {
		return opcodeMap.get(name);
	}

	static int opSize(String name) {
		String[] insts = specMap.get(name);
		if (insts == null) {
			System.out.println("NoSuchInstruction: " + name);
		}
		return insts == null ? 0 : insts.length - 1;
	}

	static Object opValue(PegAsmInst inst, int p) {
		String[] insts = specMap.get(inst.getName());
		try {
			Field f = inst.getClass().getField(insts[p + 1]);
			return f.get(inst);
		} catch (NoSuchFieldException e) {
			System.out.println("NoSuchField: " + insts[p + 1] + " of " + inst.getClass());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void stringfy(PegAsmInst inst, StringBuilder sb) {
		String name = inst.getName();
		String[] insts = specMap.get(name);
		sb.append(name.toLowerCase());
		int size = opSize(name);
		for (int i = 0; i < size; i++) {
			sb.append(" ");
			Object value = opValue(inst, i);
			switch (insts[i + 1]) {
			case "jump":
				if (value == null) {
					sb.append("null");
					break;
				}
				sb.append("L" + ((PegAsmInst) value).id);
				break;
			case "utf8":
				OStringUtils.formatUTF8(sb, (byte[]) value);
				break;
			case "byteSet":
				OStringUtils.formatHexicalByteSet(sb, (boolean[]) value);
				break;
			case "tag":
				if (value != null) {
					sb.append("#");
				}
				sb.append(value);
				break;
			case "label":
				if (value != null) {
					sb.append("$");
				}
				sb.append(value);
				break;
			default:
				sb.append(value);
			}
		}
	}

	public static PegAsmInst joinPoint(PegAsmInst inst) {
		if (inst != null) {
			inst.joinPoint = true;
		}
		return inst;
	}

	static abstract class AbstByte extends PegAsmInst {
		public final int byteChar;

		AbstByte(int byteChar, PegAsmInst next) {
			super(next);
			this.byteChar = byteChar;
		}

	}

	static abstract class AbstAny extends PegAsmInst {
		AbstAny(PegAsmInst next) {
			super(next);
		}

	}

	// static abstract class AbstSet extends PegAsmInstruction {
	// public final boolean[] bools;
	//
	// AbstSet(boolean[] byteMap, PegAsmInstruction next) {
	// super(next);
	// this.bools = byteMap;
	// }
	//
	// }

	static abstract class AbstStr extends PegAsmInst {
		public final byte[] utf8;

		public AbstStr(byte[] utf8, PegAsmInst next) {
			super(next);
			this.utf8 = utf8;
		}
	}

	// Tree Construction

	/* Symbol */

	static abstract class AbstractTableInstruction extends PegAsmInst {
		public final Symbol label;

		AbstractTableInstruction(Symbol label, PegAsmInst next) {
			super(next);
			this.label = label;
		}

	}

	/* Number */

	/* Memoization */

	static abstract class AbstMemo extends PegAsmInst {
		final MemoPoint memoPoint;
		public final int uid;
		public final boolean state;
		public final PegAsmInst jump;

		AbstMemo(MemoPoint m, boolean state, PegAsmInst next, PegAsmInst skip) {
			super(next);
			this.memoPoint = m;
			this.uid = m.id;
			this.jump = joinPoint(skip);
			this.state = state;
		}

		AbstMemo(MemoPoint m, boolean state, PegAsmInst next) {
			super(next);
			this.memoPoint = m;
			this.uid = m.id;
			this.state = state;
			this.jump = null;
		}

	}

}
