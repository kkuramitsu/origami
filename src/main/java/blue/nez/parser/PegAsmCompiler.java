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
import blue.nez.parser.ParserContext.SymbolDefinition;
import blue.nez.parser.ParserContext.SymbolExist;
import blue.nez.parser.ParserContext.SymbolExistString;
import blue.nez.parser.ParserContext.SymbolMatch;
import blue.nez.parser.ParserContext.SymbolReset;
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
import blue.nez.pegasm.ASMMlookup;
import blue.nez.pegasm.ASMMlookupTree;
import blue.nez.pegasm.ASMMmemoFail;
import blue.nez.pegasm.ASMMmemoSucc;
import blue.nez.pegasm.ASMMmemoTree;
import blue.nez.pegasm.ASMNany;
import blue.nez.pegasm.ASMNbin;
import blue.nez.pegasm.ASMNbinset;
import blue.nez.pegasm.ASMNbset;
import blue.nez.pegasm.ASMNbyte;
import blue.nez.pegasm.ASMNstr;
import blue.nez.pegasm.ASMObin;
import blue.nez.pegasm.ASMObinset;
import blue.nez.pegasm.ASMObset;
import blue.nez.pegasm.ASMObyte;
import blue.nez.pegasm.ASMOstr;
import blue.nez.pegasm.ASMRbin;
import blue.nez.pegasm.ASMRbinset;
import blue.nez.pegasm.ASMRbset;
import blue.nez.pegasm.ASMRbyte;
import blue.nez.pegasm.ASMRstr;
import blue.nez.pegasm.ASMSDec;
import blue.nez.pegasm.ASMSScan;
import blue.nez.pegasm.ASMSbegin;
import blue.nez.pegasm.ASMSdef;
import blue.nez.pegasm.ASMSdef2;
import blue.nez.pegasm.ASMSend;
import blue.nez.pegasm.ASMSpred;
import blue.nez.pegasm.ASMSpred2;
import blue.nez.pegasm.ASMTbegin;
import blue.nez.pegasm.ASMTend;
import blue.nez.pegasm.ASMTfold;
import blue.nez.pegasm.ASMTlink;
import blue.nez.pegasm.ASMTmut;
import blue.nez.pegasm.ASMTpop;
import blue.nez.pegasm.ASMTpush;
import blue.nez.pegasm.ASMTtag;
import blue.nez.pegasm.ASMalt;
import blue.nez.pegasm.ASMany;
import blue.nez.pegasm.ASMback;
import blue.nez.pegasm.ASMbin;
import blue.nez.pegasm.ASMbinset;
import blue.nez.pegasm.ASMbset;
import blue.nez.pegasm.ASMbyte;
import blue.nez.pegasm.ASMcall;
import blue.nez.pegasm.ASMdfa;
import blue.nez.pegasm.ASMdispatch;
import blue.nez.pegasm.ASMfail;
import blue.nez.pegasm.ASMnop;
import blue.nez.pegasm.ASMpos;
import blue.nez.pegasm.ASMret;
import blue.nez.pegasm.ASMstep;
import blue.nez.pegasm.ASMsucc;
import blue.nez.pegasm.ASMtrap;
import blue.nez.pegasm.PegAsm;
import blue.origami.util.OOption;
import blue.origami.util.OStringUtils;

public class PegAsmCompiler implements ParserCompiler {

	public PegAsmCompiler() {

	}

	@Override
	public ParserCompiler clone() {
		return new PegAsmCompiler();
	}

	// Local Option
	OOption options = null;
	boolean enableMemo = false;

	@Override
	public void init(OOption options) {
		this.options = options;
		this.enableMemo = options.is(ParserOption.PackratParsing, true);
	}

	@Override
	public PegAsmCode compile(ParserGrammar grammar) {
		PegAsmCode code = new PegAsmCode(grammar, this.options);
		if (this.enableMemo) {
			code.initMemoPoint();
		}
		new CompilerVisitor(code, grammar, this.options).compileAll();
		return code;
	}

	class CompilerVisitor extends ExpressionVisitor<PegAsmInstruction, PegAsmInstruction> {

		final PegAsmCode code;
		final ParserGrammar grammar;

		boolean TreeConstruction = true;
		boolean enableMemo = false;
		boolean BinaryGrammar = false;
		boolean Optimization = true;

