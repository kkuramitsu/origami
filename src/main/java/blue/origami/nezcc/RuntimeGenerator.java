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

package blue.origami.nezcc;

import java.util.ArrayList;
import java.util.List;

import blue.origami.nez.peg.Expression;
import blue.origami.nez.peg.expression.ByteSet;
import blue.origami.nez.peg.expression.PAny;
import blue.origami.nez.peg.expression.PMany;

public abstract class RuntimeGenerator<B, C> extends CodeSection<C> {

	protected void defFunc(ParserGenerator<B, C> pg, String ret, String funcName, String[] params, Block<C> block) {
		if (!pg.isDefined(funcName)) {
			SourceSection sec = this.openSection(this.RuntimeLibrary);
			pg.defineSymbol(funcName, pg.localName(funcName));
			pg.declFunc(ret, funcName, params, block);
			this.closeSection(sec);
		}
	}

	protected void defFunc(ParserGenerator<B, C> pg, String ret, String funcName, String a0, Block<C> block) {
		this.defFunc(pg, ret, funcName, new String[] { a0 }, block);
	}

	protected void defFunc(ParserGenerator<B, C> pg, String ret, String funcName, String a0, String a1,
			Block<C> block) {
		this.defFunc(pg, ret, funcName, new String[] { a0, a1 }, block);
	}

	protected void defFunc(ParserGenerator<B, C> pg, String ret, String funcName, String a0, String a1, String a2,
			Block<C> block) {
		this.defFunc(pg, ret, funcName, new String[] { a0, a1, a2 }, block);
	}

	protected void defFunc(ParserGenerator<B, C> pg, String ret, String funcName, String a0, String a1, String a2,
			String a3, Block<C> block) {
		this.defFunc(pg, ret, funcName, new String[] { a0, a1, a2, a3 }, block);
	}

	protected void defFunc(ParserGenerator<B, C> pg, String ret, String funcName, String a0, String a1, String a2,
			String a3, String a4, Block<C> block) {
		this.defFunc(pg, ret, funcName, new String[] { a0, a1, a2, a3, a4 }, block);
	}

	private String T(String var, String typeName) {
		String t = this.T(var);
		return t == null ? typeName : t;
	}

	void makeTypeDefinition(ParserGenerator<B, C> pg, int memoSize) {
		SourceSection sec = this.openSection(this.RuntimeLibrary);
		pg.declStruct(this.T("treeLog", "TreeLog"), "op?", "pos?", "data?", "tree?", "prevLog", "nextLog?");
		pg.declStruct(this.T("state", "State"), "tag?", "cnt?", "value?", "prevState?");
		pg.declFuncType(this.T("tree"), this.T("newFunc", "TreeFunc"), "tag", "inputs", "pos", "epos", "cnt");
		pg.declFuncType(this.T("tree"), this.T("setFunc", "TreeSetFunc"), "tree", "cnt", "label", "child");
		pg.declStruct(this.T("px", "NezParserContext"), "inputs", "length", "pos?", "head_pos?", "tree?", "treeLog",
				"newFunc", "setFunc", "state?", "memos?");
		if (pg.isStateful()) {
			pg.declStruct(this.T("m", "MemoEntry"), "key", "result?", "pos?", "data?", "state?");
		} else {
			pg.declStruct(this.T("m", "MemoEntry"), "key", "result?", "pos?", "data?");
		}
		pg.declFuncType(this.T("matched"), "ParserFunc", "px");
		this.closeSection(sec);
	}

	void makeParseFunc(ParserGenerator<B, C> pg) {
		this.defFunc(pg, this.T("c"), "getbyte", "px", () -> {
			C expr = pg.emitArrayIndex(pg.emitGetter("px.inputs"), pg.emitGetter("px.pos"));
			return (pg.emitUnsigned(expr));
		});
		this.defFunc(pg, this.T("c"), "nextbyte", "px", () -> {
			C inc = pg.emitInc(pg.emitGetter("px.pos"));
			if (inc != null) {
				C expr = pg.emitArrayIndex(pg.emitGetter("px.inputs"), inc);
				return ((pg.emitUnsigned(expr)));
			} else {
				B block = pg.beginBlock();
				pg.emitVarDecl(block, false, "c",
						pg.emitUnsigned(pg.emitArrayIndex(pg.emitGetter("px.inputs"), pg.emitGetter("px.pos"))));
				pg.Setter(block, "px.pos", pg.emitOp(pg.emitGetter("px.pos"), "+", pg.vInt(1)));
				pg.emitStmt(block, pg.emitReturn(pg.V("c")));
				return (pg.endBlock(block));
			}
		});
		this.defFunc(pg, this.T("matched"), "move", "px", "shift", () -> {
			B block = pg.beginBlock();
			pg.Setter(block, "px.pos", pg.emitOp(pg.emitGetter("px.pos"), "+", pg.V("shift")));
			pg.emitStmt(block, pg.emitReturn(pg.emitSucc()));
			return (pg.endBlock(block));
		});
		this.defFunc(pg, this.T("matched"), "neof", "px", () -> {
			C length = pg.emitGetter("px.length");
			return (pg.emitOp(pg.emitGetter("px.pos"), "<", length));
		});
	}

