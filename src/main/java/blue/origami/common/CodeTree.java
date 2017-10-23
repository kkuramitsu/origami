/***********************************************************************
 * Copyright 2017 Kimio Kuramitsu and ORIGAMI project
 *  *
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

package blue.origami.common;

import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.code.ErrorCode;

public class CodeTree extends Tree<CodeTree> {

	public CodeTree() {
		super();
	}

	private CodeTree(Symbol tag, OSource source, long pos, int len, int size, Object value) {
		super(tag, source, pos, len, size > 0 ? new CodeTree[size] : null, value);
	}

	@Override
	protected CodeTree dupImpl() {
		CodeTree t = new CodeTree(this.getTag(), this.getSource(), this.getSourcePosition(), this.getLength(),
				this.size(), this.getValue());
		// t.rule = this.rule;
		return t;
	}

	@Override
	protected RuntimeException newNoSuchLabel(Symbol label) {
		throw new ErrorCode(this, TFmt.YY1_does_not_exist_in_YY2, label, this.getTag());
	}

	@Override
	public Object apply(Symbol tag, OSource s, int spos, int epos, int nsubs, Object value) {
		return new CodeTree(tag, s, spos, epos - spos, nsubs, value);
	}

	@Override
	public Object apply(Object parent, int index, Symbol label, Object child) {
		((CodeTree) parent).set(index, label, (CodeTree) child);
		return parent;
	}

}
