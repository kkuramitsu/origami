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

import java.util.ArrayList;
import java.util.HashMap;

import blue.nez.parser.ParserGrammar.MemoPoint;
import blue.origami.util.OOption;

public abstract class ParserCode<I> implements ParserExecutable {

	protected final OOption options;
	protected final ParserGrammar grammar;
	protected ArrayList<I> codeList;
	protected HashMap<String, I> codeMap;

	protected ParserCode(ParserGrammar grammar, OOption options, I[] initArray) {
		this.options = options;
		this.grammar = grammar;
		this.codeList = initArray != null ? new ArrayList<>() : null;
		this.codeMap = new HashMap<>();
	}

	@Override
	public final ParserGrammar getGrammar() {
		return this.grammar;
	}

	public final I getStartInstruction() {
		return this.codeList.get(0);
	}

	public final void setInstruction(String uname, I inst) {
		this.codeMap.put(uname, inst);
	}

	public final I getInstruction(String uname) {
		return this.codeMap.get(uname);
	}

	public final int getInstructionSize() {
		return this.codeList.size();
	}

	public final void initMemoPoint() {
		this.grammar.initMemoPoint();
	}

	public final MemoPoint getMemoPoint(String uname) {
		return this.grammar.getMemoPoint(uname);
	}

	public final int getMemoPointSize() {
		return this.grammar.getMemoPointSize();
	}

	abstract public void dump();
}
