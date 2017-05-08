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

public abstract class RuntimeGenerator<C> extends CodeSection<C> {

	protected void defFunc(AbstractParserGenerator<C> pg, String ret, String funcName, String[] params,
			Block<C> block) {
		if (!pg.isDefined(funcName)) {
			SourceSection sec = this.openSection(this.RuntimeLibrary);
			pg.defineSymbol(funcName, pg.localName(funcName));
			pg.declFunc(ret, funcName, params, block);
			this.closeSection(sec);
		}
	}

	protected void defFunc(AbstractParserGenerator<C> pg, String ret, String funcName, String a0, Block<C> block) {
		this.defFunc(pg, ret, funcName, new String[] { a0 }, block);
	}

	protected void defFunc(AbstractParserGenerator<C> pg, String ret, String funcName, String a0, String a1,
			Block<C> block) {
		this.defFunc(pg, ret, funcName, new String[] { a0, a1 }, block);
	}

	protected void defFunc(AbstractParserGenerator<C> pg, String ret, String funcName, String a0, String a1, String a2,
			Block<C> block) {
		this.defFunc(pg, ret, funcName, new String[] { a0, a1, a2 }, block);
	}

	protected void defFunc(AbstractParserGenerator<C> pg, String ret, String funcName, String a0, String a1, String a2,
			String a3, Block<C> block) {
		this.defFunc(pg, ret, funcName, new String[] { a0, a1, a2, a3 }, block);
	}

	protected void defFunc(AbstractParserGenerator<C> pg, String ret, String funcName, String a0, String a1, String a2,
			String a3, String a4, Block<C> block) {
		this.defFunc(pg, ret, funcName, new String[] { a0, a1, a2, a3, a4 }, block);
	}

	private String T(String var, String typeName) {
		String t = this.T(var);
		return t == null ? typeName : t;
	}

	void makeTypeDefinition(AbstractParserGenerator<C> pg, int memoSize) {
		SourceSection sec = this.openSection(this.RuntimeLibrary);
		pg.declStruct(this.T("treeLog", "TreeLog"), "op?", "pos?", "data?", "tree?", "prevLog?");
		// if (pg.isStateful()) {
		pg.declStruct(this.T("state", "State"), "tag?", "cnt?", "value?", "prevState?");
		// pg.declFuncType(this.T("matched"), this.T("sf", "SymbolFunc"), "px",
		// "state", "tag", "pos");
		// }
		pg.declFuncType(this.T("tree"), this.T("newFunc", "TreeFunc"), "tag", "inputs", "pos", "epos", "cnt");
		pg.declFuncType(this.T("tree"), this.T("setFunc", "TreeSetFunc"), "tree", "cnt", "label", "child");
		pg.declStruct(this.T("px", "NezParserContext"), "inputs", "length", "pos?", "tree?", "treeLog?", "newFunc",
				"setFunc", "state?", "memos?", "uLog?", "uState?");
		if (memoSize > 0) {
			if (pg.isStateful()) {
				pg.declStruct(this.T("m", "MemoEntry"), "key", "result?", "pos?", "data?", "state?");
			} else {
				pg.declStruct(this.T("m", "MemoEntry"), "key", "result?", "pos?", "data?");
			}
		}
		pg.declFuncType(this.T("matched"), "ParserFunc", "px");
		this.closeSection(sec);
	}

	void makeGetFunc(AbstractParserGenerator<C> pg) {
		this.defFunc(pg, this.T("ch"), "getbyte", "px", () -> {
			C expr = pg.emitArrayIndex(pg.emitGetter("px.inputs"), pg.emitGetter("px.pos"));
			return (expr);
		});
	}

	void makeNextFunc(AbstractParserGenerator<C> pg) {
		this.defFunc(pg, this.T("ch"), "nextbyte", "px", () -> {
			C inc = pg.emitInc(pg.emitGetter("px.pos"));
			if (inc != null) {
				C expr = pg.emitArrayIndex(pg.emitGetter("px.inputs"), inc);
				return (expr);
			} else {
				C block = pg.beginBlock();
				block = pg.emitVarDecl(block, false, "ch",
						pg.emitArrayIndex(pg.emitGetter("px.inputs"), pg.emitGetter("px.pos")));
				block = pg.emitStmt(block,
						pg.emitSetter("px.pos", pg.emitOp(pg.emitGetter("px.pos"), "+", pg.vInt(1))));
				block = pg.emitStmt(block, pg.emitReturn(pg.V("ch")));
				return (block);
			}
		});
	}

