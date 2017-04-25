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

package blue.nez.parser;

import blue.nez.peg.Grammar;
import blue.nez.peg.Production;

public class ParserGrammar extends Grammar {
	private boolean isBinary = false;

	ParserGrammar(String name, boolean isBinary) {
		super(name, null);
		this.isBinary = isBinary;
	}

	public boolean isBinary() {
		return this.isBinary;
	}

	@Override
	public String getUniqueName(String name) {
		return name;
	}

	@Override
	public void addPublicProduction(String name) {
	}

	@Override
	public Production[] getPublicProductions() {
		return new Production[0];
	}
}