	void makeMatchFunc(ParserGenerator<B, C> pg) {
		/* match(px, ch) */
		this.defFunc(pg, this.T("matched"), "match", "px", "c", () -> {
			C expr = pg.emitFunc("nextbyte", pg.V("px"));
			expr = pg.emitOp(expr, "==", pg.V("c"));
			return (expr);
		});
		/* backpos(px, pos) */
		this.defFunc(pg, this.T("pos"), "backpos", "px", "pos", () -> {
			B block = pg.beginBlock();
			pg.emitIfStmt(block, pg.emitOp(pg.emitGetter("px.head_pos"), "<", pg.V("pos")), false, () -> {
				return pg.emitSetter("px.head_pos", pg.V("pos"));
			});
			pg.Return(block, pg.V("pos"));
			return pg.endBlock(block);
		});
	}

	String funcSucc(ParserGenerator<B, C> pg) {
		String funcName = "f" + pg.s("true");
		// this.defFunc(pg, pg.T("matched"), funcName, "px", () -> {
		// return pg.emitSucc();
		// });
		return funcName;
	}

	String funcFail(ParserGenerator<B, C> pg) {
		String funcName = "f" + pg.s("false");
		// this.defFunc(pg, pg.T("matched"), funcName, "px", () -> {
		// return pg.emitFail();
		// });
		return funcName;
	}

	void makeMatchLibs(ParserGenerator<B, C> pg) {
		this.makeParseFunc(pg);
		this.makeMatchFunc(pg);
		this.defFunc(pg, pg.T("matched"), "f" + pg.s("true"), "px", () -> {
			return pg.emitSucc();
		});
		this.defFunc(pg, pg.T("matched"), "f" + pg.s("false"), "px", () -> {
			return pg.emitFail();
		});
	}

	C makeManyInlineCall(ParserGenerator<B, C> pg, PMany e) {
		Expression p = Expression.deref(e.get(0));
		ByteSet bs = Expression.getByteSet(p, pg.isBinary());
		if (bs != null) {
			String pname = bs.getUnsignedByte() == -1 ? "s" : "c";
			C inner = pg.emitMatchByteSet(bs, pg.V(pname), false);
			String fname = "many" + pname + (bs.is(0) ? "0" : "");
			this.defFunc(pg, pg.T("matched"), fname, "px", pname, () -> {
				B block = pg.beginBlock();
				pg.emitWhileStmt(block, inner, () -> {
					return pg.emitMove(pg.vInt(1));
				});
				pg.Return(block, pg.emitSucc());
				return pg.endBlock(block);
			});
			C arg = bs.getUnsignedByte() == -1 ? pg.vByteSet(bs) : pg.emitChar(bs.getUnsignedByte());
			C expr = pg.emitFunc(fname, pg.V("px"), arg);
			if (e.isOneMore()) {
				expr = pg.emitAnd(pg.emitMatchByteSet(bs), expr);
			}
			return expr;
		}
		return null;
	}

	C makeOptionInlineCall(ParserGenerator<B, C> pg, Expression e) {
		Expression p = Expression.deref(e.get(0));
		ByteSet bs = Expression.getByteSet(p, pg.isBinary());
		if (bs != null) {
			String pname = bs.getUnsignedByte() == -1 ? "s" : "c";
			C inner = pg.emitMatchByteSet(bs, pg.V(pname), false);
			String fname = "option" + pname + (bs.is(0) ? "0" : "");
			this.defFunc(pg, pg.T("matched"), fname, "px", pname, () -> {
				B block = pg.beginBlock();
				pg.emitIfStmt(block, inner, false, () -> {
					return pg.emitMove(pg.vInt(1));
				});
				pg.Return(block, pg.emitSucc());
				return pg.endBlock(block);
			});
			C arg = bs.getUnsignedByte() == -1 ? pg.vByteSet(bs) : pg.emitChar(bs.getUnsignedByte());
			return pg.emitFunc(fname, pg.V("px"), arg);
		}
		return null;
	}

