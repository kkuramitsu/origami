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

package origami.nez.parser;

import origami.nez.peg.Expression;

public abstract class NZ86Instruction {
	public int id;
	public boolean joinPoint = false;
	public NZ86Instruction next;

	public NZ86Instruction(NZ86Instruction next) {
		this.id = -1;
		this.next = next;
	}

	public final String getName() {
		return this.getClass().getSimpleName();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		NZ86.stringfy(this, sb);
		return sb.toString();
	}

	public abstract void visit(NZ86Visitor v);

	public abstract NZ86Instruction exec(NZ86ParserContext<?> sc) throws ParserTerminationException;

	public final boolean isIncrementedNext() {
		if (this.next != null) {
			return this.next.id == this.id + 1;
		}
		return true; // RET or instructions that are unnecessary to go next
	}

	NZ86Instruction branch() {
		return null;
	}

	public Expression getExpression() {
		return null;// this.e;
	}

}
