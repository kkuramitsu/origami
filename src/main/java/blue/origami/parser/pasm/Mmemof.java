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

package blue.origami.parser.pasm;

import blue.origami.parser.ParserGrammar.MemoPoint;

public final class Mmemof extends PAsmInst {
	public final int memoPoint;

	public Mmemof(MemoPoint m) {
		super(null);
		this.memoPoint = m.id;
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		storeMemo(px, this.memoPoint, px.pos, false);
		return raiseFail(px);
	}

}