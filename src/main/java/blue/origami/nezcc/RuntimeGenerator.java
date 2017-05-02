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
		if (!pg.isDefinedSymbol(funcName)) {
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

	void makeContext(AbstractParserGenerator<C> pg) {
		SourceSection sec = this.openSection(this.RuntimeLibrary);
		pg.declFuncType(this.T("tree"), "TreeFunc", "tag", "inputs", "pos", "epos", "cnt");
		pg.declFuncType(this.T("tree"), "TreeSetFunc", "tree", "cnt", "label", "child");

		pg.declStruct(this.T("px"), "inputs", "length", "pos?", "tree?", "treeLog?", "newFunc", "setFunc", "state?",
				"memos?");
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
				block = pg.emitStmt(block,
						pg.emitVarDecl("ch", pg.emitArrayIndex(pg.emitGetter("px.inputs"), pg.emitGetter("px.pos"))));
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
		this.defFunc(pg, this.T("matched"), "eof", "px", () -> {
			C length = pg.emitGetter("px.length");
			return (pg.emitOp(pg.emitGetter("px.pos"), ">=", length));
		});
	}

	void makeMatchLibs(AbstractParserGenerator<C> pg) {
		this.makeContext(pg);
		this.makeGetFunc(pg);
		this.makeNextFunc(pg);
		this.makeMoveFunc(pg);
		this.makeMatchFunc(pg);
		this.makeEOfFunc(pg);
	}

	void makeTreeLog(AbstractParserGenerator<C> pg) {
		SourceSection sec = this.openSection(this.RuntimeLibrary);
		pg.declStruct(this.T("treeLog"), "op?", "pos?", "data?", "tree?", "prevLog?");
		this.closeSection(sec);
	}

	void makeNewLogFunc(AbstractParserGenerator<C> pg, int memoSize) {
		this.defFunc(pg, this.T("treeLog"), "newLog", "px", () -> {
			C block = pg.beginBlock();
			block = pg.emitStmt(block, pg.emitIfStmt(pg.emitOp(pg.emitGetter("px.uLog"), "==", pg.emitNull()), () -> {
				return pg.emitReturn(pg.emitFunc(pg.T("treeLog")));
			}));
			block = pg.emitStmt(block, pg.emitVarDecl("uLog", pg.emitGetter("px.uLog")));
			block = pg.emitStmt(block, pg.emitSetter("px.uLog", pg.emitGetter("uLog.prevLog")));
			return pg.emitReturn(pg.V("uLog"));
		});
	}

	void makeFreeLogFunc(AbstractParserGenerator<C> pg, int memoSize) {
		this.defFunc(pg, this.T("treeLog"), "freeLog", "px", "treeLog", () -> {
			C block = pg.beginBlock();
			block = pg.emitStmt(block, pg.emitVarDecl("uLog", pg.emitGetter("px.treeLog")));
			C whileBlock = pg.emitWhileStmt(pg.emitOp(pg.emitGetter("uLog"), "!=", this.V("treeLog")), () -> {
				C block2 = pg.beginBlock();
				block2 = pg.emitStmt(block2, pg.emitVarDecl("prevLog", pg.emitGetter("uLog")));
				block2 = pg.emitStmt(block2, pg.emitAssign("uLogLog", pg.emitGetter("uLog.prevLog")));
				block2 = pg.emitStmt(block2, pg.emitSetter("prevLog.prevLog", pg.emitGetter("px.uLog")));
				block2 = pg.emitStmt(block2, pg.emitSetter("px.uLog", pg.V("prevLog")));
				return pg.endBlock(block2);
			});
			block = pg.emitStmt(block, whileBlock);
			block = pg.emitStmt(block, pg.emitReturn(pg.V("treeLog")));
			return pg.endBlock(block);
		});
	}

	void makeLogTreeFunc(AbstractParserGenerator<C> pg) {
		this.defFunc(pg, this.T("matched"), "logTree", "px", "op", "pos", "data", "tree", () -> {
			C block = pg.beginBlock();
			block = pg.emitStmt(block, pg.emitVarDecl("treeLog", pg.emitFunc(pg.T("treeLog"))));
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

	static int OpNew = 0;
	static int OpTag = 1;
	static int OpValue = 2;
	static int OpLink = 3;

	void makeBeginTreeFunc(AbstractParserGenerator<C> pg) {
		this.defFunc(pg, this.T("matched"), "beginTree", "px", "shift", () -> {
			return (pg.emitFunc("logTree", pg.V("px"), pg.vInt(OpNew),
					pg.emitOp(pg.emitGetter("px.pos"), "+", this.V("shift")), pg.emitNull(), pg.emitNull()));
		});
	}

	void makeTagTreeFunc(AbstractParserGenerator<C> pg) {
		this.defFunc(pg, this.T("matched"), "tagTree", "px", "tag", () -> {
			return (pg.emitFunc("logTree", pg.V("px"), pg.vInt(OpTag), pg.vInt(0), pg.emitCast("data", this.V("tag")),
					pg.emitNull()));
		});
	}

	void makeValueTreeFunc(AbstractParserGenerator<C> pg) {
		this.defFunc(pg, this.T("matched"), "valueTree", "px", "value", () -> {
			return (pg.emitFunc("logTree", pg.V("px"), pg.vInt(OpValue), pg.vInt(0),
					pg.emitCast("data", this.V("value")), pg.emitNull()));
		});
	}

	void makeLinkTreeFunc(AbstractParserGenerator<C> pg) {
		this.defFunc(pg, this.T("matched"), "linkTree", "px", "label", () -> {
			return (pg.emitFunc("logTree", pg.V("px"), pg.vInt(OpLink), pg.vInt(0),
					pg.emitCast("data", this.V("label")), pg.emitGetter("px.tree")));
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
			block = pg.emitStmt(block, pg.emitVarDecl("cnt", pg.vInt(0)));
			block = pg.emitStmt(block, pg.emitVarDecl("treeLog", pg.emitGetter("px.treeLog")));
			block = pg.emitStmt(block, pg.emitVarDecl("prevLog", pg.emitGetter("px.treeLog")));
			/* while */
			C loopCond = pg.emitOp(pg.emitGetter("prevLog.op"), "!=", pg.vInt(OpNew));
			C loopNext = pg.emitAssign("prevLog", pg.emitGetter("prevLog.prevLog"));
			C ifLinkCond = pg.emitOp(pg.emitGetter("prevLog.op"), "==", pg.vInt(OpLink));

			block = pg.emitStmt(block, pg.emitWhileStmt(loopCond, () -> {
				C ifTagCond = pg.emitAnd(pg.emitOp(this.V("tag"), "==", pg.emitNull()),
						pg.emitOp(pg.emitGetter("prevLog.op"), "==", pg.vInt(OpTag)));
				C ifValueCond = pg.emitAnd(pg.emitOp(this.V("value"), "==", pg.emitNull()),
						pg.emitOp(pg.emitGetter("prevLog.op"), "==", pg.vInt(OpValue)));
				C block2 = pg.beginBlock();
				block2 = pg.emitStmt(block2, pg.emitIfStmt(ifLinkCond, () -> {
					return pg.emitAssign("cnt", pg.emitOp(this.V("cnt"), "+", pg.vInt(1)));
				}));
				block2 = pg.emitStmt(block2, pg.emitIfStmt(ifTagCond, () -> {
					return pg.emitAssign("tag", pg.emitCast("tag", pg.emitGetter("prevLog.data")));
				}));
				block2 = pg.emitStmt(block2, pg.emitIfStmt(ifValueCond, () -> {
					return pg.emitAssign("value", pg.emitCast("value", pg.emitGetter("prevLog.data")));
				}));
				block2 = pg.emitStmt(block2, loopNext);
				return pg.endBlock(block2);
			}));

			List<C> param = new ArrayList<>();
			param.add(pg.V("tag"));
			param.add(pg.emitGetter("px.inputs"));
			param.add(pg.emitGetter("prevLog.pos"));
			param.add(pg.emitOp(pg.emitGetter("px.pos"), "+", this.V("shift")));
			param.add(pg.V("cnt"));
			List<C> param2 = new ArrayList<>();
			param2.add(pg.V("tag"));
			param2.add(pg.V("value"));
			param2.add(pg.vInt(0));
			param2.add(pg.emitAsm(this.s("value.length")));
			param2.add(pg.V("cnt"));
			// param.add(pg.V("treeLog"));
			// param.add(pg.V("value"));
			C newTree = pg.emitIf(pg.emitOp(pg.V("value"), "==", pg.emitNull()), pg.emitFunc("px.newTree", param),
					pg.emitFunc("px.newTree", param2));
			block = pg.emitStmt(block, pg.emitSetter("px.tree", newTree));
			// set..
			block = pg.emitStmt(block, pg.emitAssign("prevLog", pg.V("treeLog")));

			block = pg.emitStmt(block, pg.emitWhileStmt(loopCond, () -> {
				C block2 = pg.beginBlock();
				block2 = pg.emitStmt(block2, pg.emitIfStmt(ifLinkCond, () -> {
					C block3 = pg.beginBlock();
					block3 = pg.emitStmt(block3, pg.emitAssign("cnt", pg.emitOp(pg.V("cnt"), "-", pg.vInt(1))));
					C setFunc = pg.emitFunc("px.setTree", pg.emitGetter("px.tree"), pg.V("cnt"),
							pg.emitCast("label", pg.emitGetter("prevLog.data")), pg.emitGetter("prevLog.tree"));
					block3 = pg.emitStmt(block3, pg.emitSetter("px.tree", setFunc));
					return pg.endBlock(block3);
				}));
				block2 = pg.emitStmt(block2, loopNext);
				return pg.endBlock(block2);
			}));
			block = pg.emitStmt(block, pg.emitBack("treeLog", pg.emitGetter("prevLog.prevLog")));
			block = pg.emitStmt(block, pg.emitReturn(pg.emitSucc()));
			return (pg.endBlock(block));
		});
	}

	void makeBackLinkFunc(AbstractParserGenerator<C> pg) {
		this.defFunc(pg, pg.s("bool"), "backLink", "px", "treeLog", "label", "tree", () -> {
			C block = pg.beginBlock();
			block = pg.emitStmt(block, pg.emitBack("treeLog", this.V("treeLog")));
			block = pg.emitStmt(block, pg.emitFunc("linkTree", pg.V("px"), pg.V("label")));
			block = pg.emitStmt(block, pg.emitBack("tree", this.V("tree")));
			block = pg.emitStmt(block, pg.emitReturn(pg.emitSucc()));
			return (pg.endBlock(block));
		});
	}

	void makeTreeLibs(AbstractParserGenerator<C> pg) {
		this.makeTreeLog(pg);
		this.makeLogTreeFunc(pg);
		this.makeBeginTreeFunc(pg);
		this.makeTagTreeFunc(pg);
		this.makeValueTreeFunc(pg);
		this.makeLinkTreeFunc(pg);
		this.makeFoldTreeFunc(pg);
		this.makeEndTreeFunc(pg);
		this.makeBackLinkFunc(pg);
	}

	/* Memo */

	static int ResultFail = 0;
	static int ResultSucc = 1;
	static int ResultUnfound = 2;

	void makeMemoStruct(AbstractParserGenerator<C> pg) {
		SourceSection sec = this.openSection(this.RuntimeLibrary);
		if (pg.isStateful()) {
			pg.declStruct(this.T("m"), "key", "result?", "pos?", "data?", "state?");
		} else {
			pg.declStruct(this.T("m"), "key", "result?", "pos?", "data?");
		}
		this.closeSection(sec);
	}

	void makeInitMemoFunc(AbstractParserGenerator<C> pg, int memoSize) {
		this.defFunc(pg, this.T("matched"), "initMemo", "px", () -> {
			C block = pg.beginBlock();
			if (memoSize > 0) {
				block = pg.emitStmt(block, pg.emitVarDecl("cnt", pg.vInt(0)));
				block = pg.emitStmt(block, pg.emitSetter("px.memos", pg.emitNewArray(this.T("m"), pg.vInt(memoSize))));
				/* while */
				C loopCond = pg.emitOp(pg.V("cnt"), "<", pg.vInt(memoSize));
				block = pg.emitStmt(block, pg.emitWhileStmt(loopCond, () -> {
					C block2 = pg.beginBlock();
					C left = pg.emitArrayIndex(pg.emitGetter("px.memos"), pg.V("cnt"));
					block2 = pg.emitStmt(block2, pg.emitAssign2(left, pg.emitFunc("MemoEntry", pg.vInt(-1))));
					block2 = pg.emitStmt(block2, pg.emitAssign("cnt", pg.emitOp(pg.V("cnt"), "+", pg.vInt(1))));
					return pg.endBlock(block2);
				}));
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
			block = pg.emitStmt(block,
					pg.emitVarDecl("key", pg.emitFunc("longkey", pg.emitGetter("px.pos"), pg.V("memoPoint"))));
			block = pg.emitStmt(block, pg.emitVarDecl("m", pg.emitFunc("getMemo", pg.V("px"), pg.V("key"))));
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
			block = pg.emitStmt(block, pg.emitVarDecl("key", pg.emitFunc("longkey", pg.V("pos"), pg.V("memoPoint"))));
			block = pg.emitStmt(block, pg.emitVarDecl("m", pg.emitFunc("getMemo", pg.V("px"), pg.V("key"))));
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
			this.makeMemoStruct(pg);
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

	//

}
