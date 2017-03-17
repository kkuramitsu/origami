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

package origami;

import origami.nez.ast.Source;
import origami.nez.ast.Tree;
import origami.trait.OStringOut;

public class OSource implements OStringOut {
	Source s = null;
	int pos;

	String fileName;
	int linenum = 0;
	String line = null;

	public OSource(Tree<?> t) {
		this.s = t.getSource();
		this.pos = (int) t.getSourcePosition();
		this.fileName = s.getResourceName();
	}

	private void lazyCheck() {
		if (line == null) {
			this.linenum = (int) s.linenum(pos);
			this.line = s.getLineString(pos);
			this.pos = s.column(pos);
			ODebug.trace("<%s>", this.line);
			s = null;
		}
	}

	public int getFileName() {
		return this.getFileName();
	}

	public int getLineNum() {
		this.lazyCheck();
		return this.linenum;
	}

	public String getLineString() {
		this.lazyCheck();
		return this.line;
	}

	public int getColumn() {
		this.lazyCheck();
		return this.pos;
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append("(");
		sb.append(this.getFileName());
		sb.append(":");
		sb.append(this.getLineNum());
		sb.append("+");
		sb.append(this.getColumn());
		sb.append(") ");
		sb.append(this.getLineString());
	}

	@Override
	public String toString() {
		return OStringOut.stringfy(this);
	}

	public static OSource from(Tree<?> t) {
		// TODO Auto-generated method stub
		return null;
	}

}
