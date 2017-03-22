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

package origami.nez.ast;

import java.util.Locale;
import java.util.ResourceBundle;

public interface LocaleFormat {

	public String error();
	public String warning();
	public String notice();

	public default String stringfy(String name) {
		String path = this.getClass().getName();
		try {
			return ResourceBundle.getBundle(path, Locale.getDefault()).getString(name);
		} catch (java.util.MissingResourceException ex) {
		}
		try {
			return ResourceBundle.getBundle(path, Locale.ENGLISH).getString(name);
		} catch (java.util.MissingResourceException ex) {
			return name.replaceAll("__", ": ").replaceAll("_", " ").replace("S", "%s").replace("YY", "$");
		}
	}

	public static LocaleFormat wrap(String fmt) {
		class SimpleFormat implements LocaleFormat {

			private final String fmt;
			SimpleFormat(String fmt) {
				this.fmt = fmt;
			}

			@Override
			public String error() {
				return "error";
			}

			@Override
			public String warning() {
				return "warning";
			}

			@Override
			public String notice() {
				return "notice";
			}

			public String toString() {
				return fmt;
			}

		}
		return new SimpleFormat(fmt);
	}

}