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

package origami.asm;

import org.objectweb.asm.Label;

import origami.code.OCode;
import origami.trait.OStackable;

class OBlock implements OStackable<OBlock> {
	OBlock onstack = null;

	OBlock() {
	}

	@Override
	public OBlock push(OBlock onstack) {
		this.onstack = onstack;
		return onstack;
	}

	@Override
	public OBlock pop() {
		return this.onstack;
	}

}

class OBreakBlock extends OBlock {
	final String name;
	final Label startLabel;
	final Label endLabel;
	OBlock onstack = null;

	OBreakBlock(OGeneratorAdapter mBuilder, String name) {
		this.name = name;
		this.startLabel = mBuilder.newLabel();
		this.endLabel = mBuilder.newLabel();
	}

	public boolean matchLabel(String label) {
		if (label == null) {
			return true;
		}
		return (name != null && name.equals(label));
	}

}

class OContinueBlock extends OBreakBlock {

	OContinueBlock(OGeneratorAdapter mBuilder, String name) {
		super(mBuilder, name);
	}

}

class OAspectBlock extends OBlock {
	OCode code;

	OAspectBlock(OCode code) {
		this.code = code;
	}

	public void weave(OAsm asm) {
		if (code != null) {
			code.generate(asm);
		}
	}

}

class OFinallyBlock extends OAspectBlock {

	OFinallyBlock(OCode code) {
		super(code);
	}

}

class OClassFieldInitBlock extends OAspectBlock {

	OClassFieldInitBlock(OCode code) {
		super(code);
	}

}