	C makeAndInlineCall(ParserGenerator<B, C> pg, Expression e) {
		Expression p = Expression.deref(e.get(0));
		if (p instanceof PAny) {
			return pg.emitFunc("neof", pg.V("px"));
		}
		ByteSet bs = Expression.getByteSet(p, pg.isBinary());
		if (bs != null) {
			return pg.emitMatchByteSet(bs, null, false);
		}
		return null;
	}

	C makeNotInlineCall(ParserGenerator<B, C> pg, Expression e) {
		Expression p = Expression.deref(e.get(0));
		if (p instanceof PAny) {
			return pg.emitNot(pg.emitFunc("neof", pg.V("px")));
		}
		ByteSet bs = Expression.getByteSet(p, pg.isBinary());
		if (bs != null) {
			bs = bs.not(pg.isBinary());
			C expr = pg.emitMatchByteSet(bs, null, false);
			// System.out.println(" ** " + e + " => " + expr);
			return expr;
		}
		return null;
	}

	/* TreeLog */

	private boolean useLinkList() {
		return true;
	}

	void makeLogTreeFunc(ParserGenerator<B, C> pg) {
		if (this.useLinkList()) {
			this.defFunc(pg, this.T("treeLog"), "useTreeLog", "px", () -> {
				B block = pg.beginBlock();
				pg.emitVarDecl(block, false, "treeLog", pg.emitGetter("px.treeLog"));
				pg.emitIfStmt(block, pg.emitIsNull(pg.emitGetter("treeLog.nextLog")), false, () -> {
					B block2 = pg.beginBlock();
					pg.Setter(block2, "treeLog.nextLog",
							pg.emitFunc(this.nullCheck(pg.T("treeLog"), "TreeLog"), pg.emitGetter("px.treeLog")));
					return pg.endBlock(block2);
				});
				pg.emitStmt(block, pg.emitReturn(pg.emitGetter("treeLog.nextLog")));
				return pg.endBlock(block);
			});
		}
		this.defFunc(pg, this.T("matched"), "logTree", "px", "op", "pos", "data", "tree", () -> {
			B block = pg.beginBlock();
			if (this.useLinkList()) {
				pg.emitVarDecl(block, false, "treeLog", pg.emitFunc("useTreeLog", pg.V("px")));
			} else {
				pg.emitVarDecl(block, false, "treeLog",
						pg.emitFunc(this.nullCheck(pg.T("treeLog"), "TreeLog"), pg.emitNull()));
			}
			pg.Setter(block, "treeLog.op", pg.V("op"));
			pg.Setter(block, "treeLog.pos", pg.V("pos"));
			pg.Setter(block, "treeLog.data", pg.V("data"));
			pg.Setter(block, "treeLog.tree", pg.V("tree"));
			if (!this.useLinkList()) {
				pg.Setter(block, "treeLog.prevLog", pg.emitGetter("px.treeLog"));
			}
			pg.Setter(block, "px.treeLog", pg.V("treeLog"));
			pg.emitStmt(block, pg.emitReturn(pg.emitSucc()));
			return (pg.endBlock(block));
		});
	}

	String nullCheck(String t, String v) {
		return t == null ? v : t;
	}

	static int OpNew = 0;
	static int OpTag = 1;
	static int OpValue = 2;
	static int OpLink = 3;

	void makeBeginTreeFunc(ParserGenerator<B, C> pg) {
		this.defFunc(pg, this.T("matched"), "beginTree", "px", "shift", () -> {
			return (pg.emitFunc("logTree", pg.V("px"), pg.vInt(OpNew),
					pg.emitOp(pg.emitGetter("px.pos"), "+", pg.V("shift")), pg.emitNull(), pg.emitNull()));
		});
		this.defFunc(pg, this.T("matched"), "tagTree", "px", "tag", () -> {
			return (pg.emitFunc("logTree", pg.V("px"), pg.vInt(OpTag), pg.vInt(0), pg.emitCast("data", pg.V("tag")),
					pg.emitNull()));
		});
		if (pg.usePointer()) {
			this.defFunc(pg, this.T("matched"), "valueTree", "px", "value", "valuelen", () -> {
				return (pg.emitFunc("logTree", pg.V("px"), pg.vInt(OpValue), pg.emitCast("pos", pg.V("valuelen")),
						pg.emitCast("data", pg.V("value")), pg.emitNull()));
			});
		} else {
			this.defFunc(pg, this.T("matched"), "valueTree", "px", "value", () -> {
				return (pg.emitFunc("logTree", pg.V("px"), pg.vInt(OpValue), pg.vInt(0),
						pg.emitCast("data", pg.V("value")), pg.emitNull()));
			});
		}

		this.defFunc(pg, this.T("matched"), "valueTree", "px", "value", "valuelen", () -> {
			return (pg.emitFunc("logTree", pg.V("px"), pg.vInt(OpValue), pg.V("valuelen"),
					pg.emitCast("data", pg.V("value")), pg.emitNull()));
		});
		this.defFunc(pg, this.T("matched"), "linkTree", "px", "label", () -> {
			return (pg.emitFunc("logTree", pg.V("px"), pg.vInt(OpLink), pg.vInt(0), pg.emitCast("data", pg.V("label")),
					pg.emitGetter("px.tree")));
		});
		this.defFunc(pg, this.T("matched"), "foldTree", "px", "shift", "label", () -> {
			C log1 = pg.emitFunc("beginTree", pg.V("px"), pg.V("shift"));
			C log2 = pg.emitFunc("linkTree", pg.V("px"), pg.V("label"));
			return (pg.emitAnd(log1, log2));
		});
	}

