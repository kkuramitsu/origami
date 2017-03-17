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

import origami.OEnv;
import origami.OLog.Messenger;
import origami.nez.ast.Tree;

public interface MessageMethods {
	// Message

	// public default Messenger openMessenger(OEnv env) {
	// Messenger m = new Messenger();
	// return env.setValue("__msg__", m, null);
	// }
	//
	// public default void closeMessenger(OEnv env, Messenger stacked) {
	// env.setValue("__msg__", stacked, null);
	// }

	public default Messenger getMessenger(OEnv env) {
		return env.get("__msg__", null);
	}

	public default void reportError(OEnv env, Tree<?> s, String fmt, Object... args) {
		Messenger m = this.getMessenger(env);
		m.reportError(s, fmt, args);
	}

	public default void reportWarning(OEnv env, Tree<?> s, String fmt, Object... args) {
		Messenger m = this.getMessenger(env);
		m.reportWarning(s, fmt, args);
	}

	public default void reportNotice(OEnv env, Tree<?> s, String fmt, Object... args) {
		Messenger m = this.getMessenger(env);
		m.reportWarning(s, fmt, args);
	}

}
