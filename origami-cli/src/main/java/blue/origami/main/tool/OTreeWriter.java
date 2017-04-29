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

package blue.origami.main.tool;

import blue.nez.ast.Tree;
import blue.origami.util.OCommonWriter;
import blue.origami.util.OOption;
import blue.origami.util.OptionalFactory;

public class OTreeWriter extends OCommonWriter implements OptionalFactory<OTreeWriter> {

	@Override
	public Class<?> keyClass() {
		return OTreeWriter.class;
	}

	@Override
	public OTreeWriter clone() {
		return new OTreeWriter();
	}

	@Override
	public void init(OOption options) {

	}

	public void write(Tree<?> t) {
		this.print(t.toString());
	}

	public void writeln(Tree<?> t) {
		this.write(t);
		this.println();
		this.flush();
	}

}