	void makeEndTreeFunc(ParserGenerator<B, C> pg) {
		this.defFunc(pg, this.T("matched"), "endTree", "px", "shift", "tag", () -> {
			B block = pg.beginBlock();
			pg.emitVarDecl(block, true, "cnt", pg.vInt(0));
			pg.emitVarDecl(block, true, "treeLog", pg.emitGetter("px.treeLog"));
			pg.emitVarDecl(block, true, "prevLog", pg.emitGetter("px.treeLog"));
			pg.emitVarDecl(block, true, "value", pg.emitNull());
			if (pg.usePointer()) {
				pg.emitVarDecl(block, true, "valuelen", pg.vInt(0));
			}
			/* while */
			C loopCond = pg.emitOp(pg.emitGetter("prevLog.op"), "!=", pg.vInt(OpNew));
			C loopNext = pg.emitAssign("prevLog", pg.emitGetter("prevLog.prevLog"));
			C ifLinkCond = pg.emitOp(pg.emitGetter("prevLog.op"), "==", pg.vInt(OpLink));

			pg.emitWhileStmt(block, loopCond, () -> {
				C ifTagCond = pg.emitAnd(pg.emitOp(pg.V("tag"), "==", pg.emitNull()),
						pg.emitOp(pg.emitGetter("prevLog.op"), "==", pg.vInt(OpTag)));
				C ifValueCond = pg.emitAnd(pg.emitOp(pg.V("value"), "==", pg.emitNull()),
						pg.emitOp(pg.emitGetter("prevLog.op"), "==", pg.vInt(OpValue)));
				B block2 = pg.beginBlock();
				pg.emitIfStmt(block2, ifLinkCond, false, () -> {
					return pg.emitAssign("cnt", pg.emitOp(pg.V("cnt"), "+", pg.vInt(1)));
				});
				pg.emitIfStmt(block2, ifTagCond, false, () -> {
					return pg.emitAssign("tag", pg.emitCast("tag", pg.emitGetter("prevLog.data")));
				});
				pg.emitIfStmt(block2, ifValueCond, false, () -> {
					B block3 = pg.beginBlock();
					pg.Assign(block3, "value", pg.emitCast("value", pg.emitGetter("prevLog.data")));
					if (pg.usePointer()) {
						pg.Assign(block3, "valuelen", pg.emitCast("valuelen", pg.emitGetter("prevLog.pos")));
					}
					return pg.endBlock(block3);
				});
				pg.emitStmt(block2, loopNext);
				return pg.endBlock(block2);
			});

			List<C> param = new ArrayList<>();
			param.add(pg.V("tag"));
			param.add(pg.emitGetter("px.inputs"));
			param.add(pg.emitGetter("prevLog.pos"));
			param.add(pg.emitOp(pg.emitGetter("px.pos"), "+", pg.V("shift")));
			param.add(pg.V("cnt"));
			List<C> param2 = new ArrayList<>();
			param2.add(pg.V("tag"));
			param2.add(pg.emitCast("inputs", pg.V("value")));
			param2.add(pg.vInt(0));
			if (pg.usePointer()) {
				param2.add(pg.emitOp(pg.V("value"), "+", pg.V("valuelen")));
			} else {
				param2.add(pg.emitArrayLength(pg.V("value")));
			}
			param2.add(pg.V("cnt"));
			C newTree = pg.emitIf(pg.emitOp(pg.V("value"), "==", pg.emitNull()),
					pg.emitApply(pg.emitGetter("px.newFunc"), param),
					pg.emitApply(pg.emitGetter("px.newFunc"), param2));
			pg.Setter(block, "px.tree", newTree);
			// set..
			pg.emitStmt(block, pg.emitAssign("prevLog", pg.V("treeLog")));

			pg.emitWhileStmt(block, loopCond, () -> {
				B block2 = pg.beginBlock();
				pg.emitIfStmt(block2, ifLinkCond, false, () -> {
					B block3 = pg.beginBlock();
					pg.emitStmt(block3, pg.emitAssign("cnt", pg.emitOp(pg.V("cnt"), "-", pg.vInt(1))));
					C setFunc = pg.emitApply(pg.emitGetter("px.setFunc"), pg.emitGetter("px.tree"), pg.V("cnt"),
							pg.emitCast("label", pg.emitGetter("prevLog.data")), pg.emitGetter("prevLog.tree"));
					pg.emitStmt(block3, pg.emitSetter("px.tree", setFunc));
					return pg.endBlock(block3);
				});
				pg.emitStmt(block2, loopNext);
				return pg.endBlock(block2);
			});
			pg.emitStmt(block, pg.emitBack("treeLog", pg.emitGetter("prevLog.prevLog")));
			pg.emitStmt(block, pg.emitReturn(pg.emitSucc()));
			return (pg.endBlock(block));
		});
	}