		CompilerVisitor(PegAsmCode code, ParserGrammar grammar, OOption options) {
			this.code = code;
			this.grammar = grammar;
			this.BinaryGrammar = grammar.isBinary();
			this.TreeConstruction = options.is(ParserOption.TreeConstruction, true);
			this.enableMemo = options.is(ParserOption.PackratParsing, true);

		}

		private PegAsmCode compileAll() {
			for (Production p : this.grammar) {
				this.compileProduction(this.code.codeList(), p, new ASMret());
			}
			for (PegAsmInstruction inst : this.code.codeList()) {
				if (inst instanceof ASMcall) {
					ASMcall call = (ASMcall) inst;
					if (call.jump == null) {
						call.jump = call.next;
						call.next = PegAsm.joinPoint(this.code.getInstruction(call.uname));// f.getCompiled();
					}
				}
			}
			return this.code;
		}

		protected void compileProduction(List<PegAsmInstruction> codeList, Production p, PegAsmInstruction next) {
			MemoPoint memoPoint = this.code.getMemoPoint(p.getUniqueName());
			next = this.compileProductionExpression(memoPoint, p.getExpression(), next);
			this.code.setInstruction(p.getUniqueName(), next);
			PegAsmInstruction block = new ASMnop(p.getUniqueName(), next);
			this.layoutCode(codeList, block);
		}

		private PegAsmInstruction compileProductionExpression(MemoPoint memoPoint, Expression p,
				PegAsmInstruction next) {
			if (memoPoint != null) {
				if (memoPoint.typeState == Typestate.Unit) {
					PegAsmInstruction memo = new ASMMmemoSucc(memoPoint, next);
					PegAsmInstruction inside = this.compile(p, memo);
					PegAsmInstruction failmemo = new ASMMmemoFail(memoPoint);
					inside = new ASMalt(failmemo, inside);
					return new ASMMlookup(memoPoint, inside, next);
				} else {
					PegAsmInstruction memo = new ASMMmemoTree(memoPoint, next);
					PegAsmInstruction inside = this.compile(p, memo);
					PegAsmInstruction failmemo = new ASMMmemoFail(memoPoint);
					inside = new ASMalt(failmemo, inside);
					return new ASMMlookupTree(memoPoint, inside, next);
				}
			}
			return this.compile(p, next);
		}

		private void layoutCode(List<PegAsmInstruction> codeList, PegAsmInstruction inst) {
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
				if (inst instanceof ASMdispatch) {
					ASMdispatch match = (ASMdispatch) inst;
					for (int ch = 0; ch < match.jumpTable.length; ch++) {
						this.layoutCode(codeList, match.jumpTable[ch]);
					}
				}
			}
		}

		// encoding

		private PegAsmInstruction compile(Expression e, PegAsmInstruction next) {
			return e.visit(this, next);
		}

		@Override
		public PegAsmInstruction visitEmpty(PEmpty p, PegAsmInstruction next) {
			return next;
		}

		private final PegAsmInstruction commonFailure = new ASMfail();

		public PegAsmInstruction fail(Expression e) {
			return this.commonFailure;
		}

		@Override
		public PegAsmInstruction visitFail(PFail p, PegAsmInstruction next) {
			return this.commonFailure;
		}

		@Override
		public PegAsmInstruction visitByte(PByte p, PegAsmInstruction next) {
			if (/* this.BinaryGrammar && */ p.byteChar() == 0) {
				return new ASMbin(next);
			}
			return new ASMbyte(p.byteChar(), next);
		}

		@Override
		public PegAsmInstruction visitByteSet(PByteSet p, PegAsmInstruction next) {
			boolean[] b = p.byteSet();
			if (this.BinaryGrammar && b[0]) {
				return new ASMbinset(b, next);
			}
			b[0] = false;
			return new ASMbset(b, next);
		}

		@Override
		public PegAsmInstruction visitAny(PAny p, PegAsmInstruction next) {
			return new ASMany(next);
		}

