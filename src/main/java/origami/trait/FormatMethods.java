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

package origami.trait;

import origami.nez.ast.Tree;

public interface FormatMethods {
	public default String format(String fmt, Object[] args) {
		return OStringOut.format(fmt, args);
	}

	public default String format(Tree<?> t, String msg, String fmt, Object[] args) {
		String m = format(fmt, args);
		if (t != null) {
			return t.getSource().formatPositionLine(msg, t.getSourcePosition(), m);
		}
		return m;
	}

}