	void makeBackLinkFunc(ParserGenerator<B, C> pg) {
		this.defFunc(pg, pg.T("matched"), "backLink", "px", "treeLog", "label", "tree", () -> {
			B block = pg.beginBlock();
			pg.emitStmt(block, pg.emitBack("treeLog", pg.V("treeLog")));
			pg.emitStmt(block, pg.emitFunc("linkTree", pg.V("px"), pg.V("label")));
			pg.emitStmt(block, pg.emitBack("tree", pg.V("tree")));
			pg.emitStmt(block, pg.emitReturn(pg.emitSucc()));
			return (pg.endBlock(block));
		});
	}

	void makeTreeLibs(ParserGenerator<B, C> pg) {
		this.makeLogTreeFunc(pg);
		this.makeBeginTreeFunc(pg);
		this.makeEndTreeFunc(pg);
		this.makeBackLinkFunc(pg);
	}

	/* SimpleTree */

	void makeSimpleTree(ParserGenerator<B, C> pg) {
		SourceSection sec = this.openSection(this.RuntimeLibrary);
		pg.declStruct("SimpleTree", "tag", "pos", "data");
		this.closeSection(sec);
	}

	/* Memo */

	static int ResultFail = 0;
	static int ResultSucc = 1;
	static int ResultUnfound = 2;

	void makeMemoStruct(ParserGenerator<B, C> pg) {
		SourceSection sec = this.openSection(this.RuntimeLibrary);
		if (pg.isStateful()) {
			pg.declStruct(this.T("m", "MemoEntry"), "key", "result?", "pos?", "data?", "state?");
		} else {
			pg.declStruct(this.T("m", "MemoEntry"), "key", "result?", "pos?", "data?");
		}
		this.closeSection(sec);
	}

	void makeInitMemoFunc(ParserGenerator<B, C> pg, int memoSize) {
		this.defFunc(pg, this.T("matched"), "initMemo", "px", () -> {
			B block = pg.beginBlock();
			if (memoSize > 0) {
				pg.emitVarDecl(block, false, "cnt", pg.vInt(0));
				pg.Setter(block, "px.memos", pg.emitNewArray(this.T("m"), pg.vInt(memoSize)));
				/* while */
				C loopCond = pg.emitOp(pg.V("cnt"), "<", pg.vInt(memoSize));
				pg.emitWhileStmt(block, loopCond, () -> {
					B block2 = pg.beginBlock();
					C left = pg.emitArrayIndex(pg.emitGetter("px.memos"), pg.V("cnt"));
					pg.emitStmt(block2, pg.emitAssign2(left, pg.emitFunc("MemoEntry", pg.vInt(-1))));
					pg.emitStmt(block2, pg.emitAssign("cnt", pg.emitOp(pg.V("cnt"), "+", pg.vInt(1))));
					return pg.endBlock(block2);
				});
			}
			pg.emitStmt(block, pg.emitReturn(pg.emitSucc()));
			return (pg.endBlock(block));
		});
	}

	void makeKeyFunc(ParserGenerator<B, C> pg, int window) {
		this.defFunc(pg, this.T("key"), "longkey", "key", "memoPoint", () -> {
			C key = pg.emitOp(pg.V("key"), "*", pg.vInt(window));
			key = pg.emitOp(key, "+", pg.V("memoPoint"));
			return (key);
		});
	}