	void makeMoveFunc(AbstractParserGenerator<C> pg) {
		this.defFunc(pg, this.T("matched"), "move", "px", "shift", () -> {
			C block = pg.beginBlock();
			block = pg.emitStmt(block, pg.emitSetter("px.pos", pg.emitOp(pg.emitGetter("px.pos"), "+", pg.V("shift"))));
			block = pg.emitStmt(block, pg.emitReturn(pg.emitSucc()));
			return (pg.endBlock(block));
		});
	}

	void makeMatchFunc(AbstractParserGenerator<C> pg) {
		this.defFunc(pg, this.T("matched"), "match", "px", "ch", () -> {
			C expr = pg.emitFunc("nextbyte", pg.V("px"));
			expr = pg.emitOp(expr, "==", pg.V("ch"));
			return (expr);
		});
	}

	void makeEOfFunc(AbstractParserGenerator<C> pg) {
		this.defFunc(pg, this.T("matched"), "neof", "px", () -> {
			C length = pg.emitGetter("px.length");
			return (pg.emitOp(pg.emitGetter("px.pos"), "<", length));
		});
	}

	String funcSucc(AbstractParserGenerator<C> pg) {
		String funcName = "f" + pg.s("true");
		// this.defFunc(pg, pg.T("matched"), funcName, "px", () -> {
		// return pg.emitSucc();
		// });
		return funcName;
	}

	String funcFail(AbstractParserGenerator<C> pg) {
		String funcName = "f" + pg.s("false");
		// this.defFunc(pg, pg.T("matched"), funcName, "px", () -> {
		// return pg.emitFail();
		// });
		return funcName;
	}

	void makeMatchLibs(AbstractParserGenerator<C> pg) {
		this.makeGetFunc(pg);
		this.makeNextFunc(pg);
		this.makeMoveFunc(pg);
		this.makeMatchFunc(pg);
		this.makeEOfFunc(pg);
		this.defFunc(pg, pg.T("matched"), "f" + pg.s("true"), "px", () -> {
			return pg.emitSucc();
		});
		this.defFunc(pg, pg.T("matched"), "f" + pg.s("false"), "px", () -> {
			return pg.emitFail();
		});

	}

	/* Log */