		@Override
		public final PegAsmInstruction visitNonTerminal(PNonTerminal n, PegAsmInstruction next) {
			Production p = n.getProduction();
			return new ASMcall(p.getUniqueName(), next);
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
		public final PegAsmInstruction visitOption(POption p, PegAsmInstruction next) {
			if (this.Optimization) {
				Expression inner = this.getInnerExpression(p);
				if (inner instanceof PByte) {
					if (/* this.BinaryGrammar && */ ((PByte) inner).byteChar() == 0) {
						return new ASMObin(next);
					}
					return new ASMObyte(((PByte) inner).byteChar(), next);
				}
				if (inner instanceof PByteSet) {
					boolean[] b = ((PByteSet) inner).byteSet();
					if (this.BinaryGrammar && b[0]) {
						return new ASMObinset(b, next);
					}
					b[0] = false;
					return new ASMObset(b, next);
				}
				if (Expression.isString(inner)) {
					byte[] utf8 = this.toMultiChar(inner);
					return new ASMOstr(utf8, next);
				}
			}
			PegAsmInstruction pop = new ASMsucc(next);
			return new ASMalt(next, this.compile(p.get(0), pop));
		}

		@Override
		public PegAsmInstruction visitRepetition(PRepetition p, PegAsmInstruction next) {
			PegAsmInstruction next2 = this.compileRepetition(p, next);
			if (p.isOneMore()) {
				next2 = this.compile(p.get(0), next2);
			}
			return next2;
		}

		private PegAsmInstruction compileRepetition(PRepetition p, PegAsmInstruction next) {
			if (this.Optimization) {
				Expression inner = this.getInnerExpression(p);
				if (inner instanceof PByte) {
					if (/* this.BinaryGrammar && */ ((PByte) inner).byteChar() == 0) {
						return new ASMRbin(next);
					}
					return new ASMRbyte(((PByte) inner).byteChar(), next);
				}
				if (inner instanceof PByteSet) {
					boolean[] b = ((PByteSet) inner).byteSet();
					if (this.BinaryGrammar && b[0]) {
						return new ASMRbinset(b, next);
					}
					b[0] = false;
					return new ASMRbset(b, next);
				}
				if (Expression.isString(inner)) {
					byte[] utf8 = this.toMultiChar(inner);
					return new ASMRstr(utf8, next);
				}
			}
			PegAsmInstruction skip = new ASMstep();
			PegAsmInstruction start = this.compile(p.get(0), skip);
			skip.next = PegAsm.joinPoint(start);
			return new ASMalt(next, start);
		}

		@Override
		public PegAsmInstruction visitAnd(PAnd p, PegAsmInstruction next) {
			PegAsmInstruction inner = this.compile(p.get(0), new ASMback(next));
			return new ASMpos(inner);
		}

		@Override
		public final PegAsmInstruction visitNot(PNot p, PegAsmInstruction next) {
			if (this.Optimization) {
				Expression inner = this.getInnerExpression(p);
				if (inner instanceof PByte) {
					if (/* this.BinaryGrammar && */ ((PByte) inner).byteChar() != 0) {
						return new ASMNbin(((PByte) inner).byteChar(), next);
					}
					return new ASMNbyte(((PByte) inner).byteChar(), next);
				}
				if (inner instanceof PByteSet) {
					boolean[] b = ((PByteSet) inner).byteSet();
					if (this.BinaryGrammar && b[0] == false) {
						return new ASMNbinset(b, next);
					}
					return new ASMNbset(b, next);
				}
				if (inner instanceof PAny) {
					return new ASMNany(next);
				}
				if (Expression.isString(inner)) {
					byte[] utf8 = this.toMultiChar(inner);
					return new ASMNstr(utf8, next);
				}
			}
			PegAsmInstruction fail = new ASMsucc(new ASMfail());
			return new ASMalt(next, this.compile(p.get(0), fail));
		}

		@Override
		public PegAsmInstruction visitPair(PPair p, PegAsmInstruction next) {
			PegAsmInstruction nextStart = next;
			for (int i = p.size() - 1; i >= 0; i--) {
				Expression e = p.get(i);
				nextStart = this.compile(e, nextStart);
			}
			return nextStart;
		}

		@Override
		public final PegAsmInstruction visitChoice(PChoice p, PegAsmInstruction next) {
			PegAsmInstruction nextChoice = this.compile(p.get(p.size() - 1), next);
			for (int i = p.size() - 2; i >= 0; i--) {
				Expression e = p.get(i);
				nextChoice = new ASMalt(nextChoice, this.compile(e, new ASMsucc(next)));
			}
			return nextChoice;
		}

		@Override
		public final PegAsmInstruction visitDispatch(PDispatch p, PegAsmInstruction next) {
			PegAsmInstruction[] compiled = new PegAsmInstruction[p.size() + 1];
			compiled[0] = this.commonFailure;
			if (this.isAllD(p)) {
				for (int i = 0; i < p.size(); i++) {
					compiled[i + 1] = this.compile(this.nextD(p.get(i)), next);
				}
				return new ASMdfa(p.indexMap, compiled);
			} else {
				for (int i = 0; i < p.size(); i++) {
					compiled[i + 1] = this.compile(p.get(i), next);
				}
				return new ASMdispatch(p.indexMap, compiled);
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
		public PegAsmInstruction visitTree(PTree p, PegAsmInstruction next) {
			if (this.TreeConstruction) {
				next = new ASMTend(p.tag, p.value, p.endShift, next);
				next = this.compile(p.get(0), next);
				if (p.folding) {
					// System.out.println("@@@ folding" + p);
					return new ASMTfold(p.label, p.beginShift, next);
				} else {
					return new ASMTbegin(p.beginShift, next);
				}
			}
			return this.compile(p.get(0), next);
		}

		@Override
		public PegAsmInstruction visitTag(PTag p, PegAsmInstruction next) {
			if (this.TreeConstruction) {
				return new ASMTtag(p.tag, next);
			}
			return next;
		}

		@Override
		public PegAsmInstruction visitReplace(PReplace p, PegAsmInstruction next) {
			if (this.TreeConstruction) {
				return new ASMTmut(p.value, next);
			}
			return next;
		}

		// Tree

		@Override
		public final PegAsmInstruction visitLinkTree(PLinkTree p, PegAsmInstruction next) {
			if (this.TreeConstruction) {
				next = new ASMTlink(p.label, next);
				next = this.compile(p.get(0), next);
				return new ASMTpush(next);
			}
			return this.compile(p.get(0), next);
		}

		@Override
		public PegAsmInstruction visitDetree(PDetree p, PegAsmInstruction next) {
			if (this.TreeConstruction) {
				next = new ASMTpop(next);
				next = this.compile(p.get(0), next);
				return new ASMTpush(next);
			}
			return this.compile(p.get(0), next);
		}

		/* Symbol */

		@Override
		public PegAsmInstruction visitSymbolScope(PSymbolScope p, PegAsmInstruction next) {
			if (p.funcName == NezFunc.block) {
				next = new ASMSend(next);
				next = this.compile(p.get(0), next);
				return new ASMSbegin(next);
			} else {
				next = new ASMSend(next);
				next = this.compile(p.get(0), next);
				next = new ASMSdef2(new SymbolReset(), p.param, next);
				return new ASMSbegin(next);
			}
		}

		@Override
		public PegAsmInstruction visitSymbolAction(PSymbolAction p, PegAsmInstruction next) {
			return new ASMpos(this.compile(p.get(0), new ASMSdef(new SymbolDefinition(), p.table, next)));
		}

		@Override
		public PegAsmInstruction visitSymbolPredicate(PSymbolPredicate p, PegAsmInstruction next) {
			switch (p.funcName) {
			case exists:
				if (p.symbol == null) {
					return new ASMSpred2(new SymbolExist(), p.table, null, next);
				} else {
					return new ASMSpred2(new SymbolExistString(), p.table, OStringUtils.utf8(p.symbol), next);
				}
			case is:
				return new ASMpos(this.compile(p.get(0), new ASMSpred(null, p.table, next)));
			case isa:
				return new ASMpos(this.compile(p.get(0), new ASMSpred(null, p.table, next)));
			case match:
				return new ASMSpred2(new SymbolMatch(), p.table, next);
			default:
				break;
			}
			return next;
		}

		@Override
		public PegAsmInstruction visitScan(PScan p, PegAsmInstruction next) {
			return new ASMpos(this.compile(p.get(0), new ASMSScan(p.mask, p.shift, next)));
		}

		@Override
		public PegAsmInstruction visitRepeat(PRepeat p, PegAsmInstruction next) {
			PegAsmInstruction check = new ASMSDec(next, null);
			PegAsmInstruction repeated = this.compile(p.get(0), check);
			check.next = repeated;
			return check;
		}

		@Override
		public PegAsmInstruction visitTrap(PTrap p, PegAsmInstruction next) {
			if (p.trapid != -1) {
				return new ASMtrap(p.trapid, p.uid, next);
			}
			return next;
		}

		/* Optimization */

		private Expression getInnerExpression(Expression p) {
			return Expression.deref(p.get(0));
		}

		// Unused

		@Override
		public PegAsmInstruction visitIf(PIfCondition e, PegAsmInstruction next) {
			PegAsmCompiler.this.options.verbose("unremoved if condition", e);
			return next;
		}

		@Override
		public PegAsmInstruction visitOn(POnCondition e, PegAsmInstruction next) {
			PegAsmCompiler.this.options.verbose("unremoved on condition", e);
			return this.compile(e.get(0), next);
		}
	}

}
