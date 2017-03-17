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

import origami.OLog;
import origami.nez.ast.Tree;
import origami.trait.OStringOut;

@SuppressWarnings("serial")
public class OrigamiException extends RuntimeException {

	public OrigamiException(OLog log) {
		super(log.toString());
	}

	public OrigamiException(String fmt, Object... args) {
		super(message(null, fmt, args));
	}

	public OrigamiException(Throwable e, String fmt, Object... args) {
		this(OStringOut.format(fmt, args) + " by " + e);
	}

	static final String message(String fmt, Object... args) {
		return OStringOut.format(fmt, args);
	}

	private static String format(Tree<?> s, String name) {
		String msg = "no " + name;
		if (s != null) {
			msg = s.getSource().formatPositionLine("error", s.getSourcePosition(), msg);
		}
		return msg;
	}

	@SuppressWarnings("serial")
	public static class NotFoundException extends OrigamiException {
		public NotFoundException(Tree<?> s, String name) {
			super(format(s, name));
		}
	}

}
