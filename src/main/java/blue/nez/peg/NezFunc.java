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

package blue.nez.peg;

/**
 * FunctionName provides a set of Nez function names. FunctionName is used to
 * Identify the type of function in Nez.Function.
 * 
 * @author kiki
 *
 */

public enum NezFunc {
	_if, on, //
	block, local, //
	symbol, //
	match, is, isa, exists, //
	scanf, repeat;

	@Override
	public String toString() {
		String s = name();
		if (s.startsWith("_")) {
			s = s.substring(1);
		}
		return s;
	}

}
