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

package blue.nez.parser;

import java.util.ArrayList;
import java.util.List;

import blue.nez.parser.ParserCode.MemoPoint;
import blue.nez.peg.Expression;
import blue.nez.peg.ExpressionVisitor;
import blue.nez.peg.NezFunc;
import blue.nez.peg.Production;
import blue.nez.peg.Typestate;
import blue.nez.peg.expression.PAnd;
import blue.nez.peg.expression.PAny;
import blue.nez.peg.expression.PByte;
import blue.nez.peg.expression.PByteSet;
import blue.nez.peg.expression.PChoice;
import blue.nez.peg.expression.PDetree;
import blue.nez.peg.expression.PDispatch;
import blue.nez.peg.expression.PEmpty;
import blue.nez.peg.expression.PFail;
import blue.nez.peg.expression.PIfCondition;
import blue.nez.peg.expression.PLinkTree;
import blue.nez.peg.expression.PNonTerminal;
import blue.nez.peg.expression.PNot;
import blue.nez.peg.expression.POnCondition;
import blue.nez.peg.expression.POption;
import blue.nez.peg.expression.PPair;
import blue.nez.peg.expression.PRepeat;
import blue.nez.peg.expression.PRepetition;
import blue.nez.peg.expression.PReplace;
import blue.nez.peg.expression.PScan;
import blue.nez.peg.expression.PSymbolAction;
import blue.nez.peg.expression.PSymbolPredicate;
import blue.nez.peg.expression.PSymbolScope;
import blue.nez.peg.expression.PTag;
import blue.nez.peg.expression.PTrap;
import blue.nez.peg.expression.PTree;
import blue.origami.util.OOption;
import blue.origami.util.OStringUtils;

public class NZ86Compiler implements ParserCompiler {

	public NZ86Compiler() {

	}

	@Override
	public ParserCompiler clone() {
		return new NZ86Compiler();
	}

	// Local Option
	OOption options = null;
	// boolean TreeConstruction = true;
	boolean enableMemo = false;
	// boolean BinaryGrammar = false;
	// boolean Optimization = true;

	@Override
	public void init(OOption options) {
		this.options = options;
		// this.TreeConstruction = options.is(ParserOption.TreeConstruction,
		// true);
		this.enableMemo = options.is(ParserOption.PackratParsing, true);
	}

	@Override
	public NZ86Code compile(ParserGrammar grammar) {
		NZ86Code code = new NZ86Code(grammar, this.options);
		if (this.enableMemo) {
			code.initMemoPoint();
		}
		new CompilerVisitor(code, grammar, this.options).compileAll();
		return code;
	}

	class CompilerVisitor extends ExpressionVisitor<NZ86Instruction, NZ86Instruction> {

		final NZ86Code code;
		final ParserGrammar grammar;

		boolean TreeConstruction = true;
		boolean enableMemo = false;
		boolean BinaryGrammar = false;
		boolean Optimization = true;

		CompilerVisitor(NZ86Code code, ParserGrammar grammar, OOption options) {
			this.code = code;
			this.grammar = grammar;
			this.BinaryGrammar = grammar.isBinary();
			this.TreeConstruction = options.is(ParserOption.TreeConstruction, true);
			this.enableMemo = options.is(ParserOption.PackratParsing, true);

		}

		private NZ86Code compileAll() {
			for (Production p : this.grammar) {
				this.compileProduction(this.code.codeList(), p, new NZ86.Ret());
			}
			for (NZ86Instruction inst : this.code.codeList()) {
				if (inst instanceof NZ86.Call) {
					NZ86.Call call = (NZ86.Call) inst;
					if (call.jump == null) {
						call.jump = call.next;
						call.next = NZ86.joinPoint(this.code.getInstruction(call.uname));// f.getCompiled();
					}
				}
			}
			return this.code;
		}

		protected void compileProduction(List<NZ86Instruction> codeList, Production p, NZ86Instruction next) {
			MemoPoint memoPoint = this.code.getMemoPoint(p.getUniqueName());
			next = this.compileProductionExpression(memoPoint, p.getExpression(), next);
			this.code.setInstruction(p.getUniqueName(), next);
			NZ86Instruction block = new NZ86.Nop(p.getUniqueName(), next);
			this.layoutCode(codeList, block);
		}

