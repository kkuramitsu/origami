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

package blue.origami.nez.ast;

public class CommonTree extends Tree<CommonTree> {

	public CommonTree() {
		super(Symbol.unique("prototype"), null, 0, 0, null, null);
	}

	public CommonTree(Symbol tag, Source source, long pos, int len, int size, Object value) {
		super(tag, source, pos, len, size > 0 ? new CommonTree[size] : null, value);
	}

	public CommonTree newInstance(Symbol tag, int size, Object value) {
		return new CommonTree(tag, this.getSource(), this.getSourcePosition(), 0, size, value);
	}

	@Override
	protected CommonTree dupImpl() {
		return new CommonTree(this.getTag(), this.getSource(), this.getSourcePosition(), this.getLength(), this.size(),
				this.getValue());
	}

	@Override
	public Object apply(Symbol tag, Source s, int spos, int epos, int nsubs, Object value) {
		Object t = new CommonTree(tag, s, spos, epos - spos, nsubs, value);
		// if (nsubs == 0) {
		// System.out.println(t);
		// }
		return t;
	}

	@Override
	public Object apply(Object tree, int index, Symbol label, Object child) {
		((CommonTree) tree).set(index, label, (CommonTree) child);
		// if (index == 0) {
		// System.out.println(tree);
		// }
		return tree;
	}

}