	void makeGetMemoFunc(ParserGenerator<B, C> pg, int memoSize) {
		this.defFunc(pg, pg.T("m"), "getMemo", "px", "key", () -> {
			C index = pg.emitCast("cnt", pg.emitOp(pg.V("key"), "%", pg.vInt(memoSize)));
			C m = pg.emitArrayIndex(pg.emitGetter("px.memos"), index);
			return (m);
		});
	}

	void makeLookupFunc(ParserGenerator<B, C> pg, boolean withTree) {
		String suffix = withTree ? "3" : "1";
		this.defFunc(pg, this.T("result"), "lookupMemo" + suffix, "px", "memoPoint", () -> {
			B block = pg.beginBlock();
			pg.emitVarDecl(block, false, "key", pg.emitFunc("longkey", pg.emitGetter("px.pos"), pg.V("memoPoint")));
			pg.emitVarDecl(block, false, "m", pg.emitFunc("getMemo", pg.V("px"), pg.V("key")));
			// m.key == key
			C cond = pg.emitOp(pg.emitGetter("m.key"), "==", pg.V("key"));
			if (pg.isStateful()) {
				// m.key == key && m.state == state
				cond = pg.emitAnd(cond, pg.emitOp(pg.emitGetter("m.state"), "==", pg.emitGetter("px.state")));
			}
			C then = pg.emitFunc("consumeMemo" + suffix, pg.V("px"), pg.V("m"));
			C result = pg.emitIf(cond, then, pg.vInt(ResultUnfound));
			pg.emitStmt(block, pg.emitReturn(result));
			return (pg.endBlock(block));
		});
	}

	void makeConsumeFunc(ParserGenerator<B, C> pg, boolean withTree) {
		String suffix = withTree ? "3" : "1";
		this.defFunc(pg, this.T("result"), "consumeMemo" + suffix, "px", "m", () -> {
			B block = pg.beginBlock();
			pg.Setter(block, "px.pos", pg.emitGetter("m.pos"));
			if (withTree) {
				pg.Setter(block, "px.tree", pg.emitCast("tree", pg.emitGetter("m.data")));
			}
			pg.emitStmt(block, pg.emitReturn(pg.emitGetter("m.result")));
			return (pg.endBlock(block));
		});
	}

	void makeMemoFunc(ParserGenerator<B, C> pg) {
		this.defFunc(pg, this.T("matched"), "storeMemo", "px", "memoPoint", "pos", "matched", () -> {
			B block = pg.beginBlock();
			pg.emitVarDecl(block, false, "key", pg.emitFunc("longkey", pg.V("pos"), pg.V("memoPoint")));
			pg.emitVarDecl(block, false, "m", pg.emitFunc("getMemo", pg.V("px"), pg.V("key")));
			pg.Setter(block, "m.key", pg.V("key"));
			pg.Setter(block, "m.result", pg.emitIf(pg.V("matched"), pg.vInt(ResultSucc), pg.vInt(ResultFail)));
			pg.Setter(block, "m.pos", pg.emitIf(pg.V("matched"), pg.emitGetter("px.pos"), pg.V("pos")));
			pg.Setter(block, "m.data", pg.emitGetter("px.tree"));
			if (pg.isStateful()) {
				pg.Setter(block, "m.state", pg.emitGetter("px.state"));
			}
			pg.emitStmt(block, pg.emitReturn(pg.V("matched")));
			return (pg.endBlock(block));
		});
	}

	void makeMemoLibs(ParserGenerator<B, C> pg, int memoSize, int windowSize) {
		if (memoSize > 0) {
			this.makeInitMemoFunc(pg, memoSize * windowSize + 1);
			this.makeKeyFunc(pg, windowSize);
			this.makeGetMemoFunc(pg, memoSize * windowSize + 1);
			this.makeConsumeFunc(pg, true);
			this.makeLookupFunc(pg, true);
			this.makeMemoFunc(pg);
		} else {
			this.makeInitMemoFunc(pg, 0);
		}
	}

	// State

	void makeUseStateFunc(ParserGenerator<B, C> pg) {
		this.defFunc(pg, this.T("state"), "useState", "px", () -> {
			B block = pg.beginBlock();
			pg.emitIfStmt(block, pg.emitOp(pg.emitGetter("px.uState"), "==", pg.emitNull()), false, () -> {
				return pg.emitReturn(pg.emitFunc(pg.T("state")));
			});
			pg.emitVarDecl(block, false, "uState", pg.emitGetter("px.State"));
			pg.Setter(block, "px.uState", pg.emitGetter("uLog.prevState"));
			pg.emitStmt(block, pg.emitReturn(pg.V("uState")));
			return pg.endBlock(block);
		});
	}

