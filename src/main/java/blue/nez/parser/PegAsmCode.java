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

import java.util.List;

import blue.nez.ast.Source;
import blue.nez.peg.Grammar;
import blue.nez.pegasm.ASMnop;
import blue.origami.util.OOption;

public class PegAsmCode extends ParserCode<PegAsmInstruction> {

	public PegAsmCode(Grammar grammar, OOption options) {
		super(grammar, options, new PegAsmInstruction[1024]);
	}

	List<PegAsmInstruction> codeList() {
		return this.codeList;
	}

	public int trapId(String type) {
		return -1;
	}

	@Override
	public <T> ParserContext<T> newContext(Source s, long pos, TreeConstructor<T> newTree, TreeConnector<T> linkTree) {
		PegAsmContext<T> ctx = new PegAsmContext<>(s, pos, newTree, linkTree);
		ctx.setTrap((TrapAction[]) this.options.get(ParserOption.TrapActions));
		int w = this.options.intValue(ParserOption.WindowSize, 64);
		if (this.getMemoPointSize() > 0 && w > 0) {
			ctx.initMemoTable(w, this.getMemoPointSize());
		}
		return ctx;
	}

	@Override
	public final <E> E exec(ParserContext<E> ctx) {
		int ppos = (int) ctx.getPosition();
		PegAsmInstruction code = this.getStartInstruction();
		boolean result = this.exec((PegAsmContext<E>) ctx, code);
		if (ctx.left == null && result) {
			ctx.left = ctx.newTree(null, ppos, (int) ctx.getPosition(), 0, null);
		}
		return result ? ctx.left : null;
	}

	private <E> boolean exec(PegAsmContext<E> ctx, PegAsmInstruction inst) {
		PegAsmInstruction cur = inst;
		try {
			while (true) {
				PegAsmInstruction next = cur.exec(ctx);
				// if (next == null) {
				// System.out.println("null " + cur);
				// }
				cur = next;
			}
		} catch (ParserTerminationException e) {
			return e.status;
		}
	}

	/* dump */

	@Override
	public void dump() {
		for (PegAsmInstruction inst : this.codeList) {
			PegAsmInstruction in = inst;
			if (in instanceof ASMnop) {
				System.out.println(((ASMnop) in).name);
				continue;
			}
			if (in.joinPoint) {
				System.out.println("L" + in.id);
			}
			System.out.println("\t" + inst);
			if (!in.isIncrementedNext()) {
				System.out.println("\tjump L" + in.next.id);
			}
		}
	}

}
