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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import blue.nez.ast.Source;
import blue.nez.ast.SourcePosition;
import blue.nez.parser.ParserCode;
import blue.nez.parser.ParserGrammar;
import blue.nez.parser.ParserGrammar.MemoPoint;
import blue.nez.parser.ParserOption;
import blue.nez.parser.TrapAction;
import blue.nez.parser.pasm.PAsmAPI.PAsmContext;
import blue.nez.parser.pasm.PAsmAPI.TreeFunc;
import blue.nez.parser.pasm.PAsmAPI.TreeSetFunc;
import blue.nez.peg.NezFmt;
import blue.origami.util.OOption;

public class PAsmCode implements ParserCode {

	protected final OOption options;
	protected final ParserGrammar grammar;
	protected ArrayList<PAsmInst> codeList;
	protected HashMap<String, PAsmInst> codeMap;

	PAsmCode(ParserGrammar grammar, OOption options) {
		this.options = options;
		this.grammar = grammar;
		this.codeList = new ArrayList<>();
		this.codeMap = new HashMap<>();
	}

	@Override
	public ParserGrammar getParserGrammar() {
		return this.grammar;
	}

	@Override
	public int match(Source s, int pos, TreeFunc newTree, TreeSetFunc linkTree) {
		PAsmContext px = new PAsmContext(s, pos, newTree, linkTree);
		px.setTrap((TrapAction[]) this.options.get(ParserOption.TrapActions));
		int w = this.options.intValue(ParserOption.WindowSize, 64);
		if (this.getMemoPointSize() > 0 && w > 0) {
			PAsmAPI.initMemo(px, w, this.getMemoPointSize());
		}
		PAsmInst code = this.getStartInstruction();
		boolean result = this.exec(px, code);
		if (result) {
			return px.pos;
		}
		return -1;
	}

	@Override
	public Object parse(Source s, int pos, TreeFunc newTree, TreeSetFunc linkTree) throws IOException {
		PAsmContext px = new PAsmContext(s, pos, newTree, linkTree);
		px.setTrap((TrapAction[]) this.options.get(ParserOption.TrapActions));
		int w = this.options.intValue(ParserOption.WindowSize, 64);
		if (this.getMemoPointSize() > 0 && w > 0) {
			PAsmAPI.initMemo(px, w, this.getMemoPointSize());
		}
		int ppos = px.pos;
		PAsmInst code = this.getStartInstruction();
		boolean result = this.exec(px, code);
		if (px.tree == null && result) {
			px.tree = newTree.apply(null, s, pos, ppos, 0, null);
		}
		if (px.tree == null) {
			this.perror(this.options, SourcePosition.newInstance(s, px.getMaximumPosition()), NezFmt.syntax_error);
			return null;
		}
		if (PAsmAPI.neof(px) && this.options.is(ParserOption.PartialFailure, false)) {
			this.pwarn(this.options, SourcePosition.newInstance(s, px.pos), NezFmt.unconsumed);
		}
		return px.tree;
	}

	PAsmInst getStartInstruction() {
		return this.codeList.get(0);
	}

	void setInstruction(String uname, PAsmInst inst) {
		this.codeMap.put(uname, inst);
	}

	PAsmInst getInstruction(String uname) {
		return this.codeMap.get(uname);
	}

	int getInstructionSize() {
		return this.codeList.size();
	}

	MemoPoint getMemoPoint(String uname) {
		return this.grammar.getMemoPoint(uname);
	}

	int getMemoPointSize() {
		return this.grammar.getMemoPointSize();
	}

	List<PAsmInst> codeList() {
		return this.codeList;
	}

	public int trapId(String type) {
		return -1;
	}

	// private String indent(PAsmContext px) {
	// PAsmStack s = px.unused;
	// int indent = 0;
	// while (s.prev != null) {
	// indent++;
	// s = s.prev;
	// }
	// StringBuilder sb = new StringBuilder();
	// for (int i = 0; i < indent; i++) {
	// sb.append(" ");
	// }
	// return sb.toString();
	//
	// }

	private boolean exec(PAsmContext px, PAsmInst inst) {
		PAsmInst cur = inst;
		try {
			while (true) {
				// System.out.println(this.indent(px) + "[" + px.pos + "] " +
				// cur);
				PAsmInst next = cur.exec(px);
				cur = next;
			}
		} catch (PAsmTerminationException e) {
			return e.status;
		}
	}

	/* dump */

	public void dump() {
		for (PAsmInst inst : this.codeList) {
			PAsmInst in = inst;
			if (in instanceof Inop) {
				System.out.println(((Inop) in).name);
				continue;
			}
			// if (in.joinPoint) {
			// System.out.println("L" + in.id);
			// }
			System.out.println("\t" + inst);
			// if (!in.isIncrementedNext()) {
			// System.out.println("\tjump L" + in.next.id);
			// }
		}
	}

}
