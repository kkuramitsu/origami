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

public final class Idfa extends PAsmInst {
	public final byte[] jumpIndex;
	public final PAsmInst[] jumpTable;

	public Idfa(byte[] jumpIndex, PAsmInst[] jumpTable) {
		super(null);
		this.jumpIndex = jumpIndex;
		this.jumpTable = jumpTable;
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		return this.jumpTable[this.jumpIndex[nextbyte(px)] & 0xff];
	}

	@Override
	public PAsmInst[] branch() {
		return this.jumpTable;
	}

}