	void makeUnuseStateFunc(ParserGenerator<B, C> pg) {
		this.defFunc(pg, this.T("state"), pg.s("Ustate"), "px", "state", () -> {
			B block = pg.beginBlock();
			pg.emitVarDecl(block, false, "uState", pg.emitGetter("px.treeState"));
			pg.emitWhileStmt(block, pg.emitOp(pg.V("uState"), "!=", pg.V("treeState")), () -> {
				B block2 = pg.beginBlock();
				pg.emitVarDecl(block2, false, "prevState", pg.V("uState"));
				pg.emitStmt(block2, pg.emitAssign("uState", pg.emitGetter("uState.prevState")));
				pg.emitStmt(block2, pg.emitSetter("prevState.prevState", pg.emitGetter("px.uState")));
				pg.emitStmt(block2, pg.emitSetter("px.uState", pg.V("prevState")));
				return pg.endBlock(block2);
			});
			pg.emitStmt(block, pg.emitReturn(pg.V("state")));
			return pg.endBlock(block);
		});
	}

	void makeStateLibs(ParserGenerator<B, C> pg) {
		if (pg.isDefined("Ustate")) {
			this.makeUseStateFunc(pg);
			this.makeUnuseStateFunc(pg);
		}
		// this.defFunc(pg, this.T("matched"), "memcmp", "inputs", "value",
		// "length", () -> {
		// return (pg.emitSucc());
		// });
		this.defFunc(pg, this.T("state"), "createState", "tag", "cnt", "value", "prevState", () -> {
			B block = pg.beginBlock();
			C newFunc = pg.isDefined("Ustate") ? pg.emitFunc("useState", pg.V("px"))
					: pg.emitFunc(this.nullCheck(pg.T("state"), "State"));
			pg.emitVarDecl(block, false, "state", newFunc);
			pg.Setter(block, "state.tag", pg.V("tag"));
			pg.Setter(block, "state.cnt", pg.V("cnt"));
			pg.Setter(block, "state.value", pg.V("value"));
			pg.Setter(block, "state.prevState", pg.V("prevState"));
			pg.emitStmt(block, pg.emitReturn(pg.V("state")));
			return (pg.endBlock(block));
		});

	}

