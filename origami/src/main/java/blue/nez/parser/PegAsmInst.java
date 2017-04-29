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

import blue.nez.parser.pegasm.PegAsm;
import blue.nez.parser.pegasm.PegAsmVisitor;

public abstract class PegAsmInst {
	public int id;
	public boolean joinPoint = false;
	public final PegAsmFunc apply;
	public PegAsmInst next;

	public PegAsmInst(PegAsmInst next) {
		this.id = -1;
		this.next = next;
		this.apply = this::exec;
	}

	public final String getName() {
		return this.getClass().getSimpleName().substring(3);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		PegAsm.stringfy(this, sb);
		return sb.toString();
	}

	public abstract void visit(PegAsmVisitor v);

	public abstract PegAsmInst exec(PegAsmContext<?> sc) throws ParserTerminationException;

	public final boolean isIncrementedNext() {
		if (this.next != null) {
			return this.next.id == this.id + 1;
		}
		return true; // RET or instructions that are unnecessary to go next
	}

	public PegAsmInst branch() {
		return null;
	}

}
