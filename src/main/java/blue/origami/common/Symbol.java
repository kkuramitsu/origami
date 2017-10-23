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

package blue.origami.common;

import java.util.ArrayList;
import java.util.HashMap;

public class Symbol {
	private static HashMap<String, Symbol> tagIdMap = new HashMap<>();
	private static ArrayList<Symbol> tagNameList = new ArrayList<>(64);

	public final static Symbol unique(String s) {
		Symbol tag = tagIdMap.get(s);
		if (tag == null) {
			tag = new Symbol(tagIdMap.size(), s);
			tagIdMap.put(s, tag);
			tagNameList.add(tag);
		}
		return tag;
	}

	public final static Symbol nullUnique(String param) {
		return param == null ? null : unique(param);
	}

	public final static int uniqueId(String symbol) {
		return unique(symbol).id;
	}

	public final static Symbol tag(int tagId) {
		return tagNameList.get(tagId);
	}

	public final static Symbol Null = unique("");
	public final static Symbol MetaSymbol = unique("$");

	final int id;
	final String symbol;

	private Symbol(int id, String symbol) {
		this.id = id;
		this.symbol = symbol;
	}

	@Override
	public final int hashCode() {
		return this.id;
	}

	@Override
	public final boolean equals(Object o) {
		return this == o;
	}

	public final int id() {
		return this.id;
	}

	public final String getSymbol() {
		return this.symbol;
	}

	@Override
	public String toString() {
		return this.symbol;
	}

}
