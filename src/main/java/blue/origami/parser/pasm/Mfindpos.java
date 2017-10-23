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

public final class Mfindpos extends PAsmInst {
	public final int memoPoint;

	public Mfindpos(MemoPoint m, PAsmInst next, PAsmInst ret) {
		super(next);
		this.memoPoint = m.id;
		// this.ret = ret;
		assert (ret instanceof Iret);
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		switch (lookupMemo1(px, this.memoPoint)) {
		case PAsmAPI.NotFound:
			return this.next;
		case PAsmAPI.SuccFound:
			// return this.ret;
			return popRet(px);
		default:
			return raiseFail(px);
		}
	}
}