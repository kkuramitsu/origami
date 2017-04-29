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

package blue.origami.ocode;

import blue.origami.lang.OEnv;
import blue.origami.util.OCommonWriter;
import blue.origami.util.OConsole;
import blue.origami.util.OOption;
import blue.origami.util.OptionalFactory;
import blue.origami.util.StringCombinator;

public class OCodeWriter extends OCommonWriter implements OptionalFactory<OCodeWriter> {

	@Override
	public Class<?> keyClass() {
		return OCodeWriter.class;
	}

	@Override
	public OCodeWriter clone() {
		return this.newClone();
	}

	protected OOption options;

	@Override
	public void init(OOption options) {
		this.options = options;
	}

	public void writeCode(OEnv env, OCode code) throws Throwable {
		// eval
		Object value = code.eval(env);
		if (!code.getType().is(void.class)) {
			String t2 = code.getType().toString();
			StringBuilder sb = new StringBuilder();
			sb.append(OConsole.color(OConsole.Gray, " => "));
			StringCombinator.appendQuoted(sb, value);
			OConsole.beginColor(sb, OConsole.Cyan);
			sb.append(" :");
			StringCombinator.append(sb, t2);
			OConsole.endColor(sb);
			this.print(sb.toString());
		}
	}

	public void writeCodeLine(OEnv env, OCode node) throws Throwable {
		this.writeCode(env, node);
		this.println();
	}

}