		private NZ86Instruction compileProductionExpression(MemoPoint memoPoint, Expression p, NZ86Instruction next) {
			if (memoPoint != null) {
				if (memoPoint.typeState == Typestate.Unit) {
					NZ86Instruction memo = new NZ86.Memo(memoPoint, next);
					NZ86Instruction inside = this.compile(p, memo);
					NZ86Instruction failmemo = new NZ86.MemoFail(memoPoint);
					inside = new NZ86.Alt(failmemo, inside);
					return new NZ86.Lookup(memoPoint, inside, next);
				} else {
					NZ86Instruction memo = new NZ86.TMemo(memoPoint, next);
					NZ86Instruction inside = this.compile(p, memo);
					NZ86Instruction failmemo = new NZ86.MemoFail(memoPoint);
					inside = new NZ86.Alt(failmemo, inside);
					return new NZ86.TLookup(memoPoint, inside, next);
				}
			}
			return this.compile(p, next);
		}

		private void layoutCode(List<NZ86Instruction> codeList, NZ86Instruction inst) {
			if (inst == null) {
				return;
			}
			if (inst.id == -1) {
				inst.id = codeList.size();
				codeList.add(inst);
				this.layoutCode(codeList, inst.next);
				// if (inst.next != null && inst.id + 1 != inst.next.id) {
				// MozInst.joinPoint(inst.next);
				// }
				this.layoutCode(codeList, inst.branch());
				if (inst instanceof NZ86.Dispatch) {
					NZ86.Dispatch match = (NZ86.Dispatch) inst;
					for (int ch = 0; ch < match.jumpTable.length; ch++) {
						this.layoutCode(codeList, match.jumpTable[ch]);
					}
				}
			}
		}

		// encoding

		private NZ86Instruction compile(Expression e, NZ86Instruction next) {
			return e.visit(this, next);
		}

		@Override
		public NZ86Instruction visitEmpty(PEmpty p, NZ86Instruction next) {
			return next;
		}

		private final NZ86Instruction commonFailure = new NZ86.Fail();

		public NZ86Instruction fail(Expression e) {
			return this.commonFailure;
		}

		@Override
		public NZ86Instruction visitFail(PFail p, NZ86Instruction next) {
			return this.commonFailure;
		}

		@Override
		public NZ86Instruction visitByte(PByte p, NZ86Instruction next) {
			if (/* this.BinaryGrammar && */ p.byteChar() == 0) {
				return new NZ86.BinaryByte(next);
			}
			return new NZ86.Byte(p.byteChar(), next);
		}

		@Override
		public NZ86Instruction visitByteSet(PByteSet p, NZ86Instruction next) {
			boolean[] b = p.byteSet();
			if (this.BinaryGrammar && b[0]) {
				return new NZ86.BinarySet(b, next);
			}
			b[0] = false;
			return new NZ86.Set(b, next);
		}

		@Override
		public NZ86Instruction visitAny(PAny p, NZ86Instruction next) {
			return new NZ86.Any(next);
		}

		@Override
		public final NZ86Instruction visitNonTerminal(PNonTerminal n, NZ86Instruction next) {
			Production p = n.getProduction();
			return new NZ86.Call(p.getUniqueName(), next);
		}

		private byte[] toMultiChar(Expression e) {
			ArrayList<Integer> l = new ArrayList<>();
			Expression.extractString(e, l);
			byte[] utf8 = new byte[l.size()];
			for (int i = 0; i < l.size(); i++) {
				utf8[i] = (byte) (int) l.get(i);
			}
			return utf8;
		}

		@Override
		public final NZ86Instruction visitOption(POption p, NZ86Instruction next) {
			if (this.Optimization) {
				Expression inner = this.getInnerExpression(p);
				if (inner instanceof PByte) {
					if (/* this.BinaryGrammar && */ ((PByte) inner).byteChar() == 0) {
						return new NZ86.BinaryOByte(next);
					}
					return new NZ86.OByte(((PByte) inner).byteChar(), next);
				}
				if (inner instanceof PByteSet) {
					boolean[] b = ((PByteSet) inner).byteSet();
					if (this.BinaryGrammar && b[0]) {
						return new NZ86.BinaryOSet(b, next);
					}
					b[0] = false;
					return new NZ86.OSet(b, next);
				}
				if (Expression.isString(inner)) {
					byte[] utf8 = this.toMultiChar(inner);
					return new NZ86.OStr(utf8, next);
				}
			}
			NZ86Instruction pop = new NZ86.Succ(next);
			return new NZ86.Alt(next, this.compile(p.get(0), pop));
		}

