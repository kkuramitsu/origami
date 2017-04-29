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

package blue.origami.rule.unit;

import blue.origami.ffi.Immutable;
import blue.origami.ffi.OAlias;
import blue.origami.ffi.OCast;
import blue.origami.ffi.OrigamiObject;
import blue.origami.util.StringCombinator;

public abstract class OUnit<This extends OUnit<This>> implements OrigamiObject, Immutable, StringCombinator {
	private final double value;

	protected OUnit(double value) {
		this.value = value;
	}

	protected OUnit() {
		this(0.0);
	}

	@OCast(cost = OCast.LESSCONV)
	public int intValue() {
		return (int) Math.round(value);
	}

	@OCast(cost = OCast.LESSCONV)
	public long longValue() {
		return Math.round(value);
	}

	@OCast(cost = OCast.LESSCONV)
	public float floatValue() {
		return (float) value;
	}

	@OCast(cost = OCast.LESSCONV)
	public double doubleValue() {
		return value;
	}

	public abstract String unit();

	public abstract This newValue(double d);

	@OAlias(name = "-")
	public This _minus() {
		return newValue(-this.value);
	}

	@OAlias(name = "+")
	public This _add(This x) {
		return newValue(this.doubleValue() + x.doubleValue());
	}

	@OAlias(name = "*")
	public This _mul(double x) {
		return newValue(this.doubleValue() * x);
	}

	@OAlias(name = "*")
	public static <This extends OUnit<This>> This _mul(double x, This y) {
		return y.newValue(x * y.doubleValue());
	}

	@OAlias(name = "/")
	public double _div(This y) {
		return this.doubleValue() / y.doubleValue();
	}

	@OAlias(name = "/")
	public This _div(double y) {
		return newValue(this.doubleValue() / y);
	}

	@OAlias(name = "%")
	public This _mod(double x) {
		return newValue(this.doubleValue() % x);
	}

	@OAlias(name = "<")
	public boolean _lt(This u) {
		double x = this.doubleValue();
		double y = u.doubleValue();
		return x < y;
	}

	@OAlias(name = "<=")
	public boolean _lte(This u) {
		double x = this.doubleValue();
		double y = u.doubleValue();
		return x <= y;
	}

	@OAlias(name = ">")
	public boolean _gt(This u) {
		double x = this.doubleValue();
		double y = u.doubleValue();
		return x > y;
	}

	@OAlias(name = ">=")
	public boolean _gte(This u) {
		double x = this.doubleValue();
		double y = u.doubleValue();
		return x >= y;
	}

	@OAlias(name = "==")
	public boolean _eq(This u) {
		double x = this.doubleValue();
		double y = u.doubleValue();
		double d = x - y;
		if (Math.abs(d) < 0.0005) {
			return true;
		}
		return false;
	}

	@OAlias(name = "!=")
	public boolean _ne(This u) {
		double x = this.doubleValue();
		double y = u.doubleValue();
		double d = x - y;
		if (Math.abs(d) < 0.0005) {
			return false;
		}
		return true;
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append(this.value);
		sb.append("[");
		sb.append(this.unit());
		sb.append("]");
	}

	@Override
	public String toString() {
		return StringCombinator.stringfy(this);
	}

}
