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

import origami.code.OBreakCode;
import origami.code.OClassInitCode;
import origami.code.OCode;
import origami.code.OContinueCode;
import origami.code.OReturnCode;

public interface OBlock {

	public default boolean matchLabel(String label) {
		return false;
	}

	public default OCode getBeforeCode(OCode code) {
		return null;
	}

	public default OCode getAfterCode(OCode code) {
		return null;
	}

}

class OFinallyBlock implements OBlock {
	final OCode weaving;

	OFinallyBlock(OCode weaving) {
		this.weaving = weaving;
	}

	@Override
	public OCode getBeforeCode(OCode code) {
		if (code instanceof OContinueCode) {
			return this.weaving;
		}
		if (code instanceof OBreakCode) {
			return this.weaving;
		}
		if (code instanceof OReturnCode) {
			return this.weaving;
		}
		return null;
	}

}

class OBreakBlock implements OBlock {
	final String name;
	final Label startLabel;
	final Label endLabel;

	OBreakBlock(OGeneratorAdapter mBuilder, String name) {
		this.name = name;
		this.startLabel = mBuilder.newLabel();
		this.endLabel = mBuilder.newLabel();
	}

	@Override
	public boolean matchLabel(String label) {
		if (label == null) {
			return true;
		}
		return (this.name != null && this.name.equals(label));
	}

}

class OBreakContinueBlock extends OBreakBlock {

	final OCode weaving;

	OBreakContinueBlock(OGeneratorAdapter mBuilder, String name, OCode weaving) {
		super(mBuilder, name);
		this.weaving = weaving;
	}

	@Override
	public OCode getBeforeCode(OCode code) {
		if (code instanceof OContinueCode) {
			return this.weaving;
		}
		return null;
	}

}

class OClassFieldInitBlock implements OBlock {

	private final OCode weaveCode;

	OClassFieldInitBlock(OCode code) {
		this.weaveCode = code;
	}

	@Override
	public OCode getAfterCode(OCode code) {
		if (code instanceof OClassInitCode) {
			return this.weaveCode;
		}
		return null;
	}

}
