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

package blue.nez.parser.pasm;

import java.util.ArrayList;
import java.util.List;

import blue.nez.parser.ParserCompiler;
import blue.nez.parser.ParserGrammar;
import blue.nez.parser.ParserGrammar.MemoPoint;
import blue.nez.parser.ParserOption;
import blue.nez.parser.pasm.PAsmAPI.SymbolDefFunc;
import blue.nez.parser.pasm.PAsmAPI.SymbolResetFunc;
import blue.nez.peg.Expression;
import blue.nez.peg.ExpressionVisitor;
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
import blue.nez.peg.expression.PScan;
import blue.nez.peg.expression.PSymbolAction;
import blue.nez.peg.expression.PSymbolPredicate;
import blue.nez.peg.expression.PSymbolScope;
import blue.nez.peg.expression.PTag;
import blue.nez.peg.expression.PTrap;
import blue.nez.peg.expression.PTree;
import blue.nez.peg.expression.PValue;
import blue.origami.util.OOption;

public class PAsmCompiler implements ParserCompiler {

	public PAsmCompiler() {

	}

	@Override
	public ParserCompiler clone() {
		return new PAsmCompiler();
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
	public PAsmCode compile(ParserGrammar grammar) {
		PAsmCode code = new PAsmCode(grammar, this.options);
		if (this.enableMemo) {
			code.initMemoPoint();
		}
		new CompilerVisitor(code, grammar, this.options).compileAll();
		return code;
	}

	class CompilerVisitor extends ExpressionVisitor<PAsmInst, PAsmInst> {

		final PAsmCode code;
		final ParserGrammar grammar;

		boolean TreeConstruction = true;
		boolean enableMemo = false;
		boolean BinaryGrammar = false;
		boolean Optimization = true;

		CompilerVisitor(PAsmCode code, ParserGrammar grammar, OOption options) {
			this.code = code;
			this.grammar = grammar;
			this.BinaryGrammar = grammar.isBinary();
			this.TreeConstruction = options.is(ParserOption.TreeConstruction, true);
			// this.enableMemo = (grammar.getMemoPointSize() > 0);
			// if (grammar.getMemoPointSize() > 0) {
			this.enableMemo = options.is(ParserOption.PackratParsing, true);
			// }
			// System.out.println("******** Memo=" + this.enableMemo);
		}

		private PAsmCode compileAll() {
			for (Production p : this.grammar) {
				this.compileProduction(this.code.codeList(), p, new ASMret());
			}
			for (PAsmInst inst : this.code.codeList()) {
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

		protected void compileProduction(List<PAsmInst> codeList, Production p, PAsmInst next) {
			MemoPoint memoPoint = this.code.getMemoPoint(p.getUniqueName());
			next = this.compileProductionExpression(memoPoint, p.getExpression(), next);
			this.code.setInstruction(p.getUniqueName(), next);
			PAsmInst block = new ASMnop(p.getUniqueName(), next);
			this.layoutCode(codeList, block);
		}

		private PAsmInst compileProductionExpression(MemoPoint memoPoint, Expression p, PAsmInst next) {
			if (memoPoint != null) {
				if (memoPoint.typeState == Typestate.Unit) {
					PAsmInst succMemo = this.compile(p, new ASMMmemoSucc(memoPoint, next));
					PAsmInst failMemo = new ASMMmemoFail(memoPoint);
					PAsmInst memo = new ASMalt(succMemo, failMemo);
					return new ASMMlookup(memoPoint, memo, next);
				} else {
					PAsmInst succMemo = this.compile(p, new ASMMmemoTree(memoPoint, next));
					PAsmInst failMemo = new ASMMmemoFail(memoPoint);
					PAsmInst memo = new ASMalt(succMemo, failMemo);
					return new ASMMlookupTree(memoPoint, memo, next);
				}
			}
			return this.compile(p, next);
		}

		private void layoutCode(List<PAsmInst> codeList, PAsmInst inst) {
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

		private PAsmInst compile(Expression e, PAsmInst next) {
			return e.visit(this, next);
		}

		@Override
		public PAsmInst visitEmpty(PEmpty p, PAsmInst next) {
			return next;
		}

		private final PAsmInst commonFailure = new ASMfail();

		public PAsmInst fail(Expression e) {
			return this.commonFailure;
		}

		@Override
		public PAsmInst visitFail(PFail p, PAsmInst next) {
			return this.commonFailure;
		}

		@Override
		public PAsmInst visitByte(PByte p, PAsmInst next) {
			if (/* this.BinaryGrammar && */ p.byteChar() == 0) {
				next = new ASMNeof(next);
			}
			return new ASMbyte(p.byteChar(), next);
		}

		@Override
		public PAsmInst visitByteSet(PByteSet p, PAsmInst next) {
			boolean[] b = p.bools();
			if (this.BinaryGrammar && b[0]) {
				next = new ASMNeof(next);
			}
			// b[0] = false;
			return new ASMbset(b, next);
		}

		@Override
		public PAsmInst visitAny(PAny p, PAsmInst next) {
			return new ASMany(next);
		}

		@Override
		public final PAsmInst visitNonTerminal(PNonTerminal n, PAsmInst next) {
			Production p = n.getProduction();
			return new ASMcall(p.getUniqueName(), next);
		}

		private byte[] toMultiChar(Expression e) {
			ArrayList<Integer> l = new ArrayList<>();
			Expression.extractMultiBytes(e, l);
			byte[] utf8 = new byte[l.size()];
			for (int i = 0; i < l.size(); i++) {
				utf8[i] = (byte) (int) l.get(i);
			}
			return utf8;
		}

		@Override
		public final PAsmInst visitOption(POption p, PAsmInst next) {
			if (this.Optimization) {
				Expression inner = this.getInnerExpression(p);
				if (inner instanceof PByte) {
					if (((PByte) inner).byteChar() == 0) {
						next = new ASMNeof(next);
					}
					return new ASMObyte(((PByte) inner).byteChar(), next);
				}
				if (inner instanceof PByteSet) {
					boolean[] b = ((PByteSet) inner).bools();
					if (b[0]) {
						next = new ASMNeof(next);
					}
					return new ASMObset(b, next);
				}
				if (Expression.isMultiBytes(inner)) {
					byte[] utf8 = this.toMultiChar(inner);
					return new ASMOstr(utf8, next);
				}
			}
			PAsmInst pop = new ASMsucc(next);
			return new ASMalt(this.compile(p.get(0), pop), next);
		}

		@Override
		public PAsmInst visitRepetition(PRepetition p, PAsmInst next) {
			PAsmInst next2 = this.compileRepetition(p, next);
			if (p.isOneMore()) {
				next2 = this.compile(p.get(0), next2);
			}
			return next2;
		}

		private PAsmInst compileRepetition(PRepetition p, PAsmInst next) {
			if (this.Optimization) {
				Expression inner = this.getInnerExpression(p);
				if (inner instanceof PByte) {
					if (((PByte) inner).byteChar() != 0) {
						return new ASMRbyte(((PByte) inner).byteChar(), next);
					}
				}
				if (inner instanceof PByteSet) {
					boolean[] b = ((PByteSet) inner).bools();
					if (!b[0]) {
						return new ASMRbset(b, next);
					}
				}
				if (Expression.isMultiBytes(inner)) {
					byte[] utf8 = this.toMultiChar(inner);
					return new ASMRstr(utf8, next);
				}
			}
			PAsmInst skip = new ASMstep();
			PAsmInst start = this.compile(p.get(0), skip);
			skip.next = PegAsm.joinPoint(start);
			return new ASMalt(start, next);
		}

		@Override
		public PAsmInst visitAnd(PAnd p, PAsmInst next) {
			PAsmInst inner = this.compile(p.get(0), new ASMback(next));
			return new ASMpos(inner);
		}

		@Override
		public final PAsmInst visitNot(PNot p, PAsmInst next) {
			if (this.Optimization) {
				Expression inner = this.getInnerExpression(p);
				if (inner instanceof PByte) {
					if (/* this.BinaryGrammar && */ ((PByte) inner).byteChar() != 0) {
						next = new ASMNeof(next);
					}
					return new ASMNbyte(((PByte) inner).byteChar(), next);
				}
				// if (inner instanceof PByteSet) {
				// boolean[] b = ((PByteSet) inner).bools();
				// if (this.BinaryGrammar && b[0] == false) {
				// next = new ASMNeof(next);
				// return new ASMNbinset(b, next);
				// }
				// return new ASMNbset(b, next);
				// }
				if (inner instanceof PAny) {
					return new ASMNany(next);
				}
				if (Expression.isMultiBytes(inner)) {
					byte[] utf8 = this.toMultiChar(inner);
					return new ASMNstr(utf8, next);
				}
			}
			PAsmInst fail = new ASMsucc(new ASMfail());
			return new ASMalt(this.compile(p.get(0), fail), next);
		}

		@Override
		public PAsmInst visitPair(PPair p, PAsmInst next) {
			PAsmInst nextStart = next;
			for (int i = p.size() - 1; i >= 0; i--) {
				Expression e = p.get(i);
				nextStart = this.compile(e, nextStart);
			}
			return nextStart;
		}

		@Override
		public final PAsmInst visitChoice(PChoice p, PAsmInst next) {
			PAsmInst nextChoice = this.compile(p.get(p.size() - 1), next);
			for (int i = p.size() - 2; i >= 0; i--) {
				Expression e = p.get(i);
				nextChoice = new ASMalt(this.compile(e, new ASMsucc(next)), nextChoice);
			}
			return nextChoice;
		}

		@Override
		public final PAsmInst visitDispatch(PDispatch p, PAsmInst next) {
			PAsmInst[] compiled = new PAsmInst[p.size() + 1];
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
		public PAsmInst visitTree(PTree p, PAsmInst next) {
			if (this.TreeConstruction) {
				next = new ASMTend(p.tag, p.value, p.endShift, next);
				next = this.compile(p.get(0), next);
				if (p.folding) {
					return new ASMTfold(p.label, p.beginShift, next);
				} else {
					return new ASMTbegin(p.beginShift, next);
				}
			}
			return this.compile(p.get(0), next);
		}

		@Override
		public PAsmInst visitTag(PTag p, PAsmInst next) {
			if (this.TreeConstruction) {
				return new ASMTtag(p.tag, next);
			}
			return next;
		}

		@Override
		public PAsmInst visitValue(PValue p, PAsmInst next) {
			if (this.TreeConstruction) {
				return new ASMTvalue(p.value, next);
			}
			return next;
		}

		// Tree

		@Override
		public final PAsmInst visitLinkTree(PLinkTree p, PAsmInst next) {
			if (this.TreeConstruction) {
				next = new ASMTlink(p.label, next);
				next = this.compile(p.get(0), next);
				return new ASMTpush(next);
			}
			return this.compile(p.get(0), next);
		}

		@Override
		public PAsmInst visitDetree(PDetree p, PAsmInst next) {
			if (this.TreeConstruction) {
				next = new ASMTpop(next);
				next = this.compile(p.get(0), next);
				return new ASMTpush(next);
			}
			return this.compile(p.get(0), next);
		}

		/* Symbol */

		@Override
		public PAsmInst visitSymbolScope(PSymbolScope p, PAsmInst next) {
			if (p.label == null) {
				next = new ASMSend(next);
				next = this.compile(p.get(0), next);
				return new ASMSbegin(next);
			} else {
				next = new ASMSend(next);
				next = this.compile(p.get(0), next);
				next = new ASMSdef2(new SymbolResetFunc(), p.label, next);
				return new ASMSbegin(next);
			}
		}

		@Override
		public PAsmInst visitSymbolAction(PSymbolAction p, PAsmInst next) {
			return new ASMpos(this.compile(p.get(0), new ASMSdef(new SymbolDefFunc(), p.label, next)));
		}

		@Override
		public PAsmInst visitSymbolPredicate(PSymbolPredicate p, PAsmInst next) {
			if (p.isEmpty()) {
				return new ASMSpred2(p.pred, p.label, next);
			} else {
				return new ASMpos(this.compile(p.get(0), new ASMSpred(p.pred, p.label, next)));
			}
		}

		@Override
		public PAsmInst visitScan(PScan p, PAsmInst next) {
			return next;
			// return new ASMpos(this.compile(p.get(0), new ASMSScan(p.mask,
			// p.shift, next)));
		}

		@Override
		public PAsmInst visitRepeat(PRepeat p, PAsmInst next) {
			return next;
			// PAsmInst check = new ASMSDec(next, null);
			// PAsmInst repeated = this.compile(p.get(0), check);
			// check.next = repeated;
			// return check;
		}

		@Override
		public PAsmInst visitTrap(PTrap p, PAsmInst next) {
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
		public PAsmInst visitIf(PIfCondition e, PAsmInst next) {
			PAsmCompiler.this.options.verbose("unremoved if condition", e);
			return next;
		}

		@Override
		public PAsmInst visitOn(POnCondition e, PAsmInst next) {
			PAsmCompiler.this.options.verbose("unremoved on condition", e);
			return this.compile(e.get(0), next);
		}
	}

}