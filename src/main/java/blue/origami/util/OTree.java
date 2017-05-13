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

package blue.origami.util;

import blue.nez.ast.Source;
import blue.nez.ast.Symbol;
import blue.nez.ast.Tree;
import blue.origami.ocode.ErrorCode;
import blue.origami.rule.OFmt;

public class OTree extends Tree<OTree> {

	public OTree() {
		super();
	}

	private OTree(Symbol tag, Source source, long pos, int len, int size, Object value) {
		super(tag, source, pos, len, size > 0 ? new OTree[size] : null, value);
	}

	@Override
	protected OTree dupImpl() {
		OTree t = new OTree(this.getTag(), this.getSource(), this.getSourcePosition(), this.getLength(), this.size(),
				this.getValue());
		// t.rule = this.rule;
		return t;
	}

	@Override
	protected RuntimeException newNoSuchLabel(Symbol label) {
		throw new ErrorCode(null, this, OFmt.YY0_does_not_exist, "$" + label);
	}

	@Override
	public Object apply(Symbol tag, Source s, int spos, int epos, int nsubs, Object value) {
		return new OTree(tag, s, spos, epos - spos, nsubs, value);
	}

	@Override
	public Object apply(Object parent, int index, Symbol label, Object child) {
		((OTree) parent).set(index, label, (OTree) child);
		return parent;
	}

}
