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

package blue.origami.nez.parser;

import java.lang.reflect.Field;
import java.util.HashMap;

import blue.origami.nez.ast.Symbol;
import blue.origami.nez.parser.ParserCode.MemoPoint;
import blue.origami.util.OStringUtils;

public class NZ86 {
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

	static Object opValue(NZ86Instruction inst, int p) {
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

	static void stringfy(NZ86Instruction inst, StringBuilder sb) {
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
				sb.append("L" + ((NZ86Instruction) value).id);
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

	protected static NZ86Instruction joinPoint(NZ86Instruction inst) {
		if (inst != null) {
			inst.joinPoint = true;
		}
		return inst;
	}

	public final static class Nop extends NZ86Instruction {
		public final String name;

		public Nop(String name, NZ86Instruction next) {
			super(next);
			this.name = name;
		}

		@Override
		public void visit(NZ86Visitor v) {
			v.visitNop(this);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			return this.next;
		}

	}

	public final static class Exit extends NZ86Instruction {
		public final boolean status;

		public Exit(boolean status) {
			super(null);
			this.status = status;
		}

		@Override
		public void visit(NZ86Visitor v) {
			v.visitExit(this);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			throw new ParserTerminationException(status);
		}
	}

	public final static class Trap extends NZ86Instruction {
		public final int type;
		public final int uid;

		public Trap(int type, int uid, NZ86Instruction next) {
			super(next);
			this.type = type;
			this.uid = uid;
		}

		@Override
		public void visit(NZ86Visitor v) {
			v.visitTrap(this);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			sc.trap(type, uid);
			return this.next;
		}
	}

	public final static class Pos extends NZ86Instruction {
		public Pos(NZ86Instruction next) {
			super(next);
		}

		@Override
		public void visit(NZ86Visitor v) {
			v.visitPos(this);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			sc.xPos();
			return this.next;
		}

	}

	public final static class Back extends NZ86Instruction {
		public Back(NZ86Instruction next) {
			super(next);
		}

		@Override
		public void visit(NZ86Visitor v) {
			v.visitBack(this);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			sc.xBack();
			return this.next;
		}

	}

	public final static class Move extends NZ86Instruction {
		public final int shift;

		public Move(int shift, NZ86Instruction next) {
			super(next);
			this.shift = shift;
		}

		@Override
		public void visit(NZ86Visitor v) {
			v.visitMove(this);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			sc.move(this.shift);
			return this.next;
		}

	}

	public final static class Jump extends NZ86Instruction {
		public NZ86Instruction jump = null;

		public Jump(NZ86Instruction jump) {
			super(null);
			this.jump = jump;
		}

		@Override
		public void visit(NZ86Visitor v) {
			v.visitJump(this);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			return this.jump;
		}

	}

	public static class Call extends NZ86Instruction {
		public NZ86Instruction jump = null;
		public String uname;

		public Call(String uname, NZ86Instruction next) {
			super(joinPoint(next));
			this.uname = uname;
		}

		public Call(NZ86Instruction jump, String uname, NZ86Instruction next) {
			super(joinPoint(jump));
			this.uname = uname;
			this.jump = joinPoint(next);
		}

		public final String getNonTerminalName() {
			return this.uname;
		}

		@Override
		public void visit(NZ86Visitor v) {
			v.visitCall(this);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			sc.xCall(uname, jump);
			return this.next;
		}

	}

	public final static class Ret extends NZ86Instruction {
		public Ret() {
			super(null);
		}

		@Override
		public void visit(NZ86Visitor v) {
			v.visitRet(this);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			return sc.xRet();
		}

	}

	public final static class Alt extends NZ86Instruction {
		public final NZ86Instruction jump;

		public Alt(NZ86Instruction failjump, NZ86Instruction next) {
			super(next);
			this.jump = joinPoint(failjump);
		}

		@Override
		public void visit(NZ86Visitor v) {
			v.visitAlt(this);
		}

		@Override
		NZ86Instruction branch() {
			return this.jump;
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			sc.xAlt(jump);
			return this.next;
		}
	}

	public final static class Succ extends NZ86Instruction {
		public Succ(NZ86Instruction next) {
			super(next);
		}

		@Override
		public void visit(NZ86Visitor v) {
			v.visitSucc(this);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			sc.xSucc();
			return this.next;
		}
	}

	public final static class Fail extends NZ86Instruction {
		public Fail() {
			super(null);
		}

		@Override
		public void visit(NZ86Visitor v) {
			v.visitFail(this);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			return sc.xFail();
		}

	}

	public final static class Guard extends NZ86Instruction {
		public Guard() {
			super(null);
		}

		@Override
		public void visit(NZ86Visitor v) {
			v.visitGuard(this);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			return sc.xStep(this.next);
		}
	}

	public final static class Step extends NZ86Instruction {
		public Step() {
			super(null);
		}

		@Override
		public void visit(NZ86Visitor v) {
			v.visitStep(this);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			return sc.xStep(this.next);
		}
	}

	/**
	 * Byte
	 * 
	 * @author kiki
	 *
	 */

	static abstract class AbstByte extends NZ86Instruction {
		public final int byteChar;

		AbstByte(int byteChar, NZ86Instruction next) {
			super(next);
			this.byteChar = byteChar;
		}

	}

	public static class Byte extends AbstByte {
		public Byte(int byteChar, NZ86Instruction next) {
			super(byteChar, next);
		}

		@Override
		public void visit(NZ86Visitor v) {
			v.visitByte(this);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			/* EOF must be checked at the next instruction */
			if (sc.read() == this.byteChar) {
				return this.next;
			}
			return sc.xFail();
		}
	}

	public final static class BinaryByte extends Byte {
		public BinaryByte(NZ86Instruction next) {
			super(0, next);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			if (sc.prefetch() == 0 && !sc.eof()) {
				sc.move(1);
				return this.next;
			}
			return sc.xFail();
		}
	}

	public static class NByte extends AbstByte {
		public NByte(int byteChar, NZ86Instruction next) {
			super(byteChar, next);
		}

		@Override
		public void visit(NZ86Visitor v) {
			v.visitNByte(this);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			if (sc.prefetch() != this.byteChar) {
				return this.next;
			}
			return sc.xFail();
		}

	}

	public final static class BinaryNByte extends NByte {
		public BinaryNByte(int byteChar, NZ86Instruction next) {
			super(byteChar, next);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			if (sc.prefetch() != this.byteChar && !sc.eof()) {
				return this.next;
			}
			return sc.xFail();
		}
	}

	public static class OByte extends AbstByte {
		public OByte(int byteChar, NZ86Instruction next) {
			super(byteChar, next);
		}

		@Override
		public void visit(NZ86Visitor v) {
			v.visitOByte(this);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			if (sc.prefetch() == this.byteChar) {
				if (this.byteChar == 0) {
					return this.next;
				}
				sc.move(1);
			}
			return this.next;
		}
	}

	public static class BinaryOByte extends OByte {
		public BinaryOByte(NZ86Instruction next) {
			super(0, next);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			if (sc.prefetch() == 0 && !sc.eof()) {
				sc.move(1);
			}
			return this.next;
		}
	}

	public static class RByte extends AbstByte {
		public RByte(int byteChar, NZ86Instruction next) {
			super(byteChar, next);
		}

		@Override
		public void visit(NZ86Visitor v) {
			v.visitRByte(this);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			while (sc.prefetch() == this.byteChar) {
				sc.move(1);
			}
			return this.next;
		}
	}

	public static class BinaryRByte extends RByte {
		public BinaryRByte(NZ86Instruction next) {
			super(0, next);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			while (sc.prefetch() == 0 && !sc.eof()) {
				sc.move(1);
			}
			return this.next;
		}
	}

	static abstract class AbstAny extends NZ86Instruction {
		AbstAny(NZ86Instruction next) {
			super(next);
		}

	}

	public final static class Any extends AbstAny {
		public Any(NZ86Instruction next) {
			super(next);
		}

		@Override
		public void visit(NZ86Visitor v) {
			v.visitAny(this);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			if (!sc.eof()) {
				sc.move(1);
				return this.next;
			}
			return sc.xFail();
		}
	}

	public final static class NAny extends AbstAny {

		public NAny(NZ86Instruction next) {
			super(next);
		}

		@Override
		public void visit(NZ86Visitor v) {
			v.visitNAny(this);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			if (sc.eof()) {
				return next;
			}
			return sc.xFail();
		}
	}

	static abstract class AbstSet extends NZ86Instruction {
		public final boolean[] byteSet;

		AbstSet(boolean[] byteMap, NZ86Instruction next) {
			super(next);
			this.byteSet = byteMap;
		}

	}

	public static class Set extends AbstSet {
		public Set(boolean[] byteMap, NZ86Instruction next) {
			super(byteMap, next);
		}

		@Override
		public void visit(NZ86Visitor v) {
			v.visitSet(this);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			int byteChar = sc.read();
			if (byteSet[byteChar]) {
				return this.next;
			}
			return sc.xFail();
		}

	}

	public final static class BinarySet extends Set {
		public BinarySet(boolean[] byteMap, NZ86Instruction next) {
			super(byteMap, next);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			int byteChar = sc.prefetch();
			if (byteSet[byteChar] && !sc.eof()) {
				sc.move(1);
				return this.next;
			}
			return sc.xFail();
		}

	}

	public static class OSet extends AbstSet {
		public OSet(boolean[] byteMap, NZ86Instruction next) {
			super(byteMap, next);
		}

		@Override
		public void visit(NZ86Visitor v) {
			v.visitOSet(this);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			int byteChar = sc.prefetch();
			if (byteSet[byteChar]) {
				sc.move(1);
			}
			return this.next;
		}

	}

	public static class BinaryOSet extends OSet {
		public BinaryOSet(boolean[] byteMap, NZ86Instruction next) {
			super(byteMap, next);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			int byteChar = sc.prefetch();
			if (byteSet[byteChar] && sc.eof()) {
				sc.move(1);
			}
			return this.next;
		}
	}

	public static class NSet extends AbstSet {
		public NSet(boolean[] byteMap, NZ86Instruction next) {
			super(byteMap, next);
		}

		@Override
		public void visit(NZ86Visitor v) {
			v.visitNSet(this);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			int byteChar = sc.prefetch();
			if (!byteSet[byteChar]) {
				return this.next;
			}
			return sc.xFail();
		}

	}

	public final static class BinaryNSet extends NSet {
		public BinaryNSet(boolean[] byteMap, NZ86Instruction next) {
			super(byteMap, next);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			int byteChar = sc.prefetch();
			if (!byteSet[byteChar] && !sc.eof()) {
				return this.next;
			}
			return sc.xFail();
		}

	}

	public static class RSet extends AbstSet {
		public RSet(boolean[] byteMap, NZ86Instruction next) {
			super(byteMap, next);
		}

		@Override
		public void visit(NZ86Visitor v) {
			v.visitRSet(this);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			while (byteSet[sc.prefetch()]) {
				sc.move(1);
			}
			return this.next;
		}

	}

	public static class BinaryRSet extends RSet {
		public BinaryRSet(boolean[] byteMap, NZ86Instruction next) {
			super(byteMap, next);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			while (byteSet[sc.prefetch()] && !sc.eof()) {
				sc.move(1);
			}
			return this.next;
		}

	}

	static abstract class AbstStr extends NZ86Instruction {
		public final byte[] utf8;

		public AbstStr(byte[] utf8, NZ86Instruction next) {
			super(next);
			this.utf8 = utf8;
		}
	}

	public final static class Str extends AbstStr {
		public Str(byte[] byteSeq, NZ86Instruction next) {
			super(byteSeq, next);
		}

		@Override
		public void visit(NZ86Visitor v) {
			v.visitStr(this);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			if (sc.match(this.utf8)) {
				return this.next;
			}
			return sc.xFail();
		}

	}

	public final static class NStr extends AbstStr {

		public NStr(byte[] byteSeq, NZ86Instruction next) {
			super(byteSeq, next);
		}

		@Override
		public void visit(NZ86Visitor v) {
			v.visitNStr(this);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			if (!sc.match(this.utf8)) {
				return this.next;
			}
			return sc.xFail();
		}

	}

	public final static class OStr extends AbstStr {
		public OStr(byte[] byteSeq, NZ86Instruction next) {
			super(byteSeq, next);
		}

		@Override
		public void visit(NZ86Visitor v) {
			v.visitOStr(this);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			sc.match(this.utf8);
			return this.next;
		}

	}

	public final static class RStr extends AbstStr {
		public RStr(byte[] byteSeq, NZ86Instruction next) {
			super(byteSeq, next);
		}

		@Override
		public void visit(NZ86Visitor v) {
			v.visitRStr(this);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			while (sc.match(this.utf8)) {
			}
			return this.next;
		}

	}

	public static class Dispatch extends NZ86Instruction {
		public final byte[] jumpIndex;
		public final NZ86Instruction[] jumpTable;

		public Dispatch(byte[] jumpIndex, NZ86Instruction[] jumpTable) {
			super(null);
			this.jumpIndex = jumpIndex;
			this.jumpTable = jumpTable;
		}

		@Override
		public void visit(NZ86Visitor v) {
			v.visitDispatch(this);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			int ch = sc.prefetch();
			return jumpTable[jumpIndex[ch] & 0xff];
		}

	}

	public final static class DDispatch extends Dispatch {
		public DDispatch(byte[] jumpIndex, NZ86Instruction[] jumpTable) {
			super(jumpIndex, jumpTable);
		}

		@Override
		public void visit(NZ86Visitor v) {
			v.visitDDispatch(this);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			return jumpTable[jumpIndex[sc.read()] & 0xff];
		}

	}

	// Tree Construction

	public final static class TPush extends NZ86Instruction {
		public TPush(NZ86Instruction next) {
			super(next);
		}

		@Override
		public void visit(NZ86Visitor v) {
			v.visitTPush(this);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			sc.xTPush();
			return this.next;
		}

	}

	public final static class TPop extends NZ86Instruction {

		public TPop(NZ86Instruction next) {
			super(next);
		}

		@Override
		public void visit(NZ86Visitor v) {
			v.visitTPop(this);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			sc.xTPop();
			return this.next;
		}

	}

	public final static class TBegin extends NZ86Instruction {
		public final int shift;

		public TBegin(int shift, NZ86Instruction next) {
			super(next);
			this.shift = shift;
		}

		@Override
		public void visit(NZ86Visitor v) {
			v.visitTBegin(this);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			sc.beginTree(shift);
			return this.next;
		}
	}

	public final static class TEnd extends NZ86Instruction {
		public final int shift;
		public final Symbol tag;
		public final String value;

		public TEnd(Symbol tag, String value, int shift, NZ86Instruction next) {
			super(next);
			this.tag = tag;
			this.value = value;
			this.shift = shift;
		}

		@Override
		public void visit(NZ86Visitor v) {
			v.visitTEnd(this);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			sc.endTree(shift, tag, value);
			return this.next;
		}
	}

	public final static class TTag extends NZ86Instruction {
		public final Symbol tag;

		public TTag(Symbol tag, NZ86Instruction next) {
			super(next);
			this.tag = tag;
		}

		@Override
		public void visit(NZ86Visitor v) {
			v.visitTTag(this);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			sc.tagTree(tag);
			return this.next;
		}

	}

	public final static class TReplace extends NZ86Instruction {
		public final String value;

		public TReplace(String value, NZ86Instruction next) {
			super(next);
			this.value = value;
		}

		@Override
		public void visit(NZ86Visitor v) {
			v.visitTReplace(this);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			sc.valueTree(value);
			return this.next;
		}

	}

	public final static class TLink extends NZ86Instruction {
		public final Symbol label;

		public TLink(Symbol label, NZ86Instruction next) {
			super(next);
			this.label = label;
		}

		@Override
		public void visit(NZ86Visitor v) {
			v.visitTLink(this);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			sc.xTLink(label);
			return this.next;
		}

	}

	public final static class TFold extends NZ86Instruction {
		public final int shift;
		public final Symbol label;

		public TFold(Symbol label, int shift, NZ86Instruction next) {
			super(next);
			this.label = label;
			this.shift = shift;
		}

		@Override
		public void visit(NZ86Visitor v) {
			v.visitTFold(this);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			sc.foldTree(shift, label);
			return this.next;
		}
	}

	public final static class TEmit extends NZ86Instruction {
		public final Symbol label;

		public TEmit(Symbol label, NZ86Instruction next) {
			super(next);
			this.label = label;
		}

		@Override
		public void visit(NZ86Visitor v) {
			v.visitTEmit(this);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			return this.next;
		}

	}

	// public final static class TStart extends MozInst {
	// public TStart(MozInst next) {
	// super(next);
	// }
	//
	// @Override
	// public void visit(InstructionVisitor v) {
	// v.visitTStart(this);
	// }
	//
	// @Override
	// public MozInst exec(ParserMachineContext<?> sc) throws
	// TerminationException {
	// return this.next;
	// }
	//
	// }

	/* Symbol */

	static abstract class AbstractTableInstruction extends NZ86Instruction {
		public final Symbol table;

		AbstractTableInstruction(Symbol tableName, NZ86Instruction next) {
			super(next);
			this.table = tableName;
		}

	}

	public final static class SOpen extends NZ86Instruction {
		public SOpen(NZ86Instruction next) {
			super(next);
		}

		@Override
		public void visit(NZ86Visitor v) {
			v.visitSOpen(this);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			sc.xSOpen();
			return this.next;
		}

	}

	public final static class SClose extends NZ86Instruction {
		public SClose(NZ86Instruction next) {
			super(next);
		}

		@Override
		public void visit(NZ86Visitor v) {
			v.visitSClose(this);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			sc.xSClose();
			return this.next;
		}
	}

	public final static class SMask extends AbstractTableInstruction {
		public SMask(Symbol tableName, NZ86Instruction next) {
			super(tableName, next);
		}

		@Override
		public void visit(NZ86Visitor v) {
			v.visitSMask(this);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			sc.xSOpen();
			sc.addSymbolMask(table);
			return this.next;
		}

	}

	public final static class SDef extends AbstractTableInstruction {
		public SDef(Symbol tableName, NZ86Instruction next) {
			super(tableName, next);
		}

		@Override
		public void visit(NZ86Visitor v) {
			v.visitSDef(this);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			int ppos = sc.xPPos();
			sc.addSymbol(table, ppos);
			return this.next;
		}
	}

	public final static class SExists extends AbstractTableInstruction {
		public SExists(Symbol tableName, NZ86Instruction next) {
			super(tableName, next);
		}

		@Override
		public void visit(NZ86Visitor v) {
			v.visitSExists(this);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			return sc.exists(table) ? this.next : sc.xFail();
		}

	}

	public final static class SIsDef extends AbstractTableInstruction {
		public final byte[] utf8;

		public SIsDef(Symbol tableName, byte[] utf8, NZ86Instruction next) {
			super(tableName, next);
			this.utf8 = utf8;
		}

		@Override
		public void visit(NZ86Visitor v) {
			v.visitSIsDef(this);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			return sc.existsSymbol(table, utf8) ? this.next : sc.xFail();
		}
	}

	public final static class SMatch extends AbstractTableInstruction {
		public SMatch(Symbol tableName, NZ86Instruction next) {
			super(tableName, next);
		}

		@Override
		public void visit(NZ86Visitor v) {
			v.visitSMatch(this);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			return sc.matchSymbol(table) ? this.next : sc.xFail();
		}

	}

	public final static class SIs extends AbstractTableInstruction {
		public SIs(Symbol tableName, NZ86Instruction next) {
			super(tableName, next);
		}

		@Override
		public void visit(NZ86Visitor v) {
			v.visitSIs(this);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			int ppos = sc.xPPos();
			return sc.equals(table, ppos) ? this.next : sc.xFail();
		}

	}

	public final static class SIsa extends AbstractTableInstruction {
		public SIsa(Symbol tableName, NZ86Instruction next) {
			super(tableName, next);
		}

		@Override
		public void visit(NZ86Visitor v) {
			v.visitSIsa(this);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			int ppos = sc.xPPos();
			return sc.contains(table, ppos) ? this.next : sc.xFail();
		}

	}

	/* Number */

	public final static class NScan extends NZ86Instruction {
		public final long mask;
		public final int shift;

		public NScan(long mask, int shift, NZ86Instruction next) {
			super(next);
			this.mask = mask;
			this.shift = shift;
		}

		@Override
		public void visit(NZ86Visitor v) {
			v.visitNScan(this);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			int ppos = sc.xPPos();
			sc.scanCount(ppos, mask, shift);
			return next;
		}
	}

	public final static class NDec extends NZ86Instruction {
		public final NZ86Instruction jump;

		public NDec(NZ86Instruction jump, NZ86Instruction next) {
			super(next);
			this.jump = jump;
		}

		@Override
		public void visit(NZ86Visitor v) {
			v.visitNDec(this);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			return sc.decCount() ? this.next : this.jump;
		}
	}

	/* Memoization */

	static abstract class AbstMemo extends NZ86Instruction {
		final MemoPoint memoPoint;
		public final int uid;
		public final boolean state;
		public final NZ86Instruction jump;

		AbstMemo(MemoPoint m, boolean state, NZ86Instruction next, NZ86Instruction skip) {
			super(next);
			this.memoPoint = m;
			this.uid = m.id;
			this.jump = joinPoint(skip);
			this.state = state;
		}

		AbstMemo(MemoPoint m, boolean state, NZ86Instruction next) {
			super(next);
			this.memoPoint = m;
			this.uid = m.id;
			this.state = state;
			this.jump = null;
		}

	}

	public final static class Lookup extends AbstMemo {
		public Lookup(MemoPoint m, NZ86Instruction next, NZ86Instruction skip) {
			super(m, m.isStateful(), next, skip);
		}

		@Override
		public void visit(NZ86Visitor v) {
			v.visitLookup(this);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			switch (sc.lookupMemo(uid)) {
			case ParserContext.NotFound:
				return this.next;
			case ParserContext.SuccFound:
				return this.jump;
			default:
				return sc.xFail();
			}
		}
	}

	public final static class Memo extends AbstMemo {
		public Memo(MemoPoint m, NZ86Instruction next) {
			super(m, m.isStateful(), next);
		}

		@Override
		public void visit(NZ86Visitor v) {
			v.visitMemo(this);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			int ppos = sc.xSuccPos();
			sc.memoSucc(uid, ppos);
			return this.next;
		}
	}

	public final static class MemoFail extends AbstMemo {
		public MemoFail(MemoPoint m) {
			super(m, m.isStateful(), null);
		}

		@Override
		public void visit(NZ86Visitor v) {
			v.visitMemoFail(this);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			sc.memoFail(uid);
			return sc.xFail();
		}

	}

	public final static class TLookup extends AbstMemo {
		public final Symbol label;

		public TLookup(MemoPoint m, NZ86Instruction next, NZ86Instruction skip) {
			super(m, m.isStateful(), next, skip);
			this.label = null;
		}

		@Override
		public void visit(NZ86Visitor v) {
			v.visitTLookup(this);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			switch (sc.lookupTreeMemo(uid)) {
			case ParserContext.NotFound:
				return this.next;
			case ParserContext.SuccFound:
				return this.jump;
			default:
				return sc.xFail();
			}
		}

	}

	public final static class TMemo extends AbstMemo {
		public TMemo(MemoPoint m, NZ86Instruction next) {
			super(m, m.isStateful(), next);
		}

		@Override
		public void visit(NZ86Visitor v) {
			v.visitTMemo(this);
		}

		@Override
		public NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException {
			int ppos = sc.xSuccPos();
			sc.memoTreeSucc(uid, ppos);
			return this.next;
		}

	}

}
