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

package blue.origami.ffi;

import static java.lang.annotation.ElementType.METHOD;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(value = { METHOD })
public @interface OCast {
	public final static int BXSAME = 0; /* Bi-directianlly same */
	public final static int SAME = 1; /* long => int */
	public final static int UPCAST = 1; /* No error */
	public final static int LESSSAME = 8; /*
											 * information loss int => long, no
											 * error
											 */
	public final static int CONV = 64; /* int => String */
	public final static int ANYCAST = 512; /*
											 * Object => int, possible
											 * ClassCastException
											 */
	public final static int DOWNCAST = 4096; /* possible runtime errors */
	public final static int LESSCONV = 4096; /*
												 * String => int, possible other
												 * RuntimeExceptions
												 */
	public final static int STUPID = 4097; /* always runtime errors */
	public final static int UNFOUND = 4096 * 8; /* always runtime errors */

	int cost() default CONV;

}
