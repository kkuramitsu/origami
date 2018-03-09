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

package blue.origami.parser.peg;

import java.util.ArrayList;
import java.util.HashMap;

public enum GrammarFlag {
	Yes, No, Unsure;

	public final static void checkFlag(ArrayList<Production> prodList, String[] flags) {
		for (String flag : flags) {
			checkFlag(prodList, flag);
		}
	}

	static void checkFlag(ArrayList<Production> prodList, String flag) {
		int size = prodList.size();
		int prev = 0;
		while (true) {
			int found = 0;
			for (Production p : prodList) {
				GrammarFlag f = getFlag(p.getGrammar(), p.getLocalName(), flag);
				if (f != Yes && f != No) {
					f = checkFlag(p.getGrammar(), p.getLocalName(), p.getExpression(), flag);
				}
				if (f == Yes || f == No) {
					found++;
				}
			}
			// ODebug.trace2("flag=%s %d %d %d", flag, prev, found, size);
			if (found == size || prev == found) {
				break;
			}
			prev = found;
		}
		for (Production p : prodList) {
			GrammarFlag f = getFlag(p.getGrammar(), p.getLocalName(), flag);
			if (f != Yes && f != No) {
				setFlag(p.getGrammar(), p.getLocalName(), flag, No);
			}
		}
	}

	static GrammarFlag checkFlag(Grammar g, String name, Expression e, String flag) {
		if (e instanceof PIf) {
			PIf p = (PIf) e;
			if (p.flagName().equals(flag)) {
				setFlag(g, name, flag, Yes);
				return Yes;
			}
		}
		if (e instanceof PNonTerminal) {
			PNonTerminal p = (PNonTerminal) e;
			GrammarFlag f = getFlag(p.getGrammar(), p.getLocalName(), flag);
			if (f == Yes || f == No) {
				setFlag(g, name, flag, f);
				return f;
			}
			return Unsure;
		}
		boolean hasUnsure = false;
		for (Expression ei : e) {
			GrammarFlag f = checkFlag(g, name, ei, flag);
			if (f == Yes) {
				return Yes;
			}
			if (f != No) {
				hasUnsure = true;
			}
		}
		return hasUnsure ? Unsure : No;
	}

	@SuppressWarnings("serial")
	private static class FlagMap extends HashMap<String, GrammarFlag> {

	}

	static GrammarFlag getFlag(Grammar g, String name, String flag) {
		FlagMap map = g.getProperty(name, FlagMap.class);
		if (map == null) {
			map = new FlagMap();
			g.setProperty(name, map);
		}
		return map.get(flag);
	}

	static void setFlag(Grammar g, String name, String flag, GrammarFlag y) {
		FlagMap map = g.getProperty(name, FlagMap.class);
		if (map == null) {
			map = new FlagMap();
			g.setProperty(name, map);
		}
		map.put(flag, y);
	}

	public final static boolean hasFlag(Production p, String flag) {
		GrammarFlag f = getFlag(p.getGrammar(), p.getLocalName(), flag);
		assert f != null;
		return f != No;
	}

}