	String makeStateFunc(ParserGenerator<B, C> pg, String func, Object thunk) {
		if (func.equals("symbol")) {
			this.defFunc(pg, this.T("matched"), "symbol1", "px", "state", "tag", "pos", () -> {
				B block = pg.beginBlock();
				pg.emitVarDecl(block, false, "value", pg.emitFunc("extract", pg.V("px"), pg.V("pos")));
				pg.emitVarDecl(block, false, "length", pg.emitOp(pg.emitGetter("px.pos"), "-", pg.V("pos")));
				pg.emitStmt(block,
						pg.emitSetter("px.state", //
								pg.emitFunc("createState", pg.V("tag"), pg.V("length"), pg.V("value"),
										pg.emitGetter("px.state"))));
				pg.emitStmt(block, pg.emitReturn(pg.emitSucc()));
				return pg.endBlock(block);
			});
			return "symbol1";
		}
		if (func.equals("remove")) {
			this.defFunc(pg, this.T("state"), "removeState", "tag", "state", () -> {
				C cond0 = pg.emitEqTag(pg.emitGetter("state.tag"), pg.V("tag"));
				C then0 = pg.emitFunc("removeState", pg.V("tag"), pg.emitGetter("state.prevState"));
				C else0 = pg.emitFunc("createState", pg.emitGetter("state.tag"), pg.emitGetter("state.cnt"),
						pg.emitGetter("state.value"), pg.emitGetter("state.prevState"));
				return pg.emitIf(pg.emitIsNull(pg.V("state")), pg.emitNull(), pg.emitIf(cond0, then0, else0));
			});
			this.defFunc(pg, this.T("matched"), "symbol1", "px", "state", "tag", "pos", () -> {
				B block = pg.beginBlock();
				pg.emitStmt(block,
						pg.emitSetter("px.state", pg.emitFunc("removeState", pg.V("tag"), pg.emitGetter("px.state"))));
				pg.emitStmt(block, pg.emitReturn(pg.emitSucc()));
				return pg.endBlock(block);
			});
			return "remove1";
		}
		if (func.startsWith("exist")) {
			if (thunk == null) {
				this.defFunc(pg, this.T("matched"), "exist1", "px", "state", "tag", "pos", () -> {
					C cond0 = pg.emitEqTag(pg.emitGetter("state.tag"), pg.V("tag"));
					C then0 = pg.emitSucc();
					C else0 = pg.emitFunc("exist1", pg.V("px"), pg.emitGetter("state.prevState"), pg.V("tag"),
							pg.V("pos"));
					return pg.emitIf(pg.emitIsNull(pg.V("state")), pg.emitFail(), pg.emitIf(cond0, then0, else0));
				});
				return "exist1";
			} else {
				String f = func + "_" + thunk;
				byte[] b = thunk.toString().getBytes();
				this.defFunc(pg, this.T("matched"), f, "px", "state", "tag", "pos", () -> {
					C cond0 = pg.emitFunc("memcmp", pg.emitGetter("state.value"), pg.vValue(thunk.toString()),
							pg.vInt(b.length));
					C then0 = pg.emitSucc();
					C else0 = pg.emitFunc(f, pg.V("px"), pg.emitGetter("state.prevState"), pg.V("tag"), pg.V("pos"));
					C cond1 = pg.emitEqTag(pg.emitGetter("state.tag"), pg.V("tag"));
					C then1 = pg.emitIf(cond0, then0, else0);
					return pg.emitIf(pg.emitIsNull(pg.V("state")), pg.emitFail(), pg.emitIf(cond1, then1, else0));
				});
				return func;
			}
		}
		if (func.equals("match")) {
			this.defFunc(pg, this.T("matched"), "match1", "px", "state", "tag", "pos", () -> {
				C cond0 = pg.emitEqTag(pg.emitGetter("state.tag"), pg.V("tag"));
				C then0 = pg.emitFunc("matchBytes", pg.V("px"), pg.emitGetter("state.value"),
						pg.emitGetter("state.cnt"));
				C else0 = pg.emitFunc("match1", pg.V("px"), pg.emitGetter("state.prevState"), pg.V("tag"), pg.V("pos"));
				return pg.emitIf(pg.emitIsNull(pg.V("state")), pg.emitFail(), pg.emitIf(cond0, then0, else0));
			});
			return "match1";
		}
		pg.log("undefined function: %s", func);
		return func;
	}

	// Tree

	void makeTreeFunc(ParserGenerator<B, C> pg) {
		this.defFunc(pg, this.T("tree"), "newTree", "tag", "inputs", "pos", "epos", "cnt", () -> {
			return pg.emitIf(pg.emitOp(pg.V("cnt"), "==", pg.vInt(0)), //
					pg.emitNewToken(pg.V("tag"), pg.V("inputs"), pg.V("pos"), pg.V("epos")), //
					pg.emitNewTree(pg.V("tag"), pg.V("cnt")));
		});
	}

	void makeTreeSetFunc(ParserGenerator<B, C> pg) {
		this.defFunc(pg, this.T("tree"), "setTree", "tree", "cnt", "label", "child", () -> {
			return pg.emitSetTree(pg.V("tree"), pg.V("cnt"), pg.V("label"), pg.V("child"));
		});
	}

	void makeParseFunc(ParserGenerator<B, C> pg, int memoSize) {
		this.defFunc(pg, this.T("tree"), "parse", "text", "newFunc", "setFunc", () -> {
			B block = pg.beginBlock();
			pg.emitVarDecl(block, false, "inputs", pg.emitCast("inputs0", pg.V("text")));
			pg.emitVarDecl(block, false, "length", pg.emitAsm("inputs0.length"));
			pg.emitVarDecl(block, false, "px",
					pg.emitFunc("NezParserContext", //
							pg.V("inputs"), pg.V("length"), //
							pg.IfNull(pg.V("newFunc"), pg.emitFuncRef("newTree")), //
							pg.IfNull(pg.V("setFunc"), pg.emitFuncRef("setTree"))));
			if (memoSize > 0) {
				pg.emitStmt(block, pg.emitFunc("initMemo", pg.V("px")));
			}
			pg.emitStmt(block,
					pg.emitReturn(pg.emitIf(pg.emitNonTerminal("e0"), pg.emitGetter("px.tree"), pg.emitNull())));
			return (pg.endBlock(block));
		});
	}

	void makeMain(ParserGenerator<B, C> pg, int memoSize) {
		this.makeTreeFunc(pg);
		this.makeTreeSetFunc(pg);
		this.makeParseFunc(pg, memoSize);
	}

}