		@Override
		public NZ86Instruction visitRepetition(PRepetition p, NZ86Instruction next) {
			NZ86Instruction next2 = this.compileRepetition(p, next);
			if (p.isOneMore()) {
				next2 = this.compile(p.get(0), next2);
			}
			return next2;
		}

		private NZ86Instruction compileRepetition(PRepetition p, NZ86Instruction next) {
			if (this.Optimization) {
				Expression inner = this.getInnerExpression(p);
				if (inner instanceof PByte) {
					if (/* this.BinaryGrammar && */ ((PByte) inner).byteChar() == 0) {
						return new NZ86.BinaryRByte(next);
					}
					return new NZ86.RByte(((PByte) inner).byteChar(), next);
				}
				if (inner instanceof PByteSet) {
					boolean[] b = ((PByteSet) inner).byteSet();
					if (this.BinaryGrammar && b[0]) {
						return new NZ86.BinaryRSet(b, next);
					}
					b[0] = false;
					return new NZ86.RSet(b, next);
				}
				if (Expression.isString(inner)) {
					byte[] utf8 = this.toMultiChar(inner);
					return new NZ86.RStr(utf8, next);
				}
			}
			NZ86Instruction skip = new NZ86.Step();
			NZ86Instruction start = this.compile(p.get(0), skip);
			skip.next = NZ86.joinPoint(start);
			return new NZ86.Alt(next, start);
		}

		@Override
		public NZ86Instruction visitAnd(PAnd p, NZ86Instruction next) {
			NZ86Instruction inner = this.compile(p.get(0), new NZ86.Back(next));
			return new NZ86.Pos(inner);
		}

		@Override
		public final NZ86Instruction visitNot(PNot p, NZ86Instruction next) {
			if (this.Optimization) {
				Expression inner = this.getInnerExpression(p);
				if (inner instanceof PByte) {
					if (/* this.BinaryGrammar && */ ((PByte) inner).byteChar() != 0) {
						return new NZ86.BinaryNByte(((PByte) inner).byteChar(), next);
					}
					return new NZ86.NByte(((PByte) inner).byteChar(), next);
				}
				if (inner instanceof PByteSet) {
					boolean[] b = ((PByteSet) inner).byteSet();
					if (this.BinaryGrammar && b[0] == false) {
						return new NZ86.BinaryNSet(b, next);
					}
					return new NZ86.NSet(b, next);
				}
				if (inner instanceof PAny) {
					return new NZ86.NAny(next);
				}
				if (Expression.isString(inner)) {
					byte[] utf8 = this.toMultiChar(inner);
					return new NZ86.NStr(utf8, next);
				}
			}
			NZ86Instruction fail = new NZ86.Succ(new NZ86.Fail());
			return new NZ86.Alt(next, this.compile(p.get(0), fail));
		}

		@Override
		public NZ86Instruction visitPair(PPair p, NZ86Instruction next) {
			NZ86Instruction nextStart = next;
			for (int i = p.size() - 1; i >= 0; i--) {
				Expression e = p.get(i);
				nextStart = this.compile(e, nextStart);
			}
			return nextStart;
		}

		@Override
		public final NZ86Instruction visitChoice(PChoice p, NZ86Instruction next) {
			NZ86Instruction nextChoice = this.compile(p.get(p.size() - 1), next);
			for (int i = p.size() - 2; i >= 0; i--) {
				Expression e = p.get(i);
				nextChoice = new NZ86.Alt(nextChoice, this.compile(e, new NZ86.Succ(next)));
			}
			return nextChoice;
		}

		@Override
		public final NZ86Instruction visitDispatch(PDispatch p, NZ86Instruction next) {
			NZ86Instruction[] compiled = new NZ86Instruction[p.size() + 1];
			compiled[0] = this.commonFailure;
			if (this.isAllD(p)) {
				for (int i = 0; i < p.size(); i++) {
					compiled[i + 1] = this.compile(this.nextD(p.get(i)), next);
				}
				return new NZ86.DDispatch(p.indexMap, compiled);
			} else {
				for (int i = 0; i < p.size(); i++) {
					compiled[i + 1] = this.compile(p.get(i), next);
				}
				return new NZ86.Dispatch(p.indexMap, compiled);
			}
		}

		private boolean isAllD(PDispatch p) {
			for (int i = 0; i < p.size(); i++) {
				if (!this.isD(p.get(i))) {
					return false;
				}
			}
			return true;
		}

		private boolean isD(Expression e) {
			if (e instanceof PPair) {
				if (e.get(0) instanceof PAny) {
					return true;
				}
				return false;
			}
			return (e instanceof PAny);
		}