	void makeLogTreeFunc(AbstractParserGenerator<C> pg) {
		this.defFunc(pg, this.T("matched"), "logTree", "px", "op", "pos", "data", "tree", () -> {
			C block = pg.beginBlock();
			C newLogFunc = pg.isDefined("UtreeLog") ? pg.emitFunc("useTreeLog", pg.V("px"))
					: pg.emitFunc(this.nullCheck(pg.T("treeLog"), "TreeLog"));
			block = pg.emitVarDecl(block, false, "treeLog", newLogFunc);
			block = pg.emitStmt(block, pg.emitSetter("treeLog.op", pg.V("op")));
			block = pg.emitStmt(block, pg.emitSetter("treeLog.pos", pg.V("pos")));
			block = pg.emitStmt(block, pg.emitSetter("treeLog.data", pg.V("data")));
			block = pg.emitStmt(block, pg.emitSetter("treeLog.tree", pg.V("tree")));
			block = pg.emitStmt(block, pg.emitSetter("treeLog.prevLog", pg.emitGetter("px.treeLog")));
			block = pg.emitStmt(block, pg.emitSetter("px.treeLog", pg.V("treeLog")));
			block = pg.emitStmt(block, pg.emitReturn(pg.emitSucc()));
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

	void makeBeginTreeFunc(AbstractParserGenerator<C> pg) {
		this.defFunc(pg, this.T("matched"), "beginTree", "px", "shift", () -> {
			return (pg.emitFunc("logTree", pg.V("px"), pg.vInt(OpNew),
					pg.emitOp(pg.emitGetter("px.pos"), "+", pg.V("shift")), pg.emitNull(), pg.emitNull()));
		});
	}

	void makeTagTreeFunc(AbstractParserGenerator<C> pg) {
		this.defFunc(pg, this.T("matched"), "tagTree", "px", "tag", () -> {
			return (pg.emitFunc("logTree", pg.V("px"), pg.vInt(OpTag), pg.vInt(0), pg.emitCast("data", pg.V("tag")),
					pg.emitNull()));
		});
	}

	void makeValueTreeFunc(AbstractParserGenerator<C> pg) {
		this.defFunc(pg, this.T("matched"), "valueTree", "px", "value", () -> {
			return (pg.emitFunc("logTree", pg.V("px"), pg.vInt(OpValue), pg.vInt(0), pg.emitCast("data", pg.V("value")),
					pg.emitNull()));
		});
	}

	void makeLinkTreeFunc(AbstractParserGenerator<C> pg) {
		this.defFunc(pg, this.T("matched"), "linkTree", "px", "label", () -> {
			return (pg.emitFunc("logTree", pg.V("px"), pg.vInt(OpLink), pg.vInt(0), pg.emitCast("data", pg.V("label")),
					pg.emitGetter("px.tree")));
		});
	}

	void makeFoldTreeFunc(AbstractParserGenerator<C> pg) {
		this.defFunc(pg, this.T("matched"), "foldTree", "px", "shift", "label", () -> {
			C log1 = pg.emitFunc("beginTree", pg.V("px"), pg.V("shift"));
			C log2 = pg.emitFunc("linkTree", pg.V("px"), pg.V("label"));
			return (pg.emitAnd(log1, log2));
		});
	}

	void makeEndTreeFunc(AbstractParserGenerator<C> pg) {
		this.defFunc(pg, this.T("matched"), "endTree", "px", "shift", "tag", "value", () -> {
			C block = pg.beginBlock();
			block = pg.emitVarDecl(block, true, "cnt", pg.vInt(0));
			block = pg.emitVarDecl(block, true, "treeLog", pg.emitGetter("px.treeLog"));
			block = pg.emitVarDecl(block, true, "prevLog", pg.emitGetter("px.treeLog"));
			/* while */
			C loopCond = pg.emitOp(pg.emitGetter("prevLog.op"), "!=", pg.vInt(OpNew));
			C loopNext = pg.emitAssign("prevLog", pg.emitGetter("prevLog.prevLog"));
			C ifLinkCond = pg.emitOp(pg.emitGetter("prevLog.op"), "==", pg.vInt(OpLink));

			block = pg.emitWhileStmt(block, loopCond, () -> {
				C ifTagCond = pg.emitAnd(pg.emitOp(pg.V("tag"), "==", pg.emitNull()),
						pg.emitOp(pg.emitGetter("prevLog.op"), "==", pg.vInt(OpTag)));
				C ifValueCond = pg.emitAnd(pg.emitOp(pg.V("value"), "==", pg.emitNull()),
						pg.emitOp(pg.emitGetter("prevLog.op"), "==", pg.vInt(OpValue)));
				C block2 = pg.beginBlock();
				block2 = pg.emitIfStmt(block2, ifLinkCond, false, () -> {
					return pg.emitAssign("cnt", pg.emitOp(pg.V("cnt"), "+", pg.vInt(1)));
				});
				block2 = pg.emitIfStmt(block2, ifTagCond, false, () -> {
					return pg.emitAssign("tag", pg.emitCast("tag", pg.emitGetter("prevLog.data")));
				});
				block2 = pg.emitIfStmt(block2, ifValueCond, false, () -> {
					return pg.emitAssign("value", pg.emitCast("value", pg.emitGetter("prevLog.data")));
				});
				block2 = pg.emitStmt(block2, loopNext);
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
			param2.add(pg.emitArrayLength(pg.V("value")));
			param2.add(pg.V("cnt"));
			C newTree = pg.emitIf(pg.emitOp(pg.V("value"), "==", pg.emitNull()),
					pg.emitApply(pg.emitGetter("px.newFunc"), param),
					pg.emitApply(pg.emitGetter("px.newFunc"), param2));
			block = pg.emitStmt(block, pg.emitSetter("px.tree", newTree));
			// set..
			block = pg.emitStmt(block, pg.emitAssign("prevLog", pg.V("treeLog")));

			block = pg.emitWhileStmt(block, loopCond, () -> {
				C block2 = pg.beginBlock();
				block2 = pg.emitIfStmt(block2, ifLinkCond, false, () -> {
					C block3 = pg.beginBlock();
					block3 = pg.emitStmt(block3, pg.emitAssign("cnt", pg.emitOp(pg.V("cnt"), "-", pg.vInt(1))));
					C setFunc = pg.emitApply(pg.emitGetter("px.setFunc"), pg.emitGetter("px.tree"), pg.V("cnt"),
							pg.emitCast("label", pg.emitGetter("prevLog.data")), pg.emitGetter("prevLog.tree"));
					block3 = pg.emitStmt(block3, pg.emitSetter("px.tree", setFunc));
					return pg.endBlock(block3);
				});
				block2 = pg.emitStmt(block2, loopNext);
				return pg.endBlock(block2);
			});
			block = pg.emitStmt(block, pg.emitBack("treeLog", pg.emitGetter("prevLog.prevLog")));
			block = pg.emitStmt(block, pg.emitReturn(pg.emitSucc()));
			return (pg.endBlock(block));
		});
	}

	void makeBackLinkFunc(AbstractParserGenerator<C> pg) {
		this.defFunc(pg, pg.T("matched"), "backLink", "px", "treeLog", "label", "tree", () -> {
			C block = pg.beginBlock();
			block = pg.emitStmt(block, pg.emitBack("treeLog", pg.V("treeLog")));
			block = pg.emitStmt(block, pg.emitFunc("linkTree", pg.V("px"), pg.V("label")));
			block = pg.emitStmt(block, pg.emitBack("tree", pg.V("tree")));
			block = pg.emitStmt(block, pg.emitReturn(pg.emitSucc()));
			return (pg.endBlock(block));
		});
	}

	void makeUseLogFunc(AbstractParserGenerator<C> pg) {
		this.defFunc(pg, this.T("treeLog"), "useTreeLog", "px", () -> {
			C block = pg.beginBlock();
			block = pg.emitIfStmt(block, pg.emitOp(pg.emitGetter("px.uLog"), "==", pg.emitNull()), false, () -> {
				return pg.emitReturn(pg.emitFunc(pg.T("treeLog")));
			});
			block = pg.emitVarDecl(block, false, "uLog", pg.emitGetter("px.uLog"));
			block = pg.emitStmt(block, pg.emitSetter("px.uLog", pg.emitGetter("uLog.prevLog")));
			block = pg.emitStmt(block, pg.emitReturn(pg.V("uLog")));
			return pg.endBlock(block);
		});
	}

	void makeUnuseLogFunc(AbstractParserGenerator<C> pg) {
		this.defFunc(pg, this.T("treeLog"), pg.s("UtreeLog"), "px", "treeLog", () -> {
			C block = pg.beginBlock();
			block = pg.emitVarDecl(block, false, "uLog", pg.emitGetter("px.treeLog"));
			block = pg.emitWhileStmt(block, pg.emitOp(pg.V("uLog"), "!=", pg.V("treeLog")), () -> {
				C block2 = pg.beginBlock();
				block2 = pg.emitVarDecl(block2, false, "prevLog", pg.V("uLog"));
				block2 = pg.emitStmt(block2, pg.emitAssign("uLog", pg.emitGetter("uLog.prevLog")));
				block2 = pg.emitStmt(block2, pg.emitSetter("prevLog.prevLog", pg.emitGetter("px.uLog")));
				block2 = pg.emitStmt(block2, pg.emitSetter("px.uLog", pg.V("prevLog")));
				return pg.endBlock(block2);
			});
			block = pg.emitStmt(block, pg.emitReturn(pg.V("treeLog")));
			return pg.endBlock(block);
		});
	}

	void makeTreeLibs(AbstractParserGenerator<C> pg) {
		if (pg.isDefined("UtreeLog")) {
			this.makeUseLogFunc(pg);
			this.makeUnuseLogFunc(pg);
		}
		this.makeLogTreeFunc(pg);
		this.makeBeginTreeFunc(pg);
		this.makeTagTreeFunc(pg);
		this.makeValueTreeFunc(pg);
		this.makeLinkTreeFunc(pg);
		this.makeFoldTreeFunc(pg);
		this.makeEndTreeFunc(pg);
		this.makeBackLinkFunc(pg);
	}

	/* SimpleTree */

	void makeSimpleTree(AbstractParserGenerator<C> pg) {
		SourceSection sec = this.openSection(this.RuntimeLibrary);
		pg.declStruct("SimpleTree", "tag", "pos", "data");
		this.closeSection(sec);
	}

	/* Memo */

	static int ResultFail = 0;
	static int ResultSucc = 1;
	static int ResultUnfound = 2;

	void makeMemoStruct(AbstractParserGenerator<C> pg) {
		SourceSection sec = this.openSection(this.RuntimeLibrary);
		if (pg.isStateful()) {
			pg.declStruct(this.T("m", "MemoEntry"), "key", "result?", "pos?", "data?", "state?");
		} else {
			pg.declStruct(this.T("m", "MemoEntry"), "key", "result?", "pos?", "data?");
		}
		this.closeSection(sec);
	}

	void makeInitMemoFunc(AbstractParserGenerator<C> pg, int memoSize) {
		this.defFunc(pg, this.T("matched"), "initMemo", "px", () -> {
			C block = pg.beginBlock();
			if (memoSize > 0) {
				block = pg.emitVarDecl(block, false, "cnt", pg.vInt(0));
				block = pg.emitStmt(block, pg.emitSetter("px.memos", pg.emitNewArray(this.T("m"), pg.vInt(memoSize))));
				/* while */
				C loopCond = pg.emitOp(pg.V("cnt"), "<", pg.vInt(memoSize));
				block = pg.emitWhileStmt(block, loopCond, () -> {
					C block2 = pg.beginBlock();
					C left = pg.emitArrayIndex(pg.emitGetter("px.memos"), pg.V("cnt"));
					block2 = pg.emitStmt(block2, pg.emitAssign2(left, pg.emitFunc("MemoEntry", pg.vInt(-1))));
					block2 = pg.emitStmt(block2, pg.emitAssign("cnt", pg.emitOp(pg.V("cnt"), "+", pg.vInt(1))));
					return pg.endBlock(block2);
				});
			}
			block = pg.emitStmt(block, pg.emitReturn(pg.emitSucc()));
			return (pg.endBlock(block));
		});
	}

	void makeKeyFunc(AbstractParserGenerator<C> pg, int window) {
		this.defFunc(pg, this.T("key"), "longkey", "key", "memoPoint", () -> {
			C key = pg.emitOp(pg.V("key"), "*", pg.vInt(window));
			key = pg.emitOp(key, "+", pg.V("memoPoint"));
			return (key);
		});
	}

	void makeGetMemoFunc(AbstractParserGenerator<C> pg, int memoSize) {
		this.defFunc(pg, pg.T("m"), "getMemo", "px", "key", () -> {
			C index = pg.emitCast("cnt", pg.emitOp(pg.V("key"), "%", pg.vInt(memoSize)));
			C m = pg.emitArrayIndex(pg.emitGetter("px.memos"), index);
			return (m);
		});
	}

	void makeLookupFunc(AbstractParserGenerator<C> pg, boolean withTree) {
		String suffix = withTree ? "3" : "1";
		this.defFunc(pg, this.T("result"), "lookupMemo" + suffix, "px", "memoPoint", () -> {
			C block = pg.beginBlock();
			block = pg.emitVarDecl(block, false, "key",
					pg.emitFunc("longkey", pg.emitGetter("px.pos"), pg.V("memoPoint")));
			block = pg.emitVarDecl(block, false, "m", pg.emitFunc("getMemo", pg.V("px"), pg.V("key")));
			// m.key == key
			C cond = pg.emitOp(pg.emitGetter("m.key"), "==", pg.V("key"));
			if (pg.isStateful()) {
				// m.key == key && m.state == state
				cond = pg.emitAnd(cond, pg.emitOp(pg.emitGetter("m.state"), "==", pg.emitGetter("px.state")));
			}
			C then = pg.emitFunc("consumeMemo" + suffix, pg.V("px"), pg.V("m"));
			C result = pg.emitIf(cond, then, pg.vInt(ResultUnfound));
			block = pg.emitStmt(block, pg.emitReturn(result));
			return (pg.endBlock(block));
		});
	}

	void makeConsumeFunc(AbstractParserGenerator<C> pg, boolean withTree) {
		String suffix = withTree ? "3" : "1";
		this.defFunc(pg, this.T("result"), "consumeMemo" + suffix, "px", "m", () -> {
			C block = pg.beginBlock();
			block = pg.emitStmt(block, pg.emitSetter("px.pos", pg.emitGetter("m.pos")));
			if (withTree) {
				block = pg.emitStmt(block, pg.emitSetter("px.tree", pg.emitCast("tree", pg.emitGetter("m.data"))));
			}
			block = pg.emitStmt(block, pg.emitReturn(pg.emitGetter("m.result")));
			return (pg.endBlock(block));
		});
	}

	void makeMemoFunc(AbstractParserGenerator<C> pg) {
		this.defFunc(pg, this.T("matched"), "storeMemo", "px", "memoPoint", "pos", "matched", () -> {
			C block = pg.beginBlock();
			block = pg.emitVarDecl(block, false, "key", pg.emitFunc("longkey", pg.V("pos"), pg.V("memoPoint")));
			block = pg.emitVarDecl(block, false, "m", pg.emitFunc("getMemo", pg.V("px"), pg.V("key")));
			block = pg.emitStmt(block, pg.emitSetter("m.key", pg.V("key")));
			block = pg.emitStmt(block,
					pg.emitSetter("m.result", pg.emitIf(pg.V("matched"), pg.vInt(ResultSucc), pg.vInt(ResultFail))));
			block = pg.emitStmt(block,
					pg.emitSetter("m.pos", pg.emitIf(pg.V("matched"), pg.emitGetter("px.pos"), pg.V("pos"))));
			block = pg.emitStmt(block, pg.emitSetter("m.data", pg.emitGetter("px.tree")));
			if (pg.isStateful()) {
				block = pg.emitStmt(block, pg.emitSetter("m.state", pg.emitGetter("px.state")));
			}
			block = pg.emitStmt(block, pg.emitReturn(pg.V("matched")));
			return (pg.endBlock(block));
		});
	}

	void makeMemoLibs(AbstractParserGenerator<C> pg, int memoSize, int windowSize) {
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

	void makeUseStateFunc(AbstractParserGenerator<C> pg) {
		this.defFunc(pg, this.T("state"), "useState", "px", () -> {
			C block = pg.beginBlock();
			block = pg.emitIfStmt(block, pg.emitOp(pg.emitGetter("px.uState"), "==", pg.emitNull()), false, () -> {
				return pg.emitReturn(pg.emitFunc(pg.T("state")));
			});
			block = pg.emitVarDecl(block, false, "uState", pg.emitGetter("px.State"));
			block = pg.emitStmt(block, pg.emitSetter("px.uState", pg.emitGetter("uLog.prevState")));
			block = pg.emitStmt(block, pg.emitReturn(pg.V("uState")));
			return pg.endBlock(block);
		});
	}

	void makeUnuseStateFunc(AbstractParserGenerator<C> pg) {
		this.defFunc(pg, this.T("state"), pg.s("Ustate"), "px", "state", () -> {
			C block = pg.beginBlock();
			block = pg.emitVarDecl(block, false, "uState", pg.emitGetter("px.treeState"));
			block = pg.emitWhileStmt(block, pg.emitOp(pg.V("uState"), "!=", pg.V("treeState")), () -> {
				C block2 = pg.beginBlock();
				block2 = pg.emitVarDecl(block2, false, "prevState", pg.V("uState"));
				block2 = pg.emitStmt(block2, pg.emitAssign("uState", pg.emitGetter("uState.prevState")));
				block2 = pg.emitStmt(block2, pg.emitSetter("prevState.prevState", pg.emitGetter("px.uState")));
				block2 = pg.emitStmt(block2, pg.emitSetter("px.uState", pg.V("prevState")));
				return pg.endBlock(block2);
			});
			block = pg.emitStmt(block, pg.emitReturn(pg.V("state")));
			return pg.endBlock(block);
		});
	}

	void makeStateLibs(AbstractParserGenerator<C> pg) {
		if (pg.isDefined("Ustate")) {
			this.makeUseStateFunc(pg);
			this.makeUnuseStateFunc(pg);
		}
		// this.defFunc(pg, this.T("matched"), "memcmp", "inputs", "value",
		// "length", () -> {
		// return (pg.emitSucc());
		// });
		this.defFunc(pg, this.T("state"), "createState", "tag", "cnt", "value", "prevState", () -> {
			C block = pg.beginBlock();
			C newFunc = pg.isDefined("Ustate") ? pg.emitFunc("useState", pg.V("px"))
					: pg.emitFunc(this.nullCheck(pg.T("state"), "State"));
			block = pg.emitVarDecl(block, false, "state", newFunc);
			block = pg.emitStmt(block, pg.emitSetter("state.tag", pg.V("tag")));
			block = pg.emitStmt(block, pg.emitSetter("state.cnt", pg.V("cnt")));
			block = pg.emitStmt(block, pg.emitSetter("state.value", pg.V("value")));
			block = pg.emitStmt(block, pg.emitSetter("state.prevState", pg.V("prevState")));
			block = pg.emitStmt(block, pg.emitReturn(pg.V("state")));
			return (pg.endBlock(block));
		});

	}

	String makeStateFunc(AbstractParserGenerator<C> pg, String func, Object thunk) {
		if (func.equals("symbol")) {
			this.defFunc(pg, this.T("matched"), "symbol1", "px", "state", "tag", "pos", () -> {
				C block = pg.beginBlock();
				block = pg.emitVarDecl(block, false, "value", pg.emitFunc("extract", pg.V("px"), pg.V("pos")));
				block = pg.emitVarDecl(block, false, "length", pg.emitOp(pg.emitGetter("px.pos"), "-", pg.V("pos")));
				block = pg.emitStmt(block,
						pg.emitSetter("px.state", //
								pg.emitFunc("createState", pg.V("tag"), pg.V("length"), pg.V("value"),
										pg.emitGetter("px.state"))));
				block = pg.emitStmt(block, pg.emitReturn(pg.emitSucc()));
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
				C block = pg.beginBlock();
				block = pg.emitStmt(block,
						pg.emitSetter("px.state", pg.emitFunc("removeState", pg.V("tag"), pg.emitGetter("px.state"))));
				block = pg.emitStmt(block, pg.emitReturn(pg.emitSucc()));
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

	void makeTreeFunc(AbstractParserGenerator<C> pg) {
		this.defFunc(pg, this.T("tree"), "newTree", "tag", "inputs", "pos", "epos", "cnt", () -> {
			return pg.emitIf(pg.emitOp(pg.V("cnt"), "==", pg.vInt(0)), //
					pg.emitNewToken(pg.V("tag"), pg.V("inputs"), pg.V("pos"), pg.V("epos")), //
					pg.emitNewTree(pg.V("tag"), pg.V("cnt")));
		});
	}

	void makeTreeSetFunc(AbstractParserGenerator<C> pg) {
		this.defFunc(pg, this.T("tree"), "setTree", "tree", "cnt", "label", "child", () -> {
			return pg.emitSetTree(pg.V("tree"), pg.V("cnt"), pg.V("label"), pg.V("child"));
		});
	}

	void makeParseFunc(AbstractParserGenerator<C> pg, int memoSize) {
		this.defFunc(pg, this.T("tree"), "parse", "text", "newFunc", "setFunc", () -> {
			C block = pg.beginBlock();
			block = pg.emitVarDecl(block, false, "inputs", pg.emitCast("inputs0", pg.V("text")));
			block = pg.emitVarDecl(block, false, "length", pg.emitAsm("inputs0.length"));
			block = pg.emitVarDecl(block, false, "px",
					pg.emitFunc("NezParserContext", //
							pg.V("inputs"), pg.V("length"), //
							pg.IfNull(pg.V("newFunc"), pg.emitFuncRef("newTree")), //
							pg.IfNull(pg.V("setFunc"), pg.emitFuncRef("setTree"))));
			if (memoSize > 0) {
				block = pg.emitStmt(block, pg.emitFunc("initMemo", pg.V("px")));
			}
			block = pg.emitStmt(block,
					pg.emitReturn(pg.emitIf(pg.emitNonTerminal("e0"), pg.emitGetter("px.tree"), pg.emitNull())));
			return (pg.endBlock(block));
		});
	}

	void makeMain(AbstractParserGenerator<C> pg, int memoSize) {
		this.makeTreeFunc(pg);
		this.makeTreeSetFunc(pg);
		this.makeParseFunc(pg, memoSize);
	}

}
