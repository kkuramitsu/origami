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

package origami.ffi;

import java.util.ArrayList;
import java.util.List;

public abstract class Rule {
	Case ifCase;
	// Function f;

	public Object match(Object target, List<Object> l) {
		l.clear();
		if (!ifCase.match(target, l)) {
			return void.class;
		}
		return this.invoke(l.toArray(new Object[l.size()]));
	}

	public abstract Object invoke(Object[] matched);

	public static Object matchAll(Object target, Rule... rules) {
		ArrayList<Object> l = new ArrayList<>(8);
		for (Rule r : rules) {
			Object result = r.match(target, l);
			if (result != void.class) {
				return result;
			}
		}
		return null;
	}
}