		private Expression nextD(Expression e) {
			if (e instanceof PPair) {
				return e.get(1);
			}
			return Expression.defaultEmpty;
		}

		@Override
		public NZ86Instruction visitTree(PTree p, NZ86Instruction next) {
			if (this.TreeConstruction) {
				next = new NZ86.TEnd(p.tag, p.value, p.endShift, next);
				next = this.compile(p.get(0), next);
				if (p.folding) {
					// System.out.println("@@@ folding" + p);
					return new NZ86.TFold(p.label, p.beginShift, next);
				} else {
					return new NZ86.TBegin(p.beginShift, next);
				}
			}
			return this.compile(p.get(0), next);
		}

		@Override
		public NZ86Instruction visitTag(PTag p, NZ86Instruction next) {
			if (this.TreeConstruction) {
				return new NZ86.TTag(p.tag, next);
			}
			return next;
		}

		@Override
		public NZ86Instruction visitReplace(PReplace p, NZ86Instruction next) {
			if (this.TreeConstruction) {
				return new NZ86.TReplace(p.value, next);
			}
			return next;
		}

		// Tree

		@Override
		public final NZ86Instruction visitLinkTree(PLinkTree p, NZ86Instruction next) {
			if (this.TreeConstruction) {
				next = new NZ86.TLink(p.label, next);
				next = this.compile(p.get(0), next);
				return new NZ86.TPush(next);
			}
			return this.compile(p.get(0), next);
		}

		@Override
		public NZ86Instruction visitDetree(PDetree p, NZ86Instruction next) {
			if (this.TreeConstruction) {
				next = new NZ86.TPop(next);
				next = this.compile(p.get(0), next);
				return new NZ86.TPush(next);
			}
			return this.compile(p.get(0), next);
		}

		/* Symbol */

		@Override
		public NZ86Instruction visitSymbolScope(PSymbolScope p, NZ86Instruction next) {
			if (p.funcName == NezFunc.block) {
				next = new NZ86.SClose(next);
				next = this.compile(p.get(0), next);
				return new NZ86.SOpen(next);
			} else {
				next = new NZ86.SClose(next);
				next = this.compile(p.get(0), next);
				return new NZ86.SMask(p.param, next);
			}
		}

		@Override
		public NZ86Instruction visitSymbolAction(PSymbolAction p, NZ86Instruction next) {
			return new NZ86.Pos(this.compile(p.get(0), new NZ86.SDef(p.table, next)));
		}

		@Override
		public NZ86Instruction visitSymbolPredicate(PSymbolPredicate p, NZ86Instruction next) {
			switch (p.funcName) {
			case exists:
				if (p.symbol == null) {
					return new NZ86.SExists(p.table, next);
				} else {
					return new NZ86.SIsDef(p.table, OStringUtils.utf8(p.symbol), next);
				}
			case is:
				return new NZ86.Pos(this.compile(p.get(0), new NZ86.SIs(p.table, next)));
			case isa:
				return new NZ86.Pos(this.compile(p.get(0), new NZ86.SIsa(p.table, next)));
			case match:
				return new NZ86.SMatch(p.table, next);
			default:
				break;
			}
			return next;
		}

		@Override
		public NZ86Instruction visitScan(PScan p, NZ86Instruction next) {
			return new NZ86.Pos(this.compile(p.get(0), new NZ86.NScan(p.mask, p.shift, next)));
		}

		@Override
		public NZ86Instruction visitRepeat(PRepeat p, NZ86Instruction next) {
			NZ86Instruction check = new NZ86.NDec(next, null);
			NZ86Instruction repeated = this.compile(p.get(0), check);
			check.next = repeated;
			return check;
		}

		@Override
		public NZ86Instruction visitTrap(PTrap p, NZ86Instruction next) {
			if (p.trapid != -1) {
				return new NZ86.Trap(p.trapid, p.uid, next);
			}
			return next;
		}

		/* Optimization */

		private Expression getInnerExpression(Expression p) {
			return Expression.deref(p.get(0));
		}

		// Unused

		@Override
		public NZ86Instruction visitIf(PIfCondition e, NZ86Instruction next) {
			NZ86Compiler.this.options.verbose("unremoved if condition", e);
			return next;
		}

		@Override
		public NZ86Instruction visitOn(POnCondition e, NZ86Instruction next) {
			NZ86Compiler.this.options.verbose("unremoved on condition", e);
			return this.compile(e.get(0), next);
		}
	}